package no.nav.pleiepengerbarn.uttak.regler.domene

import no.nav.pleiepengerbarn.uttak.kontrakter.*

typealias HjemmeloppsamlingId = String

internal class Årsaksbygger {
    private val oppsamledeHjemler = mutableMapOf<HjemmeloppsamlingId, MutableSet<Hjemmel>>()
    private var innvilgetÅrsaker = mutableMapOf<InnvilgetÅrsaker, MutableSet<Hjemmel>>()
    private var avslåttÅrsaker = mutableMapOf<AvslåttÅrsaker, MutableSet<Hjemmel>>()

    internal fun hjemmel(hjemmeloppsamlingId: HjemmeloppsamlingId, hjemmel: Hjemmel) {
        oppsamledeHjemler[hjemmeloppsamlingId] = oppsamledeHjemler
                .eksisterendeHjemler(hjemmeloppsamlingId)
                .og(hjemmel)
    }

    internal fun innvilget(hjemmeloppsamlingId: HjemmeloppsamlingId, årsak: InnvilgetÅrsaker) {
        val hjemler = oppsamledeHjemler[hjemmeloppsamlingId] ?: throw IllegalStateException("Ingen hjemler for $hjemmeloppsamlingId")
        innvilgetÅrsaker[årsak] = innvilgetÅrsaker
                .eksisterendeHjemler(årsak)
                .og(hjemler)
        oppsamledeHjemler.remove(hjemmeloppsamlingId)
    }

    internal fun avslått(hjemmeloppsamlingId: HjemmeloppsamlingId, årsak: AvslåttÅrsaker) {
        val hjemler = oppsamledeHjemler[hjemmeloppsamlingId] ?: throw IllegalStateException("Ingen hjemler for $hjemmeloppsamlingId")
        avslåttÅrsaker[årsak] = avslåttÅrsaker
                .eksisterendeHjemler(årsak)
                .og(hjemler)
        oppsamledeHjemler.remove(hjemmeloppsamlingId)
    }

    internal fun hjemmel(årsak: InnvilgetÅrsaker, hjemmel: Hjemmel) {
        innvilgetÅrsaker[årsak] = innvilgetÅrsaker
                .eksisterendeHjemler(årsak)
                .og(hjemmel)
    }

    internal fun hjemmel(årsak: AvslåttÅrsaker, hjemmel: Hjemmel) {
        avslåttÅrsaker[årsak] = avslåttÅrsaker
                .eksisterendeHjemler(årsak)
                .og(hjemmel)
    }

    private fun valider() {
        check(oppsamledeHjemler.isEmpty()) { "Finnes oppsamlede hjemler som ikke er innvilget/avslått med id'er ${oppsamledeHjemler.keys.joinToString()}}" }
        check(!(innvilgetÅrsaker.isEmpty() && avslåttÅrsaker.isEmpty())) { "Finnes hverken innvilget- eller avslåttårsaker for perioden." }
        check(!(innvilgetÅrsaker.isNotEmpty() && avslåttÅrsaker.isNotEmpty())) { "Finnes både innvilget- og avslåttårsaker for perioden." }
    }

    internal fun årsaker() : Set<Årsak>{
        valider()
        val årsaker = mutableSetOf<Årsak>()
        avslåttÅrsaker.forEach { (årsak, hjemler) ->
            årsaker.add(AvslåttÅrsak(
                    årsak = årsak,
                    hjemler = hjemler
            ))
        }
        innvilgetÅrsaker.forEach { (årsak, hjemler) ->
            årsaker.add(InnvilgetÅrsak(
                    årsak = årsak,
                    hjemler = hjemler
            ))
        }
        return årsaker.toSet()
    }
}

private fun <K> MutableMap<K, MutableSet<Hjemmel>>.eksisterendeHjemler(key: K) = getOrDefault(key, mutableSetOf())


private fun MutableSet<Hjemmel>.og(hjemmel: Hjemmel): MutableSet<Hjemmel> {
    add(hjemmel)
    return this
}

private fun MutableSet<Hjemmel>.og(hjemler: Set<Hjemmel>): MutableSet<Hjemmel> {
    addAll(hjemler)
    return this
}
