FROM registry.cn-hangzhou.aliyuncs.com/alisre/sreworks-base AS build
COPY . /app
WORKDIR /app
#RUN mvn -Dmaven.test.skip=true clean package -o
RUN mvn -Dmaven.test.skip=true clean package


FROM registry.cn-hangzhou.aliyuncs.com/alisre/openjdk:11.0.10-jre AS release
USER root
WORKDIR /root
COPY --from=build /app/plugin-clustermanage-resource-aliyun-redis-start/target/plugin-clustermanage-resource-aliyun-redis.jar /app/plugin-clustermanage-resource-aliyun-redis.jar
ENTRYPOINT ["java", "-Xmx1g", "-Xms1g", "-XX:ActiveProcessorCount=2", "-jar", "/app/plugin-clustermanage-resource-aliyun-redis.jar"]