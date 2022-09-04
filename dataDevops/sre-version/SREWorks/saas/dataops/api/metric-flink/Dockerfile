FROM sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/maven:3.8.3-adoptopenjdk-11 AS build

COPY . /app
COPY settings.xml /root/.m2/settings.xml
RUN cd /app && mvn -f pom.xml -Dmaven.test.skip=true clean package

FROM sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/alpine:latest AS release
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apk/repositories
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
RUN wget https://sreworks.oss-cn-beijing.aliyuncs.com/bin/mc-linux-amd64 -O /app/sbin/mc
RUN chmod +x /app/sbin/mc

# kafka init
RUN apk add gcc python3 python3-dev py3-pip musl-dev librdkafka-dev
RUN pip3 install -i http://mirrors.aliyun.com/pypi/simple --trusted-host mirrors.aliyun.com confluent_kafka==1.8.2

ENTRYPOINT ["/bin/sh", "/app/sbin/entrypoint.sh"]
