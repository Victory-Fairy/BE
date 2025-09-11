FROM openjdk:17-jdk

ARG SERVICE=${SERVICE}

WORKDIR /run
COPY core-$SERVICE.jar app.jar

RUN set -eux; \
    if command -v apt-get >/dev/null; then apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*; \
    elif command -v microdnf >/dev/null; then microdnf install -y curl && microdnf clean all; \
    elif command -v yum >/dev/null; then yum install -y curl && yum clean all; \
    elif command -v apk >/dev/null; then apk add --no-cache curl; \
    else echo "no pkg manager"; exit 1; fi

EXPOSE $PORT

ENTRYPOINT ["java", "-jar"]