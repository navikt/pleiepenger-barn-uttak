package no.nav.pleiepengerbarn.uttak.server.db

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.pleiepengerbarn.uttak.kontrakter.BehandlingId
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*

@Repository
class UttakRepository {

    @Autowired
    private val jdbcTemplate: JdbcTemplate? = null

    @Autowired
    private val mapper: ObjectMapper? = null

    fun lagre(saksnummer:String, behandlingId:BehandlingId, regelGrunnlag: RegelGrunnlag, uttaksplan: Uttaksplan) {
        slettTidligereUttaksplan(behandlingId)

        jdbcTemplate?.update("insert into uttaksresultat (id, saksnummer, behandling_id, regel_grunnlag, uttaksplan, slettet) values(nextval('seq_uttaksresultat'), ?, ?, ?, ?, ?)",
                saksnummer, UUID.fromString(behandlingId), tilJSON(regelGrunnlag), tilJSON(uttaksplan), false)
    }

    fun hent(behandlingId:BehandlingId):Uttaksplan? {
        val rowMapper = RowMapper { resultSet: ResultSet, _: Int ->
            resultSet.getString("uttaksplan")
        }

        val uttaksplanJSON = jdbcTemplate?.queryForObject("select uttaksplan from uttaksresultat where behandling_id = ?",
                rowMapper,
                behandlingId)

        if (uttaksplanJSON != null) {
            return fraJSON(uttaksplanJSON)
        }
        return null
    }

    private fun slettTidligereUttaksplan(behandlingId: String) {
        jdbcTemplate?.update("update uttaksresultat set slettet=true where behandling_id=?", behandlingId)
    }

    private fun tilJSON(obj:Any): String {
        return mapper?.writeValueAsString(obj) ?: ""
    }

    private fun fraJSON(json:String):Uttaksplan? {
        return mapper?.readValue(json, Uttaksplan::class.java)
    }

}