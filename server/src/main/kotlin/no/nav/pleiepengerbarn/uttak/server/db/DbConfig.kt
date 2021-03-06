package no.nav.pleiepengerbarn.uttak.server.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import no.nav.vault.jdbc.hikaricp.VaultError
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import java.util.*
import javax.sql.DataSource

@Configuration
class DbConfig {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    @Bean
    @Profile("prodConfig")
    fun getDataSource(): DataSource {
        return createDatasource("defaultDS", DatasourceRole.ADMIN, getEnvironmentClass(), 5)
    }

    @Bean
    @Profile("prodConfig")
    fun applicationDataConnection(): JdbcTemplate {
        return JdbcTemplate(getDataSource())
    }

    fun createDatasource(datasourceName: String, role: DatasourceRole, environmentClass: EnvironmentClass, maxPoolSize: Int): DataSource {
        val rolePrefix = getRolePrefix(datasourceName)
        logger.info("rolePrefix = $rolePrefix")
        logger.info("envClass = $environmentClass")
        return if (EnvironmentClass.LOCALHOST == environmentClass) {
            val config = initConnectionPoolConfig(datasourceName, null, maxPoolSize)
            val password = getProperty("$datasourceName.password")
            createLocalDatasource(config, "public", rolePrefix, password)
        } else {
            val dbRole = getRole(rolePrefix, role)
            val config = initConnectionPoolConfig(datasourceName, dbRole, maxPoolSize)
            createVaultDatasource(config, environmentClass.mountPath(), dbRole)
        }
    }

    fun getEnvironmentClass(): EnvironmentClass {
        var cluster = System.getProperty("nais.cluster.name", System.getenv("NAIS_CLUSTER_NAME"))
        if (cluster != null) {
            cluster = cluster.substring(0, cluster.indexOf("-")).uppercase()
            return if ("DEV".equals(cluster, ignoreCase = true)) {
                EnvironmentClass.PREPROD
            } else EnvironmentClass.valueOf(cluster)
        }
        return EnvironmentClass.PROD
    }


    private fun initConnectionPoolConfig(dataSourceName: String, dbRole: String?, maxPoolSize: Int): HikariConfig {
        val config = HikariConfig()
        config.jdbcUrl = getProperty("$dataSourceName.url")
        config.minimumIdle = 0
        config.maximumPoolSize = maxPoolSize
        config.connectionTestQuery = "select 1"
        config.driverClassName = "org.postgresql.Driver"
        if (dbRole != null) {
            config.connectionInitSql = "SET ROLE \"$dbRole\""
        }

        // optimaliserer inserts for postgres
        val dsProperties = Properties()
        dsProperties.setProperty("reWriteBatchedInserts", "true")
        config.dataSourceProperties = dsProperties

        // skrur av autocommit her, da kan vi bypasse dette senere når hibernate setter opp entitymanager for bedre conn mgmt
        config.isAutoCommit = false
        return config
    }


    private fun getRolePrefix(datasourceName: String): String {
        return getProperty("$datasourceName.username")
    }

    private fun getRole(rolePrefix: String, role: DatasourceRole): String {
        return "$rolePrefix-${role.name.lowercase()}"
    }

    private fun getProperty(key: String): String {
        return System.getProperty(key, System.getenv(key.uppercase(Locale.getDefault()).replace('.', '_')))
    }

    private fun createVaultDatasource(config: HikariConfig, mountPath: String, role: String): DataSource {
        return try {
            HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, mountPath, role)
        } catch (vaultError: VaultError) {
            throw RuntimeException("Vault feil ved opprettelse av databaseforbindelse", vaultError)
        }
    }

    private fun createLocalDatasource(
        config: HikariConfig,
        schema: String?,
        username: String,
        password: String
    ): DataSource {
        config.username = username
        config.password = password // NOSONAR false positive
        if (schema != null && schema.isNotEmpty()) {
            config.schema = schema
        }
        return HikariDataSource(config)
    }


}