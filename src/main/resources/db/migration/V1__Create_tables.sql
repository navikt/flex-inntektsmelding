CREATE TABLE inntektsmelding
(
    arbeidsforholdId VARCHAR(36),
    arbeidsgiverAktorId VARCHAR(13),
    arbeidsgiverFnr VARCHAR(11),
    arbeidsgiverperioder JSONB NOT NULL,
    arbeidsgivertype JSONB NOT NULL,
    arbeidstakerAktorId VARCHAR(13) NOT NULL,
    arbeidstakerFnr VARCHAR(11) NOT NULL,
    arkivreferanse VARCHAR(36) NOT NULL,
    begrunnelseForReduksjonEllerIkkeUtbetalt JSONB,
    beregnetInntekt NUMERIC(20, 2),
    endringIRefusjoner JSONB NOT NULL,
    ferieperioder JSONB NOT NULL,
    foersteFravaersdag TIMESTAMP,
    gjenopptakelseNaturalytelser JSONB NOT NULL,
    inntektsmeldingId VARCHAR(36) PRIMARY KEY,
    mottattDato TIMESTAMP NOT NULL,
    opphoerAvNaturalytelser JSONB NOT NULL,
    refusjon JSONB NOT NULL,
    status JSONB NOT NULL,
    virksomhetsnummer VARCHAR(9)
);

