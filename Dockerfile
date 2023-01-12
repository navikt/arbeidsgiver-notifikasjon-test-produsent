FROM ghcr.io/navikt/baseimages/temurin:17
COPY target/*-jar-with-dependencies.jar app.jar
