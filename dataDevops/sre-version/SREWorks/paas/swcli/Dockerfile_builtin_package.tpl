FROM swcli:latest

RUN sed -i 's/dl-cdn.alpinelinux.org/${APK_REPO_DOMAIN}/g' /etc/apk/repositories
RUN pip config set global.index-url ${PYTHON_PIP} && pip config set global.trusted-host ${PYTHON_PIP_DOMAIN}
RUN pip install requests requests_oauthlib pyyaml
RUN apk add zip curl

#COPY builtin_package/build /root/build
COPY builtin_package/saas /root/saas
COPY builtin_package/chart /root/chart

RUN wget ${MINIO_CLIENT_URL} -O /root/mc && chmod +x /root/mc
