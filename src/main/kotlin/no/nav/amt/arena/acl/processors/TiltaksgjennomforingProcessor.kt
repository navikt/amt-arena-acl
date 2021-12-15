package no.nav.amt.arena.acl.processors

//@Component
//open class TiltaksgjennomforingProcessor(
//	private val repository: ArenaDataRepository,
//	private val idTranslationRepository: ArenaDataIdTranslationRepository,
//	private val kafkaProducer: KafkaProducerClientImpl<String, String>,
//	private val ordsProxyClient: ArenaOrdsProxyClient
//) : AbstractArenaProcessor() {
//
//	@Value("\${app.env.amtTopic}")
//	lateinit var topic: String
//
//	private val logger = LoggerFactory.getLogger(javaClass)
//
//	override fun handle(data: ArenaData) {
//		val arenaTiltakGjennomforing = getMainObject(data)
//
//		val tiltakTranslation = idTranslationRepository.get(TILTAK_TABLE_NAME, arenaTiltakGjennomforing.TILTAKSKODE)
//			?: throw DependencyNotIngestedException(
//				"Tiltak med Arena ID ${arenaTiltakGjennomforing.TILTAKSKODE} må eksistere for å håndtere " +
//					"tiltakGjennomføring med Arena ID ${arenaTiltakGjennomforing.TILTAKGJENNOMFORING_ID}"
//			)
//
//
//		val translation = idTranslationRepository.get(data.arenaTableName, data.arenaId)?.let {
//			if (it.ignored) {
//				logger.debug("Tiltaksgjennomføring med id ${arenaTiltakGjennomforing.TILTAKGJENNOMFORING_ID} er ikke støttet og sendes ikke videre")
//				repository.upsert(data.markAsIgnored("Ikke et støttet tiltaksgjennomforing."))
//				return
//			}
//
//			val digest = getDigest(arenaTiltakGjennomforing.toAmtTiltakGjennomforing(it.amtId))
//
//			it
//		}
//			?: generateTranslation(data, arenaTiltakGjennomforing)
//
//		TODO("Not yet implemented")
//	}
//
//	private fun generateTranslation(data: ArenaData, tiltak: ArenaTiltakGjennomforing): ArenaDataIdTranslation {
//		TODO("Not yet implemented")
//	}
//
//	private fun getDigest(tiltak: AmtGjennomforing): String {
//		return DigestUtils.md5DigestAsHex(objectMapper.writeValueAsString(tiltak).toByteArray())
//	}
//
//	private fun getMainObject(data: ArenaData): ArenaTiltakGjennomforing {
//		return when (data.operation) {
//			AmtOperation.CREATED -> jsonObject(data.after, ArenaTiltakGjennomforing::class.java)
//			AmtOperation.MODIFIED -> jsonObject(data.after, ArenaTiltakGjennomforing::class.java)
//			AmtOperation.DELETED -> jsonObject(data.before, ArenaTiltakGjennomforing::class.java)
//		}
//			?: throw IllegalArgumentException("Expected ${data.arenaTableName} id ${data.arenaId} to have before or after correctly set.")
//	}
//
//	private fun String.toAmtTiltakGjennomforing(tiltakGjennomforingId: UUID): AmtGjennomforing {
//		return jsonObject(this, ArenaTiltakGjennomforing::class.java)?.toAmtTiltakGjennomforing(tiltakGjennomforingId)
//			?: throw IllegalArgumentException("Expected String not to be null")
//	}
//
//	private fun ArenaTiltakGjennomforing.toAmtTiltakGjennomforing(
//		tiltakGjennomforingId: UUID,
//		tiltakId: UUID
//	): AmtGjennomforing {
//		return AmtGjennomforing(
//			id = tiltakGjennomforingId,
//			kode = TILTAKSKODE,
//			navn = TILTAKSNAVN
//		)
//	}
//
//	private fun getTiltakTranslation(arenaId: String): ArenaDataIdTranslation {
//		val translation = idTranslationRepository.get(TILTAK_TABLE_NAME, arenaId)
//			?: throw DependencyNotIngestedException("Tiltak med Arena ID ${arenaId} må eksistere for å håndtere")
//	}
//
//
//}
