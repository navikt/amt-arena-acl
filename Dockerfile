FROM ghcr.io/navikt/poao-baseimages/java:17
COPY /target/amt-arena-acl.jar app.jar
