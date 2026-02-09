package no.nav.pleiepengerbarn.uttak.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.postgresql.PostgreSQLContainer
import javax.sql.DataSource

@Configuration
class DbTestConfig {

    @Bean
    fun testDataSource(): DataSource {
        postgres.start()
        return createLocalDatasource(postgres.jdbcUrl, postgres.username, postgres.password)
    }

    @Bean
    fun applicationDataConnection(): JdbcTemplate {
        return JdbcTemplate(testDataSource())
    }

    private fun createLocalDatasource(url: String, username: String, password: String): DataSource {
        val config = createConfig(url)
        config.username = username
        config.password = password // NOSONAR false positive
        return HikariDataSource(config)
    }

    private fun createConfig(url: String): HikariConfig {
        val config = HikariConfig()
        config.jdbcUrl = url
        config.minimumIdle = 0
        config.maximumPoolSize = 2
        config.connectionTestQuery = "select 1"
        config.driverClassName = "org.postgresql.Driver"
        return config
    }

    companion object {
        // Lazy because we only want it to be initialized when accessed
        private val postgres: PostgreSQLContainer by lazy {
            PostgreSQLContainer("postgres:12.11")
                    .withDatabaseName("databasename")
                    .withUsername("pleiepengerbarn_unit")
                    .withPassword("pleiepengerbarn_unit")
        }
    }
}