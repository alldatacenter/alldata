# DATA QUALITY FOR ALL DATA PLATFORM 数据治理引擎

数据平台的数据治理：数据治理是一个大而全的治理体系。需要数据质量管理、元数据管理、主数据管理、模型管理管理、数据价值管理、
数据共享管理和数据安全管理等等模块是一个活的有机体。

3、数据质量: 依托Griffin平台，为您提供全链路的数据质量方案，包括数据探查、对比、质量监控、SQL扫描和智能报警等功能：

开源方案： Apache Griffin + ES + SparkSql
Griffin measure SparkSql规则任务二次开发

3.1 Measure源码二次开发

3.2 支持用户自定义规则

3.3 内置常用的griffin规则

3.4 支持spark sql转化为规则

3.5 支持数据质量全链路告警监控

3.6 新增MySqlSink

4、安装部署

4.1 Mysql初始化数据库

create database griffin_wlhbdp; 

use griffin_wlhbdp; 

然后登录数据库

source bdp-govern/quality/service/src/main/resources/Init_quartz_mysql_innodb.sql


4.2 拷贝集群配置文件

core-site.xml/hdfs-site.xml/mapred-site.xml/yarn-site.xml/hive-site.xml

到bdp-govern/quality/service/src/main/resources目录

4.3 mvn clean package -Dskiptests=TRUE

4.4 获取tar.gz安装包, 解压

4.5 正式部署griffin的环境，创建hdfs路径hdfs://griffin/spark_conf
    
    与hdfs://griffin/batch/persist
    
    与hdfs://griffin/streaming/persist（可选）

4.6 然后把measure.jar包上传到hdfs的目录hdfs:///griffin/

4.7 把hive-site.xml文件放上去hdfs:///griffin/spark_conf/

4.5 cd 安装包bin目录，执行./griffin.sh start or ./griffin.sh stop启停

4.6 访问localhost:8090
