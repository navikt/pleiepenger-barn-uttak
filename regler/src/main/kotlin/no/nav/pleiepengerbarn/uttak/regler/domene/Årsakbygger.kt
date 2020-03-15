package no.nav.pleiepengerbarn.uttak.regler.domene

import no.nav.pleiepengerbarn.uttak.kontrakter.*

typealias HjemmeloppsamlingId = String

internal class Årsaksbygger {
    private val oppsamledeHjemler = mutableMapOf<HjemmeloppsamlingId, MutableSet<Hjemmel>>()
    private val innvilgetÅrsaker = mutableMapOf<InnvilgetÅrsaker, MutableSet<Hjemmel>>()
    private val avslåttÅrsaker = mutableMapOf<AvslåttÅrsaker, MutableSet<Hjemmel>>()

    internal fun hjemmel(hjemmeloppsamlingId: HjemmeloppsamlingId, hjemmel: Hjemmel) : Årsaksbygger {
        oppsamledeHjemler[hjemmeloppsamlingId] = oppsamledeHjemler
                .eksisterendeHjemler(hjemmeloppsamlingId)
                .og(hjemmel)
        return this
    }

    internal fun hjemler(hjemmeloppsamlingId: HjemmeloppsamlingId, hjemler: Set<Hjemmel>) : Årsaksbygger {
        oppsamledeHjemler[hjemmeloppsamlingId] = oppsamledeHjemler
                .eksisterendeHjemler(hjemmeloppsamlingId)
                .og(hjemler)
        return this
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

    internal fun hjemmel(årsak: InnvilgetÅrsaker, hjemmel: Hjemmel) : Årsaksbygger {
        innvilgetÅrsaker[årsak] = innvilgetÅrsaker
                .eksisterendeHjemler(årsak)
                .og(hjemmel)
        return this
    }

    internal fun hjemmel(årsak: AvslåttÅrsaker, hjemmel: Hjemmel) : Årsaksbygger {
        avslåttÅrsaker[årsak] = avslåttÅrsaker
                .eksisterendeHjemler(årsak)
                .og(hjemmel)
        return this
    }

    private fun valider() {
        check(oppsamledeHjemler.isEmpty()) { "Finnes oppsamlede hjemler som ikke er innvilget/avslått med id'er ${oppsamledeHjemler.keys.joinToString()}}" }
        check(!(innvilgetÅrsaker.isEmpty() && avslåttÅrsaker.isEmpty())) { "Finnes hverken innvilget- eller avslåttårsaker for perioden." }
        check(!(innvilgetÅrsaker.isNotEmpty() && avslåttÅrsaker.isNotEmpty())) { "Finnes både innvilget- og avslåttårsaker for perioden." }
    }

    private fun erInnvilget() = innvilgetÅrsaker.isNotEmpty()

    internal fun bygg() : Set<Årsak>{
        valider()
        return if (erInnvilget()) {
            byggInnvilgetÅrsaker()
        } else {
            byggAvslåttÅrsaker()
        }
    }

    internal fun byggAvslåttÅrsaker() : Set<AvslåttÅrsak> {
        valider()
        val årsaker = mutableSetOf<AvslåttÅrsak>()

        avslåttÅrsaker.forEach { (årsak, hjemler) ->
            årsaker.add(AvslåttÅrsak(
                    årsak = årsak,
                    hjemler = hjemler
            ))
        }
        return årsaker.toSet()
    }

    internal fun byggInnvilgetÅrsaker() : Set<InnvilgetÅrsak> {
        valider()
        val årsaker = mutableSetOf<InnvilgetÅrsak>()

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
