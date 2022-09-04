FROM ${MIGRATE_IMAGE}
RUN sed -i 's/dl-cdn.alpinelinux.org/${APK_REPO_DOMAIN}/g' /etc/apk/repositories \
    && apk update \
    && apk add --no-cache gettext libintl mysql-client bash tzdata
COPY ./entrypoint.sh /
WORKDIR /
ENTRYPOINT ["/entrypoint.sh"]