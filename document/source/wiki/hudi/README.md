# Hudi性能测试报告

## 一、测试背景
数据湖作为一个集中化的数据存储仓库，支持结构化、半结构化以及非结构化等多种数据格式，数据来源包含数据库数据、增量数据、日志数据以及数仓上的存量数据等。数据湖能够将这些不同来源、不同格式的数据集中存储和管理在高性价比的分布式存储系统中，对外提供统一的数据目录，支持多种计算分析方式，有效解决企业面临的数据孤岛问题，降低存储和使用数据的成本。
Apache Hudi（音：Hoodie）是数据湖的一个开源组件，能够摄入（Ingest）和管理（Manage）基于HDFS之上的大型分析数据集，支持通过Spark和Flink构建一体化数据湖解决方案。Hudi设计的主要目的是为了高效地减少摄取过程中的数据延迟，除了经典的批处理外，Hudi还提供插入更新（改变数据集）、增量拉取（获取变更数据）等流处理原语，可以通过细粒度的文件/记录级别索引方式来支持写操作的事务保证，获取最新快照结果，由此解锁基于HDFS抽象的流/增量数据处理能力，解决HDFS的可伸缩性限制问题，提供快速的ETL、建模和数据呈现。Hudi填补了在HDFS上处理数据的巨大空白，可以与大数据技术很好地共存。
本文档基于Kafka数据源，采用Flink作为计算载体，以HDFS作为底层存储组件，针对Hudi的两种表类型COW（Copy on Write）和MOR（Merge on Read），分别从读写两方面，通过响应延迟、吞吐量等技术指标，对Hudi进行性能测试和汇总对比，为业务数据的存储与计算提供读写性能参考与优化方向。

## 二、测试环境

### 2.1 软件环境

软件资源	软件版本
操作系统及内核	CentOS Linux 7.6
Linux version 3.10.0-1160.el7.x86_64
Apache Hadoop YARN	Hadoop 3.1.1.3.1.5.0-152
Apache Flink	Flink 1.14.4
Apache Hudi	hudi-flink1.14-bundle_2.11-0.12.1.jar
Apache Kafka	Kafka 2.0.0.3.1.5.0-152
flink-sql-connector-kafka_2.11-1.14-SNAPSHOT.jar （支持Batch & Streaming）

### 2.2 硬件环境

硬件资源	硬件配置
服务器数量	3台（与Kafka集群共用）
CPU	集群总核数320核（128+128+64），实际可用核数306核
内存	集群总内存1.47TB，实际可用内存1.41TB
数据盘	集群总存储153TB，实际可用存储134TB
网卡	万兆

### 2.3 数据环境

Kafka数据源	数据配置
lkzs_1yi_10p	共1亿条，单条1KB，总大小98.9GB，分区数10或100
lkzs_10yi_100fenqu	约10亿条，单条1KB，总大小993GB，分区数100
lkzs_100yi_100fenqu	约107亿条，单条1KB，总大小9.63TB，分区数100

## 三、测试方案

### 3.1 方案说明

本方案，将从Hadoop集群边缘的任一节点，使用YARN进行集群资源分配和调度，分别以Session和Appcliation Mode方式发起Hudi on Flink的读写任务，并通过Apache Flink Web Dashboard进行进度监控和结果收集。
根据数据量大小，分别对1亿、10亿、100亿数据进行读写测试。
根据表类型，分别对COW和MOR两种表进行读写测试。
根据处理方式，分别以Batch和Streaming方式进行写入测试。
根据索引类型，分别对无索引、Bucket Index和Flink State等索引方式进行写入测试。
根据写入类型，分别对upsert、bulk insert方式进行写入测试。
根据状态后端类型，分别以HashMap、RocksDB作为状态后端进行流式写入测试。
根据查询类型，分别以点查询、聚合查询、连接查询方式进行查询测试。

### 3.2 测试内容

序号	功能点	测试项	性能指标
1	COW写	1）数据量：1亿、10亿、100亿；
2）处理方式：Batch、Streaming；
3）索引：无索引、Bucket Index、Flink State；
4）写入方式：upsert、bulk_insert；
5）并行度：1-100；	吞吐量
响应延迟
并发处理能力
资源利用率
2	COW读	1）数据量：1亿、10亿、100亿；
2）处理方式：Batch、Streaming；
3）查询类型：点查询、聚合查询、连接查询；
4）并行度：1-100；
3	MOR写	1）数据量：1亿、10亿、100亿；
2）处理方式：Batch、Streaming；
3）索引：无索引、Bucket Index、Flink State；
4）写入方式：upsert、bulk_insert；
5）状态后端：HashMap、RocksDB；
6）压缩合并方式：同步、异步；
7）并行度：1-100；
4	MOR读	1）数据量：1亿、10亿、100亿；
2）处理方式：Batch、Streaming；
3）查询类型：点查询、聚合查询、连接查询；
4）并行度：1-100；

