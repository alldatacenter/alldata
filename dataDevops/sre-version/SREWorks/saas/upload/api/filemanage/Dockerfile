FROM registry.cn-hangzhou.aliyuncs.com/alisre/sreworks-base AS build
COPY . /app
WORKDIR /app
COPY settings.xml /root/.m2/settings.xml
RUN mvn -Dmaven.test.skip=true clean package

FROM registry.cn-hangzhou.aliyuncs.com/alisre/openjdk:11.0.10-jre AS release
USER root
WORKDIR /root
COPY --from=build /app/filemanage-start/target/filemanage.jar /app/filemanage.jar
ENTRYPOINT ["java", "-Xmx100m", "-Xms20m", "-XX:ActiveProcessorCount=2", "-jar", "/app/filemanage.jar"]