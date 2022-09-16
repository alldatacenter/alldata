# Apache InLong

## 一、概述

Apache InLong（应龙）是一站式的海量数据集成框架，提供自动、安全、可靠和高性能的数据传输能力，方便业务构建基于流式的数据分析、建模和应用。

InLong 项目原名 TubeMQ ，专注于高性能、低成本的消息队列服务。为了进一步释放 TubeMQ 周边的生态能力，我们将项目升级为 InLong，专注打造一站式海量数据集成框架。 Apache InLong 依托 10 万亿级别的数据接入和处理能力，整合了数据采集、汇聚、存储、分拣数据处理全流程，拥有简单易用、灵活扩展、稳定可靠等特性。

### 1.1 特性

- **简单易用**：基于SaaS模式对外服务，用户只需要按主题发布和订阅数据即可完成数据的上报，传输和分发工作
- **稳定可靠**：系统源于实际的线上系统，服务上十万亿级的高性能及上千亿级的高可靠数据数据流量，系统稳定可靠
- **功能完善**：支持各种类型的数据接入方式，多种不同类型的MQ集成，以及基于配置规则的实时数据ETL和数据分拣落地，并支持以可插拔方式扩展系统能力
- **服务集成**：支持统一的系统监控、告警，以及细粒度的数据指标呈现，对于管道的运行情况，以数据主题为核心的数据运营情况，汇总在统一的数据指标平台，并支持通过业务设置的告警信息进行异常告警提醒
- **灵活扩展**：全链条上的各个模块基于协议以可插拔方式组成服务，业务可根据自身需要进行组件替换和功能扩展

### 1.2 架构

