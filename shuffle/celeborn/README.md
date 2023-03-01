# Apache Celeborn (Incubating)

[![Celeborn CI](https://github.com/apache/incubator-celeborn/actions/workflows/maven.yml/badge.svg)](https://github.com/apache/incubator-celeborn/actions/workflows/maven.yml)  
Celeborn is dedicated to improving the efficiency and elasticity of
different map-reduce engines and provides an elastic, high-efficient 
management service for intermediate data including shuffle data, spilled data, result data, etc. Currently Celeborn is focusing on shuffle data.

## Internals
### Architecture
![Celeborn architecture](assets/img/rss.jpg)
Celeborn has three primary components: Master, Worker, and Client.
Master manages all resources and syncs shard states with each other based on Raft.
Worker processes read-write requests and merges data for each reducer.
LifecycleManager maintains metadata of each shuffle and runs within the Spark driver.

### Feature
1. Disaggregate Compute and storage.
2. Push-based shuffle write and merged shuffle read.
3. High availability and high fault tolerance.

### Shuffle Process
![Celeborn shuffle](assets/img/shuffle-procedure.jpg)
1. Mappers lazily ask LifecycleManager to registerShuffle.
2. LifecycleManager requests slots from Master.
3. Workers reserve slots and create corresponding files.
4. Mappers get worker locations from LifecycleManager.
5. Mappers push data to specified workers.
6. Workers merge and replicate data to its peer.
7. Workers flush to disk periodically.
8. Mapper tasks accomplish and trigger MapperEnd event.
9. When all mapper tasks are complete, workers commit files.
10. Reducers ask for file locations.
11. Reducers read shuffle data.

### Load Balance
![Load Balance](assets/img/rss_load_balance.jpg)

We introduce slots to achieve load balance. We will equally distribute partitions on every Celeborn worker by tracking slots usage.
The Slot is a logical concept in Celeborn Worker that represents how many partitions can be allocated on each Celeborn Worker.
Celeborn Worker's slot count is decided by `total usable disk size / average shuffle file size`.
Celeborn worker's slot count decreases when a partition is allocated and increments when a partition is freed.

## Build
Celeborn supports Spark 2.4/3.0/3.1/3.2/3.3 and only tested under Java 8.

Build for Spark
```
./build/make-distribution.sh -Pspark-2.4/-Pspark-3.0/-Pspark-3.1/-Pspark-3.2/-Pspark-3.3
```

package apache-celeborn-${project.version}-bin.tgz will be generated.

### Package Details
Build procedure will create a compressed package.
```
    ├── RELEASE                         
    ├── bin                             
    ├── conf                            
    ├── master-jars                     
    ├── worker-jars                     
    ├── sbin                            
    └── spark          // Spark client jars
```

### Compatibility
Celeborn server is compatible with all supported Spark versions.
You can run different Spark versions with the same Celeborn server. It doesn't matter whether Celeborn server is compiled with -Pspark-2.4/3.0/3.1/3.2/3.3.
However, Celeborn client must be consistent with the version of the Spark.
For example, if you are running Spark 2.4, you must compile Celeborn client with -Pspark-2.4; if you are running Spark 3.2, you must compile Celeborn client with -Pspark-3.2.

## Usage
Celeborn cluster composes of Master and Worker nodes, the Master supports both single and HA mode(Raft-based) deployments.

### Deploy Celeborn
1. Unzip the tarball to `$CELEBORN_HOME`
2. Modify environment variables in `$CELEBORN_HOME/conf/celeborn-env.sh`

EXAMPLE:
```properties
#!/usr/bin/env bash
CELEBORN_MASTER_MEMORY=4g
CELEBORN_WORKER_MEMORY=2g
CELEBORN_WORKER_OFFHEAP_MEMORY=4g
```
3. Modify configurations in `$CELEBORN_HOME/conf/celeborn-defaults.conf`

EXAMPLE: single master cluster
```properties
# used by client and worker to connect to master
celeborn.master.endpoints clb-master:9097

# used by master to bootstrap
celeborn.master.host clb-master
celeborn.master.port 9097

celeborn.metrics.enabled true
celeborn.worker.flush.buffer.size 256k
celeborn.worker.storage.dirs /mnt/disk1/,/mnt/disk2
# If your hosts have disk raid or use lvm, set celeborn.worker.monitor.disk.enabled to false
celeborn.worker.monitor.disk.enabled false
```   

EXAMPLE: HA cluster
```properties
# used by client and worker to connect to master
celeborn.master.endpoints clb-1:9097,clb-2:9098,clb-3:9099

# used by master nodes to bootstrap, every node should know the topology of whole cluster, for each node,
# `celeborn.ha.master.node.id` should be unique, and `celeborn.ha.master.node.<id>.host` is required
celeborn.ha.enabled true
celeborn.ha.master.node.id 1
celeborn.ha.master.node.1.host clb-1
celeborn.ha.master.node.1.port 9097
celeborn.ha.master.node.1.ratis.port 9872
celeborn.ha.master.node.2.host clb-2
celeborn.ha.master.node.2.port 9098
celeborn.ha.master.node.2.ratis.port 9873
celeborn.ha.master.node.3.host clb-3
celeborn.ha.master.node.3.port 9099
celeborn.ha.master.node.3.ratis.port 9874
celeborn.ha.master.ratis.raft.server.storage.dir /mnt/disk1/rss_ratis/

celeborn.metrics.enabled true
# If you want to use HDFS as shuffle storage, make sure that flush buffer size is at least 4MB or larger.
celeborn.worker.flush.buffer.size 256k
celeborn.worker.storage.dirs /mnt/disk1/,/mnt/disk2
# If your hosts have disk raid or use lvm, set celeborn.worker.monitor.disk.enabled to false
celeborn.worker.monitor.disk.enabled false
```
4. Copy Celeborn and configurations to all nodes
5. Start Celeborn master
   `$CELEBORN_HOME/sbin/start-master.sh`
6. Start Celeborn worker
   For single master cluster : `$CELEBORN_HOME/sbin/start-worker.sh rss://<master-host>:<master-port>`
   For HA cluster :`$CELEBORN_HOME/sbin/start-worker.sh`
7. If Celeborn start success, the output of Master's log should be like this:
```angular2html
22/10/08 19:29:11,805 INFO [main] Dispatcher: Dispatcher numThreads: 64
22/10/08 19:29:11,875 INFO [main] TransportClientFactory: mode NIO threads 64
22/10/08 19:29:12,057 INFO [main] Utils: Successfully started service 'MasterSys' on port 9097.
22/10/08 19:29:12,113 INFO [main] Master: Metrics system enabled.
22/10/08 19:29:12,125 INFO [main] HttpServer: master: HttpServer started on port 9098.
22/10/08 19:29:12,126 INFO [main] Master: Master started.
22/10/08 19:29:57,842 INFO [dispatcher-event-loop-19] Master: Registered worker
Host: 192.168.15.140
RpcPort: 37359
PushPort: 38303
FetchPort: 37569
ReplicatePort: 37093
SlotsUsed: 0()
LastHeartbeat: 0
Disks: {/mnt/disk1=DiskInfo(maxSlots: 6679, committed shuffles 0 shuffleAllocations: Map(), mountPoint: /mnt/disk1, usableSpace: 448284381184, avgFlushTime: 0, activeSlots: 0) status: HEALTHY dirs , /mnt/disk3=DiskInfo(maxSlots: 6716, committed shuffles 0 shuffleAllocations: Map(), mountPoint: /mnt/disk3, usableSpace: 450755608576, avgFlushTime: 0, activeSlots: 0) status: HEALTHY dirs , /mnt/disk2=DiskInfo(maxSlots: 6713, committed shuffles 0 shuffleAllocations: Map(), mountPoint: /mnt/disk2, usableSpace: 450532900864, avgFlushTime: 0, activeSlots: 0) status: HEALTHY dirs , /mnt/disk4=DiskInfo(maxSlots: 6712, committed shuffles 0 shuffleAllocations: Map(), mountPoint: /mnt/disk4, usableSpace: 450456805376, avgFlushTime: 0, activeSlots: 0) status: HEALTHY dirs }
WorkerRef: null
```

### Deploy Spark client
Copy $CELEBORN_HOME/spark/*.jar to $SPARK_HOME/jars/

### Spark Configuration
To use Celeborn, following spark configurations should be added.
```properties
spark.shuffle.manager org.apache.spark.shuffle.celeborn.RssShuffleManager
# must use kryo serializer because java serializer do not support relocation
spark.serializer org.apache.spark.serializer.KryoSerializer

# celeborn master
spark.celeborn.master.endpoints clb-1:9097,clb-2:9098,clb-3:9099
spark.shuffle.service.enabled false

# options: hash, sort
# Hash shuffle writer use (partition count) * (celeborn.push.buffer.size) * (spark.executor.cores) memory.
# Sort shuffle writer use less memory than hash shuffle writer, if your shuffle partition count is large, try to use sort hash writer.  
spark.celeborn.shuffle.writer hash

# we recommend set spark.celeborn.push.replicate.enabled to true to enable server-side data replication 
spark.celeborn.push.replicate.enabled true

# Support for Spark AQE only tested under Spark 3
# we recommend set localShuffleReader to false to get better performance of Celeborn
spark.sql.adaptive.localShuffleReader.enabled false

# we recommend enabling aqe support to gain better performance
spark.sql.adaptive.enabled true
spark.sql.adaptive.skewJoin.enabled true
```

### Best Practice
If you want to set up a production-ready Celeborn cluster, your cluster should have at least 3 masters and at least 4 workers.
Masters and works can be deployed on the same node but should not deploy multiple masters or workers on the same node.
See more detail in [CONFIGURATIONS](docs/configuration.md)

### Support Spark Dynamic Allocation
We provide a patch to enable users to use Spark with both Dynamic Resource Allocation(DRA) and Celeborn.
For Spark2.x check [Spark2 Patch](assets/spark-patch/RSS_RDA_spark2.patch).  
For Spark3.x check [Spark3 Patch](assets/spark-patch/RSS_RDA_spark3.patch).

### Metrics
Celeborn has various metrics. [METRICS](METRICS.md)

## NOTICE
If you need to fully restart a Celeborn cluster in HA mode,
you must clean ratis meta storage first
because ratis meta will store expired states of the last running cluster.

Here are some instructions:
1. Stop all workers.
2. Stop all masters.
3. Clean all master's ratis meta storage directory(celeborn.ha.master.ratis.raft.server.storage.dir).
4. Start all masters.
5. Start all workers.

## 官方项目地址
https://github.com/apache/incubator-uniffle