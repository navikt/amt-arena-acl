package no.nav.amt.arena.acl.processors.converters

import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.erAvsluttende
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker

 object ArenaDeltakerAarsakConverter {
	fun convert(
		arenaStatus: TiltakDeltaker.Status,
		status: AmtDeltaker.Status,
		statusAarsakKode: TiltakDeltaker.StatusAarsak?,
		gjennomforingStatus: GjennomforingStatus
	): AmtDeltaker.StatusAarsak? {
		val aarsakFraAarsak = utledMedArenaAarsak(statusAarsakKode)
		val aarsakFraStatus = utledMedArenaStatus(arenaStatus)
		val aarsakFraGjennomforing = utledMedGjennomforing(gjennomforingStatus, arenaStatus)

		if (!status.erAvsluttende()) return null

		if (aarsakFraGjennomforing != null) return aarsakFraGjennomforing
		if (aarsakFraAarsak == null) return aarsakFraStatus
		else return aarsakFraAarsak
	}

	private fun utledMedGjennomforing(gjennomforingStatus: GjennomforingStatus, arenaStatus: TiltakDeltaker.Status): AmtDeltaker.StatusAarsak? {
		if(gjennomforingStatus != GjennomforingStatus.AVSLUTTET) return null
		return when (arenaStatus) {
			TiltakDeltaker.Status.AKTUELL -> AmtDeltaker.StatusAarsak.FIKK_IKKE_PLASS
			TiltakDeltaker.Status.INFOMOETE -> AmtDeltaker.StatusAarsak.FIKK_IKKE_PLASS
			TiltakDeltaker.Status.VENTELISTE -> AmtDeltaker.StatusAarsak.FIKK_IKKE_PLASS
			else -> null
		}
	}

	private fun utledMedArenaStatus(arenaStatus: TiltakDeltaker.Status): AmtDeltaker.StatusAarsak {
		return when (arenaStatus) {
			TiltakDeltaker.Status.IKKEM -> AmtDeltaker.StatusAarsak.IKKE_MOTT
			TiltakDeltaker.Status.GJENN_AVB -> AmtDeltaker.StatusAarsak.AVLYST_KONTRAKT
			TiltakDeltaker.Status.GJENN_AVL -> AmtDeltaker.StatusAarsak.AVLYST_KONTRAKT
			TiltakDeltaker.Status.AVSLAG -> AmtDeltaker.StatusAarsak.FIKK_IKKE_PLASS
			else -> AmtDeltaker.StatusAarsak.ANNET
		}
	}

	private fun utledMedArenaAarsak(statusAarsakKode: TiltakDeltaker.StatusAarsak?): AmtDeltaker.StatusAarsak? {
		return when (statusAarsakKode) {
			TiltakDeltaker.StatusAarsak.SYK -> AmtDeltaker.StatusAarsak.SYK
			TiltakDeltaker.StatusAarsak.BEGA -> AmtDeltaker.StatusAarsak.FATT_JOBB
			TiltakDeltaker.StatusAarsak.FTOAT -> AmtDeltaker.StatusAarsak.TRENGER_ANNEN_STOTTE
			else -> null
		}
	}
}
