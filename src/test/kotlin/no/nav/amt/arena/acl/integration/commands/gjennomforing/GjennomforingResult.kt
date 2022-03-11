package no.nav.amt.arena.acl.integration.commands.gjennomforing

import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.db.ArenaDataIdTranslationDbo
import no.nav.amt.arena.acl.domain.kafka.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import org.junit.jupiter.api.fail

data class GjennomforingResult(
	val position: String,
	val arenaDataDbo: ArenaDataDbo,
	val translation: ArenaDataIdTranslationDbo?,
	val output: AmtKafkaMessageDto<AmtGjennomforing>?
) {

	fun arenaData(check: (data: ArenaDataDbo) -> Unit): GjennomforingResult {
		check.invoke(arenaDataDbo)
		return this
	}

	fun translation(check: (data: ArenaDataIdTranslationDbo) -> Unit): GjennomforingResult {
		if (translation == null) {
			fail("Trying to get translation, but it is null")
		}

		check.invoke(translation)
		return this
	}

	fun output(check: (data: AmtKafkaMessageDto<AmtGjennomforing>) -> Unit): GjennomforingResult {
		if (output == null) {
			fail("Trying to get output, but it is null")
		}

		check.invoke(output)
		return this
	}

	fun result(check: (arenaDataDbo: ArenaDataDbo, translation: ArenaDataIdTranslationDbo?, output: AmtKafkaMessageDto<AmtGjennomforing>?) -> Unit): GjennomforingResult {
		check.invoke(arenaDataDbo, translation, output)
		return this
	}

}
