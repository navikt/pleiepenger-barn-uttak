package no.nav.pleiepengerbarn.uttak.server.db

import no.nav.pleiepengerbarn.uttak.kontrakter.KvoteInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
class KvoteInfoRepository {

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    internal fun lagreKvoteInfo(uttaksresultatId: Long, kvoteInfo: KvoteInfo?) {
        val sql = """
            insert into 
                kvote_info (id, uttaksresultat_id, max_dato, forbrukt_kvote_hittil, forbrukt_kvote_denne_behandlingen)
                values(nextval('seq_kvote_info'), :uttaksresultat_id, :max_dato, :forbrukt_kvote_hittil, :forbrukt_kvote_denne_behandlingen)
        """.trimIndent()
        val params = MapSqlParameterSource()
                .addValue("uttaksresultat_id", uttaksresultatId)
                .addValue("max_dato", kvoteInfo?.maxDato)
                .addValue("forbrukt_kvote_hittil", kvoteInfo?.forbruktKvoteHittil)
                .addValue("forbrukt_kvote_denne_behandlingen", kvoteInfo?.forbruktKvoteDenneBehandlingen)
        jdbcTemplate.update(sql, params)
    }

    internal fun hentKvoteInfo(uttaksresultatId: Long): KvoteInfo? {
        val sql = """
            select 
                id, max_dato, forbrukt_kvote_hittil, forbrukt_kvote_denne_behandlingen
            from kvote_info
            where uttaksresultat_id = :uttaksresultat_id
        """.trimIndent()
        try {
            val kvoteInfoMapper = RowMapper { rs, _ ->
                val forbruktKvoteHittil = if (rs.getBigDecimal("forbrukt_kvote_hittil") != null) rs.getBigDecimal("forbrukt_kvote_hittil") else BigDecimal.ZERO
                val forbruktKvoteDenneBehandlingen = if (rs.getBigDecimal("forbrukt_kvote_denne_behandlingen") != null) rs.getBigDecimal("forbrukt_kvote_denne_behandlingen") else BigDecimal.ZERO

                KvoteInfo(
                        maxDato = rs.getDate("max_dato")?.toLocalDate(),
                        forbruktKvoteHittil = forbruktKvoteHittil,
                        forbruktKvoteDenneBehandlingen = forbruktKvoteDenneBehandlingen
                )
            }

            return jdbcTemplate.queryForObject(sql, mapOf("uttaksresultat_id" to uttaksresultatId), kvoteInfoMapper)
        } catch (e: EmptyResultDataAccessException) {
            return null
        }
    }
}