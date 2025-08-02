## 部署指南

### 1、后端结构

```
├── moat
│   ├── config（配置中心，必须启动）
│   ├── eureka（注册中心，必须启动）
│   ├── gateway（网关，必须启动）
│   ├── install（脚本目录，数据库脚本必须）
│   │   ├── 16gdata
│   │   ├── 16gmaster
│   │   │   ├──studio
│   │   │   │   ├──alldata-0.x.x.sql
│   │   ├── 16gsla 
│   ├── studio（各模块目录）
│   │   ├── codegen-service-parent（代码生成，可选启动）
│   │   ├── data-market-service-parent（数据服务，可选启动）
│   │   ├── data-masterdata-service-parent（数据模型，可选启动）
│   │   ├── data-metadata-service-parent（元数据管理，可选启动）
│   │   ├── data-quality-service-parent（数据质量，可选启动）
│   │   ├── data-standard-service-parent（数据标准，可选启动）
│   │   ├── data-system-service-parent（基础服务，必须启动）
│   │   ├── data-visual-service-parent（数据可视化模块，可选启动）
│   │   ├── email-service-parent（邮件管理模块，可选启动）
│   │   ├── file-service-parent（文件管理模块，可选启动）
│   │   ├── quartz-service-parent（定时任务模块，可选启动）
│   │   ├── service-data-dts-parent（数据集成模块，可选启动）
│   │   ├── system-service-parent（系统管理模块，必须启动）
│   ├── pom.xml

```

### 2、前端结构

```
├── moat_ui
│   ├── LICENSE
│   ├── babel.config.js
│   ├── jest.config.js
│   ├── npm.version
│   ├── package.json
│   ├── plopfile.js
│   ├── postcss.config.js
│   ├── public
│   ├── src
│   └── vue.config.js
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

### 4、启动基础软件

> 首先确保启动rabbitmq，mysql，redis已经启动

### 5、后端运行

5.1、前往GitHub项目页面(https://github.com/alldatacenter/alldata)
推荐使用版本控制方式下载解压到工作目录或IDEA直接从VCS获取项目代码，便于同步最新社区版改动， alldata/moat/为项目前后端存放路径。

5.2、项目导入到IDEA后，会自动加载Maven依赖包，初次加载会比较慢（根据自身网络情况而定）

5.3、创建数据库studio：到 `install/sql`目录下sql数据脚本，把` alldata.sql`和`alldata-v0.x.x.sql`导入本地或线上Mysql数据库

5.4、导入BI sql, 参考alldata/bi_quickstart.md

5.5、修改该文件 `alldata/moat/config/src/main/resources/config/application-common-dev.yml`的rabbitmq，mysql，redis为自己的服务

### 6、项目启动

6.1、运行基础模块（启动没有先后顺序）

```
DataxEurekaApplication.java（注册中心模块 必须）
DataxConfigApplication.java（配置中心模块 必须）
DataxGatewayApplication.java（网关模块 必须）
SystemServiceApplication.java（系统模块 必须，不启动无法登录）
```
其他模块可根据需求，自行决定启动

6.1 启动Eurake项目

6.1.1 找到moat/studio/eureka/src/main/java/cn/datax/eureka/DataxEurekaApplication.java 运行启动

6.1.2 浏览器访问 http://localhost:8610/，看到以下页面表示启动成功

6.2启动Config项目

6.2.1 修改bootstrap.yml文件，本地运行时eureka配置处，改成localhost

6.2.2 找到moat/studio/config/src/main/java/cn/datax/config/DataxConfigApplication.java，运行启动

6.3  启动Gateway项目

6.3.1 修改bootstrap.yml文件，本地运行时eureka配置处，改成localhost

6.3.2 找到moat/studio/gateway/src/main/java/cn/datax/gateway/DataxGatewayApplication.java，启动项目

6.3.3 启动完后，可以在Eureka中发现刚才启动的服务

6.4 启动SystemService项目，本地运行时eureka配置处，改成localhost。及其他项目同理。


### 7、前端运行
```
cd alldata/moat-ui

nvm use v16.20.2

npm install

npm run dev

npm run build
```
启动成功，会自动弹出浏览器登录页

访问：http://localhost:8013 账号密码：admin/123456


### 8、启动SystemService项目，本地运行时eureka配置处，改成localhost。及其他项目同理。
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
系统监控 - system-service-parent ~ system-service ~ SystemServiceApplication
批量/定时任务 - quartz-service-parent ~ quartz-service ~ DataxQuartzApplication
代码生成 - codegen-service-parent ~ codegen-service ~ DataxCodeGenApplication
邮件服务 - email-service-parent ~ email-service ~ DataxMailApplication
文件服务 - file-service-parent ~ file-service ~ DataxFileApplication
```

### 9、服务器集群部署
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
| gateway               | 9538 | 16gdata    |


### 10、部署方式

> 数据库版本为 **mysql5.7** 及以上版本
#### 10.1、`alldata``数据库初始化
>
> 1.1 source install/sql/alldata.sql
> 1.2 source install/sql/alldata-v0.x.x.sql
> 1.3 导入BI sql, 参考alldata/bi_quickstart.md

#### 10.2、修改 **config** 配置中心

> **config** 文件夹下的配置文件, 修改 **redis**, **mysql** 和 **rabbitmq** 的配置信息
>
#### 10.3、项目根目录下执行
```
1、缺失aspose-words,要手动安装到本地仓库
2、cd alldata/moat/common
3、安装命令：windows使用git bash执行, mac直接执行以下命令
4、mvn install:install-file -Dfile=aspose-words-20.3.jar -DgroupId=com.aspose -DartifactId=aspose-words -Dversion=20.3 -Dpackaging=jar
5、安装成功重新刷新依赖,重新打包
```
> cd alldata/moat/common
> mvn install:install-file -Dfile=/alldata/moat/common/aspose-words-20.3.jar -DgroupId=com.aspose -DartifactId=aspose-words -Dversion=20.3 -Dpackaging=jar
> mvn clean install -DskipTests && mvn clean package -DskipTests
> 获取安装包build/alldata-release-0.6.x.tar.gz
>
> 上传服务器解压
>
#### 10.4、部署`stuido`[后端]
#### 单节点启动[All In One]

> 1、启动eureka on `16gslave`
>
> 2、启动config on `16gmaster`
>
> 3、启动gateway on `16gdata`
>
> 4、启动其他Jar

#### 10.5、三节点启动[16gmaster, 16gslave, 16gdata]
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


### 11、数据集成配置教程

#### 注意目前视频能看到的功能都已开源，若发现“数据集成”菜单没有.
#### 可只导入moat/studio/install/sql下的alldata.sql + alldata-v0.x.x + 数据集成。
#### 其他菜单若发现没有的话，也可自行配置，具体参考 https://github.com/alldatacenter/alldata/issues/489

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
