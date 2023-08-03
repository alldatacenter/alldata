![logo](site/docs/ch/images/arctic_logo_for_git.png)

Arctic is a LakeHouse management system under open architecture, which on top of data lake open formats provides more optimizations for streaming and upsert scenarios, as well as a set of pluggable self-optimizing mechanisms and management services. Using Arctic could help various data platforms, tools and products build out-of-the-box, streaming and batch unified LakeHouses quickly.

## What is Arctic

Currently, Arctic is a LakeHouse management system on top of iceberg format. Benefit from the thriving ecology of Apache Iceberg, Arctic could be used on kinds of data lakes on premise or clouds with varities of engines. Several concepts should be known before your deeper steps:

![Introduce](site/docs/ch/images/introduce_arctic.png)

- AMS and optimizers - Arctic Management Service provides management features including self-optimizing mechanisms running on optimizers, which could be scaled as demand and scheduled on different platforms.
- Multiple formats — Arctic use formats analogous to MySQL or ClickHouse using storage engines to meet different scenarios. Two formats were available since Arctic v0.4.
	* Iceberg format — learn more about iceberg format details and usage with different engines: [Iceberg Docs](https://iceberg.apache.org/docs/latest/)
	* Mixed streaming format - if you are interested in advanced features like auto-bucket, logstore, hive compatible, strict PK constraints etc. learn Arctic [Mixed Iceberg format](https://arctic.netease.com/ch/concepts/table-formats/#mixed-iceberg-format) and [Mixed Hive format](https://arctic.netease.com/ch/concepts/table-formats/#mixed-hive-format)
## Arctic features

- Defining keys - supports defining primary key with strict constraints, and more types of keys in future
- Self-optimizing - user-insensitive asynchronous self-optimization mechanisms could keep lakehouse fresh and healthy
- Management features - dashboard UI to support catalog/table management, SQL terminal and all kinds of metrics
- Formats compatible - Hive/Iceberg format compatible means writing and reading through native Hive/Iceberg connector 
- Better data pipeline SLA - using LogStore like kafka to accelarate streaming data pipeline to ms/s latency
- Better OLAP performace - provides auto-bucket feature for better compaction and merge-on-read performance
- Concurrent conflicts resovling - Flink or Spark could concurrent write data without worring about conflicts

## Modules

Arctic contains modules as below:

- `arctic-core` contains core abstractions and common implementation for other modules
- `arctic-flink` is the module for integrating with Apache Flink (use arctic-flink-runtime for a shaded version)
- `arctic-spark` is the module for integrating with Apache Spark (use arctic-spark-runtime for a shaded version)
- `arctic-trino` now provides query integrating with apache trino, built on JDK17
- `arctic-ams` is arctic meta service module
  - `ams-api` contains ams thrift api
  - `ams-dashboard` is the dashboard frontend for ams
  - `ams-server` is the backend server for ams
  - `ams-optimizer` provides default optimizer implementation

## Building

Arctic is built using Maven with Java 1.8 and Java 17(only for `trino` module).

* To build Trino module need config `toolchains.xml` in `${user.home}/.m2/` dir, the content is
```
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
    <toolchain>
        <type>jdk</type>
        <provides>
            <version>17</version>
            <vendor>sun</vendor>
        </provides>
        <configuration>
            <jdkHome>${YourJDK17Home}</jdkHome>
        </configuration>
    </toolchain>
</toolchains>
```
* To invoke a build and run tests: `mvn package -P toolchain`
* To skip tests: `mvn -DskipTests package -P toolchain`
* To package without trino module and JAVA 17 dependency: `mvn clean package -DskipTests -pl '!trino'`

## Engines supported

Arctic support multiple processing engines as below:

| Processing Engine | Version                   |
| ----------------- |---------------------------|
| Flink             | 1.12.x, 1.14.x and 1.15.x |
| Spark             | 3.1, 3.2, 3.3             |
| Trino             | 406                       |

## Quickstart

Visit [https://arctic.netease.com/ch/quickstart/setup/](https://arctic.netease.com/ch/quickstart/setup/) to quickly explore what arctic can do.

## Join Community 
If you are interested in Lakehouse, Data Lake Format, welcome to join our community, we welcome any organizations, teams and individuals to grow together, and sincerely hope to help users better use Data Lake Format through open source. 

Join the Arctic WeChat Group: Add " `kllnn999` " as a friend on WeChat and specify "Arctic lover".
