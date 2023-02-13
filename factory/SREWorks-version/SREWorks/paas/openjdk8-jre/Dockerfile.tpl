FROM ${ALPINE_IMAGE}

ENV JAVA_HOME=/usr/lib/jvm/default-jvm/jre
ENV PATH $PATH:/usr/lib/jvm/default-jvm/jre/bin:/usr/lib/jvm/default-jvm/bin
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apk/repositories \
    && apk add --no-cache --update openjdk8-jre vim bash bash-doc bash-completion curl wget busybox busybox-extras tar xz coreutils openrc tzdata \
    && echo "PS1='\n\e[1;37m[\e[m\e[1;32m\u\e[m\e[1;33m@\e[m\e[1;35m\H\e[m \e[4m`pwd`\e[m\e[1;37m]\e[m\e[1;36m\e[m\n\$'" >> ~/.bashrc
CMD ["java", "-version"]
