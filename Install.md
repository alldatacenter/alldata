## 部署指南

> **提示**
近期有许多小伙伴开始关注并尝试部署AllData项目，为方便小伙伴快速本地部署体验，便有了这篇文档。

**AllData**通过汇聚大数据与AI领域生态组件，提供细分领域AllData数字化解决方案：大数据平台[数据中台]集成、大数据平台[数据中台]湖仓分析、大数据平台[数据中台]开发治理、大数据平台[数据中台]集群运维

## **后端结构**

```
├── studio
│   ├── config（配置中心，必须启动）
│   ├── eureka（注册中心，必须启动）
│   ├── gateway（网关，必须启动）
│   ├── install（脚本目录，数据库脚本必须）
│   │   ├── 16gdata
│   │   ├── 16gmaster
│   │   │   ├──studio
│   │   │  │   ├──studio-all.sql( 等于 studio.sql + studio-v0.3.7.sql + 数据集成)
│   │   │  │   ├──studio-v0.3.7.sql
│   │   │  │   ├── studio.sql 
│   ├── modules（各模块目录）
│   │   ├── codegen-service-parent（代码生成模块，可选启动）
│   │   ├── data-market-service-parent（数据集市模块，可选启动）
│   │   ├── data-masterdata-service-parent（主数据服务模块，可选启动）
│   │   ├── data-metadata-service-parent（元数据管理模块，可选启动）
│   │   ├── data-quality-service-parent（数据质量模块，可选启动）
│   │   ├── data-standard-service-parent（数据标准模块，可选启动）
│   │   ├── data-system-service-parent
│   │   ├── data-visual-service-parent（数据可视化模块，可选启动）
│   │   ├── email-service-parent（邮件管理模块，可选启动）
│   │   ├── file-service-parent（文件管理模块，可选启动）
│   │   ├── quartz-service-parent（定时任务模块，可选启动）
│   │   ├── service-data-dts-parent（数据集成模块，可选启动）
│   │   ├── system-service-parent（系统管理模块，必须启动）
│   │  └── workflow-service-parent（工作流模块，可选启动）
│   ├── pom.xml

若没有studio-all.sql，等下社区下一版本发版
```

## **前端结构**


```
│ ── ui
     ├── public // 公共文件
    │   ├── favicon.ico // favicon图标
    │   ├── index.html  // html模板
    │   └── robots.txt //爬虫协议
     ├── src    // 源代码         
    │   ├── App.vue
    │   ├── api    // 所有请求
    │   ├── assets// 主题 字体等静态资源
    │   ├── components // 全局公用组件
    │   ├── directive  // 全局指令
    │   ├── filters
    │   ├── icons 
    │   ├── layout  // 布局
    │   ├── main.js // 入口 加载组件 初始化等
    │   ├── mixins
    │   ├── router  // 路由
    │   ├── settings.js // 系统配置
    │   ├── store  // 全局 store管理
    │   ├── styles // 样式
    │   ├── utils  // 全局公用方法
    │   ├── vendor
    │   └── views  // view页面
    │       ├── components
    │       ├── dashboard
    │       ├── dts
    │       ├── features
    │       ├── generator
    │       ├── govern
    │       ├── home.vue
    │       ├── lakehouse
    │       ├── login.vue
    │       ├── market
    │       ├── masterdata
    │       ├── metadata
    │       ├── mnt
    │       ├── monitor
    │       ├── nested
    │       ├── quality
    │       ├── standard
    │       ├── system
    │       ├── tools
    │       ├── visual
    │       └── workflow
    └── vue.config.js
    ├── package.json
    ├── plopfile.js
    ├── postcss.config.js

```


## 准备工作


```
JDK >= 1.8 
Mysql >= 5.7.0 (推荐5.7及以上版本)
Redis >= 3.0
Maven >= 3.0
Node >= 10.15.3
RabbitMQ >= 3.0.x
```
> 使用Mysql 8的用户注意导入数据时的编码格式问题




## 运行系统
首先确保启动rabbitmq，mysql，redis已经启动

#后端运行

