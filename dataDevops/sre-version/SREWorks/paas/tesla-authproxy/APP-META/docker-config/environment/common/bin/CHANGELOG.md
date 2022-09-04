
http://gitlab.alibaba-inc.com/middleware-container/jar-deploy

## 2018-03-28

增加spring boot2的management.server.port=7002 的配置支持

## 2018-01-10

appctl.sh里增加应用中心日志查看页面的链接，提示用户查看启动日志，方便排查问题

## 2017-12-04

appctl.sh识别${APP_NAME}.jar并自动展开。 middleware-container/pandora-boot#265

## 2017-09-14

appctl.sh里自动识别jdk8，并调整jvm内存参数配置

## 2017-08-14

修复升级os docker镜像导致preload.sh无法正确识别本机ip的问题。 middleware-container/pandora-boot#542

## 2017-06-28

修复setevn.sh被source多次，导致hsf没有online的问题。 middleware-container/pandora-boot#486

## 2017-06-19

support os SERVICE_OPTS env. middleware-container/pandora-boot#448
