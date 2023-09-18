<!--HOSTED_DOCS_ONLY
import useBaseUrl from '@docusaurus/useBaseUrl';

export const Logo = (props) => {
  return (
    <div style={{ display: "flex", justifyContent: "center", padding: "20px", height: "190px" }}>
      <img
        alt="DataHub Logo"
        src={useBaseUrl("/img/datahub-logo-color-mark.svg")}
        {...props}
      />
    </div>
  );
};

<Logo />

<!--
HOSTED_DOCS_ONLY-->
<p align="center">
<img alt="DataHub" src="docs/imgs/datahub-logo-color-mark.svg" height="150" />
</p>
<!-- -->

# DataHub: The Metadata Platform for the Modern Data Stack
## Built with ❤️ by <img src="https://datahubproject.io/img/acryl-logo-light-mark.png" width="25"/> [Acryl Data](https://acryldata.io) and <img src="https://datahubproject.io/img/LI-In-Bug.png" width="25"/> [LinkedIn](https://engineering.linkedin.com)
[![Version](https://img.shields.io/github/v/release/datahub-project/datahub?include_prereleases)](https://github.com/datahub-project/datahub/releases/latest)
[![PyPI version](https://badge.fury.io/py/acryl-datahub.svg)](https://badge.fury.io/py/acryl-datahub)
[![build & test](https://github.com/datahub-project/datahub/workflows/build%20&%20test/badge.svg?branch=master&event=push)](https://github.com/datahub-project/datahub/actions?query=workflow%3A%22build+%26+test%22+branch%3Amaster+event%3Apush)
[![Docker Pulls](https://img.shields.io/docker/pulls/linkedin/datahub-gms.svg)](https://hub.docker.com/r/linkedin/datahub-gms)
[![Slack](https://img.shields.io/badge/slack-join_chat-white.svg?logo=slack&style=social)](https://slack.datahubproject.io)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/datahub-project/datahub/blob/master/docs/CONTRIBUTING.md)
[![GitHub commit activity](https://img.shields.io/github/commit-activity/m/datahub-project/datahub)](https://github.com/datahub-project/datahub/pulls?q=is%3Apr)
[![License](https://img.shields.io/github/license/datahub-project/datahub)](https://github.com/datahub-project/datahub/blob/master/LICENSE)
[![YouTube](https://img.shields.io/youtube/channel/subscribers/UC3qFQC5IiwR5fvWEqi_tJ5w?style=social)](https://www.youtube.com/channel/UC3qFQC5IiwR5fvWEqi_tJ5w)
[![Medium](https://img.shields.io/badge/Medium-12100E?style=for-the-badge&logo=medium&logoColor=white)](https://medium.com/datahub-project)
[![Follow](https://img.shields.io/twitter/follow/datahubproject?label=Follow&style=social)](https://twitter.com/datahubproject)
### 🏠 Hosted DataHub Docs (Courtesy of Acryl Data): [datahubproject.io](https://datahubproject.io/docs)

---

[Quickstart](https://datahubproject.io/docs/quickstart) |
[Features](https://datahubproject.io/docs/features) |
[Roadmap](https://feature-requests.datahubproject.io/roadmap) |
[Adoption](#adoption) |
[Demo](https://datahubproject.io/docs/demo) |
[Town Hall](https://datahubproject.io/docs/townhalls)

---
> 📣 DataHub Town Hall is the 4th Thursday at 9am US PT of every month - [add it to your calendar!](https://rsvp.datahubproject.io/)
>
> - Town-hall Zoom link: [zoom.datahubproject.io](https://zoom.datahubproject.io)
> - [Meeting details](docs/townhalls.md) & [past recordings](docs/townhall-history.md)

> ✨ DataHub Community Highlights:
>
> - Read our Monthly Project Updates [here](https://blog.datahubproject.io/tagged/project-updates).
> - Bringing The Power Of The DataHub Real-Time Metadata Graph To Everyone At Acryl Data: [Data Engineering Podcast](https://www.dataengineeringpodcast.com/acryl-data-datahub-metadata-graph-episode-230/)
> - Check out our most-read blog post, [DataHub: Popular Metadata Architectures Explained](https://engineering.linkedin.com/blog/2020/datahub-popular-metadata-architectures-explained) @ LinkedIn Engineering Blog.
> - Join us on [Slack](docs/slack.md)! Ask questions and keep up with the latest announcements.

## Introduction

DataHub is an open-source metadata platform for the modern data stack. Read about the architectures of different metadata systems and why DataHub excels [here](https://engineering.linkedin.com/blog/2020/datahub-popular-metadata-architectures-explained). Also read our
[LinkedIn Engineering blog post](https://engineering.linkedin.com/blog/2019/data-hub), check out our [Strata presentation](https://speakerdeck.com/shirshanka/the-evolution-of-metadata-linkedins-journey-strata-nyc-2019) and watch our [Crunch Conference Talk](https://www.youtube.com/watch?v=OB-O0Y6OYDE). You should also visit [DataHub Architecture](docs/architecture/architecture.md) to get a better understanding of how DataHub is implemented.

## Features & Roadmap

Check out DataHub's [Features](docs/features.md) & [Roadmap](https://feature-requests.datahubproject.io/roadmap).

## Demo and Screenshots

There's a [hosted demo environment](https://datahubproject.io/docs/demo) courtesy of [Acryl Data](https://acryldata.io) where you can explore DataHub without installing it locally

## Quickstart

Please follow the [DataHub Quickstart Guide](https://datahubproject.io/docs/quickstart) to get a copy of DataHub up & running locally using [Docker](https://docker.com). As the guide assumes some basic knowledge of Docker, we'd recommend you to go through the "Hello World" example of [A Docker Tutorial for Beginners](https://docker-curriculum.com) if Docker is completely foreign to you.

## Development

If you're looking to build & modify datahub please take a look at our [Development Guide](https://datahubproject.io/docs/developers).

[![DataHub Demo GIF](docs/imgs/entity.png)](https://datahubproject.io/docs/demo)

## Source Code and Repositories

- [datahub-project/datahub](https://github.com/datahub-project/datahub): This repository contains the complete source code for DataHub's metadata model, metadata services, integration connectors and the web application.
- [acryldata/datahub-actions](https://github.com/acryldata/datahub-actions): DataHub Actions is a framework for responding to changes to your DataHub Metadata Graph in real time.
- [acryldata/datahub-helm](https://github.com/acryldata/datahub-helm): Repository of helm charts for deploying DataHub on a Kubernetes cluster
- [acryldata/meta-world](https://github.com/acryldata/meta-world): A repository to store recipes, custom sources, transformations and other things to make your DataHub experience magical

## License

[Apache License 2.0](./LICENSE).

## 官方项目地址
> https://github.com/datahub-project/datahub

## DataHub本地构建-Linux

### 1、JAVA_HOME
> 1.1 安装Java-11 && 配置JAVA_HOME
>
> sudo yum install java-11-openjdk -y
> 
> sudo yum install java-11-openjdk-devel
>
> 1.2 安装Java-8 && 不需要配置JAVA_HOME
>
> yum install java-1.8.0-openjdk.x86_64
>
> yum install -y java-1.8.0-openjdk-devel.x86_64

### 2、Python3.7以上版本
> 2.1 下载python3.7 
> 
> mkdir -p /usr/local/python3 && cd /usr/local/python3
> 
> wget https://www.python.org/ftp/python/3.7.16/Python-3.7.16.tar.xz
> 
> tar -xvf Python-3.7.16.tar.xz
> 
> cd Python-3.7.16
> 
> ./configure --prefix=/usr/local/python3
>
> make && make install
> 
> ln -s /usr/local/python3/bin/python3 /usr/bin/python3
> 
> 验证python3.7版本

### 3、源码构建
> 3.1 安装sasl、fastjsonschema
>
> 3.1.1 yum -y install cyrus-sasl cyrus-sasl-devel cyrus-sasl-lib
> 
> 3.1.2 pip3 install fastjsonschema
> 
> 3.1.3 yum -y install openldap-devel
> 
> 3.1.4 pip3 install python_ldap
> 
> 3.1.5 cd cd smoke-test && pip install -r requirements.txt
> 
> 3.2 安装命令行
> 
> ./gradlew :metadata-ingestion:installDev
> 
> 3.3 后端打包 
> 
> 执行./gradlew metadata-service:war:build
> 
> 3.4 前端打包 
> 
> 修改node版本: 找到datahub-0.10.0/datahub-web-react/build.gradle, 修改version为'16.10.0'
>
> export NODE_OPTIONS="--max-old-space-size=8192"
> 
> 执行./gradlew :datahub-frontend:dist -x yarnTest -x yarnLint

### 4 启动datahub
> 新增docker-compose.yml
```
networks:
  default:
    name: datahub_network
services:
  broker:
    container_name: broker
    depends_on:
      - zookeeper
    environment:
      - KAFKA_BROKER_ID=1
      - KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181
      - KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      - KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://broker:29092,PLAINTEXT_HOST://localhost:9092
      - KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
      - KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0
      - KAFKA_HEAP_OPTS=-Xms256m -Xmx256m
      - KAFKA_CONFLUENT_SUPPORT_METRICS_ENABLE=false
    hostname: broker
    image: confluentinc/cp-kafka:7.2.2
    ports:
      - ${DATAHUB_MAPPED_KAFKA_BROKER_PORT:-9092}:9092
  datahub-actions:
    depends_on:
      - datahub-gms
    environment:
      - DATAHUB_GMS_HOST=datahub-gms
      - DATAHUB_GMS_PORT=8080
      - DATAHUB_GMS_PROTOCOL=http
      - DATAHUB_SYSTEM_CLIENT_ID=__datahub_system
      - DATAHUB_SYSTEM_CLIENT_SECRET=JohnSnowKnowsNothing
      - KAFKA_BOOTSTRAP_SERVER=broker:29092
      - KAFKA_PROPERTIES_SECURITY_PROTOCOL=PLAINTEXT
      - METADATA_AUDIT_EVENT_NAME=MetadataAuditEvent_v4
      - METADATA_CHANGE_LOG_VERSIONED_TOPIC_NAME=MetadataChangeLog_Versioned_v1
      - SCHEMA_REGISTRY_URL=http://schema-registry:8081
    hostname: actions
    image: acryldata/datahub-actions:${ACTIONS_VERSION:-head}
    restart: on-failure:5
  datahub-frontend-react:
    container_name: datahub-frontend-react
    depends_on:
      - datahub-gms
    environment:
      - DATAHUB_GMS_HOST=datahub-gms
      - DATAHUB_GMS_PORT=8080
      - DATAHUB_SECRET=YouKnowNothing
      - DATAHUB_APP_VERSION=1.0
      - DATAHUB_PLAY_MEM_BUFFER_SIZE=10MB
      - JAVA_OPTS=-Xms512m -Xmx512m -Dhttp.port=9002 -Dconfig.file=datahub-frontend/conf/application.conf -Djava.security.auth.login.config=datahub-frontend/conf/jaas.conf -Dlogback.configurationFile=datahub-frontend/conf/logback.xml -Dlogback.debug=false -Dpidfile.path=/dev/null
      - KAFKA_BOOTSTRAP_SERVER=broker:29092
      - DATAHUB_TRACKING_TOPIC=DataHubUsageEvent_v1
      - ELASTIC_CLIENT_HOST=elasticsearch
      - ELASTIC_CLIENT_PORT=9200
    hostname: datahub-frontend-react
    image: ${DATAHUB_FRONTEND_IMAGE:-linkedin/datahub-frontend-react}:${DATAHUB_VERSION:-head}
    ports:
      - ${DATAHUB_MAPPED_FRONTEND_PORT:-9002}:9002
    volumes:
      - ${HOME}/.datahub/plugins:/etc/datahub/plugins
  datahub-gms:
    container_name: datahub-gms
    depends_on:
      - mysql
    environment:
      - DATAHUB_SERVER_TYPE=${DATAHUB_SERVER_TYPE:-quickstart}
      - DATAHUB_TELEMETRY_ENABLED=${DATAHUB_TELEMETRY_ENABLED:-true}
      - DATAHUB_UPGRADE_HISTORY_KAFKA_CONSUMER_GROUP_ID=generic-duhe-consumer-job-client-gms
      - EBEAN_DATASOURCE_DRIVER=com.mysql.jdbc.Driver
      - EBEAN_DATASOURCE_HOST=mysql:3306
      - EBEAN_DATASOURCE_PASSWORD=datahub
      - EBEAN_DATASOURCE_URL=jdbc:mysql://mysql:3306/datahub?verifyServerCertificate=false&useSSL=true&useUnicode=yes&characterEncoding=UTF-8
      - EBEAN_DATASOURCE_USERNAME=datahub
      - ELASTICSEARCH_HOST=elasticsearch
      - ELASTICSEARCH_INDEX_BUILDER_MAPPINGS_REINDEX=true
      - ELASTICSEARCH_INDEX_BUILDER_SETTINGS_REINDEX=true
      - ELASTICSEARCH_PORT=9200
      - ENTITY_REGISTRY_CONFIG_PATH=/datahub/datahub-gms/resources/entity-registry.yml
      - ENTITY_SERVICE_ENABLE_RETENTION=true
      - ES_BULK_REFRESH_POLICY=WAIT_UNTIL
      - GRAPH_SERVICE_DIFF_MODE_ENABLED=true
      - GRAPH_SERVICE_IMPL=elasticsearch
      - JAVA_OPTS=-Xms1g -Xmx1g
      - KAFKA_BOOTSTRAP_SERVER=broker:29092
      - KAFKA_SCHEMAREGISTRY_URL=http://schema-registry:8081
      - MAE_CONSUMER_ENABLED=true
      - MCE_CONSUMER_ENABLED=true
      - PE_CONSUMER_ENABLED=true
      - UI_INGESTION_ENABLED=true
    hostname: datahub-gms
    image: ${DATAHUB_GMS_IMAGE:-linkedin/datahub-gms}:${DATAHUB_VERSION:-head}
    ports:
      - ${DATAHUB_MAPPED_GMS_PORT:-8080}:8080
    volumes:
      - ${HOME}/.datahub/plugins:/etc/datahub/plugins
  datahub-upgrade:
    command:
      - -u
      - SystemUpdate
    container_name: datahub-upgrade
    environment:
      - EBEAN_DATASOURCE_USERNAME=datahub
      - EBEAN_DATASOURCE_PASSWORD=datahub
      - EBEAN_DATASOURCE_HOST=mysql:3306
      - EBEAN_DATASOURCE_URL=jdbc:mysql://mysql:3306/datahub?verifyServerCertificate=false&useSSL=true&useUnicode=yes&characterEncoding=UTF-8
      - EBEAN_DATASOURCE_DRIVER=com.mysql.jdbc.Driver
      - KAFKA_BOOTSTRAP_SERVER=broker:29092
      - KAFKA_SCHEMAREGISTRY_URL=http://schema-registry:8081
      - ELASTICSEARCH_HOST=elasticsearch
      - ELASTICSEARCH_PORT=9200
      - ELASTICSEARCH_INDEX_BUILDER_MAPPINGS_REINDEX=true
      - ELASTICSEARCH_INDEX_BUILDER_SETTINGS_REINDEX=true
      - ELASTICSEARCH_BUILD_INDICES_CLONE_INDICES=false
      - GRAPH_SERVICE_IMPL=elasticsearch
      - DATAHUB_GMS_HOST=datahub-gms
      - DATAHUB_GMS_PORT=8080
      - ENTITY_REGISTRY_CONFIG_PATH=/datahub/datahub-gms/resources/entity-registry.yml
    hostname: datahub-upgrade
    image: ${DATAHUB_UPGRADE_IMAGE:-acryldata/datahub-upgrade}:${DATAHUB_VERSION:-head}
  elasticsearch:
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - ES_JAVA_OPTS=-Xms256m -Xmx512m -Dlog4j2.formatMsgNoLookups=true
    healthcheck:
      retries: 4
      start_period: 2m
      test:
        - CMD-SHELL
        - curl -sS --fail 'http://localhost:9200/_cluster/health?wait_for_status=yellow&timeout=0s' || exit 1
    hostname: elasticsearch
    image: elasticsearch:7.10.1
    mem_limit: 1g
    ports:
      - ${DATAHUB_MAPPED_ELASTIC_PORT:-9200}:9200
    volumes:
      - esdata:/usr/share/elasticsearch/data
  elasticsearch-setup:
    container_name: elasticsearch-setup
    depends_on:
      - elasticsearch
    environment:
      - ELASTICSEARCH_HOST=elasticsearch
      - ELASTICSEARCH_PORT=9200
      - ELASTICSEARCH_PROTOCOL=http
    hostname: elasticsearch-setup
    image: ${DATAHUB_ELASTIC_SETUP_IMAGE:-linkedin/datahub-elasticsearch-setup}:${DATAHUB_VERSION:-head}
  kafka-setup:
    container_name: kafka-setup
    depends_on:
      - broker
      - schema-registry
    environment:
      - DATAHUB_PRECREATE_TOPICS=${DATAHUB_PRECREATE_TOPICS:-false}
      - KAFKA_BOOTSTRAP_SERVER=broker:29092
      - KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181
    hostname: kafka-setup
    image: ${DATAHUB_KAFKA_SETUP_IMAGE:-linkedin/datahub-kafka-setup}:${DATAHUB_VERSION:-head}
  mysql:
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_bin --default-authentication-plugin=mysql_native_password
    container_name: mysql
    environment:
      - MYSQL_DATABASE=datahub
      - MYSQL_USER=datahub
      - MYSQL_PASSWORD=datahub
      - MYSQL_ROOT_PASSWORD=datahub
    hostname: mysql
    image: mysql:5.7
    ports:
      - ${DATAHUB_MAPPED_MYSQL_PORT:-33061}:3306
    volumes:
      - ../mysql/init.sql:/docker-entrypoint-initdb.d/init.sql
      - mysqldata:/var/lib/mysql
  mysql-setup:
    container_name: mysql-setup
    depends_on:
      - mysql
    environment:
      - MYSQL_HOST=mysql
      - MYSQL_PORT=3306
      - MYSQL_USERNAME=datahub
      - MYSQL_PASSWORD=datahub
      - DATAHUB_DB_NAME=datahub
    hostname: mysql-setup
    image: ${DATAHUB_MYSQL_SETUP_IMAGE:-acryldata/datahub-mysql-setup}:${DATAHUB_VERSION:-head}
  schema-registry:
    container_name: schema-registry
    depends_on:
      - broker
    environment:
      - SCHEMA_REGISTRY_HOST_NAME=schemaregistry
      - SCHEMA_REGISTRY_KAFKASTORE_SECURITY_PROTOCOL=PLAINTEXT
      - SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS=broker:29092
    hostname: schema-registry
    image: confluentinc/cp-schema-registry:7.2.2
    ports:
      - ${DATAHUB_MAPPED_SCHEMA_REGISTRY_PORT:-8081}:8081
  zookeeper:
    container_name: zookeeper
    environment:
      - ZOOKEEPER_CLIENT_PORT=2181
      - ZOOKEEPER_TICK_TIME=2000
    hostname: zookeeper
    image: confluentinc/cp-zookeeper:7.2.2
    ports:
      - ${DATAHUB_MAPPED_ZK_PORT:-2181}:2181
    volumes:
      - zkdata:/var/lib/zookeeper
version: "2.3"
volumes:
  esdata: null
  mysqldata: null
  zkdata: null
```
> python3 -m datahub docker quickstart --start -f docker-compose.yml

### 5 停止datahub
> python3 -m datahub docker quickstart --stop -f docker-compose.yml
