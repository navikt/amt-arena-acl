package no.nav.amt.arena.acl.application.kafka

import java.util.*

interface KafkaProperties {

    fun consumer(): Properties

    fun producer(): Properties

}
