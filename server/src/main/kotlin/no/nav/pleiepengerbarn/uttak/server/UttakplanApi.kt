package no.nav.pleiepengerbarn.uttak.server

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttakTjeneste
import no.nav.pleiepengerbarn.uttak.regler.UttaksplanMerger
import no.nav.pleiepengerbarn.uttak.regler.mapper.GrunnlagMapper
import no.nav.pleiepengerbarn.uttak.server.db.UttakRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder
import java.util.*

@RestController
@Tag(name = "Uttak API", description = "Operasjoner for uttak pleiepenger barn")
class UttakplanApi {

    @Autowired
    private val uttakRepository: UttakRepository? = null

    private companion object {
        private const val UttaksplanPath = "/uttaksplan"
        private const val FullUttaksplanPath = "/uttaksplan/full"
        private const val BehandlingId = "behandlingId"
    }

    @PostMapping(UttaksplanPath, consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Opprette en ny uttaksplan. Tar inn grunnlaget som skal tas med i betraktning for å utlede uttaksplanen.")
    fun opprettUttaksplan(
            @RequestBody uttaksgrunnlag: Uttaksgrunnlag,
            uriComponentsBuilder: UriComponentsBuilder): ResponseEntity<Uttaksplan> {


        val andrePartersUttaksplaner = mutableListOf<Uttaksplan>()
        uttaksgrunnlag.andrePartersBehandlinger.forEach {

        }
        //TODO hent uttaksplan for andre parter
        val regelGrunnlag = GrunnlagMapper.tilRegelGrunnlag(uttaksgrunnlag, listOf())
        val uttaksplan = UttakTjeneste.uttaksplan(regelGrunnlag)

        uttakRepository?.lagre(uttaksgrunnlag.saksnummer, UUID.fromString(uttaksgrunnlag.behandlingId), regelGrunnlag, uttaksplan)

        val uri = uriComponentsBuilder
                .path(UttaksplanPath)
                .queryParam(BehandlingId, uttaksgrunnlag.behandlingId)
                .build()
                .toUri()

        return ResponseEntity
                .created(uri)
                .body(uttaksplan)
    }

    @GetMapping(UttaksplanPath, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Uttaksplaner for alle etterspurte behandlinger.")
    fun hentUttaksplan(@RequestParam behandlingId: Set<BehandlingId>): ResponseEntity<Uttaksplaner> {
        val uttaksplanMap = mutableMapOf<BehandlingId, Uttaksplan>()
        behandlingId.forEach {
            val uttaksplan = uttakRepository?.hent(UUID.fromString(it))
            if (uttaksplan != null) {
                uttaksplanMap[it] = uttaksplan
            }
        }
        return ResponseEntity.ok(Uttaksplaner(uttaksplanMap))
    }
/*
    @GetMapping(FullUttaksplanPath, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Full uttaksplan for ett gitt saksnummer.")
    fun hentFullUttaksplan(@RequestParam saksnummer: Saksnummer): ResponseEntity<FullUttaksplan> {
        val uttaksplaner = lagredeUttaksplanerPerSaksnummer[saksnummer]
        if (uttaksplaner != null) {
            return ResponseEntity.ok(UttaksplanMerger.slåSammenUttaksplaner(uttaksplaner))
        }
        return ResponseEntity.notFound().build()
    }
*/
}