## 四、测试步骤

序号	测试步骤
1	清理干扰数据：
hadoop fs -rm -r /path/to/hudi
sleep 5
2	重置kafka数据源中相关topic的group消费位置：
/usr/hdp/3.1.5.0-152/kafka/bin/kafka-consumer-groups.sh --bootstrap-server ${bootstrapServer} --group ${group} --reset-offsets --topic ${topic} --to-earliest --execute
3	启动yarn session，设置单个TM为30 Slot，每个TM内存100GB：
yarn-session.sh -nm hudi -s 30 -jm 10240 -tm 102400 -d
4	启动作业：
sql-client.sh embedded -f hudi_mor_upsert_1yi_flinkstate.sql
5	跟踪作业执行进度，收集性能指标

## 五、测试明细
注意：Parquet基准文件为128MB，小文件指的是小于100MB的文件。

### 5.1 COW

5.1.1 写 - Bulk Insert
1）基准测试
通过1亿条Kafka源数据，对Bulk Insert写入方式进行基准测试，测试明细如下所示：
Source并行度	Sink并行度	开启Bucket索引	Bucket数量	是否排序	是否分区	堆内存
(GB)	管理内存
(GB)	网络内存
(GB)	平均写入速率
(MiB/s)	平均写入数量（条）	写入延迟	本地临时数据大小（GB)	输出数据大小
(GB)	输出文件数
（个）	小文件数
（个）
10	10	N	N	N	N	<30G	0	0	87.45	86356	19m 18s	57	15.1	129	10
10	100	N	N	N	N	<30G	0	10	60.17	59418	28m 3s	57	15.2	200	100
10	10	Y	10	N	N	<30G	10	10	47.37	46773	35m 38s	47	15.4	10	0
10	10	Y 	10	Y	N	<30G	3.33	10	71.82	70922	23m 30s	47	15.6	10	0
100	100	N	N	N	N	<30G	0	0	364.29	359712	4m 38s	47	15.2	200	100
100	100	Y	100	N	N	<30G	0	10	187.89	185528	8m 59s	47	15.7	100	0
100	100	Y	100	Y	N	<30G	10	10	290.18	286533	5m 49s	47	15.7	100	0
性能指标对比如下：

测试小结：
（1）Bulk Insert要求使用Batch Mode方式写入，需改造Kafka Connector以支持Batch Mode。
（2）写入过程中，会在本地临时存储部分数据，对本地磁盘大小有一定要求，占比约为源数据的一半。
（3）源数据与输出数据压缩比例约为6.4:1。
（4）Source分区数越多，批量入湖效率越高。
（5）Sink并行度最好与Source分区数一致，当Sink并行度设置超过Source分区数时，可能会由于Shuffle过程增加数据处理时间。
（6）Bulk Insert期间，为了保证数据唯一性，索引和排序最好同时开启，排序是为了使数据保持聚集，减少小文件数量，只索引不排序会大大降低入库效率。
（7）开启Bucket索引时，对网络内存要求较高，其入库过程分为两个阶段，上一阶段完成后才会进入下一阶段，即，只有当Source阶段的数据全部加载到本地目录（如/data/flink-1.14.4/tmp/下的flink-netty-shuffle*、flink-rpc-akka*、flink-io*、blobStore*等文件）后，才开始为Sink阶段申请合适的Slot资源进行后续处理（如Bucket分组、入库、清理临时数据等）。
（8）开启数据排序时，对管理内存要求较高，其单个排序线程所需内存约为 managed memory / slots。
（9）Sink并行度决定了小文件数量（每个Slot线程在写入最后的剩余数据时产生的文件可能是小文件，大小由剩余数据量决定）。有一种例外情况，即开启Bucket索引时，Bucket数量决定了小文件数（系统最终会生成Bucket个固定文件，若源数据量过少，可能导致每个文件都是小文件，若源数据量过多，可能导致每个文件都过大）。
（10）Bulk Insert写入成功后，进程不会自动提交Kafka Offset，需要另外增加作业监控以更新Offset。
2）批量入库
通过10亿和100亿条等不同数量级的Kafka源数据，对Bulk Insert进行写入测试，其结果如下：
源数据
（亿条）	源数据大小	Source并行度	Sink并行度	压缩类型	平均写入速率
(MiB/s)	平均写入数量（条）	写入延迟	输出数据大小
(GB)	输出文件数
（个）	小文件数
（个）
10	993 GB	100	100	gzip	　557.47	550329	30m 24s	151.2 GB	1296	100
10	993 GB	100	100	snappy	　595.33	587705	28m 28s	235.8 GB	1994	100
100	9.63 TB	100	100	gzip	　547.78	546548	5h 7m 19s	1.5 TB	12573	100
100	9.63 TB	100	100	snappy	　590.03	588869	4h 45m 14s	2.3 TB	19513	100
测试小结：
（1）Bulk Insert适合大批量数据初次入湖场景。
（2）当源数据量超过30亿条数据时，若开启索引和排序，其入库效率会呈指数级下降。大批量数据入库时，建议通过其他方式先对数据去重后，再通过Bulk Insert入库。
（3）数据导入中途，若作业执行失败，会导致全部已入数据失效。建议增加监控方案确保数据正常入库。
（4）一般默认的Gzip压缩方式基本可满足业务需求，改用Snappy方式时其写入效率未见明显提升，且需要更多磁盘空间（实际占用还需考虑HDFS副本数影响）。