![image-20220911120354637](http://kmgy.top:9090/image/2022/9/11/image-20220911120354637_repeat_1662869034799__804112_repeat_1662883824543__994597.png)

### 1.3 模块

Apache InLong 服务于数据采集到落地的整个生命周期，按数据的不同阶段提供不同的处理模块，主要包括：

- **inlong-agent**，数据采集 Agent，支持从指定目录或文件读取常规日志、逐条上报。后续也将扩展 DB 采集等能力。
- **inlong-dataproxy**，一个基于 Flume-ng 的 Proxy 组件，支持数据发送阻塞和落盘重发，拥有将接收到的数据转发到不同 MQ（消息队列）的能力。
- **inlong-tubemq**，腾讯自研的消息队列服务，专注于大数据场景下海量数据的高性能存储和传输，在海量实践和低成本方面有着良好的核心优势。
- **inlong-sort**，对从不同的 MQ 消费到的数据进行 ETL 处理，然后汇聚并写入 Hive、ClickHouse、Hbase、Iceberg 等存储系统。
- **inlong-manager**，提供完整的数据服务管控能力，包括元数据、任务流、权限，OpenAPI 等。
- **inlong-dashboard**，用于管理数据接入的前端页面，简化整个 InLong 管控平台的使用。
- **inlong-audit**，对InLong系统的Agent、DataProxy、Sort模块的入流量、出流量进行实时审计对账。

### 1.4 已支持数据节点

| Type         | Name             | Version                      | Architecture          |
| ------------ | ---------------- | ---------------------------- | --------------------- |
| Extract Node | Auto Push        | None                         | Standard              |
|              | File             | None                         | Standard              |
|              | Kafka            | 2.x                          | Lightweight, Standard |
|              | MySQL            | 5.6, 5.7, 8.0.x              | Lightweight, Standard |
|              | MongoDB          | >= 3.6                       | Lightweight           |
|              | Oracle           | 11,12,19                     | Lightweight           |
|              | PostgreSQL       | 9.6, 10, 11, 12              | Lightweight           |
|              | Pulsar           | 2.8.x                        | Lightweight           |
|              | SQLServer        | 2012, 2014, 2016, 2017, 2019 | Lightweight           |
| Load Node    | Auto Consumption | None                         | Standard              |
|              | Hive             | 1.x, 2.x, 3.x                | Lightweight, Standard |
|              | Iceberg          | 0.12.x                       | Lightweight, Standard |
|              | ClickHouse       | 20.7+                        | Lightweight, Standard |
|              | Kafka            | 2.x                          | Lightweight, Standard |
|              | HBase            | 2.2.x                        | Lightweight, Standard |
|              | PostgreSQL       | 9.6, 10, 11, 12              | Lightweight, Standard |
|              | Oracle           | 11, 12, 19                   | Lightweight, Standard |
|              | MySQL            | 5.6, 5.7, 8.0.x              | Lightweight, Standard |
|              | TDSQL-PostgreSQL | 10.17                        | Lightweight, Standard |
|              | Greenplum        | 4.x, 5.x, 6.x                | Lightweight, Standard |
|              | Elasticsearch    | 6.x, 7.x                     | Lightweight, Standard |
|              | SQLServer        | 2012, 2014, 2016, 2017, 2019 | Lightweight, Standard |
|              | HDFS             | 2.x, 3.x                     | Lightweight, Standard |

## 二、InLong 部署文档

### 2.1 单机部署（StandAlone模式）

- 环境要求

  - MySQL 5.7+

  - Flink 1.13.5

  - [Docker](https://docs.docker.com/engine/install/) 19.03.1+

#### 2.1.1 准备消息队列

InLong 当前支持以下消息队列，根据使用情况**选择其一**即可。

- [InLong TubeMQ](https://inlong.apache.org/zh-CN/docs/modules/tubemq/quick_start)
- [Apache Pulsar 2.8.x](https://pulsar.apache.org/docs/en/2.8.1/standalone/)

这边选择安装InLong TubeMQ。TubeMQ已经集成在bin包中，不需要重新下载，下载bin包后直接解压，配置TubeMQ。

##### 2.1.1.1 TubeMQ

- 部署运行

###### a. 配置示例

TubeMQ 集群包含有两个组件: **Master** 和 **Broker**. Master 和 Broker 可以部署在相同或者不同的节点上，依照业务对机器的规划进行处理。我们通过如下3台机器搭建有2台Master的生产、消费的集群进行配置示例：

| 所属角色  | TCP端口 | TLS端口 | WEB端口 | 备注                                                  |
| --------- | ------- | ------- | ------- | ----------------------------------------------------- |
| Master    | 8099    | 8199    | 8080    | 元数据存储在ZooKeeper的`/tubemq`目录                  |
| Broker    | 8123    | 8124    | 8081    | 消息储存在`/stage/msg_data`                           |
| ZooKeeper | 2181    |         |         | 存储Master元数据及Broker的Offset内容，根目录`/tubemq` |

###### b. 准备工作

- ZooKeeper集群(详情见其他 Docker 部署Zookeeper集群)

选择安装路径后，安装包解压后的目录结构如下：

```text
/INSTALL_PATH/inlong-tubemq-server/
├── bin
├── conf
├── lib
├── logs
└── resources
```

###### c. 配置Master

编辑`conf/master.ini`，根据集群信息变更以下配置项

- Master IP和端口

```ini
[master]
hostName=YOUR_SERVER_IP                   // 替换为当前主机IP
port=8099
webPort=8080
```

- 访问授权Token

```ini
confModAuthToken=abc                     // 该token用于页面配置、API调用等
```

- 配置meta_zookeeper策略

```ini
[meta_zookeeper]                              // 同一个集群里Master必须使用同一套zookeeper环境，且配置一致
zkNodeRoot=/tubemq
zkServerAddr=localhost:2181              // 指向zookeeper集群，多个地址逗号分开
```

- 配置meta_bdb策略（可选） **注意**：由于Apache依赖包的LICENSE问题，从1.1.0版本开始TubeMQ发布的包不再包含BDB包，如果需要BDB存储元数据，业务需要自行下载com.sleepycat.je-7.3.7.jar包，要不系统运行时会报“ java.lang.ClassNotFoundException: com.sleepycat.je.ReplicaConsistencyPolicy”错误。

```ini
[meta_bdb]
repGroupName=tubemqGroup1                // 同一个集群的Master必须要用同一个组名，且不同集群的组名必须不同 
repNodeName=tubemqGroupNode1             // 同一个集群的master节点名必须是不同的名称
metaDataPath=/stage/meta_data
repHelperHost=FIRST_MASTER_NODE_IP:9001  // helperHost用于创建master集群，一般配置第一个master节点ip
```

- （可选）生产环境，多master HA级别

| HA级别 | Master数量 | 描述                                                         |
| ------ | ---------- | ------------------------------------------------------------ |
| 高     | 3 masters  | 任何主节点崩溃后，集群元数据仍处于读/写状态，可以接受新的生产者/消费者。 |
| 中     | 2 masters  | 一个主节点崩溃后，集群元数据处于只读状态。对现有的生产者和消费者没有任何影响。 |
| 低     | 1 master   | 主节点崩溃后，对现有的生产者和消费者没有影响。               |

**注意**：

- 基于Docker容器化的需要，master.ini文件里对[meta_zookeeper] 或 [meta_bdb] 如上3个参数部分都是使用的缺省设置，在实际组网使用时需要以Master节点真实信息配置
- Master所有节点的IP信息要在hosts配置文件里构造IP与hostName映射关系，如类似“192.168.0.1 192-168-0-1”
- 需保证Master所有节点之间的时钟同步

###### d. 配置Broker

编辑`conf/broker.ini`，根据集群信息变更以下配置项

- Broker IP和端口

```ini
[broker]
brokerId=0
hostName=YOUR_SERVER_IP                 // 替换为当前主机IP，broker目前只支持IP
port=8123
webPort=8081
defEthName=eth1                         // 获取真实 IP 的网卡
```

- Master地址

```ini
masterAddressList=YOUR_MASTER_IP1:8099,YOUR_MASTER_IP2:8099   //多个master以逗号分隔
```

- 数据目录

```ini
primaryPath=/stage/msg_data
```

- ZooKeeper集群地址

```ini
[zookeeper]                             // 同一个集群里Master和Broker必须使用同一套zookeeper环境，且配置一致
zkNodeRoot=/tubemq                      
zkServerAddr=localhost:2181             // 指向zookeeper集群，多个地址逗号分开
```

###### e. 启动Master

进入Master节点的 `bin` 目录下，启动服务:

```bash
./tubemq.sh master start
```

访问Master的管控台 `http://YOUR_MASTER_IP:8080` ，页面可查则表示master已成功启动: ![image-20220911125418949](http://kmgy.top:9090/image/2022/9/11/image-20220911125418949_repeat_1662872059102__850774_repeat_1662883817443__649223.png)

-  配置Broker元数据

Broker启动前，首先要在Master上配置Broker元数据，增加Broker相关的管理信息。在`Broker List` 页面, `Add Single Broker`，然后填写相关信息:

![image-20220911125449692](http://kmgy.top:9090/image/2022/9/11/image-20220911125449692_repeat_1662872089851__789292_repeat_1662883822009__121349.png)

需要填写的内容包括：

1. broker IP: broker server ip
2. authToken: `conf/master.ini` 文件中 `confModAuthToken` 字段配置的 token

然后上线Broker： ![image-20220911125517776](http://kmgy.top:9090/image/2022/9/11/image-20220911125517776_repeat_1662872117941__429609_repeat_1662883821011__898226.png)

###### f. 配置Broker Id

首先在master页面进行配置获得broker id，然后将broker id 填写至broker.ini的配置文件中。

编辑`conf/broker.ini`，根据集群信息变更以下配置项

- Broker IP和端口

```ini
[broker]
brokerId=YOUR_CONF_BROKER_ID							// 替换为当前master生成的Conf中的Broker Id
```

###### g. 启动Broker

进入broker节点的 `bin` 目录下，执行以下命令启动Broker服务：

```bash
./tubemq.sh broker start
```



刷新页面可以看到 Broker 已经注册，当 `当前运行子状态` 为 `idle` 时， 可以增加topic: ![image-20220911125534493](http://kmgy.top:9090/image/2022/9/11/image-20220911125534493_repeat_1662872134633__176593_repeat_1662883820987__880246.png)

#### 2.1.2 快速使用

##### 2.1.2.1 新增 Topic

可以通过 web GUI 添加 Topic， 在 `Topic`

`列表`页面添加，需要填写相关信息，比如增加`demo` topic： ![image-20220911125550780](http://kmgy.top:9090/image/2022/9/11/image-20220911125550780_repeat_1662872150924__122543_repeat_1662883822233__436212.png)

然后选择部署 Topic 的 Broker ![image-20220911125615610](http://kmgy.top:9090/image/2022/9/11/image-20220911125615610_repeat_1662872175752__699177_repeat_1662883818888__746064.png)

此时 Broker的 `可发布` 和 `可订阅` 依旧是灰色的 ![image-20220911125631221](http://kmgy.top:9090/image/2022/9/11/image-20220911125631221_repeat_1662872191348__379419.png)

需要在 `Broker列表`页面重载Broker 配置 ![image-20220911125652672](http://kmgy.top:9090/image/2022/9/11/image-20220911125652672_repeat_1662872212810__521241.png)



之后就可以在页面查看Topic信息。

![image-20220911125707345](http://kmgy.top:9090/image/2022/9/11/image-20220911125707345_repeat_1662872227467__687463_repeat_1662883823864__839527.png)

#### 2.1.3 运行Example

可以通过上面创建的`demo` topic来测试集群。

##### 2.1.3.1 生产消息

将 `YOUR_MASTER_IP:port` 替换为实际的IP和端口，然后运行producer:

```bash
cd /INSTALL_PATH/apache-inlong-tubemq-server-[TUBEMQ-VERSION]-bin
./bin/tubemq-producer-test.sh --master-servers YOUR_MASTER_IP1:port,YOUR_MASTER_IP2:port --topicName demo
```

如果能观察下如下日志，则表示数据发送成功： ![image-20220911125720439](http://kmgy.top:9090/image/2022/9/11/image-20220911125720439_repeat_1662872240580__517993.png)

##### 2.1.3.2 消费消息

将 `YOUR_MASTER_IP:port` 替换为实际的IP和端口，然后运行Consumer:

```bash
cd /INSTALL_PATH/apache-inlong-tubemq-server-[TUBEMQ-VERSION]-bin
./bin/tubemq-consumer-test.sh --master-servers YOUR_MASTER_IP1:port,YOUR_MASTER_IP2:port --topicName demo --groupName test_consume
```

如果能观察下如下日志，则表示数据被消费者消费到：

![image-20220911125736621](http://kmgy.top:9090/image/2022/9/11/image-20220911125736621_repeat_1662872256789__755835.png)

#### 2.1.3 下载安装包

可以从 [下载页面](https://inlong.apache.org/download) 获取二进制包。

解压 `apache-inlong-[version]-bin.tar.gz` 和 `apache-inlong-[version]-sort-connectors.tar.gz`，并确保 `inlong-sort/connectors/` 目录包含 Connectors。

#### 2.1.4 DB 依赖

- 如果后端连接 MySQL 数据库，请下载 [mysql-connector-java-8.0.27.jar](https://repo1.maven.org/maven2/mysql/mysql-connector-java/8.0.27/mysql-connector-java-8.0.27.jar), 并将其放入以下目录：

```bash
inlong-agent/lib/
inlong-audit/lib/
inlong-manager/lib/
inlong-tubemq-manager/lib/
```

#### 2.1.5 配置

在`conf/inlong.conf`文件中根据实际情况配置参数。

```
local_ip=127.0.0.1
# message queue, tubemq or pulsar
mq_type=pulsar
# MySQL service, IP, port, user and password
spring_datasource_hostname=127.0.0.1  ##数据库连接地址
spring_datasource_port=3307  ##数据库连接端口
spring_datasource_username=root  ##数据库连接用户名
spring_datasource_password=AllData  ##数据库连接密码

############## Pulsar Configuration ##############
pulsar_service_url=pulsar://127.0.0.1:6650
pulsar_admin_url=http://127.0.0.1:8080

############## TubeMQ Configuration ##############
tube_master_url=127.0.0.1:8715
tube_manager_url=127.0.0.1:8080

############## Dashboard Configuration ##############
# dashboard docker image
dashboard_docker_image=inlong/dashboard:1.2.0-incubating
# dashboard service port
dashboard_mapping_port=80

############## Manager Configuration ##############
# The default value is dev, prod is available
spring_profiles_active=dev
# manager service IP
manager_server_hostname=127.0.0.1
# manager port
manager_server_port=8083

############## DataProxy Configuration ##############
dataproxy_port=46801

############## Agent Configuration ##############
agent_port=8008

############## Audit Configuration ##############
# audit proxy IP
audit_proxys_ip=127.0.0.1
# audit proxy Port
audit_proxys_port=10081
```

#### 2.1.6 启动

```shell
bin/inlong-daemon start standalone
```

## 三、InLong 部署疑难解答

### 2.1 DataProxy报错

- inlong dataProxy {"success":false,....}报错
  - 修改数据库apache_inlong_manager中的inlong_cluster表设置creator可以为null
- 登录Dashboard中遇到502 Bad GateWay
  - docker exec -it 容器id /bin/bash 进入容器
  - 进入/etc/nginx/conf.d文件夹中 修改default.conf文件中127.0.0.1:8083的地址为容器外的主机地址
  - 若无法修改则docker cp 容器id:/etc/nginx/conf.d/default.conf /usr/local/yangguang文件夹中
  - 修改完后再复制回容器docker cp 容器id:/etc/nginx/conf.d/default.conf /usr/local/yangguang文件夹中
  - 在容器根目录中使用find查找nginx
  - 重新加载nginx的配置文件

## 其他

### 一、数据集成

#### 1.1 概述

##### 1.1.1 什么是数据集成？

大数据、物联网 (IoT)、软件即服务 (SaaS)、云活动等正在导致世界上现有的数据源数量以及数据量呈爆炸性增长，但这些数据大部分都收集并存储在数据孤岛或独立的数据存储空间中。**数据集成是将这些独立的数据整合到一起，以产生更高的数据价值和更丰富的数据洞见的过程。**

在企业制定数字化转型策略时，数据集成显得尤为重要，因为要想改善运营、提高客户满意度，并在日益数字化的世界中进行竞争，就需要对所有数据具有深入了解。

##### 1.1.2 数据集成的定义

**数据集成是将不同来源的数据整合在一起，以获得统一且更有价值的视图的过程，数据集成有助于您的企业做出更快、更好的决策。**

数据集成可以整合各种数据（结构化、非结构化、批量和流式模式），以完成从库存数据库的基本查询到复杂预测分析的所有工作。

#### 2.1 数据集成面临哪些挑战？

##### 2.1.1 使用数据集成平台具有难度

经验丰富的数据专业人员很难找到而且价格昂贵，然而部署大多数数据集成平台都需要依赖这类专业人员。需要获取数据以做出业务决策的业务分析师往往依赖于这些专家，这会降低数据分析的时间价值。

##### 2.1.2 数据集成基础设施的资本支出和运营支出较高

在采购、部署、维护和管理企业级数据集成项目所需的基础设施时，资本和运营费用都会增加。基于云的数据集成作为一种代管式服务，直接解决了此类费用问题。

##### 2.1.3 与应用紧密相关的数据

在以前，数据与特定应用紧密相连，并依赖于应用而存在，以至于您无法在企业的其他地方检索和使用数据。如今我们可以看到，应用和数据层已逐渐分离，这样可以更灵活地使用数据。

##### 2.1.4 数据语义问题

表示相同含义的多个数据版本可以用不同的方式组织或编排格式。例如，日期可以用数字形式存储为“年/月/日”，也可以用字符形式存储为“X 年 X 月 X 日”。ETL 中的“转换”元素和主数据管理工具可以解决此类问题。

##### 2.1.5 数据集成工具有哪些？

数据集成平台通常包括以下许多工具：

- **数据提取工具**：借助此类工具，您可以获取和导入数据，以便立即使用或储存起来供日后使用。
- **ETL 工具**：ETL 代表提取、转换和加载，这是最常见的数据集成方法。
- **数据目录**：此类工具可帮助企业找到并盘点分散在多个数据孤岛中的数据资源。
- **数据治理工具**：确保数据的可用性、安全性、易用性和完整性的工具。
- **数据清理工具**：通过替换、修改或删除来清理脏数据的工具。
- **数据迁移工具**：此类工具用于在计算机、存储系统或应用格式之间移动数据。
- **主数据管理工具**：帮助企业遵循通用数据定义，实现单一真实来源的工具。
- **数据连接器**：此类工具可以将数据从一个数据库移动到另一个数据库，还可以进行转换。

#### 3.1 数据集成有哪些用途？

数据集成通常用于以下几个方面：

##### 3.1.1 数据湖开发

数据集成可以将数据从孤岛式的本地平台移动到数据湖中，以提高数据价值。

##### 3.1.2 数据仓储

数据集成可以将各种来源的数据整合到一个数据仓库中进行分析，以实现业务目的。

##### 3.1.3 营销

数据集成可以将您的所有营销数据（如客户人群特征、社交网络和网络分析数据）移动到一个地方以执行分析和相关操作。

##### 3.1.4 IoT

数据集成有助于将多个物联网来源的数据整合到一个地方，便于您从中获取价值。

##### 3.1.5 数据库复制

数据集成是将数据从 Oracle、MongoDB 或 MySQL 等源数据库复制到云数据仓库这一操作的核心部分。

### 二、Zookeeper 集群部署

#### 1.1 服务器列表

ip

- 10.10.10.121
- 10.10.10.122
- 10.10.10.123

#### 2.1 三台服务器创建目录和文件

创建conf目录，添加配置文件文件zoo.cfg

```
clientPort=2181
#管理界面端口
admin.serverPort=8180
dataDir=/data
dataLogDir=/data/log
#通信心跳时间，毫秒
tickTime=2000
#leader和follow初始连接最多心跳数
initLimit=5
#集群中的follower服务器(F)与leader服务器(L)之间 请求和应答 之间能容忍的最多心跳数（tickTime的数量）。
syncLimit=2
#设置多少小时清理一次客户端在与zookeeper交互过程中会产生的日志
autopurge.snapRetainCount=3
#设置保留多少个snapshot
autopurge.purgeInterval=0
#客户端的连接数限制，默认是60
maxClientCnxns=60
server.0=10.10.10.121:2888:3888
server.1=10.10.10.122:2888:3888
server.2=10.10.10.123:2888:3888
```

创建data目录，创建myid文件

```
#值分别为0，1，2
0
```

#### 3.1 三台主机运行docker

```
docker run --network host -v /home/zookeeper/data:/data -v /home/zookeeper/conf/zoo.cfg:/conf/zoo.cfg --name zookeeper_0  -d zookeeper:3.8
```

#### 4.1 查看各个服务器状态

访问：

- http://10.10.10.121:8180/commands/stats

- http://10.10.10.122:8180/commands/stats

- http://10.10.10.123:8180/commands/stats