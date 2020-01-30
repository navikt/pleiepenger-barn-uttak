# API

## Opprettelse av nytt uttak
- 'sakId' ikke brukt til noe funksjonelt i uttak, kun for 
- 'aktuelleBehandlinger' er et sett med andre id på andre behandlingsIder som skal hensyntas ved beregning av uttaket. Alle uttak som hensyntas må være knyttet til det samme barnet.
- 'fødselsdato' på 'søker' for å kunne håndheve at Det ytes ikke stønad til medlem som er fylt 70 år.
- 'tilsynbehov' på 'barn' inneholder periodiserte maksrammer for uttaket på tvers av omsorgspersoner for barnet. Det er kun disse verdiene som vil hensyntas i beregningen av uttaket, ikke tilsvarende verdier som ligger i 'aktuelleBehandlinger'. 

### Request
POST /uttak

```json

{
    "sakId": "123",
    "behandlingId": "345",
    "aktuelleBehandlinger": [
        "12345",
        "3333"
    ],
    "søker": {
        "aktørId": "4552",
        "fødselsdato": "1990-09-09"
    },
    "barn": {
        "aktørId": "123",
        "tilsynbehov" : {
            "2018-10-10/2018-12-29": 200,
            "2018-10-10/2018-12-30": 100
        }
    },
    "søknadsperioder": {
        "2018-10-10/2018-12-29": {},
        "2018-10-10/2018-12-30": {}
    },
    "arbeid": {
        "arbeidstaker": [{
            "organisasjonsnummer": "999999999",
            "norskIdentitetsnummer": null,
            "perioder": {
                "2018-10-10/2018-12-29": {
                    "skalJobbeProsent": 50.25
                }
            }
        }, {
            "organisasjonsnummer": null,
            "norskIdentitetsnummer": "29099012345",
            "perioder": {
                "2018-11-10/2018-12-29": {
                    "skalJobbeProsent": 20.00
                }
            }
        }]
    }
}
```

### Response
HTTP 201

## Endringer
### Request
PUT /uttak/{behandlingId}

Samme som ved opprettelse med noen verdier kan ikke endres;

- aktørId på søker
- aktørId på barn
- sakId

Og visse ting kan være i tillegg til opprettelsen;

- dødsdato på søker
- dødsdato på barn

## Henting av uttak
### Request
GET /uttak?behandlingId=123&behandlingId=456
### Response
```json
{
    "123": {
        "perioder": {
            "2018-10-10/2018-12-29": {
                "grad": 50.0
            },
            "2018-10-10/2018-12-30": {
                "grad": 73.5
            }
        }
    },
    "456": {
        "perioder": {
            "2018-10-10/2018-12-29": {
                "grad": 50.0
            },
            "2018-10-10/2018-12-30": {
                "grad": 73.5
            }
        }
    }
}
```
#### Utvidelsesbehov
- Grad per arbeidsgiver (for beregning)
- Ev. mer info fra grunnlaget avhengig av hva de funksjonelle behovene for hva en saksbehandler skal se i løsningen.
