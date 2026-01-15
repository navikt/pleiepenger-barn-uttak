FROM ghcr.io/navikt/sif-baseimages/java-21:2026.01.14.1455Z

LABEL org.opencontainers.image.source=https://github.com/navikt/pleiepenger-barn-uttak

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:ActiveProcessorCount=2"

COPY server/target/app.jar ./