### 5.1.2 写 - Upsert

通过1亿条Kafka源数据，对Upsert写入方式进行基准测试，测试明细如下所示：
Source并行度	Bootstrap并行度	bucket assigner并行度	Sink并行度	开启Bucket索引	Bucket数量	开启Flink State默认索引	Task堆内存
(GB)	管理内存
(GB)	网络内存
(GB)	平均写入速率
(MiB/s)	写入延迟	本地临时数据大小（GB)	输出文件数
（个）	小文件数
（个）	入库结果
10	0	0	100	Y	100	N	>50GB	0	<1GB	-1	-1	<0.10GB	-1	-1	失败
1	0	0	30	Y	30	N	>50GB	0	<1GB	0.99	> 4h 50m	<0.10GB	30	0	入库不足1.8kw
1	0	0	100	Y	100	N	>50GB	0	<1GB	2.78	> 4h 50m	<0.10GB	100	0	入库不足5kw
1	100	100	100	N	N	Y	>50GB	0	<1GB	13.48	2h 0m 44s	<0.10GB	200	100	成功
5	60	60	60	N	N	Y	>50GB	0	<1GB	-1	-1	<0.10GB	-1	-1	失败
5	0	100	100	N	N	N	>50GB	0	<1GB	33.88	48m 02s	<0.10GB	300	200	成功
测试小结：
（1）COW表在进行Upsert写入时，Source并行度应尽量降低，或对写入数据进行限流，否则容易导致Task堆内存急剧增高，TaskExecutor进程退出任务失败。
（2）开启Bucket索引时写入效率极低，运行将近5小时，入库不到5kw数据，Sink并行度越低入库越慢
（3）开启Flink State索引时，只相比Bucket索引方式效率略高。
（4）COW表Upsert写入方式对管理内存和网络内存要求低，但对Task堆内存高求高，且整体写入效率低下，失败率高，不建议使用。

### 5.1.3 读

通过1亿和1千万条Hudi源数据，以Batch方式对数据进行查询测试，结果数据以csv格式写入hdfs，测试明细如下：
操作	源数据大小
(GB)	File Slice数量	批/流读	read并行度	Task堆内存
(GB)	管理内存
(GB)	网络内存
(GB)	Job提交延迟
(sec)	JobManager启动延迟
(sec)	TaskExecutor启动延迟
(sec)	查询延迟	数据加载延迟	查询总延迟	本地临时目录	输出记录数
(条)
左连接	15.2	100	Batch	100	>50GB	<1GB	<10GB	15.192	14.753	16	2m 9s	1m 20s	3m 47s	<20GB	10kw
右连接	15.2	100	Batch	100	>50GB	<1GB	<10GB	14.901	17.571	16	28s	1m 19s	2m 8s	<20GB	1kw
内连接	15.2	100	Batch	100	>50GB	<1GB	<10GB	14.697	16.762	16	23s	1m 10s	1m 49s	<20GB	1kw
交集	15.2	100	Batch	100	>50GB	<1GB	<10GB	14.849	15.383	16	27s	1m 21s	2m 5s	<20GB	1kw
差集	15.2	100	Batch	100	>50GB	<1GB	<10GB	14.849	15.383	16	1m 17s	1m 10s	2m 44s	<20GB	9kw
并集	15.2	100	Batch	100	>50GB	<1GB	<10GB	13.546	18.351	16	1m 34s	1m 8s	2m 53s	<20GB	11kw
聚合	15.2	100	Batch	100	>50GB	<1GB	<10GB	21.976	14.408	16	15s	21s	52s	<20GB	10kw
去重	15.2	100	Batch	100	>50GB	<1GB	<10GB	14.773	15.517	16	1m 33s	1m 15s	3m 7s	<20GB	10kw
性能指标图例如下：

