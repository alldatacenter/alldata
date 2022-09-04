#FROM reg.docker.alibaba-inc.com/aone-base/alios7u2-python37:dev
FROM python:3.7-slim

ENV APP_NAME=aiops-server
ENV PRODUCT=sreworks
ENV APP_ENV=prod

COPY . /home/admin/${APP_NAME}

WORKDIR /home/admin/${APP_NAME}

#VOLUME /home/admin/logs/${APP_NAME}

RUN /bin/sh mirror_init.sh

RUN apt-get update -y && apt-get install -y python3-pip && pip3 install pip -i http://mirrors.aliyun.com/pypi/simple --trusted-host mirrors.aliyun.com --upgrade

RUN apt-get install -y gettext-base

RUN pip install -i http://mirrors.aliyun.com/pypi/simple --trusted-host mirrors.aliyun.com -r requirements.txt

#RUN celery multi restart w2 -A tsp_train.celery worker -l INFO
RUN mkdir -p /var/run/celery
RUN mkdir -p /var/log/celery
#RUN celery multi start w1 -A tsp_train.celery worker -l INFO --pidfile=/var/run/celery/%n.pid  --logfile=/var/log/celery/%n%I.log

#ENTRYPOINT ["python", "main.py"]
ENTRYPOINT ["/bin/sh", "start.sh"]