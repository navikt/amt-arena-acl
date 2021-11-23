package no.nav.amt.arena.acl.application.kafka

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.env")
data class KafkaTopicProperties(
	var arenaTiltakTopic: String = "",
	var arenaTiltaksgruppeTopic: String = "",
	var arenaTiltaksgjennomforingTopic: String = "",
	var arenaTiltakDeltakerTopic: String = "",
)
