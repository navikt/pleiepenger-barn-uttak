package no.nav.pleiepengerbarn.uttak.server

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.*
import no.nav.pleiepengerbarn.uttak.regler.mapper.GrunnlagMapper
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
        const val EndringUttaksplanPath = "/uttaksplan/endring"
        const val UttaksplanSimuleringPath = "/uttaksplan/simulering"
        const val UttaksplanSimuleringSluttfasePath = "/uttaksplan/simuleringLivetsSluttfase"
        const val BehandlingUUID = "behandlingUUID"

        private val logger = LoggerFactory.getLogger(this::class.java)

    }

    @PostMapping(UttaksplanPath, consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Opprette en ny uttaksplan. Tar inn grunnlaget som skal tas med i betraktning for å utlede uttaksplanen.")
    fun opprettUttaksplan(
            @RequestBody uttaksgrunnlag: Uttaksgrunnlag,
            uriComponentsBuilder: UriComponentsBuilder): ResponseEntity<Uttaksplan> {
        logger.info("Opprett uttaksplan for behandling=${uttaksgrunnlag.behandlingUUID}")
        uttaksgrunnlag.valider()
        val forrigeUttaksplan = uttakRepository.hentForrige(uttaksgrunnlag.saksnummer, UUID.fromString(uttaksgrunnlag.behandlingUUID))
        val nyUttaksplan = lagUttaksplan(uttaksgrunnlag, forrigeUttaksplan, true)
        val uri = uriComponentsBuilder.path(UttaksplanPath).queryParam(BehandlingUUID, uttaksgrunnlag.behandlingUUID).build().toUri()

        return ResponseEntity
            .created(uri)
            .body(nyUttaksplan)
    }

    @PostMapping(EndringUttaksplanPath, consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Endrer en uttaksplan. Kun enkelte endringer er lovlig, hvis endringen er ulovlig så vil endepunktet returnere en 'bad request'.")
    fun endrePerioder(@RequestBody endrePerioderGrunnlag: EndrePerioderGrunnlag): ResponseEntity<Uttaksplan?> {
        logger.info("Endrer uttaksplan for behandling=${endrePerioderGrunnlag.behandlingUUID}")
        endrePerioderGrunnlag.valider()
        val eksisterendeUttaksplan = uttakRepository.hent(UUID.fromString(endrePerioderGrunnlag.behandlingUUID))
            ?: return ResponseEntity.badRequest().body(null)
        val oppdatertUttaksplan = UttakTjeneste.endreUttaksplan(eksisterendeUttaksplan, endrePerioderGrunnlag.perioderSomIkkeErInnvilget)
        uttakRepository.lagre(endrePerioderGrunnlag, oppdatertUttaksplan)
        return ResponseEntity.ok(oppdatertUttaksplan)
    }

    @Deprecated("Bruk den andre simulerUttaksplan istedet")
    @PostMapping(UttaksplanSimuleringPath, consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Simuler opprettelse av en ny uttaksplan. Tar inn grunnlaget som skal tas med i betraktning for å utlede uttaksplanen.")
    fun simulerUttaksplan(
            @RequestBody uttaksgrunnlag: Uttaksgrunnlag,
            uriComponentsBuilder: UriComponentsBuilder): ResponseEntity<Simulering> {
        logger.info("Simulerer uttaksplan(PSB) for behandling=${uttaksgrunnlag.behandlingUUID}")
        val simulering = simuler(uttaksgrunnlag)
        return ResponseEntity.ok(simulering)
    }

    @PostMapping(UttaksplanSimuleringSluttfasePath, consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Simuler opprettelse av en ny uttaksplan for livets sluttfase. Tar inn grunnlaget som skal tas med i betraktning for å utlede uttaksplanen.")
    fun simulerUttaksplanForLivetsSluttfase(
            @RequestBody uttaksgrunnlag: Uttaksgrunnlag,
            uriComponentsBuilder: UriComponentsBuilder): ResponseEntity<SimuleringLivetsSluttfase> {
        logger.info("Simulerer uttaksplan(PLS) for behandling=${uttaksgrunnlag.behandlingUUID}")
        val simulering = simuler(uttaksgrunnlag)
        val erKvotenBruktOpp = BeregnBruktKvote.erKvotenOversteget(simulering.simulertUttaksplan, hentAndrePartersUttaksplanerPerBehandling(uttaksgrunnlag))
        return ResponseEntity.ok(SimuleringLivetsSluttfase(simulering.forrigeUttaksplan, simulering.simulertUttaksplan,
                simulering.uttakplanEndret, erKvotenBruktOpp.first, erKvotenBruktOpp.second))
    }

    private fun simuler(uttaksgrunnlag: Uttaksgrunnlag): Simulering {
        uttaksgrunnlag.valider()
        val forrigeUttaksplan = uttakRepository.hent(UUID.fromString(uttaksgrunnlag.behandlingUUID))
        val simulertUttaksplan = lagUttaksplan(uttaksgrunnlag, forrigeUttaksplan, false)
        val uttaksplanEndret = SimuleringTjeneste.erResultatEndret(forrigeUttaksplan, simulertUttaksplan)
        return Simulering(forrigeUttaksplan, simulertUttaksplan, uttaksplanEndret)
    }

    private fun lagUttaksplan(uttaksgrunnlag: Uttaksgrunnlag, forrigeUttaksplan: Uttaksplan?, lagre: Boolean): Uttaksplan {
        val andrePartersUttaksplanerPerBehandling = hentAndrePartersUttaksplanerPerBehandling(uttaksgrunnlag)

        val regelGrunnlag = GrunnlagMapper.tilRegelGrunnlag(uttaksgrunnlag, andrePartersUttaksplanerPerBehandling, forrigeUttaksplan)

        var uttaksplan = UttakTjeneste.uttaksplan(regelGrunnlag)
        if (forrigeUttaksplan != null) {
            uttaksplan = UttaksplanMerger.slåSammenUttaksplaner(forrigeUttaksplan, uttaksplan, regelGrunnlag.trukketUttak)
        }
        uttaksplan = EndringsstatusOppdaterer.oppdater(forrigeUttaksplan, uttaksplan)

        if (lagre) {
            uttakRepository.lagre(regelGrunnlag, uttaksplan)
        }

        return uttaksplan
    }

    private fun hentAndrePartersUttaksplanerPerBehandling(uttaksgrunnlag: Uttaksgrunnlag): Map<UUID, Uttaksplan> {
        val unikeBehandlinger = uttaksgrunnlag.kravprioritetForBehandlinger.values.flatten().toSet().map {UUID.fromString(it)}
        val andrePartersUttaksplanerPerBehandling = mutableMapOf<UUID, Uttaksplan>()
        unikeBehandlinger .forEach { behandlingUUID ->
            val uttaksplan = uttakRepository.hent(behandlingUUID)
            if (uttaksplan != null) {
                andrePartersUttaksplanerPerBehandling[behandlingUUID] = uttaksplan
            }
        }
        return andrePartersUttaksplanerPerBehandling
    }

    @GetMapping(UttaksplanPath, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        description = "Hent uttaksplan for gitt behandling.",
        parameters = [
            Parameter(name = "behandlingUUID", description = "UUID for behandling som skal hentes.")
        ]
    )
    fun hentUttaksplanForBehandling(@RequestParam behandlingUUID: BehandlingUUID, @RequestParam slåSammenLikePerioder: Boolean = false): ResponseEntity<Uttaksplan> {
        logger.info("Henter uttaksplan for behandling=$behandlingUUID slåSammenLikePerioder=$slåSammenLikePerioder")
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

    @DeleteMapping(UttaksplanPath, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        description = "Slett siste uttaksplan for behandling.",
        parameters = [
            Parameter(name ="behandlingUUID", description = "UUID for behandling hvor siste uttaksplan som skal slettes.")
        ]
    )
    fun slettUttaksplab(@RequestParam behandlingUUID: BehandlingUUID): ResponseEntity<Unit> {
        logger.info("Sletter uttaksplan for behandling=$behandlingUUID")
        val behandlingUUIDParsed = try {
            UUID.fromString(behandlingUUID)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
        uttakRepository.slett(behandlingUUIDParsed)
        return ResponseEntity.noContent().build()
    }

}