测试小结：
（1）除聚合操作外，其他查询过程中，源数据会先暂存在本地目录下的flink-netty-shuffle文件中，加载完成后才为HashJoin等具体查询操作申请计算资源，进行后续查询和计算，其查询过程需要占用与源数据大小相近的空间。
（2）实际查询所需时间 ~= Job提交延迟 + JobManager启动延迟 + TaskExecutor启动延迟（数量越多启动越慢） + 查询总延迟（包含数据加载和具体查询）。
（3）Join相关操作对网络内存和堆内存要求较高，另需少量管理内存。
（4）并行度越高，查询效率越高，但不宜超过源数据文件数量，以避免线程空跑，减少资源浪费。
（5）COW表中只包含parquet文件，因此读取时无需进行File Slice数据合并，可提高读取效率。但Flink目前未对Hudi读取过程做优化，在进行任何类型的数据查询时，都会遍历所有最新版本的Parquet文件，并且其读取过程涉及到gz文件解压，因此实际的读取延迟要视待处理的parquet文件数量而定，parquet文件越多，读取速度越慢。
（6）通过Bulk Insert + Bucket Index方式入库的Hudi数据文件数量可控，大小均衡，其读取效率较其他写入方式更优。

## 5.2 MOR

### 5.2.1 写 - Bulk Insert

同《5.1.1 写 - Bulk Insert》

### 5.2.2 写 - Upsert

通过1亿条Kafka源数据，对Upsert写入方式进行基准测试。其状态后端为hashmap，checkpoint后端采用filesystem + hdfs方式。
1）Bucket Index
开启Bucket Index，设置Bucket数量为100个，采用在线Compact方式合并文件，其测试明细如下：
Source并行度	Sink并行度	Compact并行度	Compact频率	堆内存
(GB)	管理内存
(GB)	网络内存
(GB)	平均写入速率
(MiB/s)	平均写入数量（条）	写入延迟	本地临时数据大小（GB)	File Slice数量
10	100	5	1 commit
5 min	<50GB	0	<1GB	104.95	103626	16m 5s	<0.10GB	100
20	100	5	1 commit
5 min	<50GB	0	<1GB	146.77	144927	11m 30s	<0.10GB	100
30	100	5	1 commit
5 min	<50GB	0	<1GB	224.06	221238	7m 32s	<0.10GB	100
40	100	5	1 commit
5 min	<50GB	0	<1GB	234.97	232018	7m 11s	<0.10GB	100
50	100	5	1 commit
5 min	<50GB	0	<1GB	250.68	247524	6m 44s	<0.10GB	100
60	100	5	1 commit
5 min	<50GB	0	<1GB	267.92	264550	6m 18s	<0.10GB	100
70	100	5	1 commit
5 min	<50GB	0	<1GB	248.83	245700	6m 47s	<0.10GB	100
80	100	5	1 commit
5 min	<50GB	0	<1GB	234.97	232018	7m 11s	<0.10GB	100
90	100	5	1 commit
5 min	<50GB	0	<1GB	255.74	252525	6m 36s	<0.10GB	100
100	100	5	1 commit
5 min	<50GB	0	<1GB	287.71	284090	5m 52s	<0.10GB	100
提高Compact频率和并行度（1亿条数据，一般经过5次deltacommit可全部入库），测试Compact延迟：
Source并行度	Sink并行度	Compact并行度	Compact频率	堆内存
(GB)	管理内存
(GB)	网络内存
(GB)	写入延迟	Compact延迟
100	100	20	5 commit
1 min	>50GB	0	<1GB	7m 55s	21m 47s
性能指标对比如下：

