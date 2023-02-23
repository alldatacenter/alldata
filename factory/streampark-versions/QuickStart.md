# StreamPark平台部署
StreamPark 是一个流式应用程序开发框架。 StreamPark 旨在轻松构建和管理流式应用程序，

提供使用 Apache Flink 和 Apache Spark 编写流式处理应用程序的开发框架，未来将支持更多其他引擎。 

此外，StreamPark 是一个专业的流媒体应用管理平台 ，包括应用开发、调试、交互查询、部署、运维

streampark-console提供了开箱即用的安装包，安装之前对环境有些要求，具体要求如下：

## 一、环境要求

目前 StreamPark 对 Flink 的任务发布，同时支持 Flink on YARN 和 Flink on Kubernetes 两种模式。

### 1.1 Hadoop
使用 Flink on YARN，需要部署的集群安装并配置 Hadoop的相关环境变量，如你是基于 CDH 安装的 hadoop 环境， 相关环境变量可以参考如下配置:
```
export HADOOP_HOME=/opt/cloudera/parcels/CDH/lib/hadoop #hadoop 安装目录
export HADOOP_CONF_DIR=/etc/hadoop/conf
export HIVE_HOME=$HADOOP_HOME/../hive
export HBASE_HOME=$HADOOP_HOME/../hbase
export HADOOP_HDFS_HOME=$HADOOP_HOME/../hadoop-hdfs
export HADOOP_MAPRED_HOME=$HADOOP_HOME/../hadoop-mapreduce
export HADOOP_YARN_HOME=$HADOOP_HOME/../hadoop-yarn
```

## 二、部署
你可以直接下载编译好的发行包(推荐),也可以选择手动编译安装，手动编译安装步骤如下:
### 2.1 环境要求
● Maven 3.6+  
● npm 7.11.2 ( https://nodejs.org/en/ )  
● pnpm (npm install -g pnpm)  
● JDK 1.8+  
### 2.2 环境部署
#### 2.2.1 Linux系统
基于Centos7.5系统
#### 2.2.2 JDK
基于java1.8.0_212
#### 2.2.3 maven
基于maven3.9.0
```
cd /data/software
wget https://dlcdn.apache.org/maven/maven-3/3.9.0/binaries/apache-maven-3.9.0-bin.tar.gz --no-check-certificate
mv apache-maven-3.9.0/ maven
ln -s /data/module/maven/bin/mvn /usr/bin/mvn
wget https://gitee.com/lzc2025/maven_setting/raw/master/settings.xml -O /data/module/maven/conf/settings.xml
```
#### 2.2.4 node.js
console前端部分采用vue开发，需要nodejs环境，下载安装最新的nodejs 即可。
不同版本对应下载方式https://github.com/nodesource/distributions
```
# root权限用户
curl -fsSL https://rpm.nodesource.com/setup_16.x | bash -
yum install -y nodejs

# sudo权限用户
curl -fsSL https://rpm.nodesource.com/setup_16.x | sudo bash -
sudo yum install -y nodejs

# 可以替换为国内淘宝镜像
npm config set registry http://registry.npm.taobao.org/

# 全局安装pm2
npm install -g pm2
# 查看nodejs版本
node -V 
v16.18.1
npm -v
8.19.3
```
#### 2.2.5 mysql8
1、安装部署mysql8.0.29,下载mysql8的rpm安装包
```
rpm -ivh mysql-community-common-8.0.29-1.el7.x86_64.rpm --nodeps --force
rpm -ivh mysql-community-libs-8.0.29-1.el7.x86_64.rpm --nodeps --force
rpm -ivh mysql-community-libs-compat-8.0.29-1.el7.x86_64.rpm --nodeps --force
rpm -ivh mysql-community-client-8.0.29-1.el7.x86_64.rpm --nodeps --force
rpm -ivh mysql-community-server-8.0.29-1.el7.x86_64.rpm  --nodeps --force
```

2、mysql数据库的初始化和相关配置
```
mysqld --initialize;
chown mysql:mysql /var/lib/mysql -R;
systemctl start mysqld.service;
systemctl enable mysqld;

cat /var/log/mysqld.log | grep password

ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '*****';
```
3、远程访问授权
```
create user 'root'@'%' identified with mysql_native_password by '*****';
grant all privileges on *.* to 'root'@'%' with grant option;
flush privileges;
```
#### 2.2.6 flink1.13.6

安装部署flinkstandalone集群，选择1.13.6
需要配置FLINK_HOME环境变量
```
export FLINK_HOME=/data/module/flink
```
#### 2.2.7 hadoop3.1.3
选择hadoop版本3.1.3
配置相应的环境变量
```
export HADOOP_HOME=/data/module/hadoop-3.1.3 
export HADOOP_CLASSPATH=`hadoop classpath`
```
除了正常的配置外，需要在core-site.xml中添加如下配置
```
<property>
	<name>dfs.client.datanode-restart.timeout</name>
	<value>30</value>
</property>
```
### 2.3 安装部署StreamPark
#### 2.3.1 下载并解压StreamPark2.0
```
cd /data/software
wget https://dlcdn.apache.org/incubator/streampark/2.0.0/apache-streampark_2.12-2.0.0-incubating-bin.tar.gz
tar -zxvf apache-streampark_2.12-2.0.0-incubating-bin.tar.gz -C /data/module
```

#### 2.3.2 部署StreamPark
1） 在mysql8中创建数据库和对应的用户
以下sql语句请使用mysql服务的root用户进行操作
```
create database if not exists `streampark` character set utf8mb4 collate utf8mb4_general_ci;
create user 'streampark'@'%' IDENTIFIED WITH mysql_native_password by 'streampark';
grant ALL PRIVILEGES ON streampark.* to 'streampark'@'%';
flush privileges;
```
之后可使用 streampark 用户依次执行 schema 和 data 文件夹中的 sql 文件内容来建表以及初始化表数据。
2)  初始化表
```
use streampark;

source /data/module/streampark/script/schema/mysql-schema.sql
source /data/module/streampark/script/data/mysql-data.sql
```
3) 配置连接信息
进入到conf下，修改conf/application.yml,找到spring这一项，找到profiles.active的配置，修改成对应的信息即可，如下
```
spring:
  profiles.active: mysql #[h2,pgsql,mysql]
  application.name: StreamPark
  devtools.restart.enabled: false
  mvc.pathmatch.matching-strategy: ant_path_matcher
  servlet:
    multipart:
      enabled: true
      max-file-size: 500MB
      max-request-size: 500MB
  aop.proxy-target-class: true
  messages.encoding: utf-8
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
  main:
    allow-circular-references: true
    banner-mode: off
```
在修改完 conf/application.yml 后, 还需要修改 config/application-mysql.yml 中的数据库连接信息:  
Tips: 用户需要自行下载驱动jar包并放在 $STREAMPARK_HOME/lib 中,推荐使用8.x版本,下载地址apache maven
```
spring:
  datasource:
    username: root
    password: xxxx
    driver-class-name: com.mysql.cj.jdbc.Driver   # 请根据mysql-connector-java版本确定具体的路径,例如:使用5.x则此处的驱动名称应该是：com.mysql.jdbc.Driver
    url: jdbc:mysql://localhost:3306/streampark?useSSL=false&useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=false&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=GMT%2B8
```
修改workspace
进入到conf下，修改conf/application.yml,找到 streampark 这一项，找到 workspace 的配置，修改成一个用户有权限的目录
```
streampark:
  # HADOOP_USER_NAME 如果是on yarn模式( yarn-prejob | yarn-application | yarn-session)则需要配置 hadoop-user-name
  hadoop-user-name: hdfs
  # 本地的工作空间,用于存放项目源码,构建的目录等.
  workspace:
    local: /opt/streampark_workspace # 本地的一个工作空间目录(很重要),用户可自行更改目录,建议单独放到其他地方,用于存放项目源码,构建的目录等.
    remote: hdfs:///streampark   # support hdfs:///streampark/ 、 /streampark 、hdfs://host:ip/streampark/
```
3) 启动Server
进入到bin下直接执行startup.sh, 即可启动项目，默认端口是10000
```
/data/module/streamx-console-service-1.2.2/bin/startup.sh
```
启动成功之后使用jps 可以看到如下进程。
4) 浏览器登录系统
默认端口是10000
默认用户名：admin 默认密码：streampark
## 三、系统配置
进入系统之后，第一件要做的事情就是修改系统配置，在菜单/StreamPark/Setting 下，操作界面如下:

