CREATE TABLE inntektsmelding
(
    id VARCHAR(36) PRIMARY KEY,
    fnr VARCHAR(11) NOT NULL,
    opprettet TIMESTAMP NOT NULL,
    inntektsmelding JSONB NOT NULL
);

CREATE INDEX inntektsmelding_fnr_idx ON inntektsmelding (fnr);
