FROM {{ MAVEN_IMAGE }} AS build

COPY . /app
RUN mkdir /root/.m2/ && curl {{ MAVEN_SETTINGS_XML }} -o /root/.m2/settings.xml
RUN cd /app && mvn -f pom.xml -Dmaven.test.skip=true clean package

FROM {{ ALPINE_IMAGE }} AS release
RUN sed -i 's/dl-cdn.alpinelinux.org/{{ APK_REPO_DOMAIN }}/g' /etc/apk/repositories
RUN { \
        echo '#!/bin/sh'; \
        echo 'set -e'; \
        echo; \
        echo 'dirname "$(dirname "$(readlink -f "$(which javac || which java)")")"'; \
    } > /usr/local/bin/docker-java-home \
    && chmod +x /usr/local/bin/docker-java-home
ENV JAVA_HOME /usr/lib/jvm/java-1.8-openjdk
ENV PATH $PATH:/usr/lib/jvm/java-1.8-openjdk/jre/bin:/usr/lib/jvm/java-1.8-openjdk/bin
RUN set -x && apk --update upgrade && apk add --no-cache openjdk8 && [ "$JAVA_HOME" = "$(docker-java-home)" ]

RUN apk --no-cache add curl && apk --no-cache add gettext && apk --no-cache add jq

ARG ALARM_MODULE=metric-alarm
ARG JAR_NAME=metric-flink-1.2.jar
ARG BUILD_JAR=/app/${ALARM_MODULE}/target/metric-alarm-1.2-SNAPSHOT.jar

COPY --from=build ${BUILD_JAR} /app/sbin/${JAR_NAME}
COPY ./sbin/ /app/sbin/
RUN chmod +x /app/sbin/entrypoint.sh

# minio-client
RUN wget {{ MINIO_CLIENT_URL }} -O /app/sbin/mc
RUN chmod +x /app/sbin/mc

# kafka init
RUN apk add gcc python3 python3-dev py3-pip musl-dev librdkafka-dev
RUN pip config set global.index-url {{ PYTHON_PIP }} && pip config set global.trusted-host {{ PYTHON_PIP_DOMAIN }}
RUN pip3 install confluent_kafka==1.8.2

ENTRYPOINT ["/bin/sh", "/app/sbin/entrypoint.sh"]
