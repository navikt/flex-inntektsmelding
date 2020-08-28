#!/bin/bash
echo "Bygger flex-inntektsmelding for bruk i flex-docker-compose"
./gradlew ktlintFormat
./gradlew shadowJar
docker build -t flex-inntektsmelding:latest .
