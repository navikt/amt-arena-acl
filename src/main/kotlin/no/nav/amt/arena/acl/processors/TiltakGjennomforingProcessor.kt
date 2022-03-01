package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import io.micrometer.core.instrument.MeterRegistry
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
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
		val arenaGjennomforing: ArenaTiltakGjennomforing = data.getMainObject()

		val arenaId = arenaGjennomforing.TILTAKGJENNOMFORING_ID
		val gjennomforingId = hentEllerOpprettNyGjennomforingId(data.arenaTableName, data.arenaId)

		if (!valid(data, arenaGjennomforing, gjennomforingId)) {
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

		insertTranslation(data, gjennomforingId, false)


		val amtData = AmtWrapper(
			type = "GJENNOMFORING",
			operation = data.operation,
			payload = arenaGjennomforing.toAmtGjennomforing(tiltak, gjennomforingId, virksomhetsnummer)
		)

		send(amtGjennomforing.id, objectMapper.writeValueAsString(amtData))
		repository.upsert(data.markAsHandled())
		log.info("Melding for gjennomføring id=$gjennomforingId arenaId=$arenaId transactionId=${amtData.transactionId} op=${amtData.operation} er sendt")
	}

	private fun valid(data: ArenaData, arenaGjennomforing: ArenaTiltakGjennomforing, gjennomforingId: UUID): Boolean {
		if (!isSupportedTiltak(arenaGjennomforing.TILTAKSKODE)) {
			log.info("Gjennomføring med arenaId=${arenaGjennomforing.TILTAKGJENNOMFORING_ID} er ikke støttet og sendes ikke videre")
			insertTranslation(data, gjennomforingId, true)
			repository.upsert(data.markAsIgnored("Ikke et støttet tiltak"))
			return false
		}

		if (arenaGjennomforing.ARBGIV_ID_ARRANGOR == null) {
			repository.upsert(data.markAsIncomplete("ARBGIV_ID_ARRANGOR er null"))
			return false
		}

		if (arenaGjennomforing.LOKALTNAVN == null) {
			repository.upsert(data.markAsIncomplete("LOKALTNAVN er null"))
			return false
		}

		return true
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

	private fun insertTranslation(
		data: ArenaData,
		gjennomforingId: UUID,
		ignored: Boolean,
	) {
		idTranslationRepository.insert(
			ArenaDataIdTranslation(
				amtId = gjennomforingId,
				arenaTableName = data.arenaTableName,
				arenaId = data.arenaId,
				ignored = ignored,
			)
		)
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
			status = statusConverter.convert(
				TILTAKSTATUSKODE ?: throw DataIntegrityViolationException("Forventet at TILTAKSTATUSKODE ikke er null")
			)
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
}
