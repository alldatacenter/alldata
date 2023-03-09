<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Apache Griffin Deployment Guide
If you are a new guy for Apache Griffin, please follow the instructions below to deploy Apache Griffin in your environment. Note that those steps will install all products in one physical machine, so you have to tune configurations depending on true topology.

### Prerequisites
Firstly you need to install and configure following software products, here we use [ubuntu-18.10](https://www.ubuntu.com/download) as sample OS to prepare all dependencies.
```bash
# put all download packages into /apache folder
$ mkdir /home/<user>/software
$ mkdir /home/<user>/software/data
$ sudo ln -s /home/<user>/software /apache
$ sudo ln -s /apache/data /data
$ mkdir /apache/tmp
$ mkdir /apache/tmp/hive
```

- JDK (1.8 or later versions)
```bash
$ sudo apt install openjdk-8-jre-headless

$ java -version
openjdk version "1.8.0_191"
OpenJDK Runtime Environment (build 1.8.0_191-8u191-b12-0ubuntu0.18.10.1-b12)
OpenJDK 64-Bit Server VM (build 25.191-b12, mixed mode)
```

- PostgreSQL(version 10.4) or MySQL(version 8.0.11)
```bash
# PostgreSQL
$ sudo apt install postgresql-10

# MySQL
$ sudo apt install mysql-server-5.7
```

- [npm](https://nodejs.org/en/download/) (version 6.0.0+)
```bash
$ sudo apt install nodejs
$ sudo apt install npm
$ node -v
$ npm -v
```

- [Hadoop](http://apache.claz.org/hadoop/common/) (2.6.0 or later), you can get some helps [here](https://hadoop.apache.org/docs/r2.7.2/hadoop-project-dist/hadoop-common/SingleCluster.html).

- [Hive](http://apache.claz.org/hive/) (version 2.x), you can get some helps [here](https://cwiki.apache.org/confluence/display/Hive/GettingStarted#GettingStarted-RunningHive).

- [Spark](http://spark.apache.org/downloads.html) (version 2.2.1), if you want to install Pseudo Distributed/Single Node Cluster, you can get some helps [here](http://why-not-learn-something.blogspot.com/2015/06/spark-installation-pseudo.html).

- [Livy](http://archive.cloudera.com/beta/livy/livy-server-0.3.0.zip), you can get some helps [here](http://livy.io/quickstart.html).

- [ElasticSearch](https://www.elastic.co/downloads/elasticsearch) (5.0 or later versions).
	ElasticSearch works as a metrics collector, Apache Griffin produces metrics into it, and our default UI gets metrics from it, you can use them by your own way as well.

- [Scala](https://downloads.lightbend.com/scala/2.12.8/scala-2.12.8.tgz), you can get some helps [here](https://www.scala-lang.org/).

### Configuration

#### PostgreSQL

Create database 'quartz' in PostgreSQL
```
createdb -O <username> quartz
```
Init quartz tables in PostgreSQL using [Init_quartz_postgres.sql](../../service/src/main/resources/Init_quartz_postgres.sql)
```
psql -p <port> -h <host address> -U <username> -f Init_quartz_postgres.sql quartz
```

#### MySQL

Create database 'quartz' in MySQL
```
mysql -u <username> -e "create database quartz" -p
```
Init quartz tables in MySQL using [Init_quartz_mysql_innodb.sql](../../service/src/main/resources/Init_quartz_mysql_innodb.sql)
```
mysql -u <username> -p quartz < Init_quartz_mysql_innodb.sql
```

#### Set Env

Export those variables below, or create griffin_env.sh and put it into .bashrc.
```bash
#!/bin/bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

export HADOOP_HOME=/apache/hadoop
export HADOOP_COMMON_HOME=/apache/hadoop
export HADOOP_COMMON_LIB_NATIVE_DIR=/apache/hadoop/lib/native
export HADOOP_HDFS_HOME=/apache/hadoop
export HADOOP_INSTALL=/apache/hadoop
export HADOOP_MAPRED_HOME=/apache/hadoop
export HADOOP_USER_CLASSPATH_FIRST=true
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
export SPARK_HOME=/apache/spark
export LIVY_HOME=/apache/livy
export HIVE_HOME=/apache/hive
export YARN_HOME=/apache/hadoop
export SCALA_HOME=/apache/scala

export PATH=$PATH:$HIVE_HOME/bin:$HADOOP_HOME/bin:$SPARK_HOME/bin:$LIVY_HOME/bin:$SCALA_HOME/bin
```

#### Hadoop
* **update configuration**

Put site-specific property overrides in this file **/apache/hadoop/etc/hadoop/core-site.xml**
```xml
<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://127.0.0.1:9000</value>
    </property>    
</configuration>
```

Put site-specific property overrides in this file **/apache/hadoop/etc/hadoop/hdfs-site.xml**
```xml
<configuration>
    <property>
        <name>dfs.namenode.logging.level</name>
        <value>warn</value>
    </property>
    <property>
        <name>dfs.replication</name>
        <value>1</value>
    </property>
    <property>
        <name>dfs.namenode.name.dir</name>
        <value>file:///data/hadoop-data/nn</value>
    </property>
    <property>
        <name>dfs.datanode.data.dir</name>
        <value>file:///data/hadoop-data/dn</value>
    </property>
    <property>
        <name>dfs.namenode.checkpoint.dir</name>
        <value>file:///data/hadoop-data/snn</value>
    </property>
    <property>
        <name>dfs.webhdfs.enabled</name>
        <value>true</value>
    </property>
    <property>
        <name>dfs.datanode.use.datanode.hostname</name>
        <value>false</value>
    </property>
    <property>
        <name>dfs.namenode.datanode.registration.ip-hostname-check</name>
        <value>false</value>
    </property>
</configuration>
```

* **start/stop hadoop nodes**
```bash
# format name node
# NOTE: if you already have executed namenode-format before, it'll change cluster ID in 
# name node's VERSION file after you run it again. so you need to guarantee same cluster ID
# in data node's VERSION file, otherwise data node will fail to start up.
# VERSION file resides in /apache/data/hadoop-data/nn, snn, dn denoted in previous config. 
/apache/hadoop/bin/hdfs namenode -format
# start namenode/secondarynamenode/datanode
# NOTE: you should use 'ps -ef|grep java' to check if namenode/secondary namenode/datanode
# are available after starting dfs service.
# if there is any error, please find clues from /apache/hadoop/logs/
/apache/hadoop/sbin/start-dfs.sh
# stop all nodes
/apache/hadoop/sbin/stop-dfs.sh
```
Here you can access http://127.0.0.1:50070/ to check name node.
* **start/stop hadoop ResourceManager**
```bash
# manually clear the ResourceManager state store
/apache/hadoop/bin/yarn resourcemanager -format-state-store
# startup the ResourceManager
/apache/hadoop/sbin/yarn-daemon.sh start resourcemanager
# stop the ResourceManager
/apache/hadoop/sbin/yarn-daemon.sh stop resourcemanager
```
Here you can access http://127.0.0.1:8088/cluster to check hadoop cluster.
 
Hadoop daemons also expose some information over HTTP like http://127.0.0.1:8088/stacks. Please refer to [blog](https://blog.cloudera.com/blog/2009/08/hadoop-default-ports-quick-reference/)
* **start/stop hadoop NodeManager**
```bash
# startup the NodeManager
/apache/hadoop/sbin/yarn-daemon.sh start nodemanager
# stop the NodeManager
/apache/hadoop/sbin/yarn-daemon.sh stop nodemanager
```
Here you can access http://127.0.0.1:8088/cluster/nodes to check hadoop nodes, you should see one node in the list.
* **(optional) start/stop hadoop HistoryServer**
```bash
# startup the HistoryServer
/apache/hadoop/sbin/mr-jobhistory-daemon.sh start historyserver
# stop the HistoryServer
/apache/hadoop/sbin/mr-jobhistory-daemon.sh stop historyserver
```

#### Hive
* **update configuration**
Copy hive/conf/hive-site.xml.template to hive/conf/hive-site.xml and update some fields.
```xml
<configuration>
   <property>
     <name>hive.exec.local.scratchdir</name>
     <value>/apache/tmp/hive</value>
     <description>Local scratch space for Hive jobs</description>
   </property>
   <property>
     <name>hive.downloaded.resources.dir</name>
     <value>/apache/tmp/hive/${hive.session.id}_resources</value>
     <description>Temporary local directory for added resources in the remote file system.</description>
   </property>
   <property>
   </property>
   <property>
     <name>hive.metastore.uris</name>
     <value>thrift://127.0.0.1:9083</value>
     <description>Thrift URI for the remote metastore.</description>
   </property>
   <property>
   </property>
   <property>
     <name>javax.jdo.option.ConnectionPassword</name>
     <value>secret</value>
     <description>password to use against metastore database</description>
   </property>
   <property>
   </property>
   <property>
     <name>javax.jdo.option.ConnectionURL</name>
     <value>jdbc:postgresql://127.0.0.1/myDB?ssl=false</value>
     <description>
       JDBC connect string for a JDBC metastore.
       To use SSL to encrypt/authenticate the connection, provide database-specific SSL flag in the connection URL.
   </property>
   <property>
     <name>javax.jdo.option.ConnectionDriverName</name>
     <value>org.postgresql.Driver</value>
     <description>Driver class name for a JDBC metastore</description>
   </property>
   <property>
   </property>
   <property>
     <name>javax.jdo.option.ConnectionUserName</name>
     <value>king</value>
     <description>Username to use against metastore database</description>
   </property>
   <property>
   </property>
   <property>
     <name>hive.querylog.location</name>
     <value>/apache/tmp/hive</value>
     <description>Location of Hive run time structured log file</description>
   </property>
   <property>
   </property>
   <property>
     <name>hive.server2.logging.operation.log.location</name>
     <value>/apache/tmp/hive/operation_logs</value>
   </property>
</configuration>
```

* **start up hive metastore service**
```bash
# start hive metastore service
/apache/hive/bin/hive --service metastore
```

#### Spark
* **update configuration**

Check $SPARK_HOME/conf/spark-default.conf
```
spark.master                    yarn-cluster
spark.serializer                org.apache.spark.serializer.KryoSerializer
spark.yarn.jars                 hdfs:///home/spark_lib/*
spark.yarn.dist.files		hdfs:///home/spark_conf/hive-site.xml
spark.sql.broadcastTimeout  500
```
Check $SPARK_HOME/conf/spark-env.sh
```
HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
SPARK_MASTER_HOST=localhost
SPARK_MASTER_PORT=7077
SPARK_MASTER_WEBUI_PORT=8082
SPARK_LOCAL_IP=localhost
SPARK_PID_DIR=/apache/pids
```
Upload some files otherwise you will hit `Error: Could not find or load main class org.apache.spark.deploy.yarn.ApplicationMaster`, when you schedule spark applications.
```bash
hdfs dfs -mkdir /home/spark_lib
hdfs dfs -mkdir /home/spark_conf
hdfs dfs -put $SPARK_HOME/jars/*  hdfs:///home/spark_lib/
hdfs dfs -put $HIVE_HOME/conf/hive-site.xml hdfs:///home/spark_conf/
```
* **start/stop spark nodes**
```bash
cp /apache/hive/conf/hive-site.xml /apache/spark/conf/
# start master and slave nodes
/apache/spark/sbin/start-master.sh
/apache/spark/sbin/start-slave.sh  spark://localhost:7077

# stop master and slave nodes
/apache/spark/sbin/stop-slaves.sh 
/apache/spark/sbin/stop-master.sh 

# stop all
/apache/spark/sbin/stop-all.sh
```

#### Livy
Apache Griffin need to schedule spark jobs by server, we use livy to submit our jobs.

* **update configuration**
```bash
mkdir /apache/livy/logs
```
Update $LIVY_HOME/conf/livy.conf
```bash
# update /apache/livy/conf/livy.conf
livy.server.host = 127.0.0.1
livy.spark.master = yarn
livy.spark.deployMode = cluster
livy.repl.enableHiveContext = true
livy.server.port 8998
```
* **start up livy**
```bash
/apache/livy/bin/livy-server start
```

#### Elasticsearch
* **update configuration**

Update $ES_HOME/config/elasticsearch.yml
```
network.host: 127.0.0.1
http.cors.enabled: true
http.cors.allow-origin: "*"
```
* **start up elasticsearch**
```bash
/apache/elastic/bin/elasticsearch
```
You can access http://127.0.0.1:9200/ to check elasticsearch service.

#### Griffin
You can download latest package from [official link](http://griffin.apache.org/docs/latest.html), or locally build on [source codes](https://github.com/apache/griffin.git).

Before building Griffin, you have to update those configuration depending on previous steps's configuration.

* **service/src/main/resources/application.properties**

You can get more detailed configuration description in [here](#griffin-customization).
```
# Apache Griffin server port (default 8080)
server.port = 8080
spring.application.name=griffin_service

# db configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/myDB?autoReconnect=true&useSSL=false
spring.datasource.username=king
spring.datasource.password=secret
spring.jpa.generate-ddl=true
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.show-sql=true

# Hive metastore
hive.metastore.uris=thrift://localhost:9083
hive.metastore.dbname=default
hive.hmshandler.retry.attempts=15
hive.hmshandler.retry.interval=2000ms
# Hive cache time
cache.evict.hive.fixedRate.in.milliseconds=900000

# Kafka schema registry
kafka.schema.registry.url=http://localhost:8081
# Update job instance state at regular intervals
jobInstance.fixedDelay.in.milliseconds=60000
# Expired time of job instance which is 7 days that is 604800000 milliseconds.Time unit only supports milliseconds
jobInstance.expired.milliseconds=604800000
# schedule predicate job every 5 minutes and repeat 12 times at most
#interval time unit s:second m:minute h:hour d:day,only support these four units
predicate.job.interval=5m
predicate.job.repeat.count=12
# external properties directory location
external.config.location=
# external BATCH or STREAMING env
external.env.location=
# login strategy ("default" or "ldap")
login.strategy=default
# ldap
ldap.url=ldap://hostname:port
ldap.email=@example.com
ldap.searchBase=DC=org,DC=example
ldap.searchPattern=(sAMAccountName={0})
# hdfs default name
fs.defaultFS=

# elasticsearch
# elasticsearch.host = <IP>
# elasticsearch.port = <elasticsearch rest port>
# elasticsearch.user = user
# elasticsearch.password = password
elasticsearch.host=localhost
elasticsearch.port=9200
elasticsearch.scheme=http

# livy
livy.uri=http://localhost:8998/batches
# yarn url
yarn.uri=http://localhost:8088
# griffin event listener
internal.event.listeners=GriffinJobEventHook
```  

* **service/src/main/resources/quartz.properties**
```
org.quartz.scheduler.instanceName=spring-boot-quartz
org.quartz.scheduler.instanceId=AUTO
org.quartz.threadPool.threadCount=5
org.quartz.jobStore.class=org.quartz.impl.jdbcjobstore.JobStoreTX
# If you use postgresql, set this property value to org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
# If you use mysql, set this property value to org.quartz.impl.jdbcjobstore.StdJDBCDelegate
# If you use h2, it's ok to set this property value to StdJDBCDelegate, PostgreSQLDelegate or others
org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
org.quartz.jobStore.useProperties=true
org.quartz.jobStore.misfireThreshold=60000
org.quartz.jobStore.tablePrefix=QRTZ_
org.quartz.jobStore.isClustered=true
org.quartz.jobStore.clusterCheckinInterval=20000
```

* **service/src/main/resources/sparkProperties.json**

**griffin measure path** is the location where you should put the jar file of measure module.
```
{
    "file": "hdfs:///<griffin measure path>/griffin-measure.jar",
    "className": "org.apache.griffin.measure.Application",
    "name": "griffin",
    "queue": "default",
    "numExecutors": 3,
    "executorCores": 1,
    "driverMemory": "1g",
    "executorMemory": "1g",
    "conf": {
        "spark.yarn.dist.files": "hdfs:///<path to>/hive-site.xml"
    },
    "files": [
    ],
    "jars": [
    ]
}
```

* **service/src/main/resources/env/env_batch.json**

Adjust sinks according to your requirement. At least, you will need to adjust HDFS output
directory (hdfs:///griffin/persist by default), and Elasticsearch URL (http://es:9200/griffin/accuracy by default).
Similar changes are required in `env_streaming.json`.
```
{
  "spark": {
    "log.level": "WARN"
  },
  "sinks": [
    {
      "type": "CONSOLE",
      "config": {
        "max.log.lines": 10
      }
    },
    {
      "type": "HDFS",
      "config": {
        "path": "hdfs:///griffin/persist",
        "max.persist.lines": 10000,
        "max.lines.per.file": 10000
      }
    },
    {
      "type": "ELASTICSEARCH",
      "config": {
        "method": "post",
        "api": "http://127.0.0.1:9200/griffin/accuracy",
        "connection.timeout": "1m",
        "retry": 10
      }
    }
  ],
  "griffin.checkpoint": []
}
```

It's easy to build Griffin, just run maven command `mvn clean install`. Successfully building, you can get `service-${version}.tar.gz` in root target folder and `measure-${version}.jar` in measure module target folder.

Upload measure's jar to hadoop folder.
```
# change jar name
mv measure-0.4.0.jar griffin-measure.jar
# upload measure jar file
hdfs dfs -put griffin-measure.jar /griffin/
```

Startup service，run Griffin management service.
```
cd $GRIFFIN_INSTALL_DIR
tar -zxvf target/service-${version}.tar.gz
cd service-${version}
# start service
./bin/griffin.sh start  
# or use ./bin/start.sh
# stop service
./bin/griffin.sh stop
# or use ./bin/stop.sh
```

After a few seconds, we can visit our default UI of Apache Griffin (by default the port of spring boot is 8080).
```
http://<your IP>:8080
```

You can conduct UI operations following the steps [here](../ui/user-guide.md).

**Note**: The UI does not support all the backend features, to experience the advanced features you can use service's [api](../service/api-guide.md) directly.

##### Griffin Customization
- Compression

Griffin Service is regular Spring Boot application, so it supports all customizations from Spring Boot.
To enable output compression, the following should be added to `application.properties`:
```
server.compression.enabled=true
server.compression.mime-types=application/json,application/xml,text/html,\
                              text/xml,text/plain,application/javascript,text/css
```

- SSL

It is possible to enable SSL encryption for api and web endpoints. To do that, you will need to prepare keystore in Spring-compatible format (for example, PKCS12), and add the following values to `application.properties`:
```
server.ssl.key-store=/path/to/keystore.p12
server.ssl.key-store-password=yourpassword
server.ssl.keyStoreType=PKCS12
server.ssl.keyAlias=your_key_alias
```

- LDAP

The following properties are available for LDAP:
 - **ldap.url**: URL of LDAP server.
 - **ldap.email**: Arbitrary suffix added to user's login before search, can be empty string. Used when user's DN contains some common suffix, and there is no bindDN specified. In this case, string after concatenation is used as DN for sending initial bind request.
 - **ldap.searchBase**: Subtree DN to search.
 - **ldap.searchPattern**: Filter expression, substring `{0}` is replaced with user's login after ldap.email is concatenated. This expression is used to find user object in LDAP. Access is denied if filter expression did not match any users.
 - **ldap.sslSkipVerify**: Allows to disable certificate validation for secure LDAP servers.
 - **ldap.bindDN**: Optional DN of service account used for user lookup. Useful if user's DN is different than attribute used as user's login, or if users' DNs are ambiguous.
 - **ldap.bindPassword**: Optional password of bind service account.

#### Launch Griffin Demo

* **create hadoop folder**
```bash
$ hdfs dfs -ls /
Found 3 items
drwxr-xr-x   - king supergroup          0 2019-02-21 17:25 /data
drwx-wx-wx   - king supergroup          0 2019-02-21 16:45 /tmp
drwxr-xr-x   - king supergroup          0 2019-02-26 08:48 /user

$ hdfs dfs -mkdir /griffin

$ hdfs dfs -ls /
Found 4 items
drwxr-xr-x   - king supergroup          0 2019-02-21 17:25 /data
drwxr-xr-x   - king supergroup          0 2019-02-26 10:30 /griffin
drwx-wx-wx   - king supergroup          0 2019-02-21 16:45 /tmp
drwxr-xr-x   - king supergroup          0 2019-02-26 08:48 /user

$ hdfs dfs -put griffin-measure.jar /griffin/

$ hdfs dfs -ls /griffin
-rw-r--r--   1 king supergroup   30927307 2019-02-26 10:36 /griffin/griffin-measure.jar
```
Here you can refer to [dfs commands](http://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-hdfs/HDFSCommands.html#dfs), 
get command [examples](http://fibrevillage.com/storage/630-using-hdfs-command-line-to-manage-files-and-directories-on-hadoop). 

* **integrate hadoop and hive service**
```bash
# create /home/spark_conf
# -p option behavior is much like Unix mkdir -p, creating parent directories along the path.
hdfs dfs -mkdir -p /home/spark_conf

# upload hive-site.xml
hdfs dfs -put hive-site.xml /home/spark_conf/
```

* **prepare demo tables**
```bash
# login hive client
/apache/hive/bin/hive --database default

# create demo tables
hive> CREATE EXTERNAL TABLE `demo_src`(
  `id` bigint,
  `age` int,
  `desc` string) 
PARTITIONED BY (
  `dt` string,
  `hour` string)
ROW FORMAT DELIMITED
  FIELDS TERMINATED BY '|'
LOCATION
  'hdfs://127.0.0.1:9000/griffin/data/batch/demo_src';
  
hive> CREATE EXTERNAL TABLE `demo_tgt`(
  `id` bigint,
  `age` int,
  `desc` string) 
PARTITIONED BY (
  `dt` string,
  `hour` string)
ROW FORMAT DELIMITED
  FIELDS TERMINATED BY '|'
LOCATION
  'hdfs://127.0.0.1:9000/griffin/data/batch/demo_tgt';

# check tables created  
hive> show tables;
OK
demo_src
demo_tgt
Time taken: 0.04 seconds, Fetched: 2 row(s)
```

Check table definition.
```bash
hive> show create table demo_src;
OK
CREATE EXTERNAL TABLE `demo_src`(
  `id` bigint, 
  `age` int, 
  `desc` string)
PARTITIONED BY ( 
  `dt` string, 
  `hour` string)
ROW FORMAT SERDE 
  'org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe' 
WITH SERDEPROPERTIES ( 
  'field.delim'='|', 
  'serialization.format'='|') 
STORED AS INPUTFORMAT 
  'org.apache.hadoop.mapred.TextInputFormat' 
OUTPUTFORMAT 
  'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat'
LOCATION
  'hdfs://127.0.0.1:9000/griffin/data/batch/demo_src'
TBLPROPERTIES (
  'transient_lastDdlTime'='1551168613')
Time taken: 3.762 seconds, Fetched: 20 row(s)
```

If the table definition is not correct, drop it.
```bash
hive> drop table if exists demo_src;
OK
Time taken: 3.764 seconds
hive> drop table if exists demo_tgt;
OK
Time taken: 0.632 seconds
```

* **spawn demo data**
There has been a script spawning test data, you can fetch it from [batch data](http://griffin.apache.org/data/batch/).
And then execute ./gen_demo_data.sh to get the two data source files.
```bash
/apache/data/demo$ wget http://griffin.apache.org/data/batch/gen_demo_data.sh
/apache/data/demo$ wget http://griffin.apache.org/data/batch/gen_delta_src.sh
/apache/data/demo$ wget http://griffin.apache.org/data/batch/demo_basic
/apache/data/demo$ wget http://griffin.apache.org/data/batch/delta_tgt
/apache/data/demo$ wget http://griffin.apache.org/data/batch/insert-data.hql.template
/apache/data/demo$ chmod 755 *.sh
/apache/data/demo$ ./gen_demo_data.sh
```

Create gen-hive-data.sh
```
#!/bin/bash

#create table
hive -f create-table.hql
echo "create table done"

#current hour
sudo ./gen_demo_data.sh
cur_date=`date +%Y%m%d%H`
dt=${cur_date:0:8}
hour=${cur_date:8:2}
partition_date="dt='$dt',hour='$hour'"
sed s/PARTITION_DATE/$partition_date/ ./insert-data.hql.template > insert-data.hql
hive -f insert-data.hql
src_done_path=/griffin/data/batch/demo_src/dt=${dt}/hour=${hour}/_DONE
tgt_done_path=/griffin/data/batch/demo_tgt/dt=${dt}/hour=${hour}/_DONE
hadoop fs -mkdir -p /griffin/data/batch/demo_src/dt=${dt}/hour=${hour}
hadoop fs -mkdir -p /griffin/data/batch/demo_tgt/dt=${dt}/hour=${hour}
hadoop fs -touchz ${src_done_path}
hadoop fs -touchz ${tgt_done_path}
echo "insert data [$partition_date] done"

#last hour
sudo ./gen_demo_data.sh
cur_date=`date -d '1 hour ago' +%Y%m%d%H`
dt=${cur_date:0:8}
hour=${cur_date:8:2}
partition_date="dt='$dt',hour='$hour'"
sed s/PARTITION_DATE/$partition_date/ ./insert-data.hql.template > insert-data.hql
hive -f insert-data.hql
src_done_path=/griffin/data/batch/demo_src/dt=${dt}/hour=${hour}/_DONE
tgt_done_path=/griffin/data/batch/demo_tgt/dt=${dt}/hour=${hour}/_DONE
hadoop fs -mkdir -p /griffin/data/batch/demo_src/dt=${dt}/hour=${hour}
hadoop fs -mkdir -p /griffin/data/batch/demo_tgt/dt=${dt}/hour=${hour}
hadoop fs -touchz ${src_done_path}
hadoop fs -touchz ${tgt_done_path}
echo "insert data [$partition_date] done"

#next hours
set +e
while true
do
  sudo ./gen_demo_data.sh
  cur_date=`date +%Y%m%d%H`
  next_date=`date -d "+1hour" '+%Y%m%d%H'`
  dt=${next_date:0:8}
  hour=${next_date:8:2}
  partition_date="dt='$dt',hour='$hour'"
  sed s/PARTITION_DATE/$partition_date/ ./insert-data.hql.template > insert-data.hql
  hive -f insert-data.hql
  src_done_path=/griffin/data/batch/demo_src/dt=${dt}/hour=${hour}/_DONE
  tgt_done_path=/griffin/data/batch/demo_tgt/dt=${dt}/hour=${hour}/_DONE
  hadoop fs -mkdir -p /griffin/data/batch/demo_src/dt=${dt}/hour=${hour}
  hadoop fs -mkdir -p /griffin/data/batch/demo_tgt/dt=${dt}/hour=${hour}
  hadoop fs -touchz ${src_done_path}
  hadoop fs -touchz ${tgt_done_path}
  echo "insert data [$partition_date] done"
  sleep 3600
done
set -e
```

Then we will load data into both two tables for every hour.
```bash
/apache/data/demo$ ./gen-hive-data.sh
```

After a while, you can query demo data from hive table.
```bash
hive> select * from demo_src;
124	935	935	20190226	17
124	838	838	20190226	17
124	631	631	20190226	17
......
Time taken: 2.19 seconds, Fetched: 375000 row(s)
```

See related data folder created on hdfs. 
```bash
$ hdfs dfs -ls /griffin/data/batch
drwxr-xr-x   - king supergroup          0 2019-02-26 16:13 /griffin/data/batch/demo_src
drwxr-xr-x   - king supergroup          0 2019-02-26 16:13 /griffin/data/batch/demo_tgt

$ hdfs dfs -ls /griffin/data/batch/demo_src/
drwxr-xr-x   - king supergroup          0 2019-02-26 16:14 /griffin/data/batch/demo_src/dt=20190226
```

You need to create Elasticsearch index in advance, in order to set number of shards, replicas, and other settings to desired values:
```
curl -k -H "Content-Type: application/json" -X PUT http://127.0.0.1:9200/griffin \
 -d '{
    "aliases": {},
    "mappings": {
        "accuracy": {
            "properties": {
                "name": {
                    "fields": {
                        "keyword": {
                            "ignore_above": 256,
                            "type": "keyword"
                        }
                    },
                    "type": "text"
                },
                "tmst": {
                    "type": "date"
                }
            }
        }
    },
    "settings": {
        "index": {
            "number_of_replicas": "2",
            "number_of_shards": "5"
        }
    }
}'
```
You can access http://127.0.0.1:9200/griffin to verify configuration.

Everything is ready, you can login http://127.0.0.1:8080 without username and credentials. And then create measure, job to validate data quality by [user guide](../ui/user-guide.md).