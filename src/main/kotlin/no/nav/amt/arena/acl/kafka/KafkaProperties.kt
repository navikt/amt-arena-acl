package no.nav.amt.arena.acl.kafka

import java.util.Properties

interface KafkaProperties {
	fun consumer(): Properties

	fun tempConsumer(): Properties

	fun producer(): Properties
}
