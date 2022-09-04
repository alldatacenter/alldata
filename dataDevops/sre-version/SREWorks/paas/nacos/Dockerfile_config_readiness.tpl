FROM ${ALPINE_IMAGE}
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.ustc.edu.cn/g' /etc/apk/repositories
RUN apk update && apk add curl && rm -rf /var/cache/apk/*

COPY ./build/readiness /app
RUN chmod +x /app/*.sh
ENTRYPOINT ["/app/start.sh"]