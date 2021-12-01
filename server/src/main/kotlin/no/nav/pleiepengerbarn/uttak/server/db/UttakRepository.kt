package no.nav.pleiepengerbarn.uttak.server.db

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.pleiepengerbarn.uttak.kontrakter.Saksnummer
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.sjekkOmOverlapp
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
    private lateinit var trukketUttaksperiodeRepository: TrukketUttaksperiodeRepository

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
            (id, saksnummer, behandling_id, regel_grunnlag, slettet, opprettet_tid, ytelsetype) 
            values(nextval('seq_uttaksresultat'), :saksnummer, :behandling_id, :regel_grunnlag, :slettet, :opprettet_tid, :ytelsetype::ytelsetype)            
        """.trimIndent()

        val keyHolder = GeneratedKeyHolder()
        val params = MapSqlParameterSource()
            .addValue("saksnummer", saksnummer)
            .addValue("behandling_id", behandlingId)
            .addValue("regel_grunnlag", tilJSON(regelGrunnlag))
            .addValue("slettet", false)
            .addValue("opprettet_tid", opprettetTidspunkt)
            .addValue("ytelsetype", regelGrunnlag.ytelseType.toString())

        jdbcTemplate.update(sql, params, keyHolder, arrayOf("id"))
        val uttaksresultatId = keyHolder.key as Long

        if (uttaksplan.perioder.keys.sjekkOmOverlapp()) {
            throw IllegalArgumentException("Lagre uttaksplan: Overlapp mellom perioder i uttak. ${uttaksplan.perioder.keys}")
        }
        if (regelGrunnlag.trukketUttak.sjekkOmOverlapp()) {
            throw IllegalArgumentException("Lagre uttaksplab: Overlapp mellom perioder i trukket uttak. ${regelGrunnlag.trukketUttak}")
        }
        uttaksperiodeRepository.lagrePerioder(uttaksresultatId, uttaksplan.perioder)
        trukketUttaksperiodeRepository.lagreTrukketUttaksperioder(uttaksresultatId, regelGrunnlag.trukketUttak)
    }

    internal fun hent(behandlingId:UUID):Uttaksplan? {
        return try {
            val uttaksresultatId = jdbcTemplate.queryForObject(
                "select id from uttaksresultat where behandling_id = :behandling_id and slettet=false",
                mapOf("behandling_id" to behandlingId),
                uttaksplanRowMapper)
            val perioder = uttaksperiodeRepository.hentPerioder(uttaksresultatId!!)
            val trukketUttaksperioder = trukketUttaksperiodeRepository.hentTrukketUttaksperioder(uttaksresultatId)
            if (perioder.keys.sjekkOmOverlapp()) {
                throw IllegalArgumentException("Hent uttaksplan: Overlapp mellom perioder i uttak. ${perioder.keys}")
            }
            if (trukketUttaksperioder.sjekkOmOverlapp()) {
                throw IllegalArgumentException("Hent uttaksplan: Overlapp mellom perioder i trukket uttak. $trukketUttaksperioder")
            }
            Uttaksplan(perioder = perioder, trukketUttak = trukketUttaksperioder)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    internal fun slett(behandlingId: UUID) {
        slettTidligereUttaksplan(behandlingId)
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
        val slettetTid = OffsetDateTime.now(ZoneOffset.UTC)
        jdbcTemplate.update(
            "update uttaksresultat set slettet=true, slettet_tid=:slettet_tid where behandling_id=:behandling_id and slettet=false",
            mapOf("behandling_id" to behandlingId, "slettet_tid" to slettetTid)
        )
    }

    private fun tilJSON(obj:Any): PGobject {
        val jsonString = mapper.writeValueAsString(obj) ?: ""
        val jsonObject = PGobject()
        jsonObject.type = "json"
        jsonObject.value = jsonString
        return jsonObject
    }

}