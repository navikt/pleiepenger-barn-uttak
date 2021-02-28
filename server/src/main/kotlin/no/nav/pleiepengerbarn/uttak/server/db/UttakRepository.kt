package no.nav.pleiepengerbarn.uttak.server.db

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.pleiepengerbarn.uttak.kontrakter.Saksnummer
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import org.postgresql.util.PGobject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*


@Repository
@Transactional
internal class UttakRepository {

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    private lateinit var uttaksperiodeRepository: UttaksperiodeRepository

    @Autowired
    private lateinit var mapper: ObjectMapper

    private companion object {
        private val uttaksplanRowMapper = RowMapper {resultSet, _ -> resultSet.getLong("id")}
    }

    internal fun lagre(saksnummer:String, behandlingId:UUID, regelGrunnlag: RegelGrunnlag, uttaksplan: Uttaksplan) {
        slettTidligereUttaksplan(behandlingId)
        val opprettetTidspunkt = OffsetDateTime.now(ZoneOffset.UTC)
        val sql = """
            insert into uttaksresultat 
            (id, saksnummer, behandling_id, regel_grunnlag, uttaksplan, slettet, opprettet_tid) 
            values(nextval('seq_uttaksresultat'), :saksnummer, :behandling_id, :regel_grunnlag, :uttaksplan, :slettet, :opprettet_tid)            
        """.trimIndent()

        val keyHolder = GeneratedKeyHolder()
        val params = MapSqlParameterSource()
            .addValue("saksnummer", saksnummer)
            .addValue("behandling_id", behandlingId)
            .addValue("regel_grunnlag", tilJSON(regelGrunnlag))
            .addValue("uttaksplan", tilJSON(uttaksplan))
            .addValue("slettet", false)
            .addValue("opprettet_tid", opprettetTidspunkt)

        jdbcTemplate.update(sql, params, keyHolder, arrayOf("id"))
        val uttaksresultatId = keyHolder.key as Long

        uttaksperiodeRepository.lagrePerioder(uttaksresultatId, uttaksplan.perioder)
    }

    internal fun hent(behandlingId:UUID):Uttaksplan? {
        return try {
            val uttaksresultatId = jdbcTemplate.queryForObject(
                "select id from uttaksresultat where behandling_id = :behandling_id and slettet=false",
                mapOf("behandling_id" to behandlingId),
                uttaksplanRowMapper)
            val perioder = uttaksperiodeRepository.hentPerioder(uttaksresultatId!!)
            Uttaksplan(perioder = perioder)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    internal fun hent(saksnummer:Saksnummer): Uttaksplan? {
        val sisteBehandlingUUID = finnForrigeBehandlingUUID(saksnummer, null)
        if (sisteBehandlingUUID != null) {
            return hent(sisteBehandlingUUID)
        }
        return null
    }

    fun hentForrige(saksnummer: Saksnummer, behandlingUUID: UUID): Uttaksplan? {
        val forrigeBehandlingUUID = finnForrigeBehandlingUUID(saksnummer, behandlingUUID) ?: return null
        return hent(forrigeBehandlingUUID)
    }

    private fun finnForrigeBehandlingUUID(saksnummer: Saksnummer, behandlingUUID: UUID?): UUID? {
        val behandlingUUIDer = jdbcTemplate.query(
            "select behandling_id from uttaksresultat where saksnummer = :saksnummer and slettet=false order by opprettet_tid",
            mapOf("saksnummer" to saksnummer)
        ) { resultSet, _ -> UUID.fromString(resultSet.getString("behandling_id")) }

        //Dersom behandlingUUID er angitt, så skal behandlingen før angitt behandling returneres
        if (behandlingUUID != null) {
            for ((i, b) in behandlingUUIDer.withIndex()) {
                if (b == behandlingUUID) {
                    return if (i == 0) null else behandlingUUIDer[i - 1]
                }
            }
        }

        //Dersom behandling ikke er funnet er siste behandling forrige behandling.
        if (behandlingUUIDer.isNotEmpty()) {
            return behandlingUUIDer.last()
        }

        //Ingen behandling funnet
        return null
    }


    private fun slettTidligereUttaksplan(behandlingId: UUID) {
        jdbcTemplate.update(
            "update uttaksresultat set slettet=true where behandling_id=:behandling_id",
            mapOf("behandling_id" to behandlingId)
        )
    }

    private fun tilJSON(obj:Any): PGobject {
        val jsonString = mapper.writeValueAsString(obj) ?: ""
        val jsonObject = PGobject()
        jsonObject.type = "json"
        jsonObject.value = jsonString
        return jsonObject
    }
/*
    private fun fraJSON(json:String):Uttaksplan {
        return mapper.readValue(json, Uttaksplan::class.java)
    }
*/
}