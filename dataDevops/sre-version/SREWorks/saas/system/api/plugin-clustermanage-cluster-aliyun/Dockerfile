FROM registry.cn-hangzhou.aliyuncs.com/alisre/sreworks-base AS build
COPY . /app
WORKDIR /app
#RUN mvn -Dmaven.test.skip=true clean package -o
RUN mvn -Dmaven.test.skip=true clean package


FROM registry.cn-hangzhou.aliyuncs.com/alisre/openjdk:11.0.10-jre AS release
USER root
WORKDIR /root
COPY --from=build /app/plugin-clustermanage-cluster-aliyun-start/target/plugin-clustermanage-cluster-aliyun.jar /app/plugin-clustermanage-cluster-aliyun.jar
ENTRYPOINT ["java", "-Xmx100m", "-Xms20m", "-XX:ActiveProcessorCount=2", "-jar", "/app/plugin-clustermanage-cluster-aliyun.jar"]