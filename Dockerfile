FROM navikt/java:17
COPY target/*-jar-with-dependencies.jar app.jar
