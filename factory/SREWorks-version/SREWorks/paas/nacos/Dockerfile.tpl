FROM {{ MAVEN_IMAGE }} AS build
COPY . /app
RUN mkdir /root/.m2/ && curl {{ MAVEN_SETTINGS_XML }} -o /root/.m2/settings.xml

RUN cd /app/tesla-nacos-opensource && mvn -Dmaven.test.skip=true clean install

RUN cd /app && mvn -Dmaven.test.skip=true clean package

FROM {{ JRE8_IMAGE }} AS release
ARG START_MODULE=tesla-nacos-start-private
ARG SERVER_NAME=tesla-nacos
ARG DEPENDENCY=/app/${START_MODULE}/target

# set environment
ENV MODE="standalone" \
    PREFER_HOST_MODE="hostname"\
    BASE_DIR="/home/nacos" \
    CLASSPATH=".:/home/nacos/conf:$CLASSPATH" \
    CLUSTER_CONF="/home/nacos/conf/cluster.conf" \
    FUNCTION_MODE="all" \
    NACOS_USER="nacos" \
    NACOS_SERVER_PORT="8848" \
    JVM_XMS="256m" \
    JVM_XMX="256m" \
    JVM_XMN="128m" \
    JVM_MS="128m" \
    JVM_MMS="256m" \
    NACOS_DEBUG="n" \
    TOMCAT_ACCESSLOG_ENABLED="false" \
    TIME_ZONE="Asia/Shanghai"

COPY --from=build ${DEPENDENCY}/${SERVER_NAME}.jar ${BASE_DIR}/${SERVER_NAME}.jar
COPY --from=build /app/build/bin/start.sh ${BASE_DIR}/bin/start.sh
COPY --from=build /app/build/init.d/custom.properties ${BASE_DIR}/init.d/custom.properties
COPY --from=build /app/build/conf/application.properties ${BASE_DIR}/conf/application.properties
COPY --from=build /app/build/conf/nacos-logback.xml ${BASE_DIR}/conf/nacos-logback.xml

WORKDIR $BASE_DIR

RUN ln -snf /usr/share/zoneinfo/$TIME_ZONE /etc/localtime && echo '$TIME_ZONE' > /etc/timezone

RUN chmod +x bin/start.sh

# set startup log dir
RUN mkdir -p logs \
	&& cd logs \
	&& touch start.out \
	&& ln -sf /dev/stdout start.out \
	&& ln -sf /dev/stderr start.out


EXPOSE 8848
ENTRYPOINT ["bin/start.sh"]