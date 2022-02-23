package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import io.micrometer.core.instrument.MeterRegistry
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import no.nav.amt.arena.acl.domain.Creation
import no.nav.amt.arena.acl.domain.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.amt.AmtTiltak
import no.nav.amt.arena.acl.domain.amt.AmtWrapper
import no.nav.amt.arena.acl.domain.arena.ArenaTiltakGjennomforing
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.TiltakRepository
import no.nav.amt.arena.acl.utils.asLocalDate
import no.nav.amt.arena.acl.utils.asLocalDateTime
import no.nav.amt.arena.acl.utils.asTime
import no.nav.amt.arena.acl.utils.withTime
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.util.DigestUtils
import java.time.LocalDateTime
import java.util.*

@Component
open class TiltakGjennomforingProcessor(
	repository: ArenaDataRepository,
	private val idTranslationRepository: ArenaDataIdTranslationRepository,
	private val tiltakRepository: TiltakRepository,
	private val ordsClient: ArenaOrdsProxyClient,
	meterRegistry: MeterRegistry,
	kafkaProducer: KafkaProducerClientImpl<String, String>
) : AbstractArenaProcessor<ArenaTiltakGjennomforing>(
	repository = repository,
	meterRegistry = meterRegistry,
	clazz = ArenaTiltakGjennomforing::class.java,
	kafkaProducer = kafkaProducer
) {

	private val log = LoggerFactory.getLogger(javaClass)
	private val statusConverter = GjennomforingStatusConverter()

	override fun handleEntry(data: ArenaData) {
		val arenaGjennomforing : ArenaTiltakGjennomforing = data.getMainObject()

		val arenaId = arenaGjennomforing.TILTAKGJENNOMFORING_ID

		val gjennomforingId = hentEllerOpprettNyGjennomforingId(data.arenaTableName, data.arenaId)

		if (isUnsupportedTiltakType(arenaGjennomforing)) {
			log.info("Gjennomføring med arenaId=$arenaId er ikke støttet og sendes ikke videre")
			insertTranslation(data, gjennomforingId, true, gjennomforingId::digest)
			repository.upsert(data.markAsIgnored("Ikke et støttet tiltak"))
			return
		}

		if (ugyldigGjennomforing(arenaGjennomforing)) {
			// Ikke sett til ignored i translation fordi da må man unignore når man får neste melding som kan være gyldig
			log.info("Hopper over upsert av tiltakgjennomforing som mangler data. arenaId=$arenaId")
			repository.upsert(data.markAsIgnored())
			return
		}

		val tiltakskode = arenaGjennomforing.TILTAKSKODE
		val tiltak = tiltakRepository.getByKode(tiltakskode)

		if (tiltak == null) {
			log.info("Tiltak $tiltakskode er ikke håndtert, kan derfor ikke håndtere gjennomføring med arenaId=$arenaId enda")
			repository.upsert(data.retry("Tiltaket ($tiltakskode) er ikke håndtert"))
			return
		}

		val virksomhetsnummer = ordsClient.hentVirksomhetsnummer(arenaGjennomforing.ARBGIV_ID_ARRANGOR.toString())

		val amtGjennomforing = arenaGjennomforing.toAmtGjennomforing(
			amtTiltak = tiltak,
			amtGjennomforingId = gjennomforingId,
			virksomhetsnummer = virksomhetsnummer
		)

		val translation = insertTranslation(data, gjennomforingId, false, amtGjennomforing::digest)

		if (translation.first == Creation.EXISTED) {
			val digest = amtGjennomforing.digest()

			if (translation.second.currentHash == digest) {
				log.info("Gjennomføring med kode $gjennomforingId sendes ikke videre fordi det allerede er sendt (Samme hash)")
				repository.upsert(data.markAsIgnored("Tiltaket er allerede sendt (samme hash)."))
				return
			}
		}

		val amtData = AmtWrapper(
			type = "GJENNOMFORING",
			operation = data.operation,
			payload = arenaGjennomforing.toAmtGjennomforing(tiltak, gjennomforingId, virksomhetsnummer)
		)

		send(amtGjennomforing.id, objectMapper.writeValueAsString(amtData))
		repository.upsert(data.markAsHandled())
		log.info("Melding for gjennomføring id=$gjennomforingId arenaId=$arenaId transactionId=${amtData.transactionId} op=${amtData.operation} er sendt")
	}

	private fun hentEllerOpprettNyGjennomforingId(arenaTableName: String, arenaId: String): UUID {
		val gjennomforingId = idTranslationRepository.getAmtId(arenaTableName, arenaId)

		if (gjennomforingId == null) {
			val nyGjennomforingId = UUID.randomUUID()
			log.info("Opprettet ny id for gjennomføring, id=$nyGjennomforingId arenaId=$arenaId")
			return nyGjennomforingId
		}

		return gjennomforingId
	}

	private fun isUnsupportedTiltakType(gjennomforing: ArenaTiltakGjennomforing): Boolean {
		return !isSupportedTiltak(gjennomforing.TILTAKSKODE)
	}

	private fun insertTranslation(
		data: ArenaData,
		gjennomforingId: UUID,
		ignored: Boolean,
		digestor: () -> String
	): Pair<Creation, ArenaDataIdTranslation> {
		val exists = idTranslationRepository.get(data.arenaTableName, data.arenaId)

		if (exists != null) {
			return Pair(Creation.EXISTED, exists)
		} else {
			idTranslationRepository.insert(
				ArenaDataIdTranslation(
					amtId = gjennomforingId,
					arenaTableName = data.arenaTableName,
					arenaId = data.arenaId,
					ignored = ignored,
					digestor()
				)
			)

			log.info("Opprettet translation for gjennomføring id=${gjennomforingId} arenaId=${data.arenaId}")

			val created = idTranslationRepository.get(data.arenaTableName, data.arenaId)
				?: throw IllegalStateException("Translation for id=$gjennomforingId arenaId=${data.arenaId} in table ${data.arenaTableName} should exist")

			return Pair(Creation.CREATED, created)
		}
	}

	private fun ArenaTiltakGjennomforing.toAmtGjennomforing(
		amtTiltak: AmtTiltak,
		amtGjennomforingId: UUID,
		virksomhetsnummer: String
	): AmtGjennomforing {
		val registrertDato = utledRegDato(this)

		return AmtGjennomforing(
			id = amtGjennomforingId,
			tiltak = amtTiltak,
			virksomhetsnummer = virksomhetsnummer,
			navn = LOKALTNAVN ?: throw DataIntegrityViolationException("Forventet at LOKALTNAVN ikke er null"),
			startDato = DATO_FRA?.asLocalDate(),
			sluttDato = DATO_TIL?.asLocalDate(),
			registrertDato = registrertDato,
			fremmoteDato = DATO_FREMMOTE?.asLocalDate() withTime KLOKKETID_FREMMOTE.asTime(),
			status = statusConverter.convert(TILTAKSTATUSKODE ?: throw DataIntegrityViolationException("Forventet at TILTAKSTATUSKODE ikke er null"))
		)
	}

	private fun utledRegDato(arenaGjennomforing: ArenaTiltakGjennomforing): LocalDateTime {
		val registrertDato = arenaGjennomforing.REG_DATO

		if (registrertDato != null) {
			return registrertDato.asLocalDateTime()
		}

		val modifisertDato = arenaGjennomforing.MOD_DATO

		if (modifisertDato != null) {
			log.warn("REG_DATO mangler for tiltakgjennomføring arenaId=${arenaGjennomforing.TILTAKGJENNOMFORING_ID}, bruker MOD_DATO istedenfor")
			return modifisertDato.asLocalDateTime()
		}

		log.warn("MOD_DATO mangler for tiltakgjennomføring arenaId=${arenaGjennomforing.TILTAKGJENNOMFORING_ID}, bruker nåtid istedenfor")

		return LocalDateTime.now()
	}

	private fun ugyldigGjennomforing(data: ArenaTiltakGjennomforing) =
		data.ARBGIV_ID_ARRANGOR == null || data.LOKALTNAVN == null
}

private fun UUID.digest() = DigestUtils.md5DigestAsHex(this.toString().toByteArray())
