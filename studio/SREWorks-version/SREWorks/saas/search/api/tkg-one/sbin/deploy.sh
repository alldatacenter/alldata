#!/bin/bash

set -e
set -x

# 安装 Python
apk add --no-cache --update python gettext
python -m ensurepip
rm -r /usr/lib/python*/ensurepip
pip install -i https://pypi.antfin-inc.com/simple/ --no-cache-dir --upgrade pip setuptools requests
apk add --no-cache --virtual .build-deps \
            gcc \
            make \
            libc-dev \
            musl-dev \
            linux-headers \
            pcre-dev \
            python-dev \
    && apk add --no-cache mariadb-dev \
    && sed '/st_mysql_options options;/a unsigned int reconnect;' /usr/include/mysql/mysql.h -i.bkp \
    && pip install -i https://pypi.antfin-inc.com/simple/ --no-cache-dir MySQL-python==1.2.3rc1 \
    && runDeps="$( \
            scanelf --needed --nobanner --recursive /venv \
                    | awk '{ gsub(/,/, "\nso:", $2); print "so:" $2 }' \
                    | sort -u \
                    | xargs -r apk info --installed \
                    | sort -u \
    )" \
    && apk add --virtual .python-rundeps $runDeps \
    && apk del .build-deps

# 安装 bigdatak 依赖
BIGDATAK_URL="https://gitlab-ci-token:8e30ddf9a8ff9b3dc701ea47d7cbbb@gitlab-sc.alibaba-inc.com/bigdata/bigdatak.git"
TMP_REPO=/tmp/repo_op
mkdir -p ${TMP_REPO}
apk add --no-cache --update git mysql-client
git clone --depth 1 -b $1 ${BIGDATAK_URL} ${TMP_REPO}
cd ${TMP_REPO}
pip install -i https://pypi.antfin-inc.com/simple/ --no-cache-dir --upgrade -r requirements.txt
mv ./bigdatak /usr/local/bigdatak
touch /usr/local/bigdatak/conf/global/flag_pssh_channel  # 切换不使用 tops-pssh/pscp
cd /
rm -rf ${TMP_REPO}
apk del git

# 创建 /home/admin 目录，服务自身依赖
mkdir -p /home/admin

# 拷贝 Docker 启动配置
rm -rf /app/application.properties
mv /app/application-docker.properties /app/application.properties
cat /app/application.properties
