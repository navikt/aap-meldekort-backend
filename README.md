# aap-meldekort-backend


## Arkitektur
```

                +------------------+
          +---->| http-flate       |
         /      +------------------+
        /                   |
       /                    v 
+-----+         +------------------+
| app |-------->| meldekortdomene  |
+-----+         +------------------+   
       \                    ^     ^   
        \                   |     | 
         \      +--------------+ +----------+ 
          +---->| repositories | | gateways |
                +--------------+ +----------+
           
         
         
```


## Analyse av den generelle meldekortløsnignen
Består av to tjenester:
- https://github.com/navikt/meldekort-frontend
- https://github.com/navikt/meldekort-api
 
Og benytter følgende fellestjenester:
 
- For innsending av meldekort: https://github.com/navikt/meldekortkontroll-api
- For informasjon om meldekort: https://github.com/navikt/meldekortservice

### Meldekort-api

### Meldekort-frontend

## Ny løsning for dagpenger
Består av tre tjenester:
- https://github.com/navikt/dp-rapportering-frontend
- https://github.com/navikt/dp-rapportering
- https://github.com/navikt/dp-arena-meldeplikt-adapter

Og benytter følgende fellestjenester:

- For innsending av meldekort: https://github.com/navikt/meldekortkontroll-api
- For informasjon om meldekort: https://github.com/navikt/meldekortservice
 
### dp-arena-meldplikt-adapter

#### `GET /harmeldeplikt`: 
Implementasjon:
Medlemmet har meldeplikt for dagpenger hvis
`GET https://meldekortservice/v2/meldegrupper` inneholder en meldegruppe
med `meldegruppeKode` lik `DAGP`.

#### `GET /rapporteringsperioder`:

Implementasjon:
```
GET http://meldekortservice/v2/meldekort
| .meldekortListe
| filter hoyesteMeldegruppe ∈ { "ARBS", "DAGP" }
| filter beregningstatus ∈ { "OPPRE", "SENDT" }
| extend with
    .kanSendesFra := .tilDato - 1 dag
    .periode := [.fraDato, .tilDato]
    .kanSendes := kanSendesFra <= now
    .kanIkkeEndres := 
        .kortType == KORRIGERT_ELEKTRONISK
        || .beregningsstatus == "UBEHA"
        || ∃ ml ∈ .meldekortList ( mk.meldeperiode = .meldeperiode && mk.kortType == KORRIGERT_ELEKTRONISK)
    .kanEndres := !.kanIkkeEndres
    .status := TilUtfylling
```

#### `GET /person`
Implementasjon: `GET http://meldekortservice/v2/meldekort`

#### `GET /sendterapporteringsperioder`
Implementasjon:
```
GET http://meldekortservice/v2/historiskemeldekort?antallMeldeperioder=5
| .meldekortListe
| filter hoyesteMeldegruppe ∈ { "ARBS", "DAGP" }
| extend with
    detaljer := GET http://meldekortservice/v2/meldekortdetaljer?meldekortId=${.meldekortId}
    .periode = [.fraDato, .tilDato]
    .kanSendesFra := .tilDato - 1 dag
    .kanSendes := false
    .kanEndres := (samme som for /rapporteringsperioder)
    .status :=
        FERDI, IKKE ⟼ Ferdig
        OVERM ⟼ Endret
        FEIL ⟼ Feilet
        _ ⟼ Innsendt
```

### dp-rapportering

#### `GET /harmeldeplikt`
Implementasjon: `GET http://meldekortservice/harmeldeplikt`

#### `GET /rapporteringsperioder`
Implementasjon:
```
GET dp-arena-meldekort-adapter/rapporteringsperioder
| map justerInnsendingstidspunkt
| filter "minst like høy status som alle perioder i databasen"
| map db.upsert
```

#### `GET /rapporteringsperioder/innsendete`
Implementasjon:
```
GET dp-arena-meldekort-adapter/sendterapporteringsperioder
| group by .periode.fom

```