package no.nav.amt.arena.acl.application

import no.nav.amt.arena.acl.application.models.ArenaData
import org.springframework.stereotype.Service

@Service
open class ArenaDataService {

    fun store(data: ArenaData) {
        println(data)
    }

}
