# InLong Changelog

<!---
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Be careful doing manual edits in this file. Do not change format
# of release header or remove the below marker. This file is generated.
# DO NOT REMOVE THIS MARKER; FOR INTERPOLATING CHANGES!-->

# Release InLong 1.5.0 - Released (as of 2023-01-03)

### Agent
|                            ISSUE                            | Summary                                                                |
|:-----------------------------------------------------------:|:-----------------------------------------------------------------------|
| [INLONG-6970](https://github.com/apache/inlong/issues/6970) | [Improve][Agent] Improve Agent UnitTest                                |
| [INLONG-6960](https://github.com/apache/inlong/issues/6960) | [Bug][Agent] Generate repeat UUID for different messages in some cases |
| [INLONG-6759](https://github.com/apache/inlong/issues/6759) | [Improve][Agent] Improve code style                                    |
| [INLONG-6730](https://github.com/apache/inlong/issues/6730) | [Improve][Agent] Change MOCK task type                                 |
| [INLONG-6704](https://github.com/apache/inlong/issues/6704) | [Feature][Agent] Add pulsar sink                                       |
| [INLONG-6681](https://github.com/apache/inlong/issues/6681) | [Feature][Agent] Support settings for agent's data report-to           |

### DataProxy
|                            ISSUE                            | Summary                                                                            |
|:-----------------------------------------------------------:|:-----------------------------------------------------------------------------------|
| [INLONG-7086](https://github.com/apache/inlong/issues/7086) | [Improve][DataProxy] Add default ext tag report value                              |
| [INLONG-7054](https://github.com/apache/inlong/issues/7054) | [Improve][DataProxy] Add the processing of the rtms field                          |
| [INLONG-6985](https://github.com/apache/inlong/issues/6985) | [Improve][DataProxy] Make maxMonitorCnt setting configurable                       |
| [INLONG-6964](https://github.com/apache/inlong/issues/6964) | [Bug][DataProxy] Caught when deploy in docker-compose                              |
| [INLONG-7070](https://github.com/apache/inlong/issues/7070) | [Bug][Manager][DataProxy] Fix pulling MQ configuration problem                     |
| [INLONG-6955](https://github.com/apache/inlong/issues/6955) | [Improve][Script] Support starts the DataProxy with Kafka in the deployment script |
| [INLONG-6848](https://github.com/apache/inlong/issues/6848) | [Improve][DataProxy] Support original InlongMsg protocol and headers               |
| [INLONG-6802](https://github.com/apache/inlong/issues/6802) | [Improve][DataProxy][Manager] Adapt Apache Kafka as cache cluster                  |
| [INLONG-6738](https://github.com/apache/inlong/issues/6738) | [Feature][DataProxy] New MQ sink integration with TCP source                       |
| [INLONG-6717](https://github.com/apache/inlong/issues/6717) | [Feature][DataProxy] Support BufferQueueChannel                                    |
| [INLONG-6715](https://github.com/apache/inlong/issues/6715) | [Bug][DataProxy] Fix json problem in data proxy                                    |
| [INLONG-6702](https://github.com/apache/inlong/issues/6702) | [Improve][DataProxy] Modify the default network card name to obtain the local IP   |
| [INLONG-6604](https://github.com/apache/inlong/issues/6604) | [Bug][DataProxy] Fix can not get the local IP in the docker container              |
| [INLONG-6602](https://github.com/apache/inlong/issues/6602) | [Bug][DataProxy] The IP config was invalid                                         |
| [INLONG-6593](https://github.com/apache/inlong/issues/6593) | [Bug][DataProxy] Compile and build failed                                          |
| [INLONG-6592](https://github.com/apache/inlong/issues/6592) | [Feature][DataProxy] Fix the problem of missing OrderEvent                         |
| [INLONG-6438](https://github.com/apache/inlong/issues/6438) | [Improve][SDK] DataProxy-SDK parses and handles ack response from DataProxy        |
| [INLONG-6153](https://github.com/apache/inlong/issues/6153) | [Feature][DataProxy] Optimize the sink architecture                                |

### TubeMQ
|                            ISSUE                            | Summary                                                                                |
|:-----------------------------------------------------------:|:---------------------------------------------------------------------------------------|
| [INLONG-6997](https://github.com/apache/inlong/issues/6997) | [Improve][TubeMQ] Add sendMessage and getMessage latency statistics                    |
| [INLONG-6638](https://github.com/apache/inlong/issues/6638) | [Bug][TubeMQ] The C++ client does not support filtering consume with the - character   |
| [INLONG-6611](https://github.com/apache/inlong/issues/6611) | [Feature][TubeMQ] Base implementation of TubeMQ C++ Producer                           |
| [INLONG-6579](https://github.com/apache/inlong/issues/6579) | [Bug][TubeMQ] The updated version causes the consumer group permissions to be abnormal |
| [INLONG-6535](https://github.com/apache/inlong/issues/6535) | [Improve][TubeMQ] The storage space used by the offset of TubeMQ broker is too large   |
| [INLONG-4969](https://github.com/apache/inlong/issues/4969) | [Feature][TubeMQ] Python SDK for Producing Message                                     |
| [INLONG-7106](https://github.com/apache/inlong/issues/7106) | [Improve][TubeMQ] Add reset offset API by time                                         |

### Manager
|                            ISSUE                            | Summary                                                                                                         |
|:-----------------------------------------------------------:|:----------------------------------------------------------------------------------------------------------------|
| [INLONG-7070](https://github.com/apache/inlong/issues/7070) | [Bug][Manager][DataProxy] Fix pulling MQ configuration problem                                                  |
| [INLONG-7067](https://github.com/apache/inlong/issues/7067) | [Feature][Manager] Provide MQ cluster info in consumption details                                               |
| [INLONG-7049](https://github.com/apache/inlong/issues/7049) | [Feature][Manager] Support complex datatypes in Hudi Sink                                                       |
| [INLONG-7040](https://github.com/apache/inlong/issues/7040) | [Improve][Manager] Make the display name more easy to read                                                      |
| [INLONG-7039](https://github.com/apache/inlong/issues/7039) | [Bug][Manager] Unknown method isNoneBlank                                                                       |
| [INLONG-7028](https://github.com/apache/inlong/issues/7028) | [Bug][Manager] Failed to query the task status                                                                  |
| [INLONG-7026](https://github.com/apache/inlong/issues/7026) | [Bug][Manager] A kafka topic is created without checking whether the topic exists                               |
| [INLONG-7011](https://github.com/apache/inlong/issues/7011) | [Bug][Manager] Check Pulsar non-partitioned table if exist                                                      |
| [INLONG-6992](https://github.com/apache/inlong/issues/6992) | [Bug][Manager] The agent is always in the status of TO_BE_ISSUED_DELETE.                                        |
| [INLONG-6988](https://github.com/apache/inlong/issues/6988) | [Feature][Manager] Use the data node info for StarRocks                                                         |
| [INLONG-6961](https://github.com/apache/inlong/issues/6961) | [Feature][Dashboad][Manager] Support for handling non-InlongMsg formats                                         |
| [INLONG-6938](https://github.com/apache/inlong/issues/6938) | [Feature][Manager] Support create StarRocks database or table                                                   |
| [INLONG-6933](https://github.com/apache/inlong/issues/6933) | [Improve][Manager] Add OpenAPIs for InLong stream-source and stream-transform operations                        |
| [INLONG-6918](https://github.com/apache/inlong/issues/6918) | [Improve][Manager] Command tools support Transform                                                              |
| [INLONG-6909](https://github.com/apache/inlong/issues/6909) | [Improve][Manager] Rename Flink Job to avoid conflicts                                                          |
| [INLONG-6905](https://github.com/apache/inlong/issues/6905) | [Improve][Manager] Add OpenAPIs for InLong cluster and data node operations                                     |
| [INLONG-6876](https://github.com/apache/inlong/issues/6876) | [Improve][Manager] Always enable "scan.incremental.snapshot" for MySQL extract node                             |
| [INLONG-6860](https://github.com/apache/inlong/issues/6860) | [Improve][Manager] Add open APIs related to inlong basic operations                                             |
| [INLONG-6835](https://github.com/apache/inlong/issues/6835) | [Bug][Manager] Error about backup mq resource of DataProxy getAllConfig interface.                              |
| [INLONG-6830](https://github.com/apache/inlong/issues/6830) | [Improve][Manager] Optimize the config managerment of SortSDK                                                   |
| [INLONG-6816](https://github.com/apache/inlong/issues/6816) | [Improve][Sort][Manager] Remove the DLC Iceberg-related files                                                   |
| [INLONG-6815](https://github.com/apache/inlong/issues/6815) | [Bug][Manager] Elasticsearch port parameter is missing                                                          |
| [INLONG-6806](https://github.com/apache/inlong/issues/6806) | [Bug][Manager] Failed to start the sort task because the es hosts parameter is incorrect                        |
| [INLONG-6805](https://github.com/apache/inlong/issues/6805) | [Bug][Manager] Workflow initialization threw NPE exception when esVersion attribute is null.                    |
| [INLONG-6804](https://github.com/apache/inlong/issues/6804) | [Bug][Manager] Workflow initialization threw IllegalArgumentException: Unsupported FieldType text or keyword    |
| [INLONG-6802](https://github.com/apache/inlong/issues/6802) | [Improve][DataProxy][Manager] Adapt Apache Kafka as cache cluster                                               |
| [INLONG-6785](https://github.com/apache/inlong/issues/6785) | [Feature][Manager] Support register and manage the resource of Apache Hudi                                      |
| [INLONG-6773](https://github.com/apache/inlong/issues/6773) | [Bug][Manager] Sink cannot jump from the configuration to successful configuration                              |
| [INLONG-6771](https://github.com/apache/inlong/issues/6771) | [Improve][Manager] Add login failure limit                                                                      |
| [INLONG-6768](https://github.com/apache/inlong/issues/6768) | [Bug][Manager] Unable to find the data node according to the keyword                                            |
| [INLONG-6763](https://github.com/apache/inlong/issues/6763) | [Bug][Manager] Remove unnecessary validation when saving HiveSinkRequest                                        |
| [INLONG-6745](https://github.com/apache/inlong/issues/6745) | [Feature][Manager]Support StarRocks load node management                                                        |
| [INLONG-6737](https://github.com/apache/inlong/issues/6737) | [Bug][Manager] Fix update failure by UpdateByKey methods                                                        |
| [INLONG-6735](https://github.com/apache/inlong/issues/6735) | [Feature][Sort][Manager] PostgreSQL connector supports  transferring all tables for all schemas in one database |
| [INLONG-6734](https://github.com/apache/inlong/issues/6734) | [Bug][Manager] The DDL statement format of the Pulsar source does not match the front-end config                |
| [INLONG-6732](https://github.com/apache/inlong/issues/6732) | [Bug][Manager] Fix wrong SortSDK topic properties                                                               |
| [INLONG-6676](https://github.com/apache/inlong/issues/6676) | [Feature][Manager] Support list all InlongTopicInfo under a given cluster tag                                   |
| [INLONG-6665](https://github.com/apache/inlong/issues/6665) | [Bug][Manager] Unable to save additional information for Elasticsearch field                                    |
| [INLONG-6663](https://github.com/apache/inlong/issues/6663) | [Improve][Manager] Move MQType from inlong-manager to inlong-common                                             |
| [INLONG-6659](https://github.com/apache/inlong/issues/6659) | [Improve][Manager] Using the Elasticsearch data node when creating its resource                                 |
| [INLONG-6640](https://github.com/apache/inlong/issues/6640) | [Feature][Manager][Sort] Function field type supports transform nodes                                           |
| [INLONG-6631](https://github.com/apache/inlong/issues/6631) | [Improve][Manager] Annotate the InlongGroupProcessServiceTest function                                          |
| [INLONG-6621](https://github.com/apache/inlong/issues/6621) | [Bug][Manager] Source tasks are issued repeatedly                                                               |
| [INLONG-6610](https://github.com/apache/inlong/issues/6610) | [Improve][Manager] Support the saving and query of data report-to settings                                      |
| [INLONG-6608](https://github.com/apache/inlong/issues/6608) | [Feature][Manager] Support adding extension params for sink field                                               |
| [INLONG-6570](https://github.com/apache/inlong/issues/6570) | [Bug][Manager] Cannot parse the field type with length when building the Sort config                            |
| [INLONG-6568](https://github.com/apache/inlong/issues/6568) | [Bug][Manager] The status of the deleted source is updated incorrectly                                          |
| [INLONG-6563](https://github.com/apache/inlong/issues/6563) | [Bug][Manager] No error message is displayed when the group fails to be deleted                                 |
| [INLONG-6561](https://github.com/apache/inlong/issues/6561) | [Bug][Manager] Failed to delete InlongGroup                                                                     |
| [INLONG-6544](https://github.com/apache/inlong/issues/6544) | [Bug][Manager] The source status cannot be updated after stream configuration is completed                      |
| [INLONG-6533](https://github.com/apache/inlong/issues/6533) | [Bug][Manager] The manager did not distribute the task according to the clusterName                             |
| [INLONG-6531](https://github.com/apache/inlong/issues/6531) | [Improve][Manager] Add load value in the response of getIpList method                                           |
| [INLONG-6526](https://github.com/apache/inlong/issues/6526) | [Bug][Manager] The manager-client force delete the stream source failed                                         |
| [INLONG-6525](https://github.com/apache/inlong/issues/6525) | [Bug][Manager] Stream and source status cannot be updated                                                       |
| [INLONG-6517](https://github.com/apache/inlong/issues/6517) | [Bug][Manager] Failed to delete stream sink when the inlong group was config_failed                             |
| [INLONG-6516](https://github.com/apache/inlong/issues/6516) | [Bug][Manager] NPE is thrown when startup the Sort task in the InlongStream workflow                            |
| [INLONG-6515](https://github.com/apache/inlong/issues/6515) | [Bug][Manager] NPE is thrown in the cache configuration of DataProxy                                            |
| [INLONG-6505](https://github.com/apache/inlong/issues/6505) | [Feature][Manager] Support Kafka MQ in Inlong Consume                                                           |
| [INLONG-6502](https://github.com/apache/inlong/issues/6502) | [Improve][Manager] Optimize some log and code formats                                                           |
| [INLONG-6497](https://github.com/apache/inlong/issues/6497) | [Feature][Manager] Support Elasticsearch cluster                                                                |
| [INLONG-6496](https://github.com/apache/inlong/issues/6496) | [Feature][Manager] Support Elasticsearch datanode                                                               |
| [INLONG-6491](https://github.com/apache/inlong/issues/6491) | [Feature][Manager] Support getting backup info in getAllConfig                                                  |
| [INLONG-6487](https://github.com/apache/inlong/issues/6487) | [Improve][Manager] Add API to force delete the stream source                                                    |
| [INLONG-6477](https://github.com/apache/inlong/issues/6477) | [Feature][Manager] Supplement the consume API in the manager client                                             |
| [INLONG-6463](https://github.com/apache/inlong/issues/6463) | [Improve][Manager] Support create subscription and topic of multiple pulsar cluster                             |
| [INLONG-6451](https://github.com/apache/inlong/issues/6451) | [Improve][Manager] Rewrite getSourcesMap for Kafka source operator                                              |
| [INLONG-6426](https://github.com/apache/inlong/issues/6426) | [Feature][Manager] SortSourceService support multi stream under one group                                       |
| [INLONG-6044](https://github.com/apache/inlong/issues/6044) | [Improve][Manager] Distinguish between group and stream configuration processes                                 |
| [INLONG-5024](https://github.com/apache/inlong/issues/5024) | [Bug][Manager] The error message is not right when deleting a successful inlong group                           |
| [INLONG-7104](https://github.com/apache/inlong/issues/7104) | [Improve][Manager] Add database change script of InlongManager                                                  |
| [INLONG-5776](https://github.com/apache/inlong/issues/5776) | [Improve][Manager] Add tenant param for Pulsar info                                                             |

### Sort
|                            ISSUE                            | Summary                                                                                                           |
|:-----------------------------------------------------------:|:------------------------------------------------------------------------------------------------------------------|
| [INLONG-7083](https://github.com/apache/inlong/issues/7083) | [Feature][Sort] StarRocks connector supports dirty data archives and metric                                       |
| [INLONG-7076](https://github.com/apache/inlong/issues/7076) | [Feature][Sort] Add multi table sink for MySQL                                                                    |
| [INLONG-7075](https://github.com/apache/inlong/issues/7075) | [Improve][Sort] Add table level metric and dirty data backup for PostgreSQL                                       |
| [INLONG-7066](https://github.com/apache/inlong/issues/7066) | [Improve][Sort] Kafka connector lost topic level dirty data metric and state restore for multi sink               |
| [INLONG-7037](https://github.com/apache/inlong/issues/7037) | [Bug][Sort] Remove exception when topic does not exist                                                            |
| [INLONG-7019](https://github.com/apache/inlong/issues/7019) | [Improve][Sort] Rethrow the system error when flush data to doris                                                 |
| [INLONG-7014](https://github.com/apache/inlong/issues/7014) | [Improve][Sort] Mysql state restore is compatible with old task state                                             |
| [INLONG-7006](https://github.com/apache/inlong/issues/7006) | [Improve][Sort] Optimize reading metric at the table level                                                        |
| [INLONG-6978](https://github.com/apache/inlong/issues/6978) | [Bug][Sort] Get primary keys for PostgreSQL                                                                       |
| [INLONG-6968](https://github.com/apache/inlong/issues/6968) | [Bug][Sort] Encountered SqlParseException                                                                         |
| [INLONG-6962](https://github.com/apache/inlong/issues/6962) | [Improve][Sort] Add read phase metric and table level metric for MySQL-CDC                                        |
| [INLONG-6949](https://github.com/apache/inlong/issues/6949) | [Feature] support all rowkinds for metric computing and restore metrics state for dirty metric for jdbc connector |
| [INLONG-6925](https://github.com/apache/inlong/issues/6925) | [Improve][Sort] Add read phase metric and table level metric for MongoDB-CDC                                      |
| [INLONG-6914](https://github.com/apache/inlong/issues/6914) | [Bug][Sort] Doris connector have a parallelism setting error                                                      |
| [INLONG-6912](https://github.com/apache/inlong/issues/6912) | [Improve][Sort] Add table level metric for Doris                                                                  |
| [INLONG-6908](https://github.com/apache/inlong/issues/6908) | [Feature][Sort] Support all rowkinds for metric computing for es connector                                        |
| [INLONG-6907](https://github.com/apache/inlong/issues/6907) | [Feature][Sort] Support to close StatsRecorder of pulsar client                                                   |
| [INLONG-6899](https://github.com/apache/inlong/issues/6899) | [Feature][Sort] StarRocks-CDC supports table level metrics                                                        |
| [INLONG-6898](https://github.com/apache/inlong/issues/6898) | [Feature][Sort] Unify metrics report model of SortStandalone                                                      |
| [INLONG-6896](https://github.com/apache/inlong/issues/6896) | [Feature][Sort] PostgreSQL-CDC supports metrics by database and table                                             |
| [INLONG-6886](https://github.com/apache/inlong/issues/6886) | [Improve][Sort] Add dirty message for doris sink                                                                  |
| [INLONG-6884](https://github.com/apache/inlong/issues/6884) | [Improve][Sort] Add dirty message for kafka connector                                                             |
| [INLONG-6869](https://github.com/apache/inlong/issues/6869) | [Feature][Sort] Supports dirty data side-output for elasticsearch sink                                            |
| [INLONG-6864](https://github.com/apache/inlong/issues/6864) | [Improve][Sort] Modify Hive metric computing to ensure metric data accuracy                                       |
| [INLONG-6858](https://github.com/apache/inlong/issues/6858) | [Feature][Sort] Support extract node of Apache Hudi                                                               |
| [INLONG-6839](https://github.com/apache/inlong/issues/6839) | [Bug][Sort] Clean up remain event when the related stream offline                                                 |
| [INLONG-6823](https://github.com/apache/inlong/issues/6823) | [Bug][Sort] Parse the epoch date to normal date for Doris sink                                                    |
| [INLONG-6822](https://github.com/apache/inlong/issues/6822) | [Feature][Sort]Add multi table source for MongoDb                                                                 |
| [INLONG-6819](https://github.com/apache/inlong/issues/6819) | [Feature][Sort]Add multi table sink for PostgresSQL                                                               |
| [INLONG-6816](https://github.com/apache/inlong/issues/6816) | [Improve][Sort][Manager] Remove the DLC Iceberg-related files                                                     |
| [INLONG-6813](https://github.com/apache/inlong/issues/6813) | [Bug][Sort] MetricStateUtils threw NPE when write to es and no dirtyRecordsOut counter                            |
| [INLONG-6797](https://github.com/apache/inlong/issues/6797) | [Feature][Sort] Supports dirty data side-output for filesystem sink                                               |
| [INLONG-6795](https://github.com/apache/inlong/issues/6795) | [Feature][Sort] Supports dirty data side-output for hive sink                                                     |
| [INLONG-6792](https://github.com/apache/inlong/issues/6792) | [Feature][Sort] Supports dirty data side-output for hbase sink                                                    |
| [INLONG-6784](https://github.com/apache/inlong/issues/6784) | [Feature][Sort] Support load node of Apache Hudi                                                                  |
| [INLONG-6782](https://github.com/apache/inlong/issues/6782) | [Feature][Sort] Support Apache Hudi connector                                                                     |
| [INLONG-6780](https://github.com/apache/inlong/issues/6780) | [Feature][Sort] Supports dirty data side-output for JDBC connector and related sinks                              |
| [INLONG-6770](https://github.com/apache/inlong/issues/6770) | [Improve][Sort] Optimize EsSink params                                                                            |
| [INLONG-6765](https://github.com/apache/inlong/issues/6765) | [Feature][Sort] Supports dirty data side-output for Iceberg sink                                                  |
| [INLONG-6754](https://github.com/apache/inlong/issues/6754) | [Feature][Sort] Bump verison of pulsar-connector to 1.13.6.2                                                      |
| [INLONG-6751](https://github.com/apache/inlong/issues/6751) | [Improve][Sort] Add read phase metric and table level metric for Oracle                                           |
| [INLONG-6749](https://github.com/apache/inlong/issues/6749) | [Improve][Sort] Add dirty metric for Kafka                                                                        |
| [INLONG-6747](https://github.com/apache/inlong/issues/6747) | [Feature][Sort]StarRocks connector supports transferring all tables for all schemas in one database               |
| [INLONG-6735](https://github.com/apache/inlong/issues/6735) | [Feature][Sort][Manager] PostgreSQL connector supports  transferring all tables for all schemas in one database   |
| [INLONG-6724](https://github.com/apache/inlong/issues/6724) | [Feature][Sort] Supports dirty data side-output for doris sink                                                    |
| [INLONG-6707](https://github.com/apache/inlong/issues/6707) | [Improve][Sort] Delay the release of audit metrics                                                                |
| [INLONG-6706](https://github.com/apache/inlong/issues/6706) | [Bug][Sort] The client id of Kafka sink was wrong                                                                 |
| [INLONG-6675](https://github.com/apache/inlong/issues/6675) | [Bug][Sort] Not report metrics because Flink SQL missing the metrics.audit.proxy.hosts info                       |
| [INLONG-6657](https://github.com/apache/inlong/issues/6657) | [Improve][Sort] Add dirty data metric for Hive                                                                    |
| [INLONG-6640](https://github.com/apache/inlong/issues/6640) | [Feature][Manager][Sort] Function field type supports transform nodes                                             |
| [INLONG-6639](https://github.com/apache/inlong/issues/6639) | [Bug][Sort] DynamicPulsarDeserializationSchema threw NPE when sourceMetricData is not initialized                 |
| [INLONG-6636](https://github.com/apache/inlong/issues/6636) | [Improve][Sort] Keep metric computing consistent for source MySQL and sink HBase                                  |
| [INLONG-6626](https://github.com/apache/inlong/issues/6626) | [Improve][Sort] Use SortClientV2 by default                                                                       |
| [INLONG-6620](https://github.com/apache/inlong/issues/6620) | [Feature][Sort] Support metadata for Kafka source                                                                 |
| [INLONG-6619](https://github.com/apache/inlong/issues/6619) | [Feature][Sort] Support dirty sink for connectors                                                                 |
| [INLONG-6617](https://github.com/apache/inlong/issues/6617) | [Feature][Sort] Add common process for dirty data sink and supports log sink                                      |
| [INLONG-6615](https://github.com/apache/inlong/issues/6615) | [Bug][Sort] Upsert Kafka will throw NPE when getting deleted record                                               |
| [INLONG-6600](https://github.com/apache/inlong/issues/6600) | [Bug][SortStandalone] Dead lock when one stream is removed from or migrated to one sink                           |
| [INLONG-6598](https://github.com/apache/inlong/issues/6598) | [Improve][Sort] Import small file compact for Apache Iceberg                                                      |
| [INLONG-6594](https://github.com/apache/inlong/issues/6594) | [Bug][Sort] Clickhouse connector throw exception when source is change log stream                                 |
| [INLONG-6586](https://github.com/apache/inlong/issues/6586) | [Bug][SortStandalone] Wrong node duration time of the Kafka sink                                                  |
| [INLONG-6584](https://github.com/apache/inlong/issues/6584) | [Improve][Sort] Add read phase metric and table level metric  for MySQL                                           |
| [INLONG-6578](https://github.com/apache/inlong/issues/6578) | [Bug][Sort] Failed to write Bitmap data to the Apache Doris                                                       |
| [INLONG-6575](https://github.com/apache/inlong/issues/6575) | [Improve][Sort] Add dirty data metric for Filesystem                                                              |
| [INLONG-6574](https://github.com/apache/inlong/issues/6574) | [Improve][Sort] Add dirty data metric for HBase                                                                   |
| [INLONG-6552](https://github.com/apache/inlong/issues/6552) | [Bug][Sort] MicroTimestamp data error in Oracle connector                                                         |
| [INLONG-6548](https://github.com/apache/inlong/issues/6548) | [Improve][Sort] Optimize metadata field naming for format of canal-json                                           |
| [INLONG-6538](https://github.com/apache/inlong/issues/6538) | [Bug][Sort] Wrong bool type convert in multiple sink scenes                                                       |
| [INLONG-6537](https://github.com/apache/inlong/issues/6537) | [Bug][Sort] The decimal type loss of precision during DDL synchronization                                         |
| [INLONG-6529](https://github.com/apache/inlong/issues/6529) | [Bug][Sort] Fix microtime time zone error in MySql connector                                                      |
| [INLONG-6507](https://github.com/apache/inlong/issues/6507) | [Bug][Sort] Should convert date type to timestamp type in Oracle connector                                        |
| [INLONG-6495](https://github.com/apache/inlong/issues/6495) | [Improve][Sort] Support metrics and restore metrics for single-table of Doris                                     |
| [INLONG-6484](https://github.com/apache/inlong/issues/6484) | [Bug][Sort] Fix schema update circular dependency error in multiple sink iceberg scenes                           |
| [INLONG-6471](https://github.com/apache/inlong/issues/6471) | [Bug][Sort] MySQL connector metric restore lost init data for sourceFunction                                      |
| [INLONG-6428](https://github.com/apache/inlong/issues/6428) | [Feature] [Sort] Support elasticsearch-5.x                                                                        |
| [INLONG-5628](https://github.com/apache/inlong/issues/5628) | [Feature][SortStandalone] Add buffer limit for dispatch queue of the sink                                         |
| [INLONG-7090](https://github.com/apache/inlong/issues/7090) | [Improve][Sort] Upgrade the version of Iceberg to 1.1.0                                                           |
| [INLONG-7061](https://github.com/apache/inlong/issues/7061) | [Feature][Sort] Support table level metrics for Apache Doris connector and add dirty metrics                      |
| [INLONG-6788](https://github.com/apache/inlong/issues/6788) | [Feature][Sort] Support the metric of Apache Hudi                                                                 |
| [INLONG-6545](https://github.com/apache/inlong/issues/6545) | [Improve][Sort] Accurately parse the schema type and completely match the missing precision information           |

### Audit
|                            ISSUE                            | Summary                                                                       |
|:-----------------------------------------------------------:|:------------------------------------------------------------------------------|
| [INLONG-6852](https://github.com/apache/inlong/issues/6852) | [Improve][Audit] agent and dataproxy report audit data when the program exits |

### Dashboard
|                            ISSUE                            | Summary                                                                                                                             |
|:-----------------------------------------------------------:|:------------------------------------------------------------------------------------------------------------------------------------|
| [INLONG-7092](https://github.com/apache/inlong/issues/7092) | [Feature][Dashboard] Source and Sink as sub-concepts of Stream                                                                      |
| [INLONG-7064](https://github.com/apache/inlong/issues/7064) | [Improve][Dashboard] Update the FilePathHelp value                                                                                  |
| [INLONG-7063](https://github.com/apache/inlong/issues/7063) | [Improve][Dashboard] Optimize the topic name of the new consumption page                                                            |
| [INLONG-7047](https://github.com/apache/inlong/issues/7047) | [Improve][Dashboard] Specific meanings of data subscription list fields                                                             |
| [INLONG-7045](https://github.com/apache/inlong/issues/7045) | [Improve][Dashboard] Change the verification of InlongGroupId                                                                       |
| [INLONG-7043](https://github.com/apache/inlong/issues/7043) | [Improve][Dashboard] Add field prompt for ES sink                                                                                   |
| [INLONG-7032](https://github.com/apache/inlong/issues/7032) | [Improve][Dashboard] Group creation page split basic information and MQ information                                                 |
| [INLONG-7024](https://github.com/apache/inlong/issues/7024) | [Improve][Dashboard] Type readable lowercase and title optimization in data integration                                             |
| [INLONG-7016](https://github.com/apache/inlong/issues/7016) | [Improve][Dashboard] Approve page supports multiple columns                                                                         |
| [INLONG-7005](https://github.com/apache/inlong/issues/7005) | [Feature][Dashboard] Support StarRocks node management                                                                              |
| [INLONG-7000](https://github.com/apache/inlong/issues/7000) | [Improve][Dashboard] Add AVRO format for InlongStream                                                                               |
| [INLONG-6998](https://github.com/apache/inlong/issues/6998) | [Feature][Dashboard][Manager] Support ignore deserialization error data                                                             |
| [INLONG-7009](https://github.com/apache/inlong/issues/7009) | [Improve][Dashboard] The data node type supports lowercase                                                                          |
| [INLONG-6996](https://github.com/apache/inlong/issues/6996) | [Improve][Dashboard] Add JSON format for InlongStream                                                                               |
| [INLONG-6994](https://github.com/apache/inlong/issues/6994) | [Improve][Dashboard] Add MQ type filter box for inlong group and consume list                                                       |
| [INLONG-6987](https://github.com/apache/inlong/issues/6987) | [Improve][Dashboard] Writing standard when splicing Chinese and English words                                                       |
| [INLONG-6983](https://github.com/apache/inlong/issues/6983) | [Improve][Dashboard] Group page supports multiple columns                                                                           |
| [INLONG-6982](https://github.com/apache/inlong/issues/6982) | [Improve][Dashboard] Data source and data storage bullet box title optimization                                                     |
| [INLONG-6980](https://github.com/apache/inlong/issues/6980) | [Improve][Dashboard] Optimize the detail page of InlongGroup and InlongStream                                                       |
| [INLONG-6975](https://github.com/apache/inlong/issues/6975) | [Bug][Dashboard] The cluster name of the File source does not support modification                                                  |
| [INLONG-6972](https://github.com/apache/inlong/issues/6972) | [Improve][Dashboard] Change all "consumption" to "subscription" for data consumption                                                |
| [INLONG-6961](https://github.com/apache/inlong/issues/6961) | [Feature][Dashboard][Manager] Support for handling non-InlongMsg formats                                                            |
| [INLONG-6940](https://github.com/apache/inlong/issues/6940) | [Improve][Dashboard]Type name correction                                                                                            |
| [INLONG-6948](https://github.com/apache/inlong/issues/6948) | [Improve][Dashboard] Reset and modify the password input box                                                                        |
| [INLONG-6935](https://github.com/apache/inlong/issues/6935) | [Improve][Dashboard] Add Key-Value format for InlongStream                                                                          |
| [INLONG-6930](https://github.com/apache/inlong/issues/6930) | [Improve][Dashboard] Create a new cluster token placeholder configuration i18n                                                      |
| [INLONG-6926](https://github.com/apache/inlong/issues/6926) | [Improve][Dashboard] Change the cluster type and tag to lower case                                                                  |
| [INLONG-6923](https://github.com/apache/inlong/issues/6923) | [Improve][Dashboard] Application and Approval Interchange for Approval Management                                                   |
| [INLONG-6916](https://github.com/apache/inlong/issues/6916) | [Improve][Dashboard] MQ type adds partition limit                                                                                   |
| [INLONG-6904](https://github.com/apache/inlong/issues/6904) | [Improve][Dashboard] File source type supports selecting agent cluster                                                              |
| [INLONG-6900](https://github.com/apache/inlong/issues/6900) | [Improve][Dashboard] Support pulling up the field list of Source in Sink                                                            |
| [INLONG-6874](https://github.com/apache/inlong/issues/6874) | [Improve][Dashboard] Stream removes the Raw-CSV data format                                                                         |
| [INLONG-6865](https://github.com/apache/inlong/issues/6865) | [Improve][Dashboard] Support npm ci command for stable package-lock                                                                 |
| [INLONG-6843](https://github.com/apache/inlong/issues/6843) | [Improve][Dashboard] ES datanode parameter improvement                                                                              |
| [INLONG-6837](https://github.com/apache/inlong/issues/6837) | [Improve][Dashboard] ES sink support version), documentType and primaryKey options                                                  |
| [INLONG-6826](https://github.com/apache/inlong/issues/6826) | [Feature][Dashboard] Support loading different plugins using different loaders                                                      |
| [INLONG-6824](https://github.com/apache/inlong/issues/6824) | [Feature][Dashboard] Support StarRocks sink                                                                                         |
| [INLONG-6800](https://github.com/apache/inlong/issues/6800) | [Improve][Dashboard] MySQL sink supports selecting MySQL nodes                                                                      |
| [INLONG-6786](https://github.com/apache/inlong/issues/6786) | [Feature][Dashboard] Supoort Apache Hudi sink management                                                                            |
| [INLONG-6775](https://github.com/apache/inlong/issues/6775) | [Bug][Dashboard] There is a risk of source code exposure in the production environment                                              |
| [INLONG-6772](https://github.com/apache/inlong/issues/6772) | [Feature][Dashboard] Support ClickHouse node management                                                                             |
| [INLONG-6761](https://github.com/apache/inlong/issues/6761) | [Feature][Dashboard] Support Iceberg node management                                                                                |
| [INLONG-6758](https://github.com/apache/inlong/issues/6758) | [Improve][Dashboard] Unify the log function components of Group and Stream                                                          |
| [INLONG-6744](https://github.com/apache/inlong/issues/6744) | [Improve][Dashboard] Hive sink supports selecting hive nodes                                                                        |
| [INLONG-6699](https://github.com/apache/inlong/issues/6699) | [Feature][Dashboard] Support MQTT source                                                                                            |
| [INLONG-6694](https://github.com/apache/inlong/issues/6694) | [Improve][Dashboard] Create with filter value first, if there is a filter plugin                                                    |
| [INLONG-6680](https://github.com/apache/inlong/issues/6680) | [Bug][Dashboard] Set the plugin to default when there is no target plugin                                                           |
| [INLONG-6678](https://github.com/apache/inlong/issues/6678) | [Improve][Dashboard] Add sinkType parameter in sinkFieldList                                                                        |
| [INLONG-6661](https://github.com/apache/inlong/issues/6661) | [Improve][Dashboard] Support Elasticsearch DataNode                                                                                 |
| [INLONG-6651](https://github.com/apache/inlong/issues/6651) | [Improve][Dashboard] Add settings for data report type                                                                              |
| [INLONG-6581](https://github.com/apache/inlong/issues/6581) | [Bug][Dashboard] The file format prompt of the FILE source is wrong                                                                 |
| [INLONG-6572](https://github.com/apache/inlong/issues/6572) | [Improve][Dashboard] Stream is not deleted by default when the sink is deleted                                                      |
| [INLONG-6565](https://github.com/apache/inlong/issues/6565) | [Bug][Dashboard] The operation of refreshing the sink configuration needs to be executed when the group configuration is successful |
| [INLONG-6542](https://github.com/apache/inlong/issues/6542) | [Improve][Dashboard] Optimize stream execution workflow and execution log                                                           |
| [INLONG-6511](https://github.com/apache/inlong/issues/6511) | [Bug][Dashboard] The Select/AutoComplete component in EditableTable will mistakenly change the value and cause re-rendering         |
| [INLONG-6504](https://github.com/apache/inlong/issues/6504) | [Feature][Dashboard] Support stream to view execution log and execute workflow                                                      |
| [INLONG-6498](https://github.com/apache/inlong/issues/6498) | [Improve][Dashboard] Field resolution for unified inlong group approval                                                             |
| [INLONG-6493](https://github.com/apache/inlong/issues/6493) | [Bug][Dashboard] After the Stream is configured successfully), some parameters cannot be modified                                   |
| [INLONG-6482](https://github.com/apache/inlong/issues/6482) | [Improve][Dashboard] Sink management distinguishes between save-only and save-and-submit processes                                  |
| [INLONG-6481](https://github.com/apache/inlong/issues/6481) | [Feature][Dashboard] Supports management of SQLServer source                                                                        |
| [INLONG-6456](https://github.com/apache/inlong/issues/6456) | [Feature][Dashboard] Supports management of Oracle sources                                                                          |
| [INLONG-6418](https://github.com/apache/inlong/issues/6418) | [Feature][Dashboard] Support management of MongoDB source                                                                           |

### Other
|                            ISSUE                            | Summary                                                                                                                             |
|:-----------------------------------------------------------:|:------------------------------------------------------------------------------------------------------------------------------------|
| [INLONG-7098](https://github.com/apache/inlong/issues/7098) | [Improve][Common] Add UUID in MonitorIndex                                                                                          |
| [INLONG-7093](https://github.com/apache/inlong/issues/7093) | [Improve][SDK] Add report time metric                                                                                               |
| [INLONG-7035](https://github.com/apache/inlong/issues/7035) | [Bug][SDK] The message id in DataProxy is not globally unique                                                                       |
| [INLONG-6955](https://github.com/apache/inlong/issues/6955) | [Improve][Script] Support starts the DataProxy with Kafka in the deployment script                                                  |
| [INLONG-6946](https://github.com/apache/inlong/issues/6946) | [Feature][SDK] Support to consume subset of message queue cluster or topic                                                          |
| [INLONG-6945](https://github.com/apache/inlong/issues/6945) | [Feature][SDK] Unify metrics report model of SortSDK                                                                                |
| [INLONG-6944](https://github.com/apache/inlong/issues/6944) | [Feature][SDK] Remove useless code of SortSDK                                                                                       |
| [INLONG-6893](https://github.com/apache/inlong/issues/6893) | [Bug][CI] Build and Push docker image skipped                                                                                       |
| [INLONG-6888](https://github.com/apache/inlong/issues/6888) | [Bug][CI] All Docker workflow failed                                                                                                |
| [INLONG-6882](https://github.com/apache/inlong/issues/6882) | [Improve][Docker] Update the docker-compose version requirement in the readme                                                       |
| [INLONG-6879](https://github.com/apache/inlong/issues/6879) | [Improve][CI] Use ubuntu-22.04 to replace ubuntu-latest for workflows                                                               |
| [INLONG-6867](https://github.com/apache/inlong/issues/6867) | [Improve][CVE] Protobuf Java vulnerable to Uncontrolled Resource Consumption                                                        |
| [INLONG-6862](https://github.com/apache/inlong/issues/6862) | [Improve][License] Use spotless to apply the asf header                                                                             |
| [INLONG-6841](https://github.com/apache/inlong/issues/6841) | [Improve][Docker] Integrate InLong template to Grafana                                                                              |
| [INLONG-6838](https://github.com/apache/inlong/issues/6838) | [Feature] Support multi addresses for ES load node                                                                                  |
| [INLONG-6810](https://github.com/apache/inlong/issues/6810) | [Improve][Pom] Versions for protobuf-maven-plugin and os-maven-plugin are not consistent                                            |
| [INLONG-6787](https://github.com/apache/inlong/issues/6787) | [Feature][Doc] Add the usage document for Apache Hudi                                                                               |
| [INLONG-6777](https://github.com/apache/inlong/issues/6777) | [Improve][CVE] Password exposure in H2 Database                                                                                     |
| [INLONG-6685](https://github.com/apache/inlong/issues/6685) | [Bug][Script] The bin/init-config.sh compatible with MacOS                                                                          |
| [INLONG-6673](https://github.com/apache/inlong/issues/6673) | [Improve][CVE] Apache Shiro Authentication Bypass vulnerability                                                                     |
| [INLONG-6624](https://github.com/apache/inlong/issues/6624) | [Improve][Doc] Add Stargazers and Contributor chart for readme                                                                      |
| [INLONG-6555](https://github.com/apache/inlong/issues/6555) | [Improve][CI] The `set-output` command is deprecated and will be disabled soon                                                      |
| [INLONG-6550](https://github.com/apache/inlong/issues/6550) | [Bug][SDK] Decode heartbeat response error                                                                                          |
| [INLONG-6522](https://github.com/apache/inlong/issues/6522) | [Improve][Docker] Add notes for building ARM images                                                                                 |
| [INLONG-6475](https://github.com/apache/inlong/issues/6475) | [Feature][Docker] Add a base flink environment for docker-compose                                                                   |
| [INLONG-6459](https://github.com/apache/inlong/issues/6459) | [Improve][CI] Match the branch-version as release branch                                                                            |
| [INLONG-6429](https://github.com/apache/inlong/issues/6429) | [Umbrella] Release InLong 1.4.0                                                                                                     |
| [INLONG-6417](https://github.com/apache/inlong/issues/6417) | [Improve][SDK] Support proxy-send mode                                                                                              |
| [INLONG-5669](https://github.com/apache/inlong/issues/5669) | [Improve][SDK] Extract SortClientConfig parameters key to constants                                                                 |
| [INLONG-5231](https://github.com/apache/inlong/issues/5231) | [Improve][CodeStyle] Add spotless to CI                                                                                             |
| [INLONG-4957](https://github.com/apache/inlong/issues/4957) | [Umbrella] Add Grafana Metrics Dashboard                                                                                            |
