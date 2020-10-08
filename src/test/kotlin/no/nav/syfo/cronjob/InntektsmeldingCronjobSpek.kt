package no.nav.syfo.cronjob

import io.ktor.util.KtorExperimentalAPI
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.syfo.db.finnInntektsmeldinger
import no.nav.syfo.db.lagreInntektsmelding
import no.nav.syfo.domene.tilEnkelInntektsmelding
import no.nav.syfo.inntektsmelding.cronjob.inntektsmeldingCronjob
import no.nav.syfo.testutil.TestDB
import no.nav.syfo.testutil.settOppInntektsmelding
import org.amshove.kluent.`should be equal to`
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@KtorExperimentalAPI
object InntektsmeldingCronjobSpek : Spek({
    val testDb = TestDB()

    val grunntid = ZonedDateTime.of(2020, 10, 6, 3, 3, 0, 0, ZoneId.of("Europe/Oslo"))

    val fnr = "12345678901"
    val inntektsmelding = settOppInntektsmelding(fnr)
        .copy(inntektsmeldingId = "742d3156-0162-43c7-89bd-3dcfd42e5432")
    val inntektsmeldingSomSkalSlettes = settOppInntektsmelding(fnr)
        .copy(inntektsmeldingId = "742d3156-0162-43c7-89bd-3dcfd42e5433")

    describe("Tester sletting av inntektsmeldinger") {
        it("Vi mocker") {
            mockkStatic(Instant::class)
        }

        it("Vi oppretter en inntektsmelding nå") {
            every {
                Instant.now()
            } returns grunntid.toInstant()
            testDb.lagreInntektsmelding(inntektsmelding.tilEnkelInntektsmelding())
            testDb.finnInntektsmeldinger(fnr = fnr).size `should be equal to` 1
        }

        it("Vi kjører cronjob neste dag, og ingenting slettes") {
            every {
                Instant.now()
            } returns grunntid.plusDays(1).toInstant()

            val (slettet) = inntektsmeldingCronjob(
                database = testDb
            )
            slettet `should be equal to` 0
            testDb.finnInntektsmeldinger(fnr = fnr).size `should be equal to` 1
        }

        it("Vi kjører cronjob etter 1 og 1/2 år, og ingenting slettes") {
            every {
                Instant.now()
            } returns grunntid.plusDays(365 + 182).toInstant()

            val (slettet) = inntektsmeldingCronjob(
                database = testDb
            )
            slettet `should be equal to` 0
            testDb.finnInntektsmeldinger(fnr = fnr).size `should be equal to` 1
        }

        it("Vi oppretter en inntektsmelding for 2 år siden") {
            every {
                Instant.now()
            } returns grunntid.minusYears(2).toInstant()
            testDb.lagreInntektsmelding(inntektsmeldingSomSkalSlettes.tilEnkelInntektsmelding())
            testDb.finnInntektsmeldinger(fnr = fnr).size `should be equal to` 2
        }

        it("Vi kjører cronjob nå, og en inntektsmelding slettes") {
            every {
                Instant.now()
            } returns grunntid.toInstant()
            val (slettet) = inntektsmeldingCronjob(
                database = testDb
            )
            slettet `should be equal to` 1
            testDb.finnInntektsmeldinger(fnr = fnr).size `should be equal to` 1
        }

        it("Vi avmocker") {
            unmockkStatic(Instant::class)
        }
    }
})
