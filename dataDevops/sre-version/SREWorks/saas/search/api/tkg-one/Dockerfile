FROM registry.cn-hangzhou.aliyuncs.com/alisre/sw-maven:latest AS build

COPY . /app
RUN cd /app && mvn -Dmaven.test.skip=true clean package -U

FROM registry.cn-hangzhou.aliyuncs.com/alisre/sw-openjdk8-jre:latest AS release
ARG START_MODULE=tkg-one-start
ARG TARGET_DIRECTORY=tkg-one
ARG DEPENDENCY=/app/${START_MODULE}/target/${TARGET_DIRECTORY}

COPY --from=build /app/${START_MODULE}/target/${TARGET_DIRECTORY}.jar /app/${TARGET_DIRECTORY}.jar

COPY sbin/start.sh /app/start.sh
COPY ./${START_MODULE}/src/main/resources/application.properties /app/application.properties
RUN chmod +x /app/start.sh

ENTRYPOINT ["/app/start.sh"]