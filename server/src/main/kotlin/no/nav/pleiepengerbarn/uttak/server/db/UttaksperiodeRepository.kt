package no.nav.pleiepengerbarn.uttak.server.db

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.postgresql.util.PGobject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.Types
import java.util.*

@Repository
internal class UttaksperiodeRepository {

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    private lateinit var utbetalingsgradRepository: UtbetalingsgradRepository

    @Autowired
    private lateinit var mapper: ObjectMapper

    internal fun lagrePerioder(uttaksresultatId: Long, perioder: Map<LukketPeriode, UttaksperiodeInfo>) {
        val utbetalingsgrader = mutableMapOf<Long, List<Utbetalingsgrader>>()
        perioder.forEach { periodeEntry ->
            val uttaksperiodeId = lagrePeriode(uttaksresultatId, periodeEntry.key, periodeEntry.value)
            utbetalingsgrader[uttaksperiodeId] = periodeEntry.value.utbetalingsgrader
        }
        utbetalingsgradRepository.lagre(utbetalingsgrader)
    }

    internal fun hentPerioder(uttaksresultatId: Long): Map<LukketPeriode, UttaksperiodeInfo> {

        val sql = """
            select 
                id, fom, tom,
                pleiebehov, etablert_tilsyn, andre_sokeres_tilsyn, tilgjengelig_for_soker,
                uttaksgrad, aarsaker, utfall, sokers_tapte_arbeidstid,
                inngangsvilkar, knekkpunkt_typer, kilde_behandling_uuid, annen_part
            from uttaksperiode
            where uttaksresultat_id = :uttaksresultat_id
        """.trimIndent()

        val uttaksperiodeMapper = RowMapper { rs, _ ->

            var graderingMotTilsyn: GraderingMotTilsyn? = null
            val pleiebehov = rs.getBigDecimal("pleiebehov") //TODO: pleiebehov bør flyttes utenfor GraderMotTilsyn eller fjern optional på GraderMotTilsyn i uttaksperiode.
            val etablertTilsyn = rs.getBigDecimal("etablert_tilsyn")
            if (etablertTilsyn != null) {
                graderingMotTilsyn = GraderingMotTilsyn(
                    pleiebehov = pleiebehov,
                    etablertTilsyn = etablertTilsyn,
                    andreSøkeresTilsyn = rs.getBigDecimal("andre_sokeres_tilsyn"),
                    tilgjengeligForSøker = rs.getBigDecimal("tilgjengelig_for_soker")
                )
            }

            PeriodeOgUttaksperiodeInfo(
                uttaksperiodeId = rs.getLong("id"),
                periode = LukketPeriode(
                    fom =rs.getDate("fom").toLocalDate(),
                    tom = rs.getDate("tom").toLocalDate()
                ),
                uttaksperiodeInfo = UttaksperiodeInfo(
                    utfall = Utfall.valueOf(rs.getString("utfall")),
                    uttaksgrad = rs.getBigDecimal("uttaksgrad"),
                    utbetalingsgrader = listOf(), //Blir lagt til litt lengre nede
                    søkersTapteArbeidstid = rs.getBigDecimal("sokers_tapte_arbeidstid"),
                    årsaker = årsakerFraJSON(rs.getString("aarsaker")).toSet(),
                    pleiebehov = Pleiebehov.PROSENT_100.prosent,
                    inngangsvilkår = inngangsvilkårFraJSON(rs.getString("inngangsvilkar")),
                    graderingMotTilsyn = graderingMotTilsyn,
                    knekkpunktTyper = knekkpunktTyperFraJSON(rs.getString("knekkpunkt_typer")).toSet(),
                    kildeBehandlingUUID = rs.getString("kilde_behandling_uuid"),
                    annenPart = AnnenPart.valueOf(rs.getString("annen_part"))
                )
            )
        }

        val uttaksperioder = jdbcTemplate.query(
            sql,
            mapOf("uttaksresultat_id" to uttaksresultatId),
            uttaksperiodeMapper
        )

        val utbetalingsgrader = utbetalingsgradRepository.hentUtbetalingsgrader(uttaksresultatId)

        val uttaksperioderMap = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()

        uttaksperioder.forEach { periodeOgInfo ->
            require(uttaksperioderMap[periodeOgInfo.periode] == null) {"Duplikat periode ${periodeOgInfo.periode}"}
            val utbetalingsgraderForPeriode = utbetalingsgrader[periodeOgInfo.uttaksperiodeId]
            uttaksperioderMap[periodeOgInfo.periode] = periodeOgInfo.uttaksperiodeInfo.copy(utbetalingsgrader = utbetalingsgraderForPeriode ?: listOf() )
        }

        return uttaksperioderMap

    }


