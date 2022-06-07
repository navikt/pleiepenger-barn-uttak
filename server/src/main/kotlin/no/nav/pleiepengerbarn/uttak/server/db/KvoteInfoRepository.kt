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

    internal fun lagreKvoteInfo(uttaksresultatId: Long, kvoteInfo: KvoteInfo) {
        val sql = """
            insert into 
                kvote_info (id, uttaksresultat_id, max_dato, totalt_forbrukt_kvote)
                values(nextval('seq_kvote_info'), :uttaksresultat_id, :max_dato, :totalt_forbrukt_kvote)
        """.trimIndent()
        val params = MapSqlParameterSource()
                .addValue("uttaksresultat_id", uttaksresultatId)
                .addValue("max_dato", kvoteInfo.maxDato)
                .addValue("totalt_forbrukt_kvote", kvoteInfo.totaltForbruktKvote)
        jdbcTemplate.update(sql, params)
    }

    internal fun hentKvoteInfo(uttaksresultatId: Long): KvoteInfo? {
        val sql = """
            select 
                id, max_dato, totalt_forbrukt_kvote
            from kvote_info
            where uttaksresultat_id = :uttaksresultat_id
        """.trimIndent()
        try {
            val kvoteInfoMapper = RowMapper { rs, _ ->
                val totaltForbruktKvote = if (rs.getBigDecimal("totalt_forbrukt_kvote") != null) rs.getBigDecimal("totalt_forbrukt_kvote") else BigDecimal.ZERO

                KvoteInfo(
                        maxDato = rs.getDate("max_dato")?.toLocalDate(),
                        totaltForbruktKvote = totaltForbruktKvote
                )
            }

            return jdbcTemplate.queryForObject(sql, mapOf("uttaksresultat_id" to uttaksresultatId), kvoteInfoMapper)
        } catch (e: EmptyResultDataAccessException) {
            return null
        }
    }
}