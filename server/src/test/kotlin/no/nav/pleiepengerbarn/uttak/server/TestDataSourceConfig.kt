package no.nav.pleiepengerbarn.uttak.server

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class TestDataSourceConfig {

    @Bean
    fun getDataSource(): DataSource {
        val embeddedPostgres = EmbeddedPostgres.builder().start()
        val jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
        val hikariConfig = createHikariConfig(jdbcUrl)
        return HikariDataSource(hikariConfig)
    }

    private fun createHikariConfig(jdbcUrl: String) =
            HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                maximumPoolSize = 3
                minimumIdle = 1
                idleTimeout = 10001
                connectionTimeout = 1000
                maxLifetime = 30001
            }

}