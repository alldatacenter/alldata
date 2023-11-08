## 部署指南

### 1、后端结构

```
├── studio
│   ├── config（配置中心，必须启动）
│   ├── eureka（注册中心，必须启动）
│   ├── gateway（网关，必须启动）
│   ├── install（脚本目录，数据库脚本必须）
│   │   ├── 16gdata
│   │   ├── 16gmaster
│   │   │   ├──studio
│   │   │  │   ├──studio-0.x.x.sql
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

```

### 2、前端结构


```
│ ── micro-ui
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


### 3、准备工作


```
JDK >= 1.8 
Mysql >= 5.7.0 (推荐5.7及以上版本)
Redis >= 3.0
Maven >= 3.0
Node >= 10.15.3
RabbitMQ >= 3.0.x
```
> 使用Mysql 8的用户注意导入数据时的编码格式问题

### 4、本地启动/运行系统

> 首先确保启动rabbitmq，mysql，redis已经启动

#### 4.1 后端运行

1、前往GitHub项目页面(https://github.com/alldatacenter/alldata)
推荐使用版本控制方式下载解压到工作目录或IDEA直接从VCS获取项目代码，便于同步最新社区版改动， alldata/studio/为项目前后端存放路径。

2、项目导入到IDEA后，会自动加载Maven依赖包，初次加载会比较慢（根据自身网络情况而定）

3、创建数据库studio：到 `factory/studio/install/sql`目录下sql数据脚本，把` studio.sql`和`studio-v0.x.x.sql`导入本地或线上Mysql数据库

4、导入BI sql, 参考alldata/bi_quickstart.md

5、修改该文件 `alldata/studio/config/src/main/resources/config/application-common-dev.yml`的rabbitmq，mysql，redis为自己的服务

6、打开运行基础模块（启动没有先后顺序）

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


#### 4.2 前端运行
```
cd alldata/studio/micro-ui
npm run dev
```
启动成功，会自动弹出浏览器登录页


#### 注意目前视频能看到的功能都已开源，若发现“数据集成”菜单没有.
#### 可只导入factory/studio/install/sql下的studio.sql + studio-v0.x.x + 数据集成。
#### 其他菜单若发现没有的话，也可自行配置，具体参考 https://github.com/alldatacenter/alldata/issues/489


#### 4.3 启动SystemService项目，本地运行时eureka配置处，改成localhost。及其他项目同理。
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

### 5、服务器集群部署
| 16gmaster                | port | ip             |
|--------------------------|------| -------------- |
| system-service           | 8000 | 16gmaster  |
| data-market-service      | 8822 | 16gmaster  |
| service-data-integration | 8824 | 16gmaster  |
| data-metadata-service    | 8820 | 16gmaster  |
| data-system-service      | 8810 | 16gmaster  |
| service-data-dts         | 9536 | 16gmaster  |
| config                   | 8611 | 16gmaster  |

| 16gslave                      | port | ip             |
|-------------------------------| ---- | -------------- |
| eureka                  | 8610 | 16gslave    |
| service-workflow        | 8814 | 16gslave    |
| data-metadata-service-console    | 8821 | 16gslave    |
| service-data-mapping    | 8823 | 16gslave    |
| data-masterdata-service | 8828 | 16gslave    |
| data-quality-service    | 8826 | 16gslave    |

| 16gdata               | port | ip             |
|-----------------------| ---- | -------------- |
| data-standard-service | 8825 | 16gdata |
| data-visual-service   | 8827 | 16gdata |
| email-service         | 8812 | 16gdata |
| file-service          | 8811 | 16gdata |
| quartz-service        | 8813 | 16gdata |
| gateway               | 9538 | 16gslave    |


### 6、部署方式

> 数据库版本为 **mysql5.7** 及以上版本
#### 1、`studio`数据库初始化
>
> 1.1 source install/sql/studio.sql
> 1.2 source install/sql/studio-v0.x.x.sql
> 1.3 导入BI sql, 参考alldata/bi_quickstart.md

#### 2、修改 **config** 配置中心

> **config** 文件夹下的配置文件, 修改 **redis**, **mysql** 和 **rabbitmq** 的配置信息
>
#### 3、项目根目录下执行
```
1、缺失aspose-words,要手动安装到本地仓库
2、cd alldata/studio/common
3、安装命令：windows使用git bash执行, mac直接执行以下命令
4、mvn install:install-file -Dfile=aspose-words-20.3.jar -DgroupId=com.aspose -DartifactId=aspose-words -Dversion=20.3 -Dpackaging=jar
5、安装成功重新刷新依赖,重新打包
```
> cd alldata/studio/common
> mvn install:install-file -Dfile=/alldata/studio/common/aspose-words-20.3.jar -DgroupId=com.aspose -DartifactId=aspose-words -Dversion=20.3 -Dpackaging=jar
> mvn clean install -DskipTests && mvn clean package -DskipTests
> 获取安装包build/studio-release-0.4.x.tar.gz
>
> 上传服务器解压
>
#### 4、部署`stuido`[后端]
#### 单节点启动[All In One]

> 1、启动eureka on `16gslave`
>
> 2、启动config on `16gmaster`
>
> 3、启动gateway on `16gdata`
>
> 4、启动其他Jar

#### 5、三节点启动[16gmaster, 16gslave, 16gdata]
> 1. 单独启动 eureka on `16gslave`
>
> 2. 单独启动config on `16gmaster`
>
> 3. 单独启动gateway on `16gdata`
>
> 4. 启动`16gslave`, sh start16gslave.sh
>
> 5. 启动`16gdata`, sh start16gdata.sh
>
> 6. 启动`16gmaster`, sh start16gmaster.sh

#### 6、部署`studio`[前端]:
#### 前端部署

#### 安装依赖

> 依次安装：
> nvm install v10.15.3 && nvm use v10.15.3

> npm install -g @vue/cli

> npm install script-loader

> npm install jsonlint

> npm install vue2-jsoneditor

> npm install

> npm run build:prod [生产]
>
> 生产环境启动前端micro-ui项目, 需要[配置nginx]
```markdown
# For more information on configuration, see:
#   * Official English Documentation: http://nginx.org/en/docs/
#   * Official Russian Documentation: http://nginx.org/ru/docs/

