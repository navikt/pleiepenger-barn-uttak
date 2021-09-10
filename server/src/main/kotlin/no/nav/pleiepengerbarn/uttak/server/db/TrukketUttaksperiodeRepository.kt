package no.nav.pleiepengerbarn.uttak.server.db

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement

@Repository
class TrukketUttaksperiodeRepository {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate


    internal fun lagreTrukketUttaksperioder(uttaksresultatId: Long, trukketUttaksperiodeList: List<LukketPeriode>) {
        val sql = """
            insert into
                trukket_uttaksperiode (id, uttaksresultat_id, fom, tom)
                values(nextval('seq_trukket_uttaksperiode'), ?, ?, ?)
        """.trimIndent()

        jdbcTemplate.batchUpdate(sql, object : BatchPreparedStatementSetter {

            override fun setValues(statement: PreparedStatement, index: Int) {
                statement.setLong(1, uttaksresultatId)
                with(trukketUttaksperiodeList[index]) {
                    statement.setObject(2, fom)
                    statement.setObject(3, tom)
                }
            }

            override fun getBatchSize() = trukketUttaksperiodeList.size

        })
    }

    internal fun hentTrukketUttaksperioder(uttaksresultatId: Long): List<LukketPeriode> {
        val sql = """
            select 
                fom, tom
            from 
                trukket_uttaksperiode
            where
                uttaksresultat_id = ?
            order by
                fom
        """.trimIndent()

        val mapper = RowMapper{ rs, _ -> LukketPeriode(rs.getDate("fom").toLocalDate(), rs.getDate("tom").toLocalDate()) }

        return jdbcTemplate.query(sql, mapper, uttaksresultatId)
    }


}