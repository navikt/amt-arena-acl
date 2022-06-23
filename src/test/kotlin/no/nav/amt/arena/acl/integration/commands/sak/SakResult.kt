package no.nav.amt.arena.acl.integration.commands.sak

import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.db.ArenaDataIdTranslationDbo
import no.nav.amt.arena.acl.domain.db.ArenaSakDbo
import no.nav.amt.arena.acl.domain.kafka.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import org.junit.jupiter.api.fail

class SakResult (
	val position: String,
	val arenaDataDbo: ArenaDataDbo,
	val translation: ArenaDataIdTranslationDbo?,
	val output: AmtKafkaMessageDto<AmtGjennomforing>?,
	val arenaSakDbo: ArenaSakDbo
) {
	fun arenaData(check: (data: ArenaDataDbo) -> Unit): SakResult {
		check.invoke(arenaDataDbo)
		return this
	}

	fun output(check: (data: AmtKafkaMessageDto<AmtGjennomforing>) -> Unit): SakResult {
		if (output == null) {
			fail("Trying to get output kafka message, but it is null")
		}

		check.invoke(output)
		return this
	}

	fun result(check: (arenaDataDbo: ArenaDataDbo, translation: ArenaDataIdTranslationDbo?, output: AmtKafkaMessageDto<AmtGjennomforing>?, arenaSakDbo: ArenaSakDbo) -> Unit): SakResult {
		check.invoke(arenaDataDbo, translation, output, arenaSakDbo)
		return this
	}
}
