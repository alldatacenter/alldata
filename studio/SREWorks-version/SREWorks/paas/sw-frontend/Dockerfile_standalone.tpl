FROM node:10-alpine AS build1
COPY . /app
RUN sed -i 's/dl-cdn.alpinelinux.org/{{ APK_REPO_DOMAIN }}/g' /etc/apk/repositories \
    && apk add --update --no-cache python2 make gcc g++ zip
RUN export NPM_REGISTRY_URL="{{ NPM_REGISTRY_URL }}" && cd /app/docs/ && /bin/sh /app/docs/build.sh
RUN mkdir -p /app/build && cd /app/build && mv /app/docs/pictures /app/docs/_book/pictures && mv /app/docs/_book /app/build/docs && zip -r /app/docs.zip ./

FROM {{ NODE_IMAGE }} AS build2
COPY . /app
RUN sed -i 's/dl-cdn.alpinelinux.org/{{ APK_REPO_DOMAIN }}/g' /etc/apk/repositories \
    && apk add --update --no-cache python2 make gcc g++ zip
RUN export NPM_REGISTRY_URL="{{ NPM_REGISTRY_URL }}" && /bin/sh /app/sbin/build-standalone.sh

FROM {{ ALPINE_IMAGE }} AS release
COPY ./APP-META-PRIVATE/deploy-config/config.js.tpl /app/config.js.tpl
COPY ./APP-META-PRIVATE/deploy-config/ /app/deploy-config/
COPY ./sbin/ /app/sbin/
RUN chmod -R +x /app/sbin/
RUN sed -i 's/dl-cdn.alpinelinux.org/{{ APK_REPO_DOMAIN }}/g' /etc/apk/repositories \
    && apk add --update --no-cache curl vim bash gettext sudo wget libc6-compat nginx zip

COPY --from=build2 /app/build.zip /app/build.zip
COPY --from=build1 /app/docs.zip /app/docs.zip
RUN cd /app && unzip build.zip && rm -rf build.zip && unzip docs.zip && rm -rf docs.zip
ENTRYPOINT ["/app/sbin/run.sh"]