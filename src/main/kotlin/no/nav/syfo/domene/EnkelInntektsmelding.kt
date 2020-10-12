package no.nav.syfo.domene

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.inntektsmeldingkontrakt.PengeSerialiserer
import java.math.BigDecimal

data class EnkelInntektsmelding @JsonCreator constructor(

    /** UUID Generert av inntektsmelding mottak*/
    @JsonProperty("inntektsmeldingId")
    val inntektsmeldingId: String,

    /** Arbeidstakers fødselsnummer/dnr  */
    @JsonProperty("arbeidstakerFnr")
    val arbeidstakerFnr: String,

    /** Oppgi inntekt som samsvarer med folketrygdloven § 8-28. Oppgis som månedsbeløp. Beløp med to desimaler.
     * Det skal alltid opplyses full lønn.  */
    @field: JsonSerialize(using = PengeSerialiserer::class)
    @JsonProperty("beregnetInntekt")
    val beregnetInntekt: BigDecimal? = null
)
