package no.nav.syfo.db

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.objectMapper
import org.postgresql.util.PGobject
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp

fun DatabaseInterface.finnInntektsmeldinger(fnr: String): List<Inntektsmelding> =
    connection.use {
        return it.finnInntektsmeldinger(fnr)
    }
private fun Connection.finnInntektsmeldinger(fnr: String): List<Inntektsmelding> =
    this.prepareStatement(
        """
            SELECT *
            FROM inntektsmelding
            WHERE fnr = ?;
            """
    ).use {
        it.setString(1, fnr)
        it.executeQuery().toList { toInntektsmelding() }
    }
private fun ResultSet.toInntektsmelding(): Inntektsmelding =
    // TODO: Sjekk om man kan bruke getObject i stedet for objectMapper
    Inntektsmelding(
        arbeidsforholdId = getString("arbeidsforholdId"),
        arbeidsgiverAktorId = getString("arbeidsgiverAktorId"),
        arbeidsgiverFnr = getString("arbeidsgiverFnr"),
        arbeidsgiverperioder = objectMapper.readValue(getString("arbeidsgiverperioder")),
        arbeidsgivertype = objectMapper.readValue(getString("arbeidsgivertype")),
        arbeidstakerAktorId = getString("arbeidstakerAktorId"),
        arbeidstakerFnr = getString("arbeidstakerFnr"),
        arkivreferanse = getString("arkivreferanse"),
        begrunnelseForReduksjonEllerIkkeUtbetalt = objectMapper.readValue(getString("begrunnelseForReduksjonEllerIkkeUtbetalt")), // TODO: Dette er ikke et objekt men en ukjent lang string
        beregnetInntekt = BigDecimal(getString("beregnetInntekt")),
        endringIRefusjoner = objectMapper.readValue(getString("endringIRefusjoner")),
        ferieperioder = objectMapper.readValue(getString("ferieperioder")),
        foersteFravaersdag = getObject("foersteFravaersdag", Timestamp::class.java).toLocalDateTime().toLocalDate(),
        gjenopptakelseNaturalytelser = objectMapper.readValue(getString("gjenopptakelseNaturalytelser")),
        inntektsmeldingId = getString("inntektsmeldingId"),
        mottattDato = getObject("mottattDato", Timestamp::class.java).toLocalDateTime(),
        opphoerAvNaturalytelser = objectMapper.readValue(getString("opphoerAvNaturalytelser")),
        refusjon = objectMapper.readValue(getString("refusjon")),
        status = objectMapper.readValue(getString("status")),
        virksomhetsnummer = getString("virksomhetsnummer")
    )

fun DatabaseInterface.lagreInntektsmelding(inntektsmelding: Inntektsmelding) {
    connection.use {
        return it.lagreInntektsmelding(inntektsmelding)
    }
}
private fun Connection.lagreInntektsmelding(inntektsmelding: Inntektsmelding) {
    this.prepareStatement(
        """
           INSERT INTO INNTEKTSMELDING(arbeidsforholdId, arbeidsgiverAktorId, arbeidsgiverFnr, arbeidsgiverperioder, arbeidsgivertype, arbeidstakerAktorId, arbeidstakerFnr, arkivreferanse, begrunnelseForReduksjonEllerIkkeUtbetalt, beregnetInntekt, endringIRefusjoner, ferieperioder, foersteFravaersdag, gjenopptakelseNaturalytelser, inntektsmeldingId, mottattDato, opphoerAvNaturalytelser, refusjon, status, virksomhetsnummer)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
    ).use {
        it.setString(1, inntektsmelding.arbeidsforholdId)
        it.setString(2, inntektsmelding.arbeidsgiverAktorId)
        it.setString(3, inntektsmelding.arbeidsgiverFnr)
        it.setObject(4, PGobject().apply { type = "json"; value = objectMapper.writeValueAsString(inntektsmelding.arbeidsgiverperioder) })
        it.setObject(5, PGobject().apply { type = "json"; value = objectMapper.writeValueAsString(inntektsmelding.arbeidsgivertype) })
        it.setString(6, inntektsmelding.arbeidstakerAktorId)
        it.setString(7, inntektsmelding.arbeidstakerFnr)
        it.setString(8, inntektsmelding.arkivreferanse)
        it.setObject(9, PGobject().apply { type = "json"; value = objectMapper.writeValueAsString(inntektsmelding.begrunnelseForReduksjonEllerIkkeUtbetalt) }) // TODO: Dette er ikke et objekt men en ukjent lang string
        it.setBigDecimal(10, inntektsmelding.beregnetInntekt)
        it.setObject(11, PGobject().apply { type = "json"; value = objectMapper.writeValueAsString(inntektsmelding.endringIRefusjoner) })
        it.setObject(12, PGobject().apply { type = "json"; value = objectMapper.writeValueAsString(inntektsmelding.ferieperioder) })
        it.setTimestamp(13, Timestamp.valueOf(inntektsmelding.foersteFravaersdag?.atStartOfDay()))
        it.setObject(14, PGobject().apply { type = "json"; value = objectMapper.writeValueAsString(inntektsmelding.gjenopptakelseNaturalytelser) })
        it.setString(15, inntektsmelding.inntektsmeldingId)
        it.setTimestamp(16, Timestamp.valueOf(inntektsmelding.mottattDato))
        it.setObject(17, PGobject().apply { type = "json"; value = objectMapper.writeValueAsString(inntektsmelding.opphoerAvNaturalytelser) })
        it.setObject(18, PGobject().apply { type = "json"; value = objectMapper.writeValueAsString(inntektsmelding.refusjon) })
        it.setObject(19, PGobject().apply { type = "json"; value = objectMapper.writeValueAsString(inntektsmelding.status) })
        it.setString(20, inntektsmelding.virksomhetsnummer)

        it.executeUpdate()
    }

    this.commit()
}
