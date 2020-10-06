package no.nav.syfo.testutil

import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

fun settOppInntektsmelding(fnr: String) = Inntektsmelding(
    inntektsmeldingId = "1",
    status = Status.GYLDIG,
    beregnetInntekt = BigDecimal(50000),
    arbeidstakerFnr = fnr,
    arbeidstakerAktorId = "aktorID",
    mottattDato = LocalDateTime.now(),
    arbeidsgivertype = Arbeidsgivertype.PRIVAT,
    foersteFravaersdag = LocalDate.now().minusDays(10),
    arbeidsgiverperioder = listOf(
        Periode(
            fom = LocalDate.now().minusDays(10),
            tom = LocalDate.now()
        )
    ),
    arkivreferanse = "999",
    refusjon = Refusjon(
        beloepPrMnd = BigDecimal(1000),
        opphoersdato = LocalDate.now()
    ),
    endringIRefusjoner = emptyList(),
    ferieperioder = emptyList(),
    gjenopptakelseNaturalytelser = emptyList(),
    opphoerAvNaturalytelser = emptyList()
)
