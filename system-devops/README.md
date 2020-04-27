# 1、system-devops介绍 
~~~markdown
system-devops：
	使用ELK技术栈搭建日志搜索平台；
    使用skywalking，Phoenix实现监控平台；
    使用scala、playframework，docker，k8s，shell实现云容器平台，包含服务管理（查看docker容器配置，添加容器实例，授权记录，操作记录，历史版本回溯，k8s启停服务，操作记录，对比yaml配置，更新服务）、任务管理、配置管理、镜像构建（包括环境变量和参数配置）、应用日志
    使用自动化运维平台CoDo开发system-devops；
    使用Kong开发统一网关入口系统system-api-gateway；
    使用vue、scala、playframework、docker、k8s、Prometheus、grafana开发监控告警平台system-alarm-platform；
    使用Apollo开发system-config配置中心；
system-devops目前包含2个模块，system-devops-web, system-devops-backend

1、system-devops-web: 
    1.1 主要做的事情：
        项目，机器资源管理
        项目打包发布管理页面
        项目运行log监控展示 
    1.2 主要模块xxx

2、system-devops-backend
    2.1 主要做devops的后台，目前代码功能暂时没有完全cover

3、system-devops-autogen and system-devops-automain
    3.1 自动生成scala，playframework项目crud业务代码
    3.2 增量更新system-devops-autogen这个项目
~~~
