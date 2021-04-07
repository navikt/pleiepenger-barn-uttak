package db.migration

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.pleiepengerbarn.uttak.kontrakter.OverseEtablertTilsynÅrsak
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak
import no.nav.pleiepengerbarn.uttak.regler.ÅTTI_PROSENT
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import java.lang.IllegalArgumentException

class V1_6__Oppdater_årsaker : BaseJavaMigration() {

    private val logger = LoggerFactory.getLogger(V1_6__Oppdater_årsaker::class.java)

    companion object {
        val mapper = ObjectMapper()
    }

    override fun migrate(context: Context?) {
        requireNotNull(context) {"Context kan ikke være null."}
        val jdbcTemplate = NamedParameterJdbcTemplate(SingleConnectionDataSource(context.connection, true))


        val hentÅrsakerSql = """
            select 
                id, aarsaker
            from uttaksperiode
        """.trimIndent()


        val årsakMapper = RowMapper { rs, _ ->
            val overseEtablertTilsynÅrsakString = rs.getString("overse_etablert_tilsyn_arsak")
            val overseEtablertTilsynÅrsak = if (overseEtablertTilsynÅrsakString != null) OverseEtablertTilsynÅrsak.valueOf(overseEtablertTilsynÅrsakString) else null
            ÅrsakerOgTilsyn(
                    periodeId = rs.getLong("id"),
                    årsakerSomJsonString = rs.getString("aarsaker"),
                    etablertTilsyn = rs.getBigDecimal("etablert_tilsyn"),
                    andreSøkeresTilsyn = rs.getBigDecimal("andre_sokeres_tilsyn"),
                    overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak
            )
        }

        val årsakerOgTilsynListe = jdbcTemplate.query(hentÅrsakerSql, årsakMapper)


        årsakerOgTilsynListe.forEach { årsakerOgTilsyn ->
            val (oppdatert, nyeÅrsaker)  = migrerÅrsaker(årsakerOgTilsyn)
            if (oppdatert) {
                logger.info("PeriodeId ${årsakerOgTilsyn.periodeId} oppdatert med følgende årsaker: $nyeÅrsaker   (Grunnlag: $årsakerOgTilsyn)")
                oppdaterÅrsaker(jdbcTemplate, årsakerOgTilsyn.periodeId, nyeÅrsaker)
            }
        }

    }

    private fun oppdaterÅrsaker(jdbcTemplate: NamedParameterJdbcTemplate, periodeId: Long, nyeÅrsaker: Set<Årsak>) {
        val sql = """
            update uttaksperiode 
            set aarsaker = :aarsaker
            where id = :periode_id
        """.trimIndent()
        jdbcTemplate.update(sql, mapOf("aarsaker" to nyeÅrsaker, "periode_id" to periodeId))
    }

    private fun migrerÅrsaker(årsakerOgTilsyn: ÅrsakerOgTilsyn): Pair<Boolean, Set<Årsak>> {
        val årsakerSomStringer = mapper.readValue(årsakerOgTilsyn.årsakerSomJsonString, Array<String>::class.java)
        val årsaker = mutableSetOf<Årsak>()
        var oppdatert = false
        årsakerSomStringer.forEach { årsakSomString ->
            if (årsakSomString == "FOR_LAV_GRAD") {
                val årsak = utledForLavGradÅrsak(årsakerOgTilsyn.etablertTilsyn, årsakerOgTilsyn.andreSøkeresTilsyn, årsakerOgTilsyn.overseEtablertTilsynÅrsak)
                if (årsak != null) {
                    årsaker.add(årsak)
                }
                oppdatert = true
            } else {
                try {
                    val årsak = Årsak.valueOf(årsakSomString)
                    årsaker.add(årsak)
                } catch(e: IllegalArgumentException) {
                    oppdatert = true
                    logger.warn("Årsak ($årsakSomString) er ukjent. Blir fjernet dra periode.")
                }
            }
        }
        return Pair(oppdatert, årsaker)
    }

    private fun utledForLavGradÅrsak(etablertTilsynsprosent: Prosent, andreSøkeresTilsyn: Prosent, overseEtablertTilsynÅrsak: OverseEtablertTilsynÅrsak?): Årsak? {
        if (overseEtablertTilsynÅrsak != null) {
            if (andreSøkeresTilsyn > ÅTTI_PROSENT) {
                return Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE
            }
        } else {
            when {
                andreSøkeresTilsyn > ÅTTI_PROSENT -> {
                    return Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE
                }
                etablertTilsynsprosent > ÅTTI_PROSENT -> {
                    return Årsak.FOR_LAV_REST_PGA_ETABLERT_TILSYN
                }
                andreSøkeresTilsyn + etablertTilsynsprosent > ÅTTI_PROSENT -> {
                    return Årsak.FOR_LAV_REST_PGA_ETABLERT_TILSYN_OG_ANDRE_SØKERE
                }
            }
        }
        return null
    }

}

private data class ÅrsakerOgTilsyn(
        val periodeId: Long,
        val etablertTilsyn: Prosent,
        val andreSøkeresTilsyn: Prosent,
        val årsakerSomJsonString: String,
        val overseEtablertTilsynÅrsak: OverseEtablertTilsynÅrsak?
)
