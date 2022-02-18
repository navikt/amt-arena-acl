package no.nav.amt.arena.acl.integration.commands

import no.nav.amt.arena.acl.utils.ObjectMapperFactory
import java.time.format.DateTimeFormatter

abstract class Command {

	companion object {

		const val GENERIC_STRING = "STRING_NOT_SET"
		const val GENERIC_INT = Int.MIN_VALUE
		const val GENERIC_LONG = Long.MIN_VALUE
		const val GENERIC_FLOAT = Float.MIN_VALUE

		val opTsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
		val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

		val objectMapper = ObjectMapperFactory.get()
	}

}
