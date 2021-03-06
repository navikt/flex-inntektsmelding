package no.nav.syfo.domene

import no.nav.inntektsmeldingkontrakt.Inntektsmelding

fun Inntektsmelding.tilEnkelInntektsmelding(): EnkelInntektsmelding {
    return EnkelInntektsmelding(
        beregnetInntekt = this.beregnetInntekt,
        inntektsmeldingId = this.inntektsmeldingId,
        arbeidstakerFnr = this.arbeidstakerFnr
    )
}
