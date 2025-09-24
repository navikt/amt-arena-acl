package no.nav.amt.arena.acl.consumer.converters

import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker

object ArenaDeltakerAarsakConverter {
	fun convert(
		arenaStatus: TiltakDeltaker.Status,
		status: AmtDeltaker.Status,
		statusAarsakKode: TiltakDeltaker.StatusAarsak?,
		erGjennomforingAvsluttet: Boolean
	): AmtDeltaker.StatusAarsak? = if (!status.erAvsluttende()) {
		null
	} else {
		utledIkkeMott(arenaStatus) ?: utledMedGjennomforing(erGjennomforingAvsluttet, arenaStatus)
		?: utledMedArenaAarsak(statusAarsakKode) ?: utledMedArenaStatus(arenaStatus)
	}

	private fun utledIkkeMott(arenaStatus: TiltakDeltaker.Status): AmtDeltaker.StatusAarsak? =
		if (arenaStatus == TiltakDeltaker.Status.IKKEM) {
			AmtDeltaker.StatusAarsak.IKKE_MOTT
		} else {
			null
		}

	private fun utledMedGjennomforing(
		erGjennomforingAvsluttet: Boolean, arenaStatus: TiltakDeltaker.Status
	): AmtDeltaker.StatusAarsak? {
		if (!erGjennomforingAvsluttet) return null

		return when (arenaStatus) {
			TiltakDeltaker.Status.AKTUELL -> AmtDeltaker.StatusAarsak.FIKK_IKKE_PLASS
			TiltakDeltaker.Status.INFOMOETE -> AmtDeltaker.StatusAarsak.FIKK_IKKE_PLASS
			TiltakDeltaker.Status.VENTELISTE -> AmtDeltaker.StatusAarsak.FIKK_IKKE_PLASS
			else -> null
		}
	}

	private fun utledMedArenaAarsak(statusAarsakKode: TiltakDeltaker.StatusAarsak?): AmtDeltaker.StatusAarsak? =
		when (statusAarsakKode) {
			TiltakDeltaker.StatusAarsak.SYK -> AmtDeltaker.StatusAarsak.SYK
			TiltakDeltaker.StatusAarsak.BEGA -> AmtDeltaker.StatusAarsak.FATT_JOBB
			TiltakDeltaker.StatusAarsak.FTOAT -> AmtDeltaker.StatusAarsak.TRENGER_ANNEN_STOTTE
			else -> null
		}

	private fun utledMedArenaStatus(arenaStatus: TiltakDeltaker.Status): AmtDeltaker.StatusAarsak? =
		when (arenaStatus) {
			TiltakDeltaker.Status.FULLF -> null
			TiltakDeltaker.Status.GJENN_AVB -> AmtDeltaker.StatusAarsak.AVLYST_KONTRAKT
			TiltakDeltaker.Status.GJENN_AVL -> AmtDeltaker.StatusAarsak.AVLYST_KONTRAKT
			TiltakDeltaker.Status.AVSLAG -> AmtDeltaker.StatusAarsak.FIKK_IKKE_PLASS
			else -> AmtDeltaker.StatusAarsak.ANNET
		}
}