测试小结：
（1）Bucket索引无需占用管理内存，对网络内存要求低，Task堆内存使用也不多，写入效率高。
（2）在线Compact线程与写入任务共用Task堆内存，合并过程会占用较多内存，需要合理控制内存大小。
（3）Compact合并效率一般（数据全部入库后，将每5分钟合并一次，每次5个线程共合并5个File Slice）在很长时间内目录里会存在大量小文件（Parquet），直到所有文件合并完成，小文件才会消失。通过提高Compact频率和并行度可加快Compact处理速度，但需控制Compact内存使用大小。
（4）最终会生成与Bucket数量相同个数的File Slice，这些File Slice不受Parquet基准文件大小限制，因此在建表之初，需要按照可能写入的数据量，合理计算Bucket数量，写入后将不可更改。建议计算公式：【Bucket数量 = 源数据MB/压缩比6.4/Parquet基准文件大小128MB】，实际Parquet文件大小略小于128MB，约在124MB左右。Sink并行度尽量与Bucket数量匹配。
（5）一般建议单个Bucket最大不超过3GB，但若Bucket数量过少，会导致并发能力不足。
（6）默认需要关闭unaligned，可通过execution.checkpointing.unaligned.forced强制开启。
2）Flink State Index
开启Flink State Index，采用在线Compact方式合并文件，其测试明细如下：
Source并行度	Bootstrap并行度	bucket assigner并行度	Sink并行度	Compact并行度	Compact频率	堆内存
(GB)	管理内存
(GB)	网络内存
(GB)	平均写入速率
(MiB/s)	平均写入数量（条）	写入延迟	本地临时数据大小（GB)	输出数据大小
(GB)	File Slice数量
10	100	100	100	5	1 commit
5 min	>50GB	0	<1GB	99.48	98231	16m 58s	<0.10GB	52.8	290
20	100	100	100	5	1 commit
5 min	>50GB	0	<1GB	145.09	143266	11m 38s	<0.10GB	45.1	300
30	100	100	100	5	1 commit
5 min	>50GB	0	<1GB	142.44	140646	11m 51s	<0.10GB	44	284
40	100	100	100	5	1 commit
5 min	>50GB	0	<1GB	164.14	162074	10m 17s	<0.10GB	36.8	256
50	100	100	100	5	1 commit
5 min	>50GB	0	<1GB	155.33	153374	10m 52s	<0.10GB	51.7	260
60	100	100	100	5	1 commit
5 min	>50GB	0	<1GB	155.09	153139	10m 53s	<0.10GB	49.2	256
70	100	100	100	5	1 commit
5 min	>50GB	0	<1GB	151.38	149476	11m 9s	<0.10GB	48.5	256
80	100	100	100	5	1 commit
5 min	>50GB	0	<1GB	149.15	147275	11m 19s	<0.10GB	46.4	300
90	100	100	100	5	1 commit
5 min	>50GB	0	<1GB	147.2	145348	11m 28s	<0.10GB	35.3	256
100	100	100	100	5	1 commit
5 min	>50GB	0	<1GB	160.24	158227	10m 32s	<0.10GB	52.7	256
性能指标对比如下：

测试小结：
（1）Flink State索引无需占用管理内存，对网络内存要求低，Task堆内存使用也不多，写入效率高，只是略低于Bucket索引。
（2）在线Compact线程与写入任务共用Task堆内存，合并过程会占用较多内存，需要合理控制内存大小。
（3）相比Bucket索引，Flink State索引的小文件数量更多，Compact合并效率更低。

### 5.2.3 读

