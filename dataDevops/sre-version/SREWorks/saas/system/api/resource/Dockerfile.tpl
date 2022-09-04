FROM {{ PYTHON3_IMAGE }}
RUN sed -i 's/dl-cdn.alpinelinux.org/{{ APK_REPO_DOMAIN }}/g' /etc/apk/repositories
COPY . /app
WORKDIR /app
RUN apk add zip curl
RUN wget {{ MINIO_CLIENT_URL }}  -O /app/mc && chmod +x /app/mc
RUN chmod +x /app/resource.sh
ENTRYPOINT ["/bin/sh", "/app/resource.sh"]