    private fun lagrePeriode(uttaksperiodeId: Long, periode: LukketPeriode, info: UttaksperiodeInfo): Long {
        val sql = """
            insert into 
                uttaksperiode (id, uttaksresultat_id, fom, tom, pleiebehov, etablert_tilsyn, andre_sokeres_tilsyn,
                    tilgjengelig_for_soker, uttaksgrad, aarsaker, utfall, sokers_tapte_arbeidstid, inngangsvilkar, knekkpunkt_typer,
                    kilde_behandling_uuid, annen_part)
                values(nextval('seq_uttaksperiode'), :uttaksresultat_id, :fom, :tom, :pleiebehov, :etablert_tilsyn, :andre_sokeres_tilsyn,
                    :tilgjengelig_for_soker, :uttaksgrad, :aarsaker, :utfall::utfall, :sokers_tapte_arbeidstid, :inngangsvilkar, :knekkpunkt_typer,
                    :kilde_behandling_uuid, :annen_part::annen_part)
       
        """.trimIndent()
        val keyHolder = GeneratedKeyHolder()
        val params = MapSqlParameterSource()
            .addValue("uttaksresultat_id", uttaksperiodeId)
            .addValue("fom", periode.fom)
            .addValue("tom", periode.tom)
            .addValue("pleiebehov", info.pleiebehov)
            .addValue("etablert_tilsyn", info.graderingMotTilsyn?.etablertTilsyn)
            .addValue("andre_sokeres_tilsyn", info.graderingMotTilsyn?.andreSøkeresTilsyn)
            .addValue("tilgjengelig_for_soker", info.graderingMotTilsyn?.tilgjengeligForSøker)
            .addValue("uttaksgrad", info.uttaksgrad)
            .addValue("aarsaker", tilJSON(info.årsaker))
            .addValue("utfall", info.utfall.toString(), Types.OTHER)
            .addValue("sokers_tapte_arbeidstid", info.søkersTapteArbeidstid)
            .addValue("inngangsvilkar", tilJSON(info.inngangsvilkår))
            .addValue("knekkpunkt_typer", tilJSON(info.knekkpunktTyper))
            .addValue("kilde_behandling_uuid", UUID.fromString(info.kildeBehandlingUUID))
            .addValue("annen_part", info.annenPart.toString())

        jdbcTemplate.update(sql, params, keyHolder, arrayOf("id"))
        return keyHolder.key as Long
    }

    private fun tilJSON(obj:Any): PGobject {
        val jsonString = mapper.writeValueAsString(obj) ?: ""
        val jsonObject = PGobject()
        jsonObject.type = "json"
        jsonObject.value = jsonString
        return jsonObject
    }

    private fun årsakerFraJSON(json: String): List<Årsak> {
        return mapper.readerForListOf(Årsak::class.java).readValue(json)
    }

    private fun knekkpunktTyperFraJSON(json: String): List<KnekkpunktType> {
        return mapper.readerForListOf(KnekkpunktType::class.java).readValue(json)
    }

    private fun inngangsvilkårFraJSON(json: String): Map<String, Utfall> {
        return mapper.readerForMapOf(Utfall::class.java).readValue(json)
    }

}

private data class PeriodeOgUttaksperiodeInfo(
    val uttaksperiodeId: Long,
    val periode: LukketPeriode,
    val uttaksperiodeInfo: UttaksperiodeInfo
)