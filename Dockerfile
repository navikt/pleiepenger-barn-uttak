FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.11.23.1517Z

LABEL org.opencontainers.image.source=https://github.com/navikt/pleiepenger-barn-uttak

WORKDIR /app
COPY server/target/app.jar ./
