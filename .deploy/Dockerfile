FROM openjdk:17-jdk
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

ARG SERVICE=${SERVICE}

WORKDIR /run
COPY core-$SERVICE.jar app.jar

EXPOSE $PORT

ENTRYPOINT ["java", "-jar"]