user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log;
pid /run/nginx.pid;

# Load dynamic modules. See /usr/share/doc/nginx/README.dynamic.
include /usr/share/nginx/modules/*.conf;

events {
worker_connections 1024;
}

http {
log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
'$status $body_bytes_sent "$http_referer" '
'"$http_user_agent" "$http_x_forwarded_for"';

    access_log  /var/log/nginx/access.log  main;

    sendfile            on;
    tcp_nopush          on;
    tcp_nodelay         on;
    keepalive_timeout   65;
    types_hash_max_size 4096;

    include             /etc/nginx/mime.types;
    default_type        application/octet-stream;

    # Load modular configuration files from the /etc/nginx/conf.d directory.
    # See http://nginx.org/en/docs/ngx_core_module.html#include
    # for more information.
    include /etc/nginx/conf.d/*.conf;
    server {
		listen       80;
		server_name  16gmaster;	
		add_header Access-Control-Allow-Origin *;
		add_header Access-Control-Allow-Headers X-Requested-With;
		add_header Access-Control-Allow-Methods GET,POST,OPTIONS;
		location / {
			root /studio/micro-ui/dist;
			index index.html;
			try_files $uri $uri/ /index.html;
		}
		location /api/ {
			proxy_pass  http://16gdata:9538/;
			proxy_set_header Host $proxy_host;
			proxy_set_header X-Real-IP $remote_addr;
			proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
		}
	}
}
```
> 测试环境启动前端micro-ui项目
>
> npm run dev [测试]
>
> 访问`studio`页面
>
> curl http://localhost:8013
>
> 用户名：admin 密码：123456

### 7、数据集成配置教程

> 先找到用户管理-菜单管理, 新增【数据集成】目录
>
> 新增【数据集成】下面的菜单, 菜单各项按如下配置输入, 之后进入角色管理
>
> 配置admin账号的目录数据权限, 选中刚才新增的数据集成目录及里面的菜单, 刷新或重新登录即可访问【数据集成】

<br/>
<img width="1215" alt="image" src="https://user-images.githubusercontent.com/9457212/233446739-41ea4501-bb09-4eb2-86de-21c168784564.png">
<br/>
<br/>
<img width="1215" alt="image" src="https://user-images.githubusercontent.com/9457212/233446763-cbb15105-b209-4b8f-b3f2-c41b5a607dd9.png">
<br/>
<br/>
<img width="1215" alt="image" src="https://user-images.githubusercontent.com/9457212/233447516-c952efd0-f8e2-4181-8608-1f513f9c0e93.png">
<br/>


### 8、常见问题
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

```
前置 -
1、数据集成教程

运行 -
1、数据集成教程

原因 -
1、教程
https://github.com/alldatacenter/alldata/blob/master/studio/modules/service-data-dts-parent/DTS_QuickStart.md

```

```
前置 -
1、数据集成教程

运行 -
1、数据集成教程

原因 -
1、教程
2、依赖datax,安装datax: https://blog.csdn.net/qq_18896247/article/details/123127487
3、https://github.com/alldatacenter/alldata/blob/master/studio/modules/service-data-dts-parent/DTS_QuickStart.md

```

```
前置 -
1、元数据数据库文档下载

运行 -
1、元数据数据库文档下载，依赖报错

原因 -
1、缺失aspose-words,要手动安装到本地仓库
2、cd alldata/studio/common
3、安装命令：windows使用git bash执行, mac直接执行以下命令
4、mvn install:install-file -Dfile=aspose-words-20.3.jar -DgroupId=com.aspose -DartifactId=aspose-words -Dversion=20.3 -Dpackaging=jar
5、安装成功重新刷新依赖,重新打包
```