1）Bucket Index
通过1亿和1千万条Hudi源数据，以Batch方式对Bucket Index数据进行查询测试，结果数据以csv格式写入hdfs，测试明细如下：
操作	源数据大小
(GB)	File Slice数量	批/流读	read并行度	Task堆内存
(GB)	管理内存
(GB)	网络内存
(GB)	Job提交延迟
(sec)	JobManager启动延迟
(sec)	TaskExecutor启动延迟
(sec)	查询延迟	数据加载延迟	查询总延迟	本地临时目录	输出记录数
(条)
左连接	15.2	100	Batch	100	>50GB	<1GB	<10GB	15.036	14.637	16	1m 50s	1m 43s	3m 49s	<20GB	10kw
右连接	15.2	100	Batch	100	>50GB	<1GB	<10GB	15.101	14.265	16	27s	1m 34s	2m 18s	<20GB	1kw
内连接	15.2	100	Batch	100	>50GB	<1GB	<10GB	14.899	14.432	16	28s	1m 31s	2m 17s	<20GB	1kw
交集	15.2	100	Batch	100	>50GB	<1GB	<10GB	15.365	16.841	16	28s	1m 29s	2m 13s	<20GB	1kw
差集	15.2	100	Batch	100	>50GB	<1GB	<10GB	15.285	15.201	16	1m 7s	1m 13s	2m 36s	<20GB	9kw
并集	15.2	100	Batch	100	>50GB	<1GB	<10GB	13.538	15.686	16	1m 43s	1m 15s	2m 54s	<20GB	11kw
聚合	15.2	100	Batch	100	>50GB	<1GB	<10GB	14.626	14.337	16	14s	19s	45s	<20GB	10kw
去重	15.2	100	Batch	100	>50GB	<1GB	<10GB	14.44	16.378	16	1m 34s	1m 9s	2m 57s	<20GB	10kw
性能指标图例如下：

2）Flink State Index
通过1亿和1千万条Hudi源数据，以Batch方式对Flink State Index数据（均为小文件）进行查询测试，结果数据以csv格式写入hdfs，测试明细如下：
操作	源数据大小
(GB)	File Slice数量	批/流读	read并行度	Task堆内存
(GB)	管理内存
(GB)	网络内存
(GB)	Job提交延迟
(sec)	JobManager启动延迟
(sec)	TaskExecutor启动延迟
(sec)	查询延迟	数据加载延迟	查询总延迟	本地临时目录	输出记录数
(条)
左连接	15.2	256	Batch	100	>50GB	<1GB	<10GB	15.334	14.008	16	1m 47s	1m 15s	3m 18s	<20GB	10kw
右连接	15.2	256	Batch	100	>50GB	<1GB	<10GB	15.265	16.113	16	28s	1m 21s	2m 6s	<20GB	1kw
内连接	15.2	256	Batch	100	>50GB	<1GB	<10GB	14.946	14.646	16	32s	1m 21s	2m 10s	<20GB	1kw
交集	15.2	256	Batch	100	>50GB	<1GB	<10GB	15.906	19.053	16	26s	1m 23s	2m 5s	<20GB	1kw
差集	15.2	256	Batch	100	>50GB	<1GB	<10GB	15.584	14.63	16	1m 8s	1m 7s	2m 31s	<20GB	9kw
并集	15.2	256	Batch	100	>50GB	<1GB	<10GB	13.652	14.626	16	1m 21s	1m 11s	2m 31s	<20GB	11kw
聚合	15.2	256	Batch	100	>50GB	<1GB	<10GB	14.864	14.667	16	14s	23s	54s	<20GB	10kw
去重	15.2	256	Batch	100	>50GB	<1GB	<10GB	14.717	15.602	16	1m 36s	1m 10s	3m 6s	<20GB	10kw
性能指标图例如下：

3）测试小结
（1）除聚合操作外，其他查询过程中，源数据会先暂存在本地目录下的flink-netty-shuffle文件中，全部加载完成后才为HashJoin等具体查询操作申请计算资源，进行后续查询和计算，其查询过程需要占用与源数据大小相近的空间。
（2）实际查询所需时间 ~= Job提交延迟 + JobManager启动延迟 + TaskExecutor启动延迟（数量越多启动越慢） + 查询总延迟（包含数据加载和具体查询）。
（3）Join相关操作对网络内存和堆内存要求较高，另需少量管理内存。
（4）并行度越高，查询效率越高，但不宜超过源数据File Slice（每个File Slice包含1个Parquet和多个Avro文件）数量，以避免线程空跑，减少资源浪费。
（5）MOR表读取时需要进行File Slice数据合并，会适当降低读取效率。Flink目前未对Hudi读取过程做优化，在进行任何类型的数据查询时，都会遍历所有File Slice中的文件，进行数据合并，并且其读取过程涉及到gz文件解压，因此实际的读取延迟要视待处理的File Slice数量和文件大小而定，数量越多，读取速度越慢。
（6）通过Bulk Insert + Bucket Index方式入库的Hudi数据文件数量可控，大小均衡，其读取效率较其他写入方式更优。
六、测试总结
6.1 写
针对亿级数据量，根据Bulk Insert和Upsert两种写入方式，使用不同索引方式，对COW和MOR两种类型的Hudi表进行写入性能对比，如下所示：


