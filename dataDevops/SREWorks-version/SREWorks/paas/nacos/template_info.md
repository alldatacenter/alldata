# 源码自动生成模板 pandora-boot-archetype-docker

### 概述

* 模板: pandora-boot-archetype-docker
* 模板答疑人: [子观](https://work.alibaba-inc.com/nwpipe/u/64988)
* 模板使用时间: 2019-08-09 13:53:08

### Docker
* Image: reg.docker.alibaba-inc.com/bootstrap/image
* Tag: 0.1
* SHA256: e4b70f4f7d0b60aa3e5666eba441a376b31ec6e0bd550a4efc5af8f057c6d7d8

### 用户输入参数
* repoUrl: "git@gitlab.alibaba-inc.com:PE3/tesla-nacos.git" 
* appName: "tesla-nacos" 
* javaVersion: "1.8" 
* groupId: "com.alibaba.tesla" 
* artifactId: "tesla-nacos" 
* style: "springmvc,diamond" 
* operator: "183744" 

### 上下文参数
* appName: tesla-nacos
* operator: 183744
* gitUrl: git@gitlab.alibaba-inc.com:PE3/tesla-nacos.git
* branch: master


### 命令行
	sudo docker run --rm -v `pwd`:/workspace -e repoUrl="git@gitlab.alibaba-inc.com:PE3/tesla-nacos.git" -e appName="tesla-nacos" -e javaVersion="1.8" -e groupId="com.alibaba.tesla" -e artifactId="tesla-nacos" -e style="springmvc,diamond" -e operator="183744"  reg.docker.alibaba-inc.com/bootstrap/image:0.1

