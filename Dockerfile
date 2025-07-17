FROM bellsoft/liberica-openjdk-alpine-musl:21
WORKDIR /app
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENV TZ="Europe/Oslo"
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]