
http://gitlab.alibaba-inc.com/middleware-container/jar-deploy

## 2017-09-14

appctl.sh里自动识别jdk8，并调整jvm内存参数配置

## 2017-08-14

修复升级os docker镜像导致preload.sh无法正确识别本机ip的问题。 middleware-container/pandora-boot#542

## 2017-06-28

修复setevn.sh被source多次，导致hsf没有online的问题。 middleware-container/pandora-boot#486

## 2017-06-19

support os SERVICE_OPTS env. middleware-container/pandora-boot#448