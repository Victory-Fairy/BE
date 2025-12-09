FROM eclipse-temurin:17-jre

ARG SERVICE=${SERVICE}

WORKDIR /run
COPY core-$SERVICE.jar app.jar

EXPOSE $PORT

ENTRYPOINT ["java", "-jar", "app.jar"]