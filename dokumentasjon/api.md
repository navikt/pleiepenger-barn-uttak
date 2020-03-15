# API

### Request
POST /uttaksplan

```json
{
	"saksnummer": "ABC123",
	"behandlingId": "474abb91-0e61-4459-ba5f-7e960d45c165",
	"andrePartersSaker": [
		"ABC124",
		"ABC125"
	],
	"søker": {
		"fødselsdato": "1990-09-29",
		"dødsdato": "2020-01-01"
	},
	"barn": {
		"dødsdato": "2020-01-01"
	},
	"søknadsperioder": [
		"2020-01-01/2020-03-31"
	],
	"lovbestemtFerie": [
		"2020-01-01/2020-03-10"
	],
	"arbeid": [{
		"arbeidsforhold": {
			"type": "Arbeidstaker",
			"organisasjonsnummer": "999999999",
			"aktørId": null,
			"arbeidsforholdId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
		},
		"perioder": {
			"2020-01-01/2020-03-31": {
				"jobberNormaltPerUke": "PT7H30M",
				"skalJobbeProsent": 20.50
			}
		}
	}],
	"tilsynsbehov": {
		"2020-01-01/2020-03-31": {
			"prosent": 100
		},
		"2020-04-01/2020-04-31": {
			"prosent": 200
		}
	},
	"medlemskap": {
		"2020-01-01/2020-03-31": {}
	}
}
```

### Response
HTTP 201

```json
{
	"perioder": {
		"2020-01-01/2020-03-31": {
			"utfall": "INNVILGET",
			"grad": 100,
			"utbetalingsgrader": [{
				"arbeidsforhold": {
					"type": "Arbeidstaker",
					"organisasjonsnummer": "999999999",
					"aktørId": null,
					"arbeidsforholdId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
				},
				"utbetalingsgrad": 75.00
			}]
		},
		"2020-02-02/2020-02-15": {
			"utfall": "INNVILGET",
			"årsak": "AVKORTET_MOT_INNTEKT",
			"hjemler": [],
			"grad": 80,
			"utbetalingsgrader": [{
				"arbeidsforhold": {
					"type": "Arbeidstaker",
					"organisasjonsnummer": "999999999",
					"aktørId": null,
					"arbeidsforholdId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
				},
				"utbetalingsgrad": 80.00
			}]
		},
		"2020-02-16/2020-02-25": {
			"utfall": "AVSLÅTT",
			"årsaker": [{
				"årsak": "IKKE_MEDLEM_I_FOLKETRYGDEN",
				"hjemler": [{
					"henvisning": "Folketrygdloven LOV-1997-02-28-19 Kapittel 2",
					"anvendelse": "Fastsatt at personen ikke er medlem av folketrygden i perioden."
				}]
			}]
		}
	}
}
```

## Henting av uttak
### Request
GET /uttaksplan?behandlingId=123&behandlingId=456

### Response

```json
{
  "uttaksplaner": {
    "123": {
      // Samme format som response ved opprettelse
    },
    "456": {
      // Samme format som response ved opprettelse
    }
  }
}
```
