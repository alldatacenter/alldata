# TODO: persist the SQLite file in the volumes

# Stage 1: build frontend ui
FROM node:16-alpine as ui-build
WORKDIR /usr/src/ui
COPY ./ui .

## Use api endpoint from same host and build production static bundle
RUN echo 'REACT_APP_API_ENDPOINT=' >> .env.production
RUN npm install && npm run build

# Stage 2: build feathr runtime jar
FROM gradle:7.6.0-jdk8 as gradle-build
WORKDIR /usr/src/feathr
COPY . .
RUN ./gradlew build

# Stage 3: build the docker image for feathr sandbox
FROM jupyter/pyspark-notebook

USER root

## Install dependencies
RUN apt-get update -y && apt-get install -y nginx freetds-dev sqlite3 libsqlite3-dev lsb-release redis gnupg redis-server lsof

# UI Sectioin
## Remove default nginx index page and copy ui static bundle files
RUN rm -rf /usr/share/nginx/html/*
COPY --from=ui-build /usr/src/ui/build /usr/share/nginx/html
COPY ./deploy/nginx.conf /etc/nginx/nginx.conf

# Feathr Package Installation Section
# always install feathr from main
WORKDIR /home/jovyan/work
COPY --chown=1000:100 ./feathr_project ./feathr_project
COPY --chown=1000:100 --from=gradle-build /usr/src/feathr/build/libs .
RUN python -m pip install -e ./feathr_project

# Registry Section
# install registry
COPY ./registry /usr/src/registry
WORKDIR /usr/src/registry/sql-registry
RUN pip install -r requirements.txt

## Start service and then start nginx
WORKDIR /usr/src/registry
COPY ./feathr-sandbox/start_local.sh /usr/src/registry/

# default dir by the jupyter image
WORKDIR /home/jovyan/work
USER jovyan
# copy as the jovyan user
# UID is like this: uid=1000(jovyan) gid=100(users) groups=100(users)
COPY --chown=1000:100 ./docs/samples/local_quickstart_notebook.ipynb .
COPY --chown=1000:100 ./feathr-sandbox/feathr_init_script.py .

# Run the script so that maven cache can be added for better experience. Otherwise users might have to wait for some time for the maven cache to be ready.
RUN python -m pip install interpret

USER root
WORKDIR /usr/src/registry
RUN ["chmod", "+x", "/usr/src/registry/start_local.sh"]

# remove ^M chars in Linux to make sure the script can run
RUN sed -i "s/\r//g" /usr/src/registry/start_local.sh


# install a Kafka single node instance
# Reference: https://www.looklinux.com/how-to-install-apache-kafka-single-node-on-ubuntu/
# RUN wget https://archive.apache.org/dist/kafka/3.3.1/kafka_2.12-3.3.1.tgz && tar xzf kafka_2.12-3.3.1.tgz && mv kafka_2.12-3.3.1 /usr/local/kafka && rm kafka_2.12-3.3.1.tgz

# /usr/local/kafka/bin/zookeeper-server-start.sh /usr/local/kafka/config/zookeeper.properties
# /usr/local/kafka/bin/kafka-server-start.sh  /usr/local/kafka/config/server.properties

WORKDIR /home/jovyan/work

USER jovyan

ENV API_BASE="api/v1" 
ENV FEATHR_SANDBOX=True
# Run the script so that maven cache can be added for better experience. Otherwise users might have to wait for some time for the maven cache to be ready.
RUN /usr/src/registry/start_local.sh -m build_docker && python feathr_init_script.py

USER root

# 80: Feathr UI
# 8888: Jupyter
# 7080: Interpret
EXPOSE 80 8888 7080 2181
# run the service so we can initialize
CMD ["/bin/bash", "/usr/src/registry/start_local.sh"]
