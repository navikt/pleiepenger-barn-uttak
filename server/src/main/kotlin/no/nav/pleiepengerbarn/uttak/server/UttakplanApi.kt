package no.nav.pleiepengerbarn.uttak.server

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttakTjeneste
import no.nav.pleiepengerbarn.uttak.regler.UttaksplanMerger
import no.nav.pleiepengerbarn.uttak.regler.mapper.GrunnlagMapper
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder

@RestController
@Tag(name = "Uttak API", description = "Operasjoner for uttak pleiepenger barn")
class UttakplanApi {

    private val lagredeUttaksplanerPerBehandling = mutableMapOf<BehandlingId, Uttaksplan>()
    private val lagredeUttaksplanerPerSaksnummer = mutableMapOf<Saksnummer, MutableList<Uttaksplan>>()

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

        val andrePartersUttaksplan = lagredeUttaksplanerPerBehandling
                .filterKeys { uttaksgrunnlag.andrePartersBehandlinger.contains(it) }
                .values
                .toList()

        val uttaksplan = UttakTjeneste.uttaksplan(GrunnlagMapper.tilRegelGrunnlag(
                uttaksgrunnlag = uttaksgrunnlag,
                andrePartersUttakplan = andrePartersUttaksplan
        ))

        lagredeUttaksplanerPerBehandling[uttaksgrunnlag.behandlingId] = uttaksplan
        if (lagredeUttaksplanerPerSaksnummer[uttaksgrunnlag.saksnummer] == null) {
            lagredeUttaksplanerPerSaksnummer[uttaksgrunnlag.saksnummer] = mutableListOf(uttaksplan)
        } else {
            lagredeUttaksplanerPerSaksnummer[uttaksgrunnlag.saksnummer]?.add(uttaksplan)
        }

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

        val uttaksplaner = Uttaksplaner(
                uttaksplaner = lagredeUttaksplanerPerBehandling
                        .filterKeys { behandlingId.contains(it) }
        )
        return ResponseEntity.ok(uttaksplaner)
    }

    @GetMapping(FullUttaksplanPath, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Full uttaksplan for ett gitt saksnummer.")
    fun hentFullUttaksplan(@RequestParam saksnummer: Saksnummer): ResponseEntity<FullUttaksplan> {
        val uttaksplaner = lagredeUttaksplanerPerSaksnummer[saksnummer]
        if (uttaksplaner != null) {
            return ResponseEntity.ok(UttaksplanMerger.slåSammenUttaksplaner(uttaksplaner))
        }
        return ResponseEntity.notFound().build()
    }

}

