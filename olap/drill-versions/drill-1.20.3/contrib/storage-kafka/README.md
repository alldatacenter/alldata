# Drill Kafka Plugin

Drill kafka storage plugin allows you to perform interactive analysis using SQL against Apache Kafka.

<h4 id="Supported kafka versions">Supported Kafka Version</h4>
Kafka-0.10 and above </p>

<h4 id="Supported Message Formats">Message Formats</h4>
Currently this plugin supports reading only Kafka messages of type <strong>JSON</strong>.


<h4>Message Readers</h4>
<p>Message Readers are used for reading messages from Kafka. Type of the MessageReaders supported as of now are</p>

<table style="width:100%">
  <tr>
    <th>MessageReader</th>
    <th>Description</th>
    <th>Key DeSerializer</th> 
    <th>Value DeSerializer</th>
  </tr>
  <tr>
    <td>JsonMessageReader</td>
    <td>To read Json messages</td>
    <td>org.apache.kafka.common.serialization.ByteArrayDeserializer</td> 
    <td>org.apache.kafka.common.serialization.ByteArrayDeserializer</td>
  </tr>
</table>


<h4 id="Plugin Configurations">Plugin Configurations</h4>
Drill Kafka plugin supports following properties
<ul>
   <li><strong>kafkaConsumerProps</strong>: These are typical <a href="https://kafka.apache.org/documentation/#consumerconfigs">Kafka consumer properties</a>.</li>
<li><strong>System options</strong>: These are Drill Kafka plugin  system options. <ul>
<li><strong>store.kafka.record.reader</strong>: Message Reader implementation to use while reading messages from Kafka. Default value is  set to org.apache.drill.exec.store.kafka.decoders.JsonMessageReader
</li>
<li><strong>store.kafka.poll.timeout</strong>: Polling timeout used by Kafka client while fetching messages from Kafka cluster. Default value is 200 milliseconds. </li>
</ul>
</li>
</ul>

<h4 id="Plugin Registration">Plugin Registration</h4>
To register the kafka plugin, open the drill web interface. To open the drill web interface, enter <strong>http://drillbit:8047/storage</strong> in your browser.

<p>The following is an example plugin registration configuration</p>
<pre>
{
  "type": "kafka",
  "kafkaConsumerProps": {
    "key.deserializer": "org.apache.kafka.common.serialization.ByteArrayDeserializer",
    "auto.offset.reset": "earliest",
    "bootstrap.servers": "localhost:9092",
    "group.id": "drill-query-consumer-1",
    "enable.auto.commit": "true",
    "value.deserializer": "org.apache.kafka.common.serialization.ByteArrayDeserializer",
    "session.timeout.ms": "30000"
  },
  "enabled": true
}
</pre>

<h4 id="Abstraction"> Abstraction </h4>
<p>In Drill, each Kafka topic is mapped to a SQL table and when a query is issued on a table, it scans all the messages from the earliest offset to the latest offset of that topic at that point of time. This plugin automatically discovers all the topics (tables), to allow you perform analysis without executing DDL statements.

<h4 id="Mapping">MetaData</h4>
This plugin also fetches the additional information about each message. The following additional fields are supported as now
<ul>
	<li>kafkaTopic</li>
	<li>kafkaPartitionId</li>
	<li>kafkaMsgOffset</li>
	<li>kafkaMsgTimestamp</li>
	<li>kafkaMsgKey, unless it is not null</li>
</ul>

<h4 id="Examples"> Examples </h4>

Kafka topics and message offsets

