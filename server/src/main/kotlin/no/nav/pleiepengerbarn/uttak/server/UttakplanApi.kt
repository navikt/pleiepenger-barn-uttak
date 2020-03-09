package no.nav.pleiepengerbarn.uttak.server

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttakTjeneste
import no.nav.pleiepengerbarn.uttak.regler.mapper.GrunnlagMapper
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder

@RestController
@Tag(name = "Uttak API", description = "Operasjoner for uttak pleiepenger barn")
class UttakplanApi {

    private val lagredeUttaksplaner = mutableMapOf<BehandlingId, Uttaksplan>()

    private companion object {
        private const val Path = "/uttaksplan"
        private const val BehandlingId = "behandlingId"
    }

    @PostMapping(Path, consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Opprette en ny uttaksplan. Tar inn grunnlaget som skal tas med i betraktning for å utlede uttaksplanen.")
    fun opprettUttaksplan(
            @RequestBody uttaksgrunnlag: Uttaksgrunnlag,
            uriComponentsBuilder: UriComponentsBuilder): ResponseEntity<Uttaksplan> {

        val andrePartersUttaksplan = lagredeUttaksplaner
                .filterKeys { uttaksgrunnlag.andrePartersBehandlinger.contains(it) }
                .values
                .toList()

        val uttaksplan = UttakTjeneste.uttaksplan(GrunnlagMapper.tilRegelGrunnlag(
                uttaksgrunnlag = uttaksgrunnlag,
                andrePartersUttakplan = andrePartersUttaksplan
        ))

        lagredeUttaksplaner[uttaksgrunnlag.behandlingId] = uttaksplan

        val uri = uriComponentsBuilder
                .path(Path)
                .queryParam(BehandlingId, uttaksgrunnlag.behandlingId)
                .build()
                .toUri()

        return ResponseEntity
                .created(uri)
                .body(uttaksplan)
    }


    @GetMapping(Path, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Uttaksplaner for alle etterspurte behandlinger.")
    fun hentUttaksplan(@RequestParam behandlingId: Set<BehandlingId>): ResponseEntity<Uttaksplaner> {

        val uttaksplaner = Uttaksplaner(
                uttaksplaner = lagredeUttaksplaner
                        .filterKeys { behandlingId.contains(it) }
        )

        return ResponseEntity.ok(uttaksplaner)
    }
}

