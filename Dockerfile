FROM ghcr.io/navikt/baseimages/temurin:21-appdynamics

LABEL org.opencontainers.image.source=https://github.com/navikt/pleiepenger-barn-uttak

RUN curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.6.0/opentelemetry-javaagent.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:ActiveProcessorCount=2"

COPY init-scripts/init-app.sh /init-scripts/init-app.sh
COPY server/target/app.jar ./
