FROM sreworks-registry.cn-beijing.cr.aliyuncs.com/sreworks/sw-maven:v0.5 AS build
COPY . /app
RUN cd /app && mvn -Dmaven.test.skip=true clean package -U -q

FROM sreworks-registry.cn-beijing.cr.aliyuncs.com/sreworks/sw-openjdk8-jre:v0.5 AS release
USER root
WORKDIR /root
COPY --from=build /app/sreworks-helloworld-start/target/sreworks-helloworld.jar /app/sreworks-helloworld.jar
ENTRYPOINT ["java", "-Xmx1g", "-Xms1g", "-XX:ActiveProcessorCount=2", "-jar", "/app/sreworks-helloworld.jar"]


