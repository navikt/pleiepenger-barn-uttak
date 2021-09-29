package no.nav.pleiepengerbarn.uttak.server

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.EndringsstatusOppdaterer
import no.nav.pleiepengerbarn.uttak.regler.UttakTjeneste
import no.nav.pleiepengerbarn.uttak.regler.UttaksplanMerger
import no.nav.pleiepengerbarn.uttak.regler.mapper.GrunnlagMapper
import no.nav.pleiepengerbarn.uttak.regler.sjekk
import no.nav.pleiepengerbarn.uttak.server.db.UttakRepository
import org.slf4j.LoggerFactory
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

        private val logger = LoggerFactory.getLogger(this::class.java)

    }

    @PostMapping(UttaksplanPath, consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Opprette en ny uttaksplan. Tar inn grunnlaget som skal tas med i betraktning for å utlede uttaksplanen.")
    fun opprettUttaksplan(
            @RequestBody uttaksgrunnlag: Uttaksgrunnlag,
            uriComponentsBuilder: UriComponentsBuilder): ResponseEntity<Uttaksplan> {
        logger.info("Opprett uttaksplan for behanding=${uttaksgrunnlag.behandlingUUID}")
        return lagUttaksplan(uttaksgrunnlag, true, uriComponentsBuilder)
    }

    @PostMapping(UttaksplanSimuleringPath, consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Simuler opprettelse av en ny uttaksplan. Tar inn grunnlaget som skal tas med i betraktning for å utlede uttaksplanen.")
    fun simulerUttaksplan(
            @RequestBody uttaksgrunnlag: Uttaksgrunnlag,
            uriComponentsBuilder: UriComponentsBuilder): ResponseEntity<Uttaksplan> {
        logger.info("Simulerer uttaksplan for behanding=${uttaksgrunnlag.behandlingUUID}")
        return lagUttaksplan(uttaksgrunnlag, false, uriComponentsBuilder)
    }

    private fun lagUttaksplan(uttaksgrunnlag: Uttaksgrunnlag, lagre: Boolean, uriComponentsBuilder: UriComponentsBuilder): ResponseEntity<Uttaksplan> {
        val valideringsfeil = uttaksgrunnlag.sjekk()
        if (valideringsfeil.isNotEmpty()) {
            throw ValideringException("Valideringsfeil: $valideringsfeil")
        }

        //TODO denne bolken med kode skal etterhvert erstartes av koden nedenfor
        val andrePartersUttaksplanerPerSak = mutableMapOf<Saksnummer, Uttaksplan>()
        uttaksgrunnlag.andrePartersSaksnummer.forEach { saksnummer ->
            val uttaksplan = uttakRepository.hent(saksnummer)
            if (uttaksplan != null) {
                andrePartersUttaksplanerPerSak[saksnummer] = uttaksplan
            }
        }

        val andrePartersUttaksplanerPerBehandling = mutableMapOf<UUID, Uttaksplan>()
        uttaksgrunnlag.andrePartersBehandling.map {UUID.fromString(it)} .forEach { behandlingUUID ->
            val uttaksplan = uttakRepository.hent(behandlingUUID)
            if (uttaksplan != null) {
                andrePartersUttaksplanerPerBehandling[behandlingUUID] = uttaksplan
            }
        }

        val forrigeUttaksplan = uttakRepository.hentForrige(uttaksgrunnlag.saksnummer, UUID.fromString(uttaksgrunnlag.behandlingUUID))
        val regelGrunnlag = GrunnlagMapper.tilRegelGrunnlag(uttaksgrunnlag, andrePartersUttaksplanerPerSak, andrePartersUttaksplanerPerBehandling, forrigeUttaksplan)

        var uttaksplan = UttakTjeneste.uttaksplan(regelGrunnlag)
        if (forrigeUttaksplan != null) {
            uttaksplan = UttaksplanMerger.slåSammenUttaksplaner(forrigeUttaksplan, uttaksplan, regelGrunnlag.trukketUttak)
        }
        uttaksplan = EndringsstatusOppdaterer.oppdater(forrigeUttaksplan, uttaksplan)

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
    fun hentUttaksplanForBehandling(@RequestParam behandlingUUID: BehandlingUUID, @RequestParam slåSammenLikePerioder: Boolean = false): ResponseEntity<Uttaksplan> {
        logger.info("Henter uttaksplan for behanding=$behandlingUUID slåSammenLikePerioder=$slåSammenLikePerioder")
        val behandlingUUIDParsed = try {
            UUID.fromString(behandlingUUID)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }

        val uttaksplan = uttakRepository.hent(behandlingUUIDParsed) ?: return ResponseEntity.noContent().build()
        if (slåSammenLikePerioder) {
            return ResponseEntity.ok(uttaksplan.slåSammenLikePerioder())
        }
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
        logger.info("Henter forenklet uttaksplan for behanding=$behandlingUUID")
        val behandlingUUIDParsed = try {
            UUID.fromString(behandlingUUID)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
        val uttaksplan = uttakRepository.hent(behandlingUUIDParsed) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(uttaksplan.tilForenkletUttaksplan())
    }

    @DeleteMapping(UttaksplanPath, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        description = "Slett siste uttaksplan for behandling.",
        parameters = [
            Parameter(name ="behandlingUUID", description = "UUID for behandling hvor siste uttaksplan som skal slettes.")
        ]
    )
    fun slettUttaksplab(@RequestParam behandlingUUID: BehandlingUUID): ResponseEntity<Unit> {
        logger.info("Sletter uttaksplan for behanding=$behandlingUUID")
        val behandlingUUIDParsed = try {
            UUID.fromString(behandlingUUID)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
        uttakRepository.slett(behandlingUUIDParsed)
        return ResponseEntity.noContent().build()
    }

}

