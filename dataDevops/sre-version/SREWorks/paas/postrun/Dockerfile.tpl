FROM ${SW_PYTHON3_IMAGE}
COPY . /app
RUN sed -i 's/dl-cdn.alpinelinux.org/${APK_REPO_DOMAIN}/g' /etc/apk/repositories
RUN apk update && apk add --no-cache gettext libintl mysql-client bash tzdata
RUN pip config set global.index-url ${PYTHON_PIP} && pip config set global.trusted-host ${PYTHON_PIP_DOMAIN}
RUN pip install -r /app/requirements.txt
WORKDIR /app
ENTRYPOINT ["/app/entrypoint.sh"]