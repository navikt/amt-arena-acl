package no.nav.amt.arena.acl.kafka

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.env")
data class KafkaTopicProperties(
	var arenaTiltakGjennomforingTopic: String = "",
	var arenaTiltakDeltakerTopic: String = "",
	var arenaHistTiltakDeltakerTopic: String = "",
	var amtTopic: String = "",
	var amtEnkeltplassDeltakerTopic: String = "",
)
