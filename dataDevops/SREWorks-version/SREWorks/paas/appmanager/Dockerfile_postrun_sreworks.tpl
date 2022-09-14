FROM sw-postrun:latest
COPY ./APP-META-PRIVATE/postrun /app/postrun
ENV SREWORKS_INIT "enable"
RUN sed -i 's/dl-cdn.alpinelinux.org/${APK_REPO_DOMAIN}/g' /etc/apk/repositories
RUN pip config set global.index-url ${PYTHON_PIP} && pip config set global.trusted-host ${PYTHON_PIP_DOMAIN}
RUN pip install requests requests_oauthlib
RUN rm -rf /app/postrun/00_init_app_manager_flow.py && \
    apk update && \
    apk add wget bind-tools