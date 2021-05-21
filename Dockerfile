FROM navikt/java:11-appdynamics
ENV APPD_ENABLED=true
LABEL org.opencontainers.image.source=https://github.com/navikt/pleiepenger-barn-uttak

COPY server/target/app.jar ./
