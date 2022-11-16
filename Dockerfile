FROM ghcr.io/navikt/baseimages/temurin:11-appdynamics
ENV APPD_ENABLED=true
LABEL org.opencontainers.image.source=https://github.com/navikt/pleiepenger-barn-uttak

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 "

COPY server/target/app.jar ./
