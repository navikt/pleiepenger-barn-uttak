package no.nav.pleiepengerbarn.uttak.server.db

import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.Types
import java.time.Duration

@Repository
class UtbetalingsgradRepository {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    internal fun lagre(utbetalingsgrader: Map<Long, List<Utbetalingsgrader>>) {

        val periodeIdOgGraderListe = mutableListOf<PeriodeIdOgGrader>()
        utbetalingsgrader.forEach {
            it.value.forEach { grader ->
                periodeIdOgGraderListe.add(PeriodeIdOgGrader(periodeId = it.key, grader = grader))
            }
        }

        val sql = """
            insert into 
            utbetalingsgrad(id, uttaksperiode_id, arbeidstype, organisasjonsnummer, aktoer_id, arbeidsforhold_id, normal_arbeidstid, faktisk_arbeidstid, utbetalingsgrad, tilkommet)
            values(nextval('seq_utbetalingsgrad'), ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        jdbcTemplate.batchUpdate(sql, object : BatchPreparedStatementSetter {

            override fun setValues(statement: PreparedStatement, index: Int) {

                val periodeIdOgGrader = periodeIdOgGraderListe[index]

                statement.setLong(1, periodeIdOgGrader.periodeId)

                with(periodeIdOgGrader.grader.arbeidsforhold) {
                    statement.setString(2, type)
                    statement.setString(3, organisasjonsnummer)
                    statement.setString(4, aktørId)
                    statement.setObject(5, arbeidsforholdId, Types.OTHER)
                }
                with(periodeIdOgGrader.grader) {
                    statement.setString(6, normalArbeidstid.toString())
                    statement.setString(7, faktiskArbeidstid?.toString())
                    statement.setBigDecimal(8, utbetalingsgrad)
                    statement.setBoolean(9, tilkommet?:false)
                }

            }

            override fun getBatchSize() = periodeIdOgGraderListe.size

        })
    }


    internal fun hentUtbetalingsgrader(uttaksresultatId: Long): Map<Long, List<Utbetalingsgrader>> {

        val sql = """
            select ug.uttaksperiode_id, ug.arbeidstype, ug.organisasjonsnummer, ug.aktoer_id, ug.arbeidsforhold_id, ug.normal_arbeidstid, ug.faktisk_arbeidstid, ug.utbetalingsgrad, ug.tilkommet
            from uttaksresultat ur, uttaksperiode up, utbetalingsgrad ug
            where ur.id = ? and ur.id = up.uttaksresultat_id and up.id = ug.uttaksperiode_id
            order by ug.arbeidstype, ug.organisasjonsnummer, ug.aktoer_id, ug.arbeidsforhold_id
        """.trimIndent()


        val utbetalingsgradMapper = RowMapper { rs, _ ->
            val uttaksperiodeId = rs.getLong("uttaksperiode_id")
            val utbetalingsgrader = Utbetalingsgrader(
                arbeidsforhold = Arbeidsforhold(
                    type = rs.getString("arbeidstype"),
                    organisasjonsnummer = rs.getString("organisasjonsnummer"),
                    aktørId = rs.getString("aktoer_id"),
                    arbeidsforholdId = rs.getString("arbeidsforhold_id")
                ),
                normalArbeidstid = Duration.parse(rs.getString("normal_arbeidstid")),
                faktiskArbeidstid = Duration.parse(rs.getString("faktisk_arbeidstid")),
                utbetalingsgrad = rs.getBigDecimal("utbetalingsgrad"),
                tilkommet = rs.getBoolean("tilkommet")
            )
            UttaksperiodeIdOgUtbetalingsgrader(uttaksperiodeId, utbetalingsgrader)
        }

        val uttaksperiodeIdOgUtbetalingsgraderList = jdbcTemplate.query(
            sql,
            utbetalingsgradMapper,
            uttaksresultatId
        )


        val utbetalingsgraderMap = mutableMapOf<Long, MutableList<Utbetalingsgrader>>()

        uttaksperiodeIdOgUtbetalingsgraderList.forEach {
            val grader = utbetalingsgraderMap[it.uttaksperiodeId]
            if (grader == null) {
                utbetalingsgraderMap[it.uttaksperiodeId] = mutableListOf(it.utbetalingsgrad)
            } else {
                grader.add(it.utbetalingsgrad)
            }
        }

        return utbetalingsgraderMap
    }



}

private data class PeriodeIdOgGrader(
    val periodeId: Long,
    val grader: Utbetalingsgrader
)

private data class UttaksperiodeIdOgUtbetalingsgrader(
    val uttaksperiodeId: Long,
    val utbetalingsgrad: Utbetalingsgrader
)
