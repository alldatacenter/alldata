#!/bin/bash

set -e

# 环境变量定义
export PRODUCT=${PRODUCT}
export ROOT_DIR=/home/admin/${APP_NAME}
export HTTP_SERVER_PORT=${HTTP_SERVER_PORT-"7001"}

# 增加自定义环境变量
PYTHON_BIN=/usr/local/t-tesla-python27/bin/python
[ ! -e "${PYTHON_BIN}" ] && PYTHON_BIN=python

# 启动服务
exec /usr/local/t-tesla-python27/bin/gunicorn --workers 2 --threads 8 -k gevent --timeout 300 --bind 0.0.0.0:${HTTP_SERVER_PORT} app:app
