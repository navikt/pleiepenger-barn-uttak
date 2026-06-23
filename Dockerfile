FROM ghcr.io/navikt/sif-baseimages/java-25:2026.06.22.0724Z

LABEL org.opencontainers.image.source=https://github.com/navikt/pleiepenger-barn-uttak

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:ActiveProcessorCount=2"

COPY server/target/app.jar ./
