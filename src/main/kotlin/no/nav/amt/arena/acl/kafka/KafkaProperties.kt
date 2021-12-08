package no.nav.amt.arena.acl.kafka

import java.util.*

interface KafkaProperties {

    fun consumer(): Properties

    fun producer(): Properties

}
