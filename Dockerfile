FROM ghcr.io/navikt/poao-baseimages/java:15
COPY /target/arena-acl-*.jar app.jar
