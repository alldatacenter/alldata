FROM reg.docker.alibaba-inc.com/abm-aone/python27 AS release
COPY ./build/config /app
RUN chmod +x /app/*.sh \
    && apk add --update --no-cache bash \
    && apk add --update --no-cache gettext \
    && rm -rf /root/.cache

WORKDIR /app
EXPOSE 80
ENV PYTHONPATH=/app
ENTRYPOINT ["/app/start.sh"]