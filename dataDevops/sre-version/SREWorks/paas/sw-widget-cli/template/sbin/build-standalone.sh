#!/bin/bash

set -x
set -e

cd /app/docs/
sh /app/docs/build.sh

# 编译打包 (包含当前版本)
cd /app
yarn install --registry=https://registry.npmmirror.com
npm run build -e production production
cd /app/build
mv /app/docs/pictures /app/docs/_book/pictures
mv /app/docs/_book /app/build/docs
rm -rf service-worker.js
NOW=$(date '+%Y%m%d%H%M%S')
echo "version: ${NOW}" > config.yaml
zip -r /app/build.zip ./

