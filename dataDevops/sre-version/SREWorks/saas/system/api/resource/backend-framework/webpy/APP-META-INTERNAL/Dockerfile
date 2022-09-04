FROM reg.docker.alibaba-inc.com/aone-base/alios7u2-python27:dev

ENV APP_NAME=faas-tesla-qiuqiang-test
ENV PRODUCT=tesla

COPY . /home/admin/${APP_NAME}
COPY ./APP-META-INTERNAL/run.sh /home/admin/${APP_NAME}/run.sh

WORKDIR /home/admin/${APP_NAME}

VOLUME /root/logs/${APP_NAME}

#RUN chmod +x APP-META-INTERNAL/*.sh && sh APP-META-INTERNAL/deploy.sh ${APP_NAME}

RUN /usr/local/t-tesla-python27/bin/pip install -i https://pypi.antfin-inc.com/simple/ -r requirements.txt

ENTRYPOINT ["sh", "run.sh"]
#ENTRYPOINT ["tail", "-f", "/dev/null"]