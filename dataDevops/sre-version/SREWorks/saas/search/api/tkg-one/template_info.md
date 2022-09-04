# 源码自动生成模板 pandora-boot-archetype-docker

### 概述

* 模板: pandora-boot-archetype-docker
* 模板答疑人: [子观](https://work.alibaba-inc.com/nwpipe/u/64988)
* 模板使用时间: 2019-02-25 22:30:22

### Docker
* Image: reg.docker.alibaba-inc.com/bootstrap/image
* Tag: 0.1
* SHA256: e4b70f4f7d0b60aa3e5666eba441a376b31ec6e0bd550a4efc5af8f057c6d7d8

### 用户输入参数
* repoUrl: "git@gitlab.alibaba-inc.com:pe3/tkg-one.git" 
* appName: "tkg-one" 
* javaVersion: "1.8" 
* groupId: "com.alibaba.tesla" 
* artifactId: "tkg-one" 
* style: "hsf,tddl,diamond" 
* operator: "83242" 

### 上下文参数
* appName: tkg-one
* operator: 83242
* gitUrl: git@gitlab.alibaba-inc.com:pe3/tkg-one.git
* branch: master


### 命令行
	sudo docker run --rm -v `pwd`:/workspace -e repoUrl="git@gitlab.alibaba-inc.com:pe3/tkg-one.git" -e appName="tkg-one" -e javaVersion="1.8" -e groupId="com.alibaba.tesla" -e artifactId="tkg-one" -e style="hsf,tddl,diamond" -e operator="83242"  reg.docker.alibaba-inc.com/bootstrap/image:0.1

