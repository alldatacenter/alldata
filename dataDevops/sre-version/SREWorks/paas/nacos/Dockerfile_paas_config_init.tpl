FROM {{ PYTHON2_IMAGE }} AS env
COPY ./requirements.txt /requirements.txt
RUN apk add --update --no-cache gcc libc-dev wget \
    && wget "http://nacos-daily.tesla.alibaba-inc.com/nacos/v1/cs/configs?export=true&group=ABM-PAAS-SYSTEM&tenant=2d663381-28ed-4b22-bcf7-7e9c9f268913&appName=&ids=&dataId=" -O nacos_config_export.zip \
    && pip install -i http://mirrors.aliyun.com/pypi/simple --trusted-host mirrors.aliyun.com -r /requirements.txt

FROM {{ PYTHON2_IMAGE }} AS release
COPY ./requirements.txt /requirements.txt
COPY ./build/config /app
COPY --from=env /root/.cache /root/.cache
COPY --from=env /nacos_config_export.zip /app/nacos_config_export.zip
RUN chmod +x /app/*.sh \
    && pip install -i http://mirrors.aliyun.com/pypi/simple --trusted-host mirrors.aliyun.com -r /requirements.txt \
    && apk add --update --no-cache bash \
    && apk add --update --no-cache gettext \
    && rm -rf /root/.cache

WORKDIR /app
EXPOSE 80
ENV PYTHONPATH=/app
ENTRYPOINT ["/app/start.sh"]