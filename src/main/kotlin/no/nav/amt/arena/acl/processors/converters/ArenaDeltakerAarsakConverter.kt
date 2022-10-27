package no.nav.amt.arena.acl.processors.converters

import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.kafka.amt.erAvsluttende
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker

data class ArenaDeltakerAarsakConverter (
	private val arenaStatus: TiltakDeltaker.Status,
	private val status: AmtDeltaker.Status,
	private val statusAarsakKode: TiltakDeltaker.StatusAarsak?,
	private val gjennomforingStatus: AmtGjennomforing.Status
){
	fun convert(): AmtDeltaker.StatusAarsak? {
		val aarsakFraAarsak = utledMedArenaAarsak()
		val aarsakFraStatus = utledMedArenaStatus()
		val aarsakFraGjennomforing = utledMedGjennomforing()

		if (!status.erAvsluttende()) return null

		if (aarsakFraGjennomforing != null) return aarsakFraGjennomforing
		if (aarsakFraAarsak == null) return aarsakFraStatus
		else return aarsakFraAarsak
	}

	private fun utledMedGjennomforing(): AmtDeltaker.StatusAarsak? {
		if(gjennomforingStatus != AmtGjennomforing.Status.AVSLUTTET) return null
		return when (arenaStatus) {
			TiltakDeltaker.Status.AKTUELL -> AmtDeltaker.StatusAarsak.FIKK_IKKE_PLASS
			TiltakDeltaker.Status.INFOMOETE -> AmtDeltaker.StatusAarsak.FIKK_IKKE_PLASS
			TiltakDeltaker.Status.VENTELISTE -> AmtDeltaker.StatusAarsak.FIKK_IKKE_PLASS
			else -> null
		}
	}

	private fun utledMedArenaStatus(): AmtDeltaker.StatusAarsak {
		return when (arenaStatus) {
			TiltakDeltaker.Status.IKKEM -> AmtDeltaker.StatusAarsak.IKKE_MOTT
			TiltakDeltaker.Status.GJENN_AVB -> AmtDeltaker.StatusAarsak.AVLYST_KONTRAKT
			TiltakDeltaker.Status.GJENN_AVL -> AmtDeltaker.StatusAarsak.AVLYST_KONTRAKT
			TiltakDeltaker.Status.AVSLAG -> AmtDeltaker.StatusAarsak.FIKK_IKKE_PLASS
			else -> AmtDeltaker.StatusAarsak.ANNET
		}
	}

	private fun utledMedArenaAarsak(): AmtDeltaker.StatusAarsak? {
		return when (statusAarsakKode) {
			TiltakDeltaker.StatusAarsak.SYK -> AmtDeltaker.StatusAarsak.SYK
			TiltakDeltaker.StatusAarsak.BEGA -> AmtDeltaker.StatusAarsak.FATT_JOBB
			TiltakDeltaker.StatusAarsak.FTOAT -> AmtDeltaker.StatusAarsak.TRENGER_ANNEN_STOTTE
			else -> null
		}
	}
}
