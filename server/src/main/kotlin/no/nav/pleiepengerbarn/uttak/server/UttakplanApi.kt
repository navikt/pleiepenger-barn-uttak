package no.nav.pleiepengerbarn.uttak.server

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.annotation.PostConstruct
import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.*
import no.nav.pleiepengerbarn.uttak.regler.mapper.GrunnlagMapper
import no.nav.pleiepengerbarn.uttak.server.db.UttakRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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

    @Value("\${git.commit.id:}")
    private val commitId: String? = null

    private var utvidetLogging: Boolean = false

    companion object {
        const val UttaksplanPath = "/uttaksplan"
        const val UttaksplanSimuleringPath = "/uttaksplan/simulering"
        const val UttaksplanSimuleringSluttfasePath = "/uttaksplan/simuleringLivetsSluttfase"
        const val UttaksplanNedjusterSøkersUttaksgradPath = "/uttaksplan/nedjusterUttaksgrad"

        const val BehandlingUUID = "behandlingUUID"

        private val logger = LoggerFactory.getLogger(this::class.java)

    }

    init {
        utvidetLogging = System.getenv("UTVIDET_LOGGING").toBoolean()
    }

    @PostMapping(
        UttaksplanPath,
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(description = "Opprette en ny uttaksplan. Tar inn grunnlaget som skal tas med i betraktning for å utlede uttaksplanen.")
    fun opprettUttaksplan(
        @RequestBody uttaksgrunnlag: Uttaksgrunnlag,
        uriComponentsBuilder: UriComponentsBuilder
    ): ResponseEntity<Uttaksplan> {
        val logMelding = if (utvidetLogging) {
            "Opprett uttaksplan for behandling=${uttaksgrunnlag.behandlingUUID} grunnlag=$uttaksgrunnlag"
        } else {
            "Opprett uttaksplan for behandling=${uttaksgrunnlag.behandlingUUID}"
        }
        logger.info(logMelding)
        uttaksgrunnlag.valider()
        val forrigeUttaksplan =
            uttakRepository.hentForrige(uttaksgrunnlag.saksnummer, UUID.fromString(uttaksgrunnlag.behandlingUUID))
        val nyUttaksplan = lagUttaksplan(uttaksgrunnlag, forrigeUttaksplan, true)
        val uri =
            uriComponentsBuilder.path(UttaksplanPath).queryParam(BehandlingUUID, uttaksgrunnlag.behandlingUUID).build()
                .toUri()

        if (utvidetLogging) {
            logger.info("Resultat for behandling=${uttaksgrunnlag.behandlingUUID} uttaksplan=$nyUttaksplan")
        }
        return ResponseEntity
            .created(uri)
            .body(nyUttaksplan)
    }

    @Deprecated("Bruk den andre simulerUttaksplan istedet")
    @PostMapping(
        UttaksplanSimuleringPath,
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(description = "Simuler opprettelse av en ny uttaksplan. Tar inn grunnlaget som skal tas med i betraktning for å utlede uttaksplanen.")
    fun simulerUttaksplan(
        @RequestBody uttaksgrunnlag: Uttaksgrunnlag,
        uriComponentsBuilder: UriComponentsBuilder
    ): ResponseEntity<Simulering> {
        val logMelding = if (utvidetLogging) {
            "Simulerer uttaksplan(PSB) for behandling=${uttaksgrunnlag.behandlingUUID} grunnlag=$uttaksgrunnlag"
        } else {
            "Simulerer uttaksplan(PSB) for behandling=${uttaksgrunnlag.behandlingUUID}"
        }
        logger.info(logMelding)
        val simulering = simuler(uttaksgrunnlag)
        if (utvidetLogging) {
            logger.info("Simulering for ${uttaksgrunnlag.behandlingUUID} simulering=$simulering")
        }
        return ResponseEntity.ok(simulering)
    }

    @PostMapping(
        UttaksplanSimuleringSluttfasePath,
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(description = "Simuler opprettelse av en ny uttaksplan for livets sluttfase. Tar inn grunnlaget som skal tas med i betraktning for å utlede uttaksplanen.")
    fun simulerUttaksplanForLivetsSluttfase(
        @RequestBody uttaksgrunnlag: Uttaksgrunnlag,
        uriComponentsBuilder: UriComponentsBuilder
    ): ResponseEntity<SimuleringLivetsSluttfase> {
        logger.info("Simulerer uttaksplan(PLS) for behandling=${uttaksgrunnlag.behandlingUUID}")
        val simulering = simuler(uttaksgrunnlag)
        val erKvotenBruktOpp = BeregnBruktKvote.erKvotenOversteget(
            simulering.simulertUttaksplan,
            hentAndrePartersUttaksplanerPerBehandling(uttaksgrunnlag)
        )
        return ResponseEntity.ok(
            SimuleringLivetsSluttfase(
                simulering.forrigeUttaksplan, simulering.simulertUttaksplan,
                simulering.uttakplanEndret, erKvotenBruktOpp.first, erKvotenBruktOpp.second
            )
        )
    }

    private fun simuler(uttaksgrunnlag: Uttaksgrunnlag): Simulering {
        uttaksgrunnlag.valider()
        val forrigeUttaksplan = uttakRepository.hent(UUID.fromString(uttaksgrunnlag.behandlingUUID))
        val simulertUttaksplan = lagUttaksplan(uttaksgrunnlag, forrigeUttaksplan, false)
        val uttaksplanEndret = SimuleringTjeneste.erResultatEndret(forrigeUttaksplan, simulertUttaksplan)
        return Simulering(forrigeUttaksplan, simulertUttaksplan, uttaksplanEndret)
    }

    private fun lagUttaksplan(
        uttaksgrunnlag: Uttaksgrunnlag,
        forrigeUttaksplan: Uttaksplan?,
        lagre: Boolean
    ): Uttaksplan {
        val andrePartersUttaksplanerPerBehandling = hentAndrePartersUttaksplanerPerBehandling(uttaksgrunnlag)
        val vedtatteUttaksplanerPerBehandling = hentVedtatteUttaksplanerPerBehandling(uttaksgrunnlag)

        val regelGrunnlag =
            GrunnlagMapper.tilRegelGrunnlag(
                uttaksgrunnlag,
                andrePartersUttaksplanerPerBehandling,
                vedtatteUttaksplanerPerBehandling,
                forrigeUttaksplan
            )

        var uttaksplan = UttakTjeneste.uttaksplan(regelGrunnlag)
        if (forrigeUttaksplan != null) {
            uttaksplan =
                UttaksplanMerger.slåSammenUttaksplaner(forrigeUttaksplan, uttaksplan, regelGrunnlag.trukketUttak)
        }
        uttaksplan = EndringsstatusOppdaterer.oppdater(forrigeUttaksplan, uttaksplan)

        if (lagre) {
            uttakRepository.lagre(regelGrunnlag, uttaksplan)
        }

        return uttaksplan
    }

    private fun hentAndrePartersUttaksplanerPerBehandling(uttaksgrunnlag: Uttaksgrunnlag): Map<UUID, Uttaksplan> {
        val unikeBehandlinger =
            uttaksgrunnlag.kravprioritetForBehandlinger.values.flatten()
                .toSet()
                .map { UUID.fromString(it) }
        val andrePartersUttaksplanerPerBehandling = mutableMapOf<UUID, Uttaksplan>()

        unikeBehandlinger.forEach { behandlingUUID ->
            val uttaksplan = uttakRepository.hent(behandlingUUID)
            if (uttaksplan != null) {
                andrePartersUttaksplanerPerBehandling[behandlingUUID] = uttaksplan
            }
        }
        return andrePartersUttaksplanerPerBehandling
    }

    private fun hentVedtatteUttaksplanerPerBehandling(uttaksgrunnlag: Uttaksgrunnlag): Map<UUID, Uttaksplan> {
        val vedtatteBehandlinger = uttaksgrunnlag.sisteVedtatteUttaksplanForBehandling.filterValues { it != null }
            .values.toSet()
            .map { UUID.fromString(it) }
        val andrePartersUttaksplanerPerBehandling = mutableMapOf<UUID, Uttaksplan>()

        vedtatteBehandlinger.forEach { behandlingUUID ->
            if (!andrePartersUttaksplanerPerBehandling.containsKey(behandlingUUID)) {
                val uttaksplan = uttakRepository.hent(behandlingUUID)
                if (uttaksplan != null) {
                    andrePartersUttaksplanerPerBehandling[behandlingUUID] = uttaksplan
                }
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
    fun hentUttaksplanForBehandling(
        @RequestParam behandlingUUID: BehandlingUUID,
        @RequestParam slåSammenLikePerioder: Boolean = false
    ): ResponseEntity<Uttaksplan> {
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
            Parameter(
                name = "behandlingUUID",
                description = "UUID for behandling hvor siste uttaksplan som skal slettes."
            )
        ]
    )
    fun slettUttaksplan(@RequestParam behandlingUUID: BehandlingUUID): ResponseEntity<Unit> {
        logger.info("Sletter uttaksplan for behandling=$behandlingUUID")
        val behandlingUUIDParsed = try {
            UUID.fromString(behandlingUUID)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
        uttakRepository.slett(behandlingUUIDParsed)
        return ResponseEntity.noContent().build()
    }

    @PostConstruct
    fun init() {
        logger.info("GIT commit id: $commitId")
    }

}

