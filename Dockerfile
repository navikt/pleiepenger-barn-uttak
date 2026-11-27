FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.11.26.0840Z

LABEL org.opencontainers.image.source=https://github.com/navikt/pleiepenger-barn-uttak

WORKDIR /app
COPY server/target/app.jar ./
