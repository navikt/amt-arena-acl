package no.nav.amt.arena.acl.kafka

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.env")
data class KafkaTopicProperties(
	var arenaTiltakTopic: String = "",
	var arenaTiltaksgjennomforingTopic: String = "",
	var arenaTiltakDeltakerTopic: String = "",
)
