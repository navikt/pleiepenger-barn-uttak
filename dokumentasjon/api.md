# API

### Request
POST /uttaksplan

```json
{
	"sakId": "ABC123",
	"behandlingId": "474abb91-0e61-4459-ba5f-7e960d45c165",
	"andrePartersBehandlinger": [
	  "474abb91-0e61-4459-ba5f-7e960d45c164", 
	  "474abb91-0e61-4459-ba5f-7e960d45c112"
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
			"arbeidstype": "ARBEIDSGIVER",
			"organisasjonsnummer": "999999999",
			"arbeidsforholdId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
		},
		"perioder": {
			"2020-01-01/2020-03-31": {
				"jobberNormalt": "PT7H30M",
				"skalJobbe": "20"
			}
		}
	}],
	"tilsynsbehov": {
		"2020-01-01/2020-03-31": {
			"prosent": "100"
		},
		"2020-04-01/2020-04-31": {
			"prosent": "200"
		}
	},
	"medlemskap": {
		"2020-01-01/2020-03-31": {
			"frivilligEllerPliktigMedlem": true
		}
	}
}
```

### Response
HTTP 201

```json
{
	"perioder": {
		"2020-01-01/2020-03-31": {
			"type": "innvilget",
			"grad": 100.00,
			"utbetalingsgrader": [{
				"arbeidsforhold": {
					"arbeidstype": "ARBEIDSGIVER",
					"organisasjonsnummer": "999999999",
					"fødselsnummer": null,
					"arbeidsforholdId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
				},
				"utbetalingsgrad": 80.00
			}]
		},
		"2020-02-02/2020-02-15": {
			"type": "innvilget",
			"grad": 80.00,
			"utbetalingsgrader": [{
				"arbeidsforhold": {
					"arbeidstype": "ARBEIDSGIVER",
					"organisasjonsnummer": "999999999",
					"fødselsnummer": null,
					"arbeidsforholdId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
				},
				"utbetalingsgrad": 75.00
			}]
		},
		"2020-02-16/2020-02-25": {
			"type": "avslått",
			"avslagsÅrsaker": ["IKKE_MEDLEM"]
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
