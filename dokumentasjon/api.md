# API

## Opprette uttaksplan

### Request
#### POST /uttaksplan

```json
{
  "ytelseType": "PSB",
  "barn": {
    "aktørId": "123"
  },
  "søker": {
    "aktørId": "456"
  },
  "saksnummer": "pqwert",
  "behandlingUUID": "474abb91-0e61-4459-ba5f-7e960d45c165",
  "søktUttak": [
    {
      "periode": "2021-01-01/2021-01-10",
      "oppgittTilsyn": "PT7H30M"
    }
  ],
  "arbeid": [
    {
      "arbeidsforhold": {
        "type": "AT",
        "organisasjonsnummer": "123456789"
      },
      "perioder": {
        "2021-01-01/2021-01-10": {
          "jobberNormalt": "PT7H30M",
          "jobberNå": "PT0H"
        }
      }
    }
  ],
  "pleiebehov": {
    "2021-01-01/2021-01-10": "100"
  },
  "inngangsvilkår": {
    "K9_VK_1": [
      {
        "periode": "2021-01-01/2021-01-10",
        "utfall": "OPPFYLT"
      }
    ]
  },
  "kravprioritetForBehandlinger": {
    "2021-01-01/2021-01-10": [
      "474abb91-0e61-4459-ba5f-7e960d45c165"
    ]
  }
  
}
```




### Response
HTTP 201

```json
{
  "perioder": {
    "2021-01-01/2021-01-01": {
      "utfall": "OPPFYLT",
      "uttaksgrad": 100,
      "utbetalingsgrader": [
        {
          "arbeidsforhold": {
            "type": "AT",
            "organisasjonsnummer": "123456789",
            "aktørId": null,
            "arbeidsforholdId": null
          },
          "normalArbeidstid": "PT7H30M",
          "faktiskArbeidstid": "PT0S",
          "utbetalingsgrad": 100
        }
      ],
      "søkersTapteArbeidstid": 100,
      "oppgittTilsyn": "PT7H30M",
      "årsaker": [
        "FULL_DEKNING"
      ],
      "inngangsvilkår": {
        "K9_VK_1": "OPPFYLT"
      },
      "pleiebehov": 100,
      "graderingMotTilsyn": {
        "etablertTilsyn": 0,
        "overseEtablertTilsynÅrsak": null,
        "andreSøkeresTilsyn": 0,
        "andreSøkeresTilsynReberegnet": false,
        "tilgjengeligForSøker": 100
      },
      "knekkpunktTyper": [],
      "kildeBehandlingUUID": "474abb91-0e61-4459-ba5f-7e960d45c165",
      "annenPart": "ALENE",
      "nattevåk": null,
      "beredskap": null,
      "endringsstatus": "NY",
      "utenlandsoppholdUtenÅrsak": false,
      "søkersTapteTimer": "PT7H30M"
    },
    "2021-01-04/2021-01-08": {
      "utfall": "OPPFYLT",
      "uttaksgrad": 100,
      "utbetalingsgrader": [
        {
          "arbeidsforhold": {
            "type": "AT",
            "organisasjonsnummer": "123456789",
            "aktørId": null,
            "arbeidsforholdId": null
          },
          "normalArbeidstid": "PT7H30M",
          "faktiskArbeidstid": "PT0S",
          "utbetalingsgrad": 100
        }
      ],
      "søkersTapteArbeidstid": 100,
      "oppgittTilsyn": "PT7H30M",
      "årsaker": [
        "FULL_DEKNING"
      ],
      "inngangsvilkår": {
        "K9_VK_1": "OPPFYLT"
      },
      "pleiebehov": 100,
      "graderingMotTilsyn": {
        "etablertTilsyn": 0,
        "overseEtablertTilsynÅrsak": null,
        "andreSøkeresTilsyn": 0,
        "andreSøkeresTilsynReberegnet": false,
        "tilgjengeligForSøker": 100
      },
      "knekkpunktTyper": [],
      "kildeBehandlingUUID": "474abb91-0e61-4459-ba5f-7e960d45c165",
      "annenPart": "ALENE",
      "nattevåk": null,
      "beredskap": null,
      "endringsstatus": "NY",
      "utenlandsoppholdUtenÅrsak": false,
      "søkersTapteTimer": "PT7H30M"
    }
  },
  "trukketUttak": []
}
```

## Slette uttaksplan
Ved å opprette en ny uttaksplan på en `behandlingId` det allerede finnes en uttaksplan for vil i praksis den forrige uttaksplanen bli slettet (POST-endepunktet over).
Dette endepunktet tilbyr å slette en uttaksplan uten å erstatte det med en ny. 

Det vil fortsatt være mulig å hente opp uttaksplanen på behandlingsnivå, men den vil ikke lenger bli brukt for å sammenstille uttaksplan for en sak.

### Request
DELETE /uttaksplan/{behandlingId}

### Response
HTTP 204

## Henting av uttaksplan
Hente uttaksplaner enten på behandling eller saksnivå.
Kan ikke kombineres ved å sende `behandlingId` og `saksnummer` om hverandre i samme request.
Optional fom/tom

### Request
GET /uttaksplan?behandlingId=123&behandlingId=456&fom=2020-01-01&om=2020-02-01

GET /uttaksplan?sakssnummer=789&saksnummer=101112&fom=2020-01-01&om=2020-02-01

### Response

```json
{
  "fom": "2019-12-24", // Faktiske fom så consumer vet om det finnes mer før
  "tom": "2021-02-01", // Faktiske tom så consumer vet om det finnes mer etter
  "uttaksplaner": {
    "{behandlingId}/{saksnummer}": {
      // Samme format som response ved opprettelse
    },
    "{behandlingId}/{saksnummer}": {
      // Samme format som response ved opprettelse
    }
  }
}
```
