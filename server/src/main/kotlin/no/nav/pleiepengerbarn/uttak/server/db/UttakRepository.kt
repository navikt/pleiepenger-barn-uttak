package no.nav.pleiepengerbarn.uttak.server.db

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.pleiepengerbarn.uttak.kontrakter.Saksnummer
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import org.postgresql.util.PGobject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*


@Repository
internal class UttakRepository {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var mapper: ObjectMapper

    private companion object {
        private val uttaksplanRowMapper = RowMapper {resultSet, _ -> resultSet.getString("uttaksplan")}
    }

    internal fun lagre(saksnummer:String, behandlingId:UUID, regelGrunnlag: RegelGrunnlag, uttaksplan: Uttaksplan) {
        slettTidligereUttaksplan(behandlingId)
        val opprettetTidspunkt = OffsetDateTime.now(ZoneOffset.UTC)
        jdbcTemplate.update("insert into uttaksresultat (id, saksnummer, behandling_id, regel_grunnlag, uttaksplan, slettet, opprettet_tid) values(nextval('seq_uttaksresultat'), ?, ?, ?, ?, ?, ?)",
                saksnummer, behandlingId, tilJSON(regelGrunnlag), tilJSON(uttaksplan), false, opprettetTidspunkt)
    }

    internal fun hent(behandlingId:UUID):Uttaksplan? {
        val uttaksplanJSON = jdbcTemplate.queryForObject("select uttaksplan from uttaksresultat where behandling_id = ? and slettet=false",
                uttaksplanRowMapper,
                behandlingId)

        if (uttaksplanJSON != null) {
            return fraJSON(uttaksplanJSON)
        }
        return null
    }

    internal fun hent(saksnummer:Saksnummer):List<Uttaksplan> {
        val uttaksplanJSONListe = jdbcTemplate.query("select uttaksplan from uttaksresultat where saksnummer = ? and slettet=false order by opprettet_tid desc",
                uttaksplanRowMapper,
                saksnummer)

        return uttaksplanJSONListe.map { json -> fraJSON(json) }
    }

    fun hentForrige(saksnummer: Saksnummer, behandlingUUID: UUID): Uttaksplan? {
        val forrigeBehandlingUUID = finnForrigeBehandlingUUID(saksnummer, behandlingUUID) ?: return null
        return hent(forrigeBehandlingUUID)
    }

    private fun finnForrigeBehandlingUUID(saksnummer: Saksnummer, behandlingUUID: UUID): UUID? {
        val behandlingUUIDer = jdbcTemplate.query("select behandling_id from uttaksresultat where saksnummer = ? and slettet=false order by opprettet_tid",
            {resultSet, _ -> UUID.fromString(resultSet.getString("behandling_id"))},
            saksnummer)

        for ((i, b) in behandlingUUIDer.withIndex()) {
            if (b == behandlingUUID) {
                return if (i == 0) null else behandlingUUIDer[i - 1]
            }
        }
        if (behandlingUUIDer.isNotEmpty()) {
            //Dersom behandling ikke er funner er siste behandling forrige behandling.
            return behandlingUUIDer[0]
        }
        return null
    }


    private fun slettTidligereUttaksplan(behandlingId: UUID) {
        jdbcTemplate.update("update uttaksresultat set slettet=true where behandling_id=?", behandlingId)
    }

    private fun tilJSON(obj:Any): PGobject {
        val jsonString = mapper.writeValueAsString(obj) ?: ""
        val jsonObject = PGobject()
        jsonObject.type = "json"
        jsonObject.value = jsonString
        return jsonObject
    }

    private fun fraJSON(json:String):Uttaksplan {
        return mapper.readValue(json, Uttaksplan::class.java)
    }

}