1、前往GitHub项目页面(https://github.com/alldatacenter/alldata)
推荐使用版本控制方式下载解压到工作目录或IDEA直接从VCS获取项目代码，便于同步最新社区版改动， alldata/factory/studio/为项目前后端存放路径。

2、项目导入到IDEA后，会自动加载Maven依赖包，初次加载会比较慢（根据自身网络情况而定）

3、创建数据库studio：到 `factory/studio/install/16gmaster/studio`目录下sql数据脚本，把` studio.sql`和`studio-v0.3.7.sql`和`studio-v0.3.8.sql`导入本地或线上Mysql数据库

4、修改该文件 `alldata/factory/studio/config/src/main/resources/config/application-common-dev.yml`的rabbitmq，mysql，redis为自己的服务

5、打开运行基础模块（启动没有先后顺序）

```
DataxEurekaApplication.java（注册中心模块 必须）
DataxConfigApplication.java（配置中心模块 必须）
DataxGatewayApplication.java（网关模块 必须）
SystemServiceApplication.java（系统模块 必须，不启动无法登录）
```
其他模块可根据需求，自行决定启动

5.1 启动Eurake项目

1. 找到factory/studio/eureka/src/main/java/cn/datax/eureka/DataxEurekaApplication.java 运行启动

2. 浏览器访问 http://localhost:8610/，看到以下页面表示启动成功

5.2 启动Config项目
1. 修改bootstrap.yml文件，本地运行时eureka配置处，改成localhost
2. 找到factory/studio/config/src/main/java/cn/datax/config/DataxConfigApplication.java，运行启动

5.3  启动Gateway项目
1. 修改bootstrap.yml文件，本地运行时eureka配置处，改成localhost

2. 找到factory/studio/gateway/src/main/java/cn/datax/gateway/DataxGatewayApplication.java，启动项目

3. 启动完后，可以在Eureka中发现刚才启动的服务

5.4 启动SystemService项目，本地运行时eureka配置处，改成localhost。及其他项目同理。


# 前端运行
```
cd alldata/factory/studio/ui
npm run dev
```
启动成功，会自动弹出浏览器登录页


##### 注意目前视频能看到的功能都已开源，若发现“数据集成”菜单没有，可只导入factory/studio/install/16gmaster/studio下 的 studio-all.sql文件，该文件等于studio.sql + studio-v0.3.7.sql + studio-v0.3.8.sql + 数据集成。其他菜单若发现没有的话，也可自行配置，具体参考 https://github.com/alldatacenter/alldata/issues/489


## 启动SystemService项目，本地运行时eureka配置处，改成localhost。及其他项目同理。
```
系统管理 - system-service-parent ~ system-service ~ SystemServiceApplication
数据集成 - service-data-dts-parent ~ service-data-dts ~ DataDtsServiceApplication
元数据管理 - data-metadata-service-parent ~ data-metadata-service ~ DataxMetadataApplication
元数据管理 - data-metadata-service-parent ~ data-metadata-service-console ~ DataxConsoleApplication
数据标准 - data-standard-service-parent ~ data-standard-service ~ DataxStandardApplication
数据质量 - data-quality-service-parent ~ data-quality-service ~ DataxQualityApplication
数据资产 - data-masterdata-service-parent ~ data-masterdata-service ~ DataxMasterdataApplication
数据市场 - data-market-service-parent ~ data-market-service ~ DataxMarketApplication
数据市场 - data-market-service-parent ~ data-market-service-integration ~ DataxIntegrationApplication
数据市场 - data-market-service-parent ~ data-market-service-mapping ~ DataxMappingApplication
数据对比 - data-compare-service-parent ~ data-compare-service ~ DataCompareApplication
BI报表 - data-visual-service-parent ~ data-visual-service ~ DataxVisualApplication
流程编排 - workflow-service-parent ~ workflow-service ~ DataxWorkflowApplication
系统监控 - system-service-parent ~ system-service ~ SystemServiceApplication
批量/定时任务 - quartz-service-parent ~ quartz-service ~ DataxQuartzApplication
代码生成 - codegen-service-parent ~ codegen-service ~ DataxCodeGenApplication
邮件服务 - email-service-parent ~ email-service ~ DataxMailApplication
文件服务 - file-service-parent ~ file-service ~ DataxFileApplication
```

## 常见问题
```
前置 -
1、启动前是删除了pom.xml；
2、本地是V16版本的nodejs;

运行 -
1、启动后端相关服务；
2、启动前端npm run dev,报错：
multi ./node modules/.pnpm/webpack-dev-server3.1.3 webpack04.28.4/node modules/webpack-dev-server/clienthtp://192.168.0.118:8013/sockjs-node(webpack)/hot/dev-server.js ./src/main.js
Module not found: Error: Can't resolvebabel-loader'in D: workspaceldatacenter workspacelscit-datacenter-ui

原因 -
前端UI对应nodejs版本是v10.15.3 , 需要切换版本，为开发方便，一遍采用nvm进行管理
1、卸载nodejs;
2、安装nvm - https://www.jianshu.com/p/13c0b3ca7c71
3、安装v10.15.3版本： nvm install v10.15.3
4、根据实际切换版本：nvm use v10.15.3
5、安装依赖：npm install
6、启动前端：npm run dev
```
