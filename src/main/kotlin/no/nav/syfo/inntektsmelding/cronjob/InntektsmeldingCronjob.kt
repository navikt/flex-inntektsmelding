package no.nav.syfo.inntektsmelding.cronjob

import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.Environment
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.finnInntektsmeldingerEldreEnn18Mnd
import no.nav.syfo.db.slettInntektsmelding
import no.nav.syfo.log
import no.nav.syfo.util.PodLeaderCoordinator
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import kotlin.concurrent.fixedRateTimer

fun inntektsmeldingCronjob(
    database: DatabaseInterface
): InntektsmeldingCronjobResultat {
    val resultat = InntektsmeldingCronjobResultat()

    log.info("Kjører inntektsmelding cronjob")

    val inntektsmeldingerSomSkalSlettes = database.finnInntektsmeldingerEldreEnn18Mnd()

    inntektsmeldingerSomSkalSlettes.forEach { inntektsmelding ->
        database.slettInntektsmelding(inntektsmelding.inntektsmeldingId)
        resultat.slettet++
        log.info("Slettet inntektsmelding ${inntektsmelding.inntektsmeldingId}")
    }

    return resultat
}

data class InntektsmeldingCronjobResultat(
    var slettet: Int = 0
)

@KtorExperimentalAPI
fun settOppInntektsmeldingCronjob(
    podLeaderCoordinator: PodLeaderCoordinator,
    database: DatabaseInterface,
    env: Environment
) {

    val (klokkeslett, period) = hentKlokekslettOgPeriode(env)

    fixedRateTimer(
        startAt = klokkeslett,
        period = period
    ) {
        if (podLeaderCoordinator.isLeader()) {
            inntektsmeldingCronjob(database = database)
        } else {
            log.debug("Jeg er ikke leder")
        }
    }
}

private fun hentKlokekslettOgPeriode(env: Environment): Pair<Date, Long> {
    val osloTz = ZoneId.of("Europe/Oslo")
    val now = ZonedDateTime.now(osloTz)
    if (env.cluster == "dev-gcp" || env.cluster == "flex") {
        val omEtMinutt = now.plusSeconds(60)
        val femMinutter = Duration.ofMinutes(5)
        return Pair(Date.from(omEtMinutt.toInstant()), femMinutter.toMillis())
    }
    if (env.cluster == "prod-gcp") {
        val enDag = Duration.ofDays(1).toMillis()
        val nesteNatt = now.next(LocalTime.of(2, 0, 0))
        return Pair(nesteNatt, enDag)
    }
    throw IllegalStateException("Ukjent cluster name for cronjob ${env.cluster}")
}

private fun ZonedDateTime.next(atTime: LocalTime): Date {
    return if (this.toLocalTime().isAfter(atTime)) {
        Date.from(
            this.plusDays(1).withHour(atTime.hour).withMinute(atTime.minute).withSecond(atTime.second).toInstant()
        )
    } else {
        Date.from(this.withHour(atTime.hour).withMinute(atTime.minute).withSecond(atTime.second).toInstant())
    }
}
