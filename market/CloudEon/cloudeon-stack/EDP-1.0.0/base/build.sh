docker build -f Jdk-Dockerfile -t jdk:1.8.141 .
 docker tag jdk:1.8.141  registry.cn-hangzhou.aliyuncs.com/udh/jdk:1.8.141
 docker push  registry.cn-hangzhou.aliyuncs.com/udh/jdk:1.8.141