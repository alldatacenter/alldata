FROM sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/maven:3.8.3-adoptopenjdk-11 AS build
COPY . /app
WORKDIR /app
RUN mkdir /root/.m2/ && curl https://sreworks.oss-cn-beijing.aliyuncs.com/resource/settings.xml -o /root/.m2/settings.xml
RUN mvn -Dmaven.test.skip=true clean package

FROM sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/openjdk:11.0.10-jre AS release
USER root
WORKDIR /root
COPY --from=build /app/clustermanage-start/target/clustermanage.jar /app/clustermanage.jar
COPY --from=build /app/helm /app/helm
COPY --from=build /app/client-deploy-packages /app/client-deploy-packages
ENTRYPOINT ["java", "-Xmx100m", "-Xms20m", "-XX:ActiveProcessorCount=2", "-jar", "/app/clustermanage.jar"]