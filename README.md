# Pleiepenger barn uttak

## Slik regner vi grad av ytelse
Dette regnestykket gjøres for hver `periode`. En `periode` kan være alt fra en enkeltdag til en lengre periode. Hvordan vi kommer frem til disse periodene er ikke en del av denne forklaringen.

1. Fastsetter `antall virkedager` i perioden. Det hensyntas ikke helligdager, så er kun antall virkedager (mandag-fredag)
2. Fastseter `antall virketimer` i perioden. Dette fastsetets ved å ta `antall virkedager` og ganger med 7 timer og 30 minutter.
3. Fastsetter `tilsynsgrad`. Dette fastsettes ved å ta antall timer barnet er i en `tilsynsordning` og deler på `antall virketimer`
4. Fastsetter `pleiepengegrad` som fastsettes ved 100 minus `tilsynsgrad`
5. Fastsetter `takForYtelsePåGrunnAvTilsynsgrad`  
    - Om `tilsynsgrad` er under 10% settes den til 100%
    - Om `tilsynsgrad` er over 80% settes den til 0%
    - I alle andre tilfeller settes den ved å ta 100 minus `tilsynsgrad`
6. Fastsetter `maksimaltAntallVirketimerViKanGiYtelseForIPerioden`. Dette fastsettes ved å å ta `antallVirketimerIPerioden` og ganger med `takForYtelsePåGrunnAvTilsynsgrad`
7. Summerer `fraværIPerioden` på tvers av alle arbeidsforhold.
8. Beregner `grad av ytelse` 
    - Om `tilsynsgrad` er over 80% får man 0%
    - Om `fraværIPerioden` er større enn `antallVirketimerIPerioden` blir graden lik `takForYtelsePåGrunnAvTilsynsgrad`
    - I alle andre tilfeller må vi avklare `avkortetFraværIPerioden`. Dette er det samme som `fraværIPerioden`, men avkortet mot `maksimaltAntallVirketimerViKanGiYtelseForIPerioden`. Om f.eks. `fraværIPerioden` er 30 timer, men `maksimaltAntallVirketimerViKanGiYtelseForIPerioden` er 20 timer er det sistnevnte som blir `avkortetFraværIPerioden`. Videre finner vi da `grad av ytelse` ved å ta `avkortetFraværIPerioden` delt på `antallVirketimerIPerioden`
9. Avkorter `grad av ytelse` mot `tilsynsgrad` og andre omsorgspersoner perioden.
    - Fastsetter `tilsynsbehov` i perioden som er enten 100% eller 200% 
    - Finner andre omsorgspersoner sin `grad av ytelse` i samme periode.
    - Om `tilsynsbehov` minus `tilsynsgrad` minus det som dekkes av andre omsorgspersoner er større eller lik `grad av ytlese` (regnet i tallpunktet over) gjøres det ingen avkorting. Ellers avkortes det til resultatet av regnetykket.
10. Om resultatet fra forrge tallpunkt er mindre enn 20% blir `grad av ytelse` 0% ellers er har vi kommet frem til din `grad av ytelse` for perioden.

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

Interne henvendelser kan sendes via Slack i kanalen #sykdom-i-familien.
