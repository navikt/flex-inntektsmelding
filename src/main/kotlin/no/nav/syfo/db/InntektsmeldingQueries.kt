package no.nav.syfo.db

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.objectMapper
import org.postgresql.util.PGobject
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit

fun DatabaseInterface.finnInntektsmelding(id: String, fnr: String): Inntektsmelding? =
    connection.use { conn ->
        return conn.prepareStatement(
            """
            SELECT *
            FROM inntektsmelding
            WHERE fnr = ?
            AND id = ?;
            """
        ).use {
            it.setString(1, fnr)
            it.setString(2, id)
            it.executeQuery()
                .toList { toInntektsmelding() }
                .firstOrNull()
        }
    }

fun DatabaseInterface.finnInntektsmeldinger(fnr: String): List<Inntektsmelding> =
    connection.use { conn ->
        return conn.prepareStatement(
            """
            SELECT *
            FROM inntektsmelding
            WHERE fnr = ?;
            """
        ).use {
            it.setString(1, fnr)
            it.executeQuery().toList { toInntektsmelding() }
        }
    }

fun DatabaseInterface.lagreInntektsmelding(inntektsmelding: Inntektsmelding) {
    connection.use { conn ->
        conn.prepareStatement(
            """
            INSERT INTO INNTEKTSMELDING(id, fnr, opprettet, inntektsmelding) VALUES (?, ?, ?, ?)
            """
        ).use {
            it.setString(1, inntektsmelding.inntektsmeldingId)
            it.setString(2, inntektsmelding.arbeidstakerFnr)
            it.setTimestamp(3, Timestamp.from(Instant.now()))
            it.setObject(4, PGobject().apply { type = "json"; value = objectMapper.writeValueAsString(inntektsmelding) })

            it.executeUpdate()
        }

        conn.commit()
    }
}

fun DatabaseInterface.finnInntektsmeldingerEldreEnn18Mnd(): List<Inntektsmelding> =
    connection.use { conn ->
        return conn.prepareStatement(
            """
            SELECT *
            FROM inntektsmelding
            WHERE opprettet < ?;
            """
        ).use {
            it.setTimestamp(1, Timestamp.from(Instant.now().minus(365 + 182, ChronoUnit.DAYS)))
            it.executeQuery().toList { toInntektsmelding() }
        }
    }

fun DatabaseInterface.slettInntektsmelding(id: String) {
    connection.use { conn ->
        conn.prepareStatement(
            """
            DELETE FROM inntektsmelding
            WHERE id = ?
            """
        ).use {
            it.setString(1, id)
            it.execute()
        }
        conn.commit()
    }
}

private fun ResultSet.toInntektsmelding(): Inntektsmelding =
    objectMapper.readValue(getString("inntektsmelding"))
