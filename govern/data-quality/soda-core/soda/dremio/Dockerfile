
ARG JAVA_IMAGE="openjdk:8-jdk"
FROM ${JAVA_IMAGE} as base

LABEL maintainer=Dremio

ARG DOWNLOAD_URL="https://download.dremio.com/community-server/dremio-community-LATEST.tar.gz"

RUN \
  apt-get update \
  && apt-get install wget -y \
  && rm -rf /var/lib/apt/lists/* \
  \
  && mkdir -p /opt/dremio \
  && mkdir -p /var/lib/dremio \
  && mkdir -p /var/run/dremio \
  && mkdir -p /var/log/dremio \
  && mkdir -p /opt/dremio/data \
  \
  && groupadd --system dremio \
  && useradd --base-dir /var/lib/dremio --system --gid dremio dremio \
  && chown -R dremio:dremio /opt/dremio/data \
  && chown -R dremio:dremio /var/run/dremio \
  && chown -R dremio:dremio /var/log/dremio \
  && chown -R dremio:dremio /var/lib/dremio \
  && wget -q "${DOWNLOAD_URL}" -O dremio.tar.gz \
  && tar vxfz dremio.tar.gz -C /opt/dremio --strip-components=1 \
  && rm -rf dremio.tar.gz

EXPOSE 9047/tcp
EXPOSE 31010/tcp
EXPOSE 32010/tcp
EXPOSE 45678/tcp

USER dremio
WORKDIR /opt/dremio
ENV DREMIO_HOME /opt/dremio
ENV DREMIO_PID_DIR /var/run/dremio
ENV DREMIO_GC_LOGS_ENABLED="yes"
ENV DREMIO_GC_LOG_TO_CONSOLE="yes"
ENV DREMIO_LOG_DIR="/var/log/dremio"
ENTRYPOINT ["bin/dremio", "start-fg"]
