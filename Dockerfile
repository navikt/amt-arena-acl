FROM ghcr.io/navikt/poao-baseimages/java:15
COPY /target/amt-arena-acl.jar app.jar
