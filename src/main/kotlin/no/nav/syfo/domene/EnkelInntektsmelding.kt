package no.nav.syfo.domene

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.PengeSerialiserer
import java.math.BigDecimal

data class EnkelInntektsmelding @JsonCreator constructor(

    /** UUID Generert av inntektsmelding mottak*/
    @JsonProperty("inntektsmeldingId")
    val inntektsmeldingId: String,

    /** Arbeidstakers fødselsnummer/dnr  */
    @JsonProperty("arbeidstakerFnr")
    val arbeidstakerFnr: String,

    /** Arbeidstakers aktørId */
    @JsonProperty("arbeidstakerAktorId")
    val arbeidstakerAktorId: String,

    /** Virksomhetsnummer for den virksomheten arbeidstaker er knyttet til (har arbeidsforhold hos)
     * Denne skal ha verdi hvis arbeidsgivertype er virksomhet */
    @JsonProperty("virksomhetsnummer")
    val virksomhetsnummer: String? = null,

    /** Arbeidsgivers fødselsnummer/dnr
     * Denne skal ha verdi hvis arbeidsgiver er en privatperson */
    @JsonProperty("arbeidsgiverFnr")
    val arbeidsgiverFnr: String? = null,

    /** Arbeidsgivers aktørId
     * Denne skal ha verdi hvis arbeidsgiver er en privatperson */
    @JsonProperty("arbeidsgiverAktorId")
    val arbeidsgiverAktorId: String? = null,

    /** Er arbeidsgiver en organisasjon (identifisert med virksomhetsnummer), eller en privatperson (identifisert med fnr/aktørId) */
    @JsonProperty("arbeidsgivertype")
    val arbeidsgivertype: Arbeidsgivertype,

    /** Oppgi inntekt som samsvarer med folketrygdloven § 8-28. Oppgis som månedsbeløp. Beløp med to desimaler.
     * Det skal alltid opplyses full lønn.  */
    @field: JsonSerialize(using = PengeSerialiserer::class)
    @JsonProperty("beregnetInntekt")
    val beregnetInntekt: BigDecimal? = null
)
