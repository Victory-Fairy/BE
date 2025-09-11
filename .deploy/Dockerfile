FROM openjdk:17-jdk

ARG SERVICE=${SERVICE}

WORKDIR /run
COPY core-$SERVICE.jar app.jar

EXPOSE $PORT

ENTRYPOINT ["java", "-jar"]