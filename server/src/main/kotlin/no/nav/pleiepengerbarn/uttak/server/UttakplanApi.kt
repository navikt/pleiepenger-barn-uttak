package no.nav.pleiepengerbarn.uttak.server

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import java.lang.IllegalArgumentException
import java.util.*

@RestController
@Tag(name = "Uttak API", description = "Operasjoner for uttak pleiepenger barn")
class UttakplanApi {

    @Autowired
    private lateinit var uttakRepository: UttakRepository

    companion object {
        const val UttaksplanPath = "/uttaksplan"
        const val FullUttaksplanForTilkjentYtelsePath = "/uttaksplan/ty"
        const val UttaksplanSimuleringPath = "/uttaksplan/simulering"
        const val BehandlingUUID = "behandlingUUID"
    }

    @PostMapping(UttaksplanPath, consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Opprette en ny uttaksplan. Tar inn grunnlaget som skal tas med i betraktning for å utlede uttaksplanen.")
    fun opprettUttaksplan(
            @RequestBody uttaksgrunnlag: Uttaksgrunnlag,
            uriComponentsBuilder: UriComponentsBuilder): ResponseEntity<Uttaksplan> {

        return lagUttaksplan(uttaksgrunnlag, true, uriComponentsBuilder)
    }

    @PostMapping(UttaksplanSimuleringPath, consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Simuler opprettelse av en ny uttaksplan. Tar inn grunnlaget som skal tas med i betraktning for å utlede uttaksplanen.")
    fun simulerUttaksplan(
            @RequestBody uttaksgrunnlag: Uttaksgrunnlag,
            uriComponentsBuilder: UriComponentsBuilder): ResponseEntity<Uttaksplan> {

        return lagUttaksplan(uttaksgrunnlag, false, uriComponentsBuilder)
    }

    private fun lagUttaksplan(uttaksgrunnlag: Uttaksgrunnlag, lagre: Boolean, uriComponentsBuilder: UriComponentsBuilder): ResponseEntity<Uttaksplan> {
        val andrePartersUttaksplaner = mutableMapOf<Saksnummer, Uttaksplan>()
        uttaksgrunnlag.andrePartersSaksnummer.forEach { saksnummer ->
            andrePartersUttaksplaner[saksnummer] = hentUttaksplanerOgSlåSammen(saksnummer)
        }
        val regelGrunnlag = GrunnlagMapper.tilRegelGrunnlag(uttaksgrunnlag, andrePartersUttaksplaner)

        var uttaksplan = UttakTjeneste.uttaksplan(regelGrunnlag)
        val forrigeUttaksplan = uttakRepository.hentForrige(uttaksgrunnlag.saksnummer, UUID.fromString(uttaksgrunnlag.behandlingUUID))
        if (forrigeUttaksplan != null) {
            uttaksplan = UttaksplanMerger.slåSammenUttaksplaner(listOf(uttaksplan, forrigeUttaksplan))
        }

        if (lagre) {
            uttakRepository.lagre(uttaksgrunnlag.saksnummer, UUID.fromString(uttaksgrunnlag.behandlingUUID), regelGrunnlag, uttaksplan)
        }

        val uri = uriComponentsBuilder.path(UttaksplanPath).queryParam(BehandlingUUID, uttaksgrunnlag.behandlingUUID).build().toUri()

        return ResponseEntity
                .created(uri)
                .body(uttaksplan)
    }

    @GetMapping(UttaksplanPath, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        description = "Hent uttaksplan for gitt behandling.",
        parameters = [
            Parameter(name = "behandlingUUID", description = "UUID for behandling som skal hentes.")
        ]
    )
    fun hentUttaksplanForBehandling(@RequestParam behandlingUUID: BehandlingUUID): ResponseEntity<Uttaksplan> {
        val behandlingUUIDParsed = try {
            UUID.fromString(behandlingUUID)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }

        val uttaksplan = uttakRepository.hent(behandlingUUIDParsed) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(uttaksplan)
    }

    @GetMapping(FullUttaksplanForTilkjentYtelsePath, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        description = "Hent forenklet uttaksplan for behandling.",
        parameters = [
            Parameter(name = "behandlingUUID", description = "UUID for behandling som skal hentes.")
        ]
    )
    fun hentFullUttaksplanForTilkjentYtelse(@RequestParam behandlingUUID: BehandlingUUID): ResponseEntity<ForenkletUttaksplan> {
        val behandlingUUIDParsed = try {
            UUID.fromString(behandlingUUID)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
        val uttaksplan = uttakRepository.hent(behandlingUUIDParsed) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(uttaksplan.tilForenkletUttaksplan())
    }

    private fun hentUttaksplanerOgSlåSammen(saksnummer:Saksnummer): Uttaksplan {
        val uttaksplanListe = uttakRepository.hent(saksnummer)
        return UttaksplanMerger.slåSammenUttaksplaner(uttaksplanListe)
    }

}