```
$bin/kafka-topics --list --zookeeper localhost:2181
clicks
clickstream
clickstream-json-demo

$ bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic clickstream-json-demo --from-beginning | more
{"userID":"055e9af4-8c3c-4834-8482-8e05367a7bef","sessionID":"7badf08e-1e1d-4aeb-b853-7df2df4431ac","pageName":"shoes","refferalUrl":"yelp","ipAddress":"20.44.183.126","userAgent":"Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_1 like Mac OS X) AppleWebKit/603.1.30 (KHTML, like Gecko) Version/10.0 Mobile/14E304 Safari/602.1","client_ts":1509926023099}
{"userID":"a29454b3-642d-481e-9dd8-0e0d7ef32ef5","sessionID":"b4a89204-b98c-4b4b-a1a9-f28f22d5ead3","pageName":"books","refferalUrl":"yelp","ipAddress":"252.252.113.190","userAgent":"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 Safari/537.36 OPR/38.0.2220.41","client_ts":1509926023100}
{"userID":"8c53b1c6-da47-4b5a-989d-61b5594f3a1d","sessionID":"baae3a1d-25b2-4955-8d07-20191f29ab32","pageName":"login","refferalUrl":"yelp","ipAddress":"110.170.214.255","userAgent":"Mozilla/5.0 (Macintosh; Intel Mac OS X x.y; rv:42.0) Gecko/20100101 Firefox/42.0","client_ts":1509926023100}

$ bin/kafka-run-class kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic clickstream-json-demo --time -2
clickstream-json-demo:2:2765000
clickstream-json-demo:1:2765000
clickstream-json-demo:3:2765000
clickstream-json-demo:0:2765000

$ bin/kafka-run-class kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic clickstream-json-demo --time -1
clickstream-json-demo:2:2765245
clickstream-json-demo:1:2765245
clickstream-json-demo:3:2765245
clickstream-json-demo:0:2765245


```


Drill queries on Kafka

```
$ bin/sqlline -u jdbc:drill:zk=localhost:2181
Java HotSpot(TM) 64-Bit Server VM warning: ignoring option MaxPermSize=512M; support was removed in 8.0
apache drill 1.12.0-SNAPSHOT
"json ain't no thang"
0: jdbc:drill:zk=localhost:2181> use kafka;
+-------+------------------------------------+
|  ok   |              summary               |
+-------+------------------------------------+
| true  | Default schema changed to [kafka]  |
+-------+------------------------------------+
1 row selected (0.564 seconds)
0: jdbc:drill:zk=localhost:2181> show tables;
+---------------+------------------------------+
| TABLE_SCHEMA  |          TABLE_NAME          |
+---------------+------------------------------+
| kafka         | clickstream-json-demo        |
| kafka         | clickstream                  |
| kafka         | clicks                       |
+---------------+------------------------------+
17 rows selected (1.908 seconds)
0: jdbc:drill:zk=localhost:2181> ALTER SESSION SET `store.kafka.poll.timeout` = 200;
+-------+------------------------------------+
|  ok   |              summary               |
+-------+------------------------------------+
| true  | store.kafka.poll.timeout updated.  |
+-------+------------------------------------+
1 row selected (0.102 seconds)
0: jdbc:drill:zk=localhost:2181> ALTER SESSION SET `store.kafka.record.reader` = 'org.apache.drill.exec.store.kafka.decoders.JsonMessageReader';
+-------+-------------------------------------+
|  ok   |               summary               |
+-------+-------------------------------------+
| true  | store.kafka.record.reader updated.  |
+-------+-------------------------------------+
1 row selected (0.082 seconds)
0: jdbc:drill:zk=localhost:2181> select * from kafka.`clickstream-json-demo` limit 2;
+---------------------------------------+---------------------------------------+-------------+--------------+------------------+-----------------------------------------------------------------------------------+----------------+------------------------+-------------------+-----------------+--------------------+
|                userID                 |               sessionID               |  pageName   | refferalUrl  |    ipAddress     |                                     userAgent                                     |   client_ts    |       kafkaTopic       | kafkaPartitionId  | kafkaMsgOffset  | kafkaMsgTimestamp  |
+---------------------------------------+---------------------------------------+-------------+--------------+------------------+-----------------------------------------------------------------------------------+----------------+------------------------+-------------------+-----------------+--------------------+
| 6b55a8fa-d0fd-41f0-94e3-7f6b551cdede  | e3bd34a8-b546-4cd5-a0c6-5438589839fc  | categories  | bing         | 198.105.119.221  | Mozilla/5.0 (Macintosh; Intel Mac OS X x.y; rv:42.0) Gecko/20100101 Firefox/42.0  | 1509926023098  | clickstream-json-demo  | 2                 | 2765000         | 1509926023098      |
| 74cffc37-2df0-4db4-aff9-ed0027a12d03  | 339e3821-5254-4d79-bbae-69bc12808eca  | furniture   | bing         | 161.169.50.60    | Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0     | 1509926023099  | clickstream-json-demo  | 2                 | 2765001         | 1509926023099      |
+---------------------------------------+---------------------------------------+-------------+--------------+------------------+-----------------------------------------------------------------------------------+----------------+------------------------+-------------------+-----------------+--------------------+
2 rows selected (1.18 seconds)
0: jdbc:drill:zk=localhost:2181> select count(*) from kafka.`clickstream-json-demo`;
+---------+
| EXPR$0  |
+---------+
| 980     |
+---------+
1 row selected (0.732 seconds)
0: jdbc:drill:zk=localhost:2181> select kafkaPartitionId, MIN(kafkaMsgOffset) as minOffset, MAX(kafkaMsgOffset) as maxOffset from kafka.`clickstream-json-demo` group by kafkaPartitionId;
+-------------------+------------+------------+
| kafkaPartitionId  | minOffset  | maxOffset  |
+-------------------+------------+------------+
| 2                 | 2765000    | 2765244    |
| 1                 | 2765000    | 2765244    |
| 3                 | 2765000    | 2765244    |
| 0                 | 2765000    | 2765244    |
+-------------------+------------+------------+
4 rows selected (3.081 seconds)
0: jdbc:drill:zk=localhost:2181> select kafkaPartitionId, from_unixtime(MIN(kafkaMsgTimestamp)/1000) as minKafkaTS, from_unixtime(MAX(kafkaMsgTimestamp)/1000) as maxKafkaTs from kafka.`clickstream-json-demo` group by kafkaPartitionId;
+-------------------+----------------------+----------------------+
| kafkaPartitionId  |      minKafkaTS      |      maxKafkaTs      |
+-------------------+----------------------+----------------------+
| 2                 | 2017-11-05 15:53:43  | 2017-11-05 15:53:43  |
| 1                 | 2017-11-05 15:53:43  | 2017-11-05 15:53:43  |
| 3                 | 2017-11-05 15:53:43  | 2017-11-05 15:53:43  |
| 0                 | 2017-11-05 15:53:43  | 2017-11-05 15:53:43  |
+-------------------+----------------------+----------------------+
4 rows selected (2.758 seconds)
0: jdbc:drill:zk=localhost:2181> select distinct(refferalUrl) from kafka.`clickstream-json-demo`;
+--------------+
| refferalUrl  |
+--------------+
| bing         |
| yahoo        |
| yelp         |
| google       |
+--------------+
4 rows selected (2.944 seconds)
0: jdbc:drill:zk=localhost:2181> select pageName, count(*) from kafka.`clickstream-json-demo` group by pageName;
+--------------+---------+
|   pageName   | EXPR$1  |
+--------------+---------+
| categories   | 89      |
| furniture    | 89      |
| mobiles      | 89      |
| clothing     | 89      |
| sports       | 89      |
| offers       | 89      |
| shoes        | 89      |
| books        | 89      |
| login        | 90      |
| electronics  | 89      |
| toys         | 89      |
+--------------+---------+
11 rows selected (2.493 seconds)

```


