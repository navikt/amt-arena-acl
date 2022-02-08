package no.nav.amt.arena.acl.integration

import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.integration.kafka.SingletonKafkaProvider
import no.nav.amt.arena.acl.integration.utils.IntegrationTestTiltakUtils
import no.nav.amt.arena.acl.integration.utils.TiltakIntegrationTestResult
import no.nav.amt.arena.acl.kafka.KafkaProperties
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.TiltakRepository
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import javax.sql.DataSource

@RunWith(SpringRunner::class)
@SpringBootTest
@DirtiesContext
@Import(IntegrationTestConfiguration::class)
@ActiveProfiles("integration")
@TestConfiguration("application-integration.properties")
abstract class IntegrationTestBase {

	@Autowired
	lateinit var kafkaProducerClientImpl: KafkaProducerClientImpl<String, String>

	@Autowired
	lateinit var arenaDataRepository: ArenaDataRepository

	@Autowired
	lateinit var tiltakRepository: TiltakRepository

	companion object {
		private var position = 0
	}

	fun nyttTiltak(kode: String, name: String): TiltakIntegrationTestResult {
		return IntegrationTestTiltakUtils(
			kafkaProducerClientImpl,
			arenaDataRepository,
			tiltakRepository
		)
			.nyttTiltak(position++, kode, name)
	}

	fun oppdaterTiltak(kode: String, gammeltNavn: String, nyttNavn: String): TiltakIntegrationTestResult {
		return IntegrationTestTiltakUtils(
			kafkaProducerClientImpl,
			arenaDataRepository,
			tiltakRepository
		)
			.oppdaterTiltak(position++, kode, gammeltNavn, nyttNavn)
	}

	fun slettTiltak(kode: String, navn: String): TiltakIntegrationTestResult {
		return IntegrationTestTiltakUtils(
			kafkaProducerClientImpl,
			arenaDataRepository,
			tiltakRepository
		)
			.slettTiltak(position++, kode, navn)
	}
}

@TestConfiguration
open class IntegrationTestConfiguration {

	@Bean
	open fun dataSource(): DataSource {
		return SingletonPostgresContainer.getDataSource()
	}

	@Bean
	open fun kafkaProperties(): KafkaProperties {
		return SingletonKafkaProvider.getKafkaProperties()
	}
}