总结如下：
1）Bulk Insert写入方式中，两表整体写入性能相近，由于运行时环境产生变化，结果可能产生细微偏差。
2）Upsert写入方式中，COW写入效率极低，且稳定性差，MOR写入效率相对更高。
3）写入效率及适用场景如下，排名分先后：
写入效率	表类型	写入类型	索引类型	是否排序	是否去重	适用数据量	适合场景
高	MOR/COW	Bulk Insert	N	N	N	>1亿	一次性全量入库，但需由其他组件保证数据唯一
较高	MOR/COW	Bulk Insert	Bucket	Y	Y	<5亿	批量入库
一般	MOR	Upsert	Bucket	N	Y	<1亿	流式增量入库
一般	MOR/COW	Bulk Insert	Bucket	N	Y	<5亿	批量入库
较低	MOR	Upsert	Flink State	N	Y	<5亿	流式增量入库
较低	MOR	Upsert	N	N	Y	<5亿	流式增量入库
低	COW	Upsert	N	N	Y	<1千万	不稳定，不建议使用
6.2 读
针对亿级数据量，根据Bulk Insert和Upsert两种写入方式，使用不同索引方式，对COW和MOR两种类型的Hudi表进行查询性能对比，如下所示：


总结如下：
1）Hudi暂未对Flink读取方式进行优化，每次查询过程都需要遍历每个File Slice，因此数据读取效率与写入类型关联性不大。
2）File Slice读时合并机制，对数据加载过程略有影响，但对整体查询效率影响不大。
3）读取时设置的并行度越高（不超过File Slice数量），其读取效率越高。
七、优化建议
1）1亿级以上数据：
对数据进行分区，将每个分区的数据量尽量控制在1亿级以内，针对每个分区，分批入库。
2）如果是不常变更的分区全量数据，可采用 MOR + Bulk Insert + Bucket Index + Sort 方式批量入库，根据源数据量合理计算Bucket数量，其相关计算公式如下：
【write.sort.memory <= managed memory / slots】，排序时使用的内存通过管理内存进行分配，Sort算子执行时不能超过管理内存上限。
【Bucket数量 = 源数据MB/压缩比6.4/Parquet基准文件大小】，
其中Parquet基准文件大小【write.parquet.max.file.size】默认为120MB，一般设置为hdfs-site.xml中dfs.blocksize的倍数。
【write.parquet.max.file.size >= base file + N* log file * 0.35】，其中base file指Parquet文件，log file为Avro追加写入文件。
【并行处理粒度 =  write.parquet.max.file.size/dfs.blocksize * nfiles】，资源充足时，可适当增大Parquet文件大小，但应控制在1GB以内。
3）如果分区数据偶尔发生变更，可先采用 MOR + Bulk Insert + Bucket Index + Sort 批处理方式全量入库，然后采用 MOR + Upsert + Bootstrap + Bucket Index/Flink State Index + Online Compact 方式流式增量入库：
Bootstrap过程会针对全量数据进行数据索引，当第一次checkpoint完成时，表示索引全部加载并持久化完成，之后才会进行数据接入和写入操作，保证数据唯一性。
因此，初次开启增量接入时，应提高作业需使用的资源，如内存、并行度等。
后续入库可关闭Bootstrap索引加载过程。
在线Compact与具体作业运行时同属一个进程中的不同线程，资源使用时会互相影响，应根据数据量适当控制Compact并行度和内存使用量，避免发生OOM，其计算公式为：
【compaction.tasks * compaction.max_memory < Task堆内存 - 其他算子所需堆内存】，
【compaction.tasks】默认为4，资源充足时可适当调大，加快Compact进度，但需考虑Task堆内存大小限制。
【compaction.max_memory】默认为100MB，在资源充足时可适当调大，但不建议超过1GB。
离线Compact目前只支持Yarn-Session模式，功能尚不成熟，暂不建议使用。
4）如果分区数据频繁发生变更，可采用 MOR + Upsert + Flink State Index + Online Compact方式流式入库，计算公式同（3）。
5）由于Hudi暂未对Flink读取方式进行更多优化，其读取效率基本与hive相近，因此可通过在数据入库到MOR表的同时开启hive同步功能，使用hive外部表方式，满足对数据的分析统计需求。
