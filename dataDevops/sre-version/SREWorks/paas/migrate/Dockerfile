FROM migrate/migrate
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories \
    && apk update \
    && apk add --no-cache gettext libintl mysql-client bash tzdata
COPY ./entrypoint.sh /
WORKDIR /
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
