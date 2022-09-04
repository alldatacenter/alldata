FROM ${SW_PYTHON3_IMAGE}

ARG PIP_ARG="--no-cache-dir --disable-pip-version-check"

COPY ./APP-META-PRIVATE/cluster-init /app/sbin

RUN sed -i 's/dl-cdn.alpinelinux.org/${APK_REPO_DOMAIN}/g' /etc/apk/repositories
RUN pip config set global.index-url ${PYTHON_PIP} && pip config set global.trusted-host ${PYTHON_PIP_DOMAIN}

# 安装依赖，构建镜像
RUN pip install requests requests_oauthlib
ENTRYPOINT ["/app/sbin/cluster_run.sh"]