Note: 

- store.kafka.record.reader system option can be used for setting record reader and default is org.apache.drill.exec.store.kafka.decoders.JsonMessageReader
- Default store.kafka.poll.timeout is set to 200, user has to set this accordingly
- Custom record reader can be implemented by extending org.apache.drill.exec.store.kafka.decoders.MessageReader and setting store.kafka.record.reader accordingly


In case of JSON message format, following system / session options can be used accordingly. More details can be found in [Drill Json Model](https://drill.apache.org/docs/json-data-model/) and in [Drill system options configurations](https://drill.apache.org/docs/configuration-options-introduction/)

<ul>
  <li>ALTER SESSION SET `store.kafka.record.reader` = 'org.apache.drill.exec.store.kafka.decoders.JsonMessageReader';</li>
  <li>ALTER SESSION SET `store.kafka.poll.timeout` = 200;</li>
  <li>ALTER SESSION SET `exec.enable_union_type` = true; </li>
  <li>ALTER SESSION SET `store.kafka.all_text_mode` = true;</li>
  <li>ALTER SESSION SET `store.kafka.read_numbers_as_double` = true;</li>
  <li>ALTER SESSION SET `store.kafka.skip_invalid_records` = true;</li>
  <li>ALTER SESSION SET `store.kafka.allow_nan_inf` = true;</li>
  <li>ALTER SESSION SET `store.kafka.allow_escape_any_char` = true;</li>
</ul>

<h4 id="RoadMap">RoadMap</h4>
 <ul>
   <li>AVRO Message format support</li>
 </ul>