主要配置项分为以下几类:   
● System Setting  
● Alert Setting  
● Flink Home  
● Flink Cluster  
### 3.1 System Setting
当前系统配置包括:  
● Maven配置  
● Docker环境配置  
● 警告邮箱配置  
● k8s Ingress 配置  
### 3.2 Alert Setting
Alert Email 相关的配置是配置发送者邮件的信息，具体配置请查阅相关邮箱资料和文档进行配置。

### 3.3 Flink Home
这里配置全局的 Flink Home,此处是系统唯一指定 Flink 环境的地方，会作用于所有的作业。

### 3.4 Flink Cluster
Flink 当前支持的集群模式包括:  
● Standalone集群  
● Yarn集群  
● Kubernetes集群  

## 四、基础使用
启动FlinkSqlClient客户端
```
/data/module/flink/bin/sql-client.sh embedded
```
### 4.1 部署FlinkSql任务
1、填写对应的flinksql
```
-- source端 data_gen_source 中的数据来源于 flink sql 连接器 datagen 生成的随机数据，
create table data_gen (
    id integer comment '订单id',
    product_count integer comment '购买商品数量',
    one_price double comment '单个商品价格'
) with (
    'connector' = 'datagen',
    'rows-per-second' = '1',
    'fields.id.kind' = 'random',
    'fields.id.min' = '1',
    'fields.id.max' = '10',
    'fields.product_count.kind' = 'random',
    'fields.product_count.min' = '1',
    'fields.product_count.max' = '50',
    'fields.one_price.kind' = 'random',
    'fields.one_price.min' = '1.0',
    'fields.one_price.max' = '5000'
);

-- sink端 
CREATE TABLE print_sink (
    id INT,
    product_count INT,
    one_price DOUBLE
) WITH (
    'connector' = 'print'
);
 
 
insert into print_sink 
select id, product_count, one_price
from data_gen;
 
 ```

2、填写对应的Maven依赖Dependency  

* 如果sink端是mysql，可以填写如下依赖  

```
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.29</version>
</dependency>

<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-connector-jdbc</artifactId>
    <version>1.15.3</version>
</dependency>
```
3、在mysql8数据库中创建对应的数据表
```
 CREATE TABLE `goods_sink` (
 `id` int NOT NULL,
 `product_count` int NOT NULL,
 `one_price` double NOT NULL
 ) ENGINE=InnoDB;
```
填写Application Name和选择resolveOrder为parent-first，然后其他默认或者自行设置，最后点击提交按钮。  
提交成功后，点击上线（Launch Application）、streampark会根据maven依赖从mvn库拉下对应的依赖，进行build。  
build成功后，点击开始start，提交flinksql程序到flink集群中，  
也可以点击取消cancel，取消掉flink集群中对应的任务。

 
 

