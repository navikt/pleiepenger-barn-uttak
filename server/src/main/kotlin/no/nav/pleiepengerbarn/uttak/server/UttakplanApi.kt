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
    private lateinit var uttakRepository: UttakRepository

    private companion object {
        private const val UttaksplanPath = "/uttaksplan"
        private const val BehandlingId = "behandlingId"
    }

    @PostMapping(UttaksplanPath, consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Opprette en ny uttaksplan. Tar inn grunnlaget som skal tas med i betraktning for å utlede uttaksplanen.")
    fun opprettUttaksplan(
            @RequestBody uttaksgrunnlag: Uttaksgrunnlag,
            uriComponentsBuilder: UriComponentsBuilder): ResponseEntity<Uttaksplan> {

        //Hent uttaksplaner for andre parters sakser
        val andrePartersUttaksplaner = mutableMapOf<Saksnummer, Uttaksplan>()
        uttaksgrunnlag.andrePartersSaksnummer.forEach { saksnummer ->
            andrePartersUttaksplaner[saksnummer] = hentUttaksplan(saksnummer)
        }

        val regelGrunnlag = GrunnlagMapper.tilRegelGrunnlag(uttaksgrunnlag, andrePartersUttaksplaner)
        val uttaksplan = UttakTjeneste.uttaksplan(regelGrunnlag)

        uttakRepository.lagre(uttaksgrunnlag.saksnummer, UUID.fromString(uttaksgrunnlag.behandlingId), regelGrunnlag, uttaksplan)

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
    fun hentUttaksplan(@RequestParam(required = false) behandlingId: Set<BehandlingId>?, @RequestParam(required = false) saksnummer: Set<Saksnummer>?): ResponseEntity<Uttaksplaner> {
        if (behandlingId != null && saksnummer != null && behandlingId.isNotEmpty() && saksnummer.isNotEmpty()) {
            return ResponseEntity.badRequest().build()
        }
        if (behandlingId.isNullOrEmpty() && saksnummer.isNullOrEmpty()) {
            return ResponseEntity.badRequest().build()
        }
        val uttaksplanMap = mutableMapOf<BehandlingId, Uttaksplan>()
        if (behandlingId != null && behandlingId.isNotEmpty()) {
            behandlingId.forEach {
                val uttaksplan = uttakRepository.hent(UUID.fromString(it))
                if (uttaksplan != null) {
                    uttaksplanMap[it] = uttaksplan
                }
            }
        } else {
            saksnummer?.forEach {
                uttaksplanMap[it] = hentUttaksplan(it)
            }
        }
        return ResponseEntity.ok(Uttaksplaner(uttaksplanMap))
    }

    @PatchMapping(UttaksplanPath)
    @Operation(description = "Sette uttaksplan for en behandling til inaktiv.")
    fun settInaktiv(behandlingId: BehandlingId): ResponseEntity<String> {
        uttakRepository.settInaktiv(UUID.fromString(behandlingId))
        return ResponseEntity.ok().build()
    }

    private fun hentUttaksplan(saksnummer:Saksnummer):Uttaksplan {
        val uttaksplanListe = uttakRepository.hent(saksnummer)
        return UttaksplanMerger.slåSammenUttaksplaner(uttaksplanListe)
    }

}

