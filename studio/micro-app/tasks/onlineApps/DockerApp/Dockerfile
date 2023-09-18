FROM java:8
# 在主机/var/lib/docker创建临时文件链接到容器的/tmp
VOLUME /tmp
# 将jar包添加到容器中并更名为real-app.jar
ADD docker-app.jar real-app.jar
# 运行jar包
RUN bash -c 'touch /real-app.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/real-app.jar"]

