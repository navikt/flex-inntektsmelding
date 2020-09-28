package no.nav.syfo.metrikk

import io.prometheus.client.Counter

const val METRICS_NS = ""

val MOTTATT_INNTEKTSMELDING: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name("mottatt_inntektsmelding_counter")
    .help("Antall mottatte inntektsmeldinger")
    .register()
