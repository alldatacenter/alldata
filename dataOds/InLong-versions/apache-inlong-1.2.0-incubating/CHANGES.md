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

## Release InLong 1.2.0-incubating - Released (as of 2022-06-08)

### Agent
| ISSUE                                                                 | Summary                                                                                                |
|:----------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------|
| [INLONG-4483](https://github.com/apache/incubator-inlong/issues/4483) | [Bug][Agent] Many ConnectException logs in unit test of Kafka source                                   |
| [INLONG-4193](https://github.com/apache/incubator-inlong/issues/4193) | [Improve][Agent] Add Java docs                                                                         |
| [INLONG-4112](https://github.com/apache/incubator-inlong/issues/4112) | [Feature][Agent] Support collect data from a specified position for MySQL binlog                       |
| [INLONG-2563](https://github.com/apache/incubator-inlong/issues/2563) | [Feature][Agent] Move public domain from agent to agent-common                                         |
| [INLONG-4397](https://github.com/apache/incubator-inlong/issues/4397) | [Feature][Agent] Supports collect of full data for file type                                           |
| [INLONG-4359](https://github.com/apache/incubator-inlong/issues/4359) | [Improve][Agent] Simplify agent process commands                                                       |
| [INLONG-4292](https://github.com/apache/incubator-inlong/issues/4292) | [Improve][Agent][TubeMQ][Sort] Upgrade the property file for all modules from log4j to log4j2          |
| [INLONG-4283](https://github.com/apache/incubator-inlong/issues/4283) | [Bug][Agent] The kafka-clients with 3.0.0 does not package to the jar                                  |
| [INLONG-4235](https://github.com/apache/incubator-inlong/issues/4235) | [Bug][Agent] Config the log info for unit tests of the agent plugin                                    |

### Audit
| ISSUE                                                                 | Summary                                                                                                |
|:----------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------|
| [INLONG-4594](https://github.com/apache/incubator-inlong/issues/4594) | [Improve][Audit] Make Elasticsearch authentication configurable                                        |
| [INLONG-4520](https://github.com/apache/incubator-inlong/issues/4520) | [Improve][Audit] Audit-proxy supports Pulsar authenticate                                              |
| [INLONG-4477](https://github.com/apache/incubator-inlong/issues/4477) | [Improve][Audit] Audit-store supports Pulsar authenticate                                              |
| [INLONG-3895](https://github.com/apache/incubator-inlong/issues/3895) | [Bug][Audit] Proxy store startup script log path error                                                 |
| [INLONG-3853](https://github.com/apache/incubator-inlong/issues/3853) | [Bug][Audit] audit can not start for script bug                                                        |

### Dashboard
| ISSUE                                                                 | Summary                                                                                                |
|:----------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------|
| [INLONG-4612](https://github.com/apache/incubator-inlong/issues/4612) | [Improve][Dashboard][Manager] Iceberg sink configuration protocol key update                           |
| [INLONG-4610](https://github.com/apache/incubator-inlong/issues/4610) | [Improve][Dashboard] Save sink fields failed                                                           |
| [INLONG-4588](https://github.com/apache/incubator-inlong/issues/4588) | [Improve][Dashboard] Approval management splits different routes                                       |
| [INLONG-4580](https://github.com/apache/incubator-inlong/issues/4580) | [Bug][Dashboard] File source IP and Hive conf dir are mandatory                                        |
| [INLONG-4577](https://github.com/apache/incubator-inlong/issues/4577) | [Feature][Dashboard] Support TubeMQ cluster management                                                 |
| [INLONG-4550](https://github.com/apache/incubator-inlong/issues/4550) | [Feature][Dashboard] Support DataProxy Cluster and Node management                                     |
| [INLONG-4544](https://github.com/apache/incubator-inlong/issues/4544) | [Improve][Dashboard] Pulsar Cluster support tenant param                                               |
| [INLONG-4523](https://github.com/apache/incubator-inlong/issues/4523) | [Feature][Dashboard] Support Clusters Module                                                           |
| [INLONG-4500](https://github.com/apache/incubator-inlong/issues/4500) | [Improve][Manager][Dashboard] Remove non-null restriction on hiveConfDir parameter                     |
| [INLONG-4436](https://github.com/apache/incubator-inlong/issues/4436) | [Bug][Dashboard] Hive Sink confDir parameter tooltip error                                             |
| [INLONG-4488](https://github.com/apache/incubator-inlong/issues/4488) | [Improve][Dashboard] Remove useless modules                                                            |
| [INLONG-4423](https://github.com/apache/incubator-inlong/issues/4423) | [Improve][Dashboard] Hive Sink adds hiveConfDir parameter                                              |
| [INLONG-4319](https://github.com/apache/incubator-inlong/issues/4319) | [Improve][Dashboard] Approve page support cluster info                                                 |
| [INLONG-4284](https://github.com/apache/incubator-inlong/issues/4284) | [Improve][Dashboard] Modify ClickHouse sink parameters                                                 |
| [INLONG-4274](https://github.com/apache/incubator-inlong/issues/4274) | [Improve][Dashboard] Update the parameters to adapt the manager module                                 |
| [INLONG-4218](https://github.com/apache/incubator-inlong/issues/4218) | [Feature][Dashboard] Unified group page                                                                |
| [INLONG-4119](https://github.com/apache/incubator-inlong/issues/4119) | [Improve][Dashboard] Change the keyWord to keyword in query request params                             |
| [INLONG-4102](https://github.com/apache/incubator-inlong/issues/4102) | [Improve][Dashboard] Add tooltip for dataPath of the Hive sink                                         |
| [INLONG-4089](https://github.com/apache/incubator-inlong/issues/4089) | [Feature][Dashboard] Create ClickHouse sink need to fill more params                                   |
| [INLONG-4031](https://github.com/apache/incubator-inlong/issues/4031) | [Bug][Dashboard] Kafka sink storage topic partitionNum key error                                       |
| [INLONG-4028](https://github.com/apache/incubator-inlong/issues/4028) | [Bug][Dashboard] ClickHouse types do not match what ClickHouse using                                   |
| [INLONG-3938](https://github.com/apache/incubator-inlong/issues/3938) | [Bug][Dashboard] CI workflow build failed incidentally                                                 |
| [INLONG-3851](https://github.com/apache/incubator-inlong/issues/3851) | [Improve][Dashboard] Unify the naming of data streams                                                  |

### DataProxy
| ISSUE                                                                 | Summary                                                                                                |
|:----------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------|
| [INLONG-4521](https://github.com/apache/incubator-inlong/issues/4521) | [Improve][DataProxy][Manager] Change the naming of third-party cluster related classes                 |
| [INLONG-4056](https://github.com/apache/incubator-inlong/issues/4056) | [Feature][DataProxy] Return the response to the SDK/Agent after saving the event to the cache cluster  |

### Manager
| ISSUE                                                                 | Summary                                                                                                |
|:----------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------|
| [INLONG-4626](https://github.com/apache/incubator-inlong/issues/4626) | [Improve][Manager] Remove redundant connector sub directory                                            |
| [INLONG-4622](https://github.com/apache/incubator-inlong/issues/4622) | [Bug][Manager] Append db name in JDBC URL for load node                                                |
| [INLONG-4615](https://github.com/apache/incubator-inlong/issues/4615) | [Improve][Manager] PluginClassLoader adapts to the Windows system                                      |
| [INLONG-4612](https://github.com/apache/incubator-inlong/issues/4612) | [Improve][Dashboard][Manager] Iceberg sink configuration protocol key update                           |
| [INLONG-4607](https://github.com/apache/incubator-inlong/issues/4607) | [Bug][Manager] Add ClickHouse field types for FormatInfo                                               |
| [INLONG-4601](https://github.com/apache/incubator-inlong/issues/4601) | [Bug][Manager] The fieldList in SinkRequest should be changed to sinkFieldList                         |
| [INLONG-4598](https://github.com/apache/incubator-inlong/issues/4598) | [Bug][Manager] Pulsar topic incorrect                                                                  |
| [INLONG-4586](https://github.com/apache/incubator-inlong/issues/4586) | [Improve][Manager] Fix redefined streamFields in stream source                                         |
| [INLONG-4583](https://github.com/apache/incubator-inlong/issues/4583) | [Bug][Manager] Query InlongGroup info error from the browser                                           |
| [INLONG-4563](https://github.com/apache/incubator-inlong/issues/4563) | [Feature][Manager] Support create TubeMQ resources by its origin APIs                                  |
| [INLONG-4556](https://github.com/apache/incubator-inlong/issues/4556) | [Improve][Manager] Optimize ProcessorExecutor Logic                                                    |
| [INLONG-4552](https://github.com/apache/incubator-inlong/issues/4552) | [Improve][Manager] Add composite index for table in DDL                                                |
| [INLONG-4543](https://github.com/apache/incubator-inlong/issues/4543) | [Improve][Manager] Query MQ clusters from inlong cluster table                                         |
| [INLONG-4541](https://github.com/apache/incubator-inlong/issues/4541) | [Feature][Manager] Support save extension params for inlong cluster                                    |
| [INLONG-4534](https://github.com/apache/incubator-inlong/issues/4534) | [Improve][Manager] Add complete unit test to create data flow                                          |
| [INLONG-4532](https://github.com/apache/incubator-inlong/issues/4532) | [Improve][Manager] Init sort config error during initing group info in the client                      |
| [INLONG-4529](https://github.com/apache/incubator-inlong/issues/4529) | [Improve][Manager] Fix PluginClassLoader unit test error                                               |
| [INLONG-4528](https://github.com/apache/incubator-inlong/issues/4528) | [Feature][Manager] Support Greenplum sink                                                              |
| [INLONG-4521](https://github.com/apache/incubator-inlong/issues/4521) | [Improve][DataProxy][Manager] Change the naming of third-party cluster related classes                 |
| [INLONG-4519](https://github.com/apache/incubator-inlong/issues/4519) | [Feature][Manager] Support Elasticsearch sink                                                          |
| [INLONG-4516](https://github.com/apache/incubator-inlong/issues/4516) | [Improve][Manager] Get the MQ cluster for data proxy from the inlong cluster table                     |
| [INLONG-4513](https://github.com/apache/incubator-inlong/issues/4513) | [Improve][Manager] Add Iceberg and HBase examples for the manager client                               |
| [INLONG-4512](https://github.com/apache/incubator-inlong/issues/4512) | [Improve][Manager] Remove third_party_cluster and data_proxy_cluster tables                            |
| [INLONG-4510](https://github.com/apache/incubator-inlong/issues/4510) | [Improve][Manager] Remove GsonUtils, InlongParser, and Gson dependency                                 |
| [INLONG-4508](https://github.com/apache/incubator-inlong/issues/4508) | [Improve][Manager] Remove the management of the DB and file servers' config                            |
| [INLONG-4505](https://github.com/apache/incubator-inlong/issues/4505) | [Improve][Manager] Improve the return information of validation rules                                  |
| [INLONG-4503](https://github.com/apache/incubator-inlong/issues/4503) | [Bug][Manager] Client executes request error                                                           |
| [INLONG-4500](https://github.com/apache/incubator-inlong/issues/4500) | [Improve][Manager][Dashboard] Remove non-null restriction on hiveConfDir parameter                     |
| [INLONG-4497](https://github.com/apache/incubator-inlong/issues/4497) | [Improve][Manager] Fix deleteGroup Async method in Client                                              |
| [INLONG-4487](https://github.com/apache/incubator-inlong/issues/4487) | [Improve][Manager] Fix json parse exception for streamSource/streamSink                                |
| [INLONG-4481](https://github.com/apache/incubator-inlong/issues/4481) | [Improve][Manager] Restore iceberg fields that may still have dependents                               |
| [INLONG-4473](https://github.com/apache/incubator-inlong/issues/4473) | [Bug][Manager] DuplicateKeyException when save StreamSinkField                                         |
| [INLONG-4471](https://github.com/apache/incubator-inlong/issues/4471) | [Improve][Manager] Fix json parse exception in Client                                                  |
| [INLONG-4468](https://github.com/apache/incubator-inlong/issues/4468) | [Improve][Manager] Use config url to create pulsar admin                                               |
| [INLONG-4466](https://github.com/apache/incubator-inlong/issues/4466) | [Bug][Manager] The Oracle data source could not be parsed                                              |
| [INLONG-4464](https://github.com/apache/incubator-inlong/issues/4464) | [Improve][Manager] Fix problems emerged from full link path test                                       |
| [INLONG-4462](https://github.com/apache/incubator-inlong/issues/4462) | [Improve][Manager] Fix NPE when parsing pageInfo in manager client                                     |
| [INLONG-4476](https://github.com/apache/incubator-inlong/issues/4476) | [Improve][Sort][Manager] Remove zk and related classes                                                 |
| [INLONG-4458](https://github.com/apache/incubator-inlong/issues/4458) | [Improve][Sort][Manager] Unify the meta field naming                                                   |
| [INLONG-4445](https://github.com/apache/incubator-inlong/issues/4445) | [Improve][Manager] Change the relationship to relation to adapt the Sort protocol                      |
| [INLONG-4443](https://github.com/apache/incubator-inlong/issues/4443) | [Improve][Manager] Remove some deprecated classes                                                      |
| [INLONG-4439](https://github.com/apache/incubator-inlong/issues/4439) | [Improve][Manager] Refactor gson adapter for managerctl                                                |
| [INLONG-4438](https://github.com/apache/incubator-inlong/issues/4438) | [Bug][Manager] Caught BusinessException when deploy InLong cluster firstly                             |
| [INLONG-4428](https://github.com/apache/incubator-inlong/issues/4428) | [Improve][Sort][Manager] Optimize the name for Data Node related modules and classes                   |
| [INLONG-4427](https://github.com/apache/incubator-inlong/issues/4427) | [Improve][Manager] The manager-client module reuses the source classes in manager-common               |
| [INLONG-4421](https://github.com/apache/incubator-inlong/issues/4421) | [Improve][Manager] Remove unused classes and table structures                                          |
| [INLONG-4419](https://github.com/apache/incubator-inlong/issues/4419) | [Improve][Manager] The manager-client module reuses the sink classes in manager-common                 |
| [INLONG-4417](https://github.com/apache/incubator-inlong/issues/4417) | [Improve][Manager] Manager print error logs when deploy by docker-compose                              |
| [INLONG-4414](https://github.com/apache/incubator-inlong/issues/4414) | [Feature][Manager]  Support MySQL data sink                                                            |
| [INLONG-4408](https://github.com/apache/incubator-inlong/issues/4408) | [Feature][Manager][Sort] Add iceberg sink load node                                                    |
| [INLONG-4406](https://github.com/apache/incubator-inlong/issues/4406) | [Improve][Manager] Fix InlongGroupInfo parse exception                                                 |
| [INLONG-4398](https://github.com/apache/incubator-inlong/issues/4398) | [Bug][Manager] Status display incomplete for managerctl                                                |
| [INLONG-4388](https://github.com/apache/incubator-inlong/issues/4388) | [Bug][Manager] Elasticsearch create command scaling_factor use wrong mapping info                      |
| [INLONG-4384](https://github.com/apache/incubator-inlong/issues/4384) | [Improve][Manager] Store the specific field params of the Iceberg to extParams                         |
| [INLONG-4381](https://github.com/apache/incubator-inlong/issues/4381) | [Improve][Manager] Optimize client http request                                                        |
| [INLONG-4377](https://github.com/apache/incubator-inlong/issues/4377) | [Feature][Manager] Add hbase and elasticsearch sink type support in manager client sdk                 |
| [INLONG-4376](https://github.com/apache/incubator-inlong/issues/4376) | [Feature][Manager] Support SqlServer sink                                                              |
| [INLONG-4369](https://github.com/apache/incubator-inlong/issues/4369) | [Improve][Manager] Support for modification of information after approval                              |
| [INLONG-4364](https://github.com/apache/incubator-inlong/issues/4364) | [Improve][Manager] Optimize sort protocol                                                              |
| [INLONG-4362](https://github.com/apache/incubator-inlong/issues/4362) | [Bug][Manager] Lack of flink dependencies for inlong-manager/manager-plugins                           |
| [INLONG-4361](https://github.com/apache/incubator-inlong/issues/4361) | [Improve][Manager] Refactor the manager client module                                                  |
| [INLONG-4358](https://github.com/apache/incubator-inlong/issues/4358) | [Feature][Manager] Support SqlServer source                                                            |
| [INLONG-4337](https://github.com/apache/incubator-inlong/issues/4337) | [Improve][Manager] Remove inlong group pulsar related tables and classes                               |
| [INLONG-4332](https://github.com/apache/incubator-inlong/issues/4332) | [Feature][Manager][Sort] Support ClickHouse load node                                                  |
| [INLONG-4325](https://github.com/apache/incubator-inlong/issues/4325) | [Bug][Manager] Register sql function exception                                                         |
| [INLONG-4315](https://github.com/apache/incubator-inlong/issues/4315) | [Bug][Manager] Incorrect task service node order in create-group workflow                              |
| [INLONG-4310](https://github.com/apache/incubator-inlong/issues/4310) | [Feature][Manager] Manager-plugin adapt changes in sort entrance                                       |
| [INLONG-4309](https://github.com/apache/incubator-inlong/issues/4309) | [Feature][Manager] Supplement hbase dependencies and license file                                      |
| [INLONG-4301](https://github.com/apache/incubator-inlong/issues/4301) | [Feature][Manager] Support oracle source                                                               |
| [INLONG-4300](https://github.com/apache/incubator-inlong/issues/4300) | [Improve][Manager] Autowired the workflow resources to simply the bean management                      |
| [INLONG-4299](https://github.com/apache/incubator-inlong/issues/4299) | [Improve][Manager] Add InlongStreamExtensionInfo                                                       |
| [INLONG-4295](https://github.com/apache/incubator-inlong/issues/4295) | [Improve][Manager] Add Json sub type support for InlongGroupInfo                                       |
| [INLONG-4280](https://github.com/apache/incubator-inlong/issues/4280) | [Feature][Manager] Add params for Iceberg sink                                                         |
| [INLONG-4278](https://github.com/apache/incubator-inlong/issues/4278) | [Improve][Manager] Support pulsar extract node in Manager                                              |
| [INLONG-4273](https://github.com/apache/incubator-inlong/issues/4273) | [Feature][Manager] Add MongoDB source support                                                          |
| [INLONG-4270](https://github.com/apache/incubator-inlong/issues/4270) | [Feature][Manager] Inlong group supports extensions of different types of MQ                           |
| [INLONG-4263](https://github.com/apache/incubator-inlong/issues/4263) | [Feature][Manager] Support HBase sink resource creation                                                |
| [INLONG-4254](https://github.com/apache/incubator-inlong/issues/4254) | [Bug][Manager] Lack of flink dependencies for inlong-manager/manager-plugins                           |
| [INLONG-4247](https://github.com/apache/incubator-inlong/issues/4247) | [Improve][Manager] Add stream create/suspend/restart/delete api                                        |
| [INLONG-4245](https://github.com/apache/incubator-inlong/issues/4245) | [Improve][Manager][Sort] Manager transmit consumer group name of kafka to sort                         |
| [INLONG-4240](https://github.com/apache/incubator-inlong/issues/4240) | [Feature][Manager] Add postgres source and sink node configuration utils                               |
| [INLONG-4239](https://github.com/apache/incubator-inlong/issues/4239) | [Improve][Manager] Remove fastjson dependency                                                          |
| [INLONG-4236](https://github.com/apache/incubator-inlong/issues/4236) | [Feature][Manager] Support operating the indices and mappings for Elasticsearch                        |
| [INLONG-4228](https://github.com/apache/incubator-inlong/issues/4228) | [Feature][Sort][Manager] Adaptive HDFS Load Node                                                       |
| [INLONG-4223](https://github.com/apache/incubator-inlong/issues/4223) | [Improve][Manager] Refactor the consumption table structure                                            |
| [INLONG-4221](https://github.com/apache/incubator-inlong/issues/4221) | [Improve][Manager] Remove duplicate serializationType in KafkaSourceListResponse                       |
| [INLONG-4220](https://github.com/apache/incubator-inlong/issues/4220) | [Feature][Manager] Add StreamResourceProcessForm                                                       |
| [INLONG-4212](https://github.com/apache/incubator-inlong/issues/4212) | [Bug][Manager] The processor executor maybe throws a null pointer exception                            |
| [INLONG-4208](https://github.com/apache/incubator-inlong/issues/4208) | [Improve][Manager] Merge UpdateGroupProcessForm into GroupResourceProcessForm                          |
| [INLONG-4203](https://github.com/apache/incubator-inlong/issues/4203) | [Improve][Manager] Improve the HTTP request and response parse                                         |
| [INLONG-4197](https://github.com/apache/incubator-inlong/issues/4197) | [Feature][Manager] Add Hbase sink info and load node utils                                             |
| [INLONG-4194](https://github.com/apache/incubator-inlong/issues/4194) | [Improve][Manager] Add update sort config API in manager client                                        |
| [INLONG-4188](https://github.com/apache/incubator-inlong/issues/4188) | [Improve][Manager] Check whether the stream exists in the manager client                               |
| [INLONG-4186](https://github.com/apache/incubator-inlong/issues/4186) | [Improve][Manager] Add token field in cluster and node table                                           |
| [INLONG-4183](https://github.com/apache/incubator-inlong/issues/4183) | [Improve][Manager] Fix splitFields in manager service                                                  |
| [INLONG-4181](https://github.com/apache/incubator-inlong/issues/4181) | [Improve][Manager]Fix transform update api                                                             |
| [INLONG-4179](https://github.com/apache/incubator-inlong/issues/4179) | [Improve][Manager] Add comments in manager modules                                                     |
| [INLONG-4177](https://github.com/apache/incubator-inlong/issues/4177) | [Feature][Manager] Refactor getSortClusterConfig interface                                             |
| [INLONG-4168](https://github.com/apache/incubator-inlong/issues/4168) | [Feature][Manager] Update inlong_group table schema                                                    |
| [INLONG-4164](https://github.com/apache/incubator-inlong/issues/4164) | [Improve][Manager] Migrate the use of third_party_cluster table to inlong_cluster table                |
| [INLONG-4155](https://github.com/apache/incubator-inlong/issues/4155) | [Improve][Manager] Change constants and remove clone method in WorkflowContext                         |
| [INLONG-4146](https://github.com/apache/incubator-inlong/issues/4146) | [Feature][Manager] Get DataProxy configuration data from inlong_cluster table                          |
| [INLONG-4142](https://github.com/apache/incubator-inlong/issues/4142) | [Improve][Manager] Improve ServiceTask clone method                                                    |
| [INLONG-4139](https://github.com/apache/incubator-inlong/issues/4139) | [Improve][Manager] Query inlong group by the status list in manager client                             |
| [INLONG-4133](https://github.com/apache/incubator-inlong/issues/4133) | [Improve][Manager] Not pass the type field when querying sources and sinks                             |
| [INLONG-4128](https://github.com/apache/incubator-inlong/issues/4128) | [Improve][Manager] Abstracting the logic for creating Hive tables                                      |
| [INLONG-4120](https://github.com/apache/incubator-inlong/issues/4120) | [Improve][Manager] Change the keyWord to keyword in query params                                       |
| [INLONG-4118](https://github.com/apache/incubator-inlong/issues/4118) | [Improve][Manager] It should not return an empty list when paging the auto-push source                 |
| [INLONG-4110](https://github.com/apache/incubator-inlong/issues/4110) | [Improve][Manager] Create cascade function wrapper                                                     |
| [INLONG-4108](https://github.com/apache/incubator-inlong/issues/4108) | [Improve][Manager] Merge two enums of manager                                                          |
| [INLONG-4105](https://github.com/apache/incubator-inlong/issues/4105) | [Improve][Manager] Refactor the sink workflow and sink resource operator                               |
| [INLONG-4100](https://github.com/apache/incubator-inlong/issues/4100) | [Improve][Manager] Add id in update request for source/sink/transform                                  |
| [INLONG-4099](https://github.com/apache/incubator-inlong/issues/4099) | [Feature][Manager] Support iceberg stream sink resource operator                                       |
| [INLONG-4095](https://github.com/apache/incubator-inlong/issues/4095) | [Bug][Manager] Managerctl unit test error and characters are not aligned                               |
| [INLONG-4092](https://github.com/apache/incubator-inlong/issues/4092) | [Improve][Manager] Add primary key in Kafka source                                                     |
| [INLONG-4085](https://github.com/apache/incubator-inlong/issues/4085) | [Improve][Manager] Change the inlong_group and inlong_stream table structure                           |
| [INLONG-4084](https://github.com/apache/incubator-inlong/issues/4084) | [Improve][Manager] Add some fields for stream_source and stream_sink                                   |
| [INLONG-4078](https://github.com/apache/incubator-inlong/issues/4078) | [Improve][Manager] Add notes for manager client api                                                    |
| [INLONG-4077](https://github.com/apache/incubator-inlong/issues/4077) | [Improve][Manager] Implement the APIs of data node management                                          |
| [INLONG-4071](https://github.com/apache/incubator-inlong/issues/4071) | [Feature][Manager] Add data cluster table                                                              |
| [INLONG-4070](https://github.com/apache/incubator-inlong/issues/4070) | [Improve][Manager] Add update api In Inlong stream                                                     |
| [INLONG-4069](https://github.com/apache/incubator-inlong/issues/4069) | [Improve][Manager] Add ext_tag field for inlong cluster table                                          |
| [INLONG-4067](https://github.com/apache/incubator-inlong/issues/4067) | [Improve][Manager] Optimize fieldRelationShip in split transform                                       |
| [INLONG-4063](https://github.com/apache/incubator-inlong/issues/4063) | [Umbrella][Manager] Refactor the inlong group interfaces to support easily extending different MQs     |
| [INLONG-4060](https://github.com/apache/incubator-inlong/issues/4060) | [Improve][Manager] Fix NodeUtils for sort                                                              |
| [INLONG-4052](https://github.com/apache/incubator-inlong/issues/4052) | [Improve][Manager] Remove zone tag field of inlong cluster table                                       |
| [INLONG-4046](https://github.com/apache/incubator-inlong/issues/4046) | [Feature][Manager] Fix Filter Function of Join node                                                    |
| [INLONG-4042](https://github.com/apache/incubator-inlong/issues/4042) | [Improve][Manager] Add properties in MysqlExtractNode for migrating all databases                      |
| [INLONG-4040](https://github.com/apache/incubator-inlong/issues/4040) | [Bug][Manager] Hive sink table path config error                                                       |
| [INLONG-4034](https://github.com/apache/incubator-inlong/issues/4034) | [Improve][Manager] Add originFieldName in StreamField                                                  |
| [INLONG-4026](https://github.com/apache/incubator-inlong/issues/4026) | [Improve][Manager] Fix field type of StreamSourceFieldMapper                                           |
| [INLONG-4017](https://github.com/apache/incubator-inlong/issues/4017) | [Bug][Manager] Fix DataType deserialized exception                                                     |
| [INLONG-4012](https://github.com/apache/incubator-inlong/issues/4012) | [Feature][Manager] Add kafka sink resource operator                                                    |
| [INLONG-4000](https://github.com/apache/incubator-inlong/issues/4000) | [Improve][Manager] Refactor the implementations of heartbeat interfaces                                |
| [INLONG-3993](https://github.com/apache/incubator-inlong/issues/3993) | [Improve][Manager] Support HiveLoadNode in light mode                                                  |
| [INLONG-3981](https://github.com/apache/incubator-inlong/issues/3981) | [Improve][Manager] Fix status check of lightweight Group                                               |
| [INLONG-3958](https://github.com/apache/incubator-inlong/issues/3958) | [Improve][Manager] Fix NPEs in manager service                                                         |
| [INLONG-3951](https://github.com/apache/incubator-inlong/issues/3951) | [Bug][Manager] Unique index error for cluster_name in SQL file                                         |
| [INLONG-3947](https://github.com/apache/incubator-inlong/issues/3947) | [Improve][Manager] Support blank middleware                                                            |
| [INLONG-3946](https://github.com/apache/incubator-inlong/issues/3946) | [Improve][Manager] Implement the APIs of cluster management                                            |
| [INLONG-3943](https://github.com/apache/incubator-inlong/issues/3943) | [Feature][Manager] Add inlong_cluster and inlong_cluster_node tables                                   |
| [INLONG-3942](https://github.com/apache/incubator-inlong/issues/3942) | [Umbrella][Manager] Provides cluster management features for the InLong                                |
| [INLONG-3929](https://github.com/apache/incubator-inlong/issues/3929) | [Improve][Manager] Support deDuplicate transform in manager                                            |
| [INLONG-3921](https://github.com/apache/incubator-inlong/issues/3921) | [Improve][Manager] Add primary key in Kafka source                                                     |
| [INLONG-3920](https://github.com/apache/incubator-inlong/issues/3920) | [Improve][Manager] Add primary key in Binlog source                                                    |
| [INLONG-3919](https://github.com/apache/incubator-inlong/issues/3919) | [Improve][Manager] Data consumption and data source module supports access control                     |
| [INLONG-3915](https://github.com/apache/incubator-inlong/issues/3915) | [Bug][Manager] Missing delete flag on entity creation                                                  |
| [INLONG-3888](https://github.com/apache/incubator-inlong/issues/3888) | [Bug][Manager] The method of batchSaveAll occurred error as the sink_name was null                     |
| [INLONG-3883](https://github.com/apache/incubator-inlong/issues/3883) | [Feature][Manager] Support create table of ClickHouse                                                  |
| [INLONG-3879](https://github.com/apache/incubator-inlong/issues/3879) | [Improve][Manager] Update status enum for group, stream,  source, and sink                             |
| [INLONG-3876](https://github.com/apache/incubator-inlong/issues/3876) | [Bug][Manager] The managerctl tool crashes when lunches with  -h or --help argument                    |
| [INLONG-3874](https://github.com/apache/incubator-inlong/issues/3874) | [Improve][Manager] Abstract the operator class for creating Sink resources                             |
| [INLONG-3866](https://github.com/apache/incubator-inlong/issues/3866) | [Improve][Manager] Rename the third-party package to resource                                          |
| [INLONG-3861](https://github.com/apache/incubator-inlong/issues/3861) | [Improve][Manager] Support the expansion of TubeMQ, Pulsar, or other message queues                    |
| [INLONG-3858](https://github.com/apache/incubator-inlong/issues/3858) | [Bug][Manager] The other admin role can not list all inlong group info and stream info                 |
| [INLONG-3856](https://github.com/apache/incubator-inlong/issues/3856) | [Feature][Manager] Add heartbeat report handler                                                        |
| [INLONG-3855](https://github.com/apache/incubator-inlong/issues/3855) | [Feature][Manager] Add lightweight mode for inlong group                                               |
| [INLONG-3782](https://github.com/apache/incubator-inlong/issues/3782) | [Improve][Manager] Improve the log configuration                                                       |
| [INLONG-3776](https://github.com/apache/incubator-inlong/issues/3776) | [Improve][Manager] Update the third-party components LICENSEs                                          |
| [INLONG-3775](https://github.com/apache/incubator-inlong/issues/3775) | [Improve][Manager] Add a specific meaning for the managerctl status output                             |
| [INLONG-3731](https://github.com/apache/incubator-inlong/issues/3731) | [Improve][Manager] Make state more abstract for managerctl                                             |
| [INLONG-3719](https://github.com/apache/incubator-inlong/issues/3719) | [Feature][Manager] Support data_transformation feature                                                 |
| [INLONG-3698](https://github.com/apache/incubator-inlong/issues/3698) | [Feature][Manager] Support complex data type distribution for hive-sink                                |
| [INLONG-3636](https://github.com/apache/incubator-inlong/issues/3636) | [Improve][Manager] Fix the warning logs during the build                                               |
| [INLONG-3392](https://github.com/apache/incubator-inlong/issues/3392) | [Improve][Manager] Optimize groupName and groupId                                                      |
| [INLONG-3238](https://github.com/apache/incubator-inlong/issues/3238) | [Bug][Manager] The data grouping list cannot be edited?                                                |
| [INLONG-2901](https://github.com/apache/incubator-inlong/issues/2901) | [Bug][Manager] Maven cannot run test classes                                                           |
| [INLONG-2544](https://github.com/apache/incubator-inlong/issues/2544) | [Improve][Manager] Refactor the CreateStreamWorkflowDefinition                                         |
| [INLONG-2305](https://github.com/apache/incubator-inlong/issues/2305) | [Feature][Manager] Add Cluster Management for MQ/DataProxy/Sort Cluster                                |
| [INLONG-1858](https://github.com/apache/incubator-inlong/issues/1858) | [Bug][Manager] Adding columns to a Hive table does not take effect                                     |

### SDK
| ISSUE                                                                 | Summary                                                                                                |
|:----------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------|
| [INLONG-4400](https://github.com/apache/incubator-inlong/issues/4400) | [Improve][SDK] Unified dataproxy sdk clusterId property name                                           |
| [INLONG-4354](https://github.com/apache/incubator-inlong/issues/4354) | [Bug][SDK] ProxyConfigEntry cluster id NPE                                                             |

### Sort
| ISSUE                                                                 | Summary                                                                                                |
|:----------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------|
| [INLONG-4638](https://github.com/apache/incubator-inlong/issues/4638) | [Improve][Sort] Remove the dependency of spotbugs-annotations                                          |
| [INLONG-4624](https://github.com/apache/incubator-inlong/issues/4624) | [Improve][Sort] Package Pulsar and Hive connectors to the same file                                    |
| [INLONG-4619](https://github.com/apache/incubator-inlong/issues/4619) | [Bug][Sort] Fix maven package problem of Hive connector                                                |
| [INLONG-4596](https://github.com/apache/incubator-inlong/issues/4596) | [Bug][Sort] Fix jdbc-connector packaging lost dependency                                               |
| [INLONG-4591](https://github.com/apache/incubator-inlong/issues/4591) | [Bug][Sort] Set the default Hive version and upgrade the log4j to log4j2                               |
| [INLONG-4557](https://github.com/apache/incubator-inlong/issues/4557) | [Bug][Sort] Fix HBase connector dependency and pom relocation problem                                  |
| [INLONG-4527](https://github.com/apache/incubator-inlong/issues/4527) | [Improve][Sort] Upgrade the Elasticsearch version and add related license                              |
| [INLONG-4493](https://github.com/apache/incubator-inlong/issues/4493) | [Improve][Sort] Remove hiveConfDir not null constraint                                                 |
| [INLONG-4491](https://github.com/apache/incubator-inlong/issues/4491) | [Bug][Sort] Missing dependencies after packaging for sort-dist module                                  |
| [INLONG-4476](https://github.com/apache/incubator-inlong/issues/4476) | [Improve][Sort][Manager] Remove zk and related classes                                                 |
| [INLONG-4458](https://github.com/apache/incubator-inlong/issues/4458) | [Improve][Sort][Manager] Unify the meta field naming                                                   |
| [INLONG-4448](https://github.com/apache/incubator-inlong/issues/4448) | [Feature][Sort] Add Greenplum database data load support                                               |
| [INLONG-4429](https://github.com/apache/incubator-inlong/issues/4429) | [Improve][Sort] Add sqlserver jdbc driver and management version                                       |
| [INLONG-4428](https://github.com/apache/incubator-inlong/issues/4428) | [Improve][Sort][Manager] Optimize the name for Data Node related modules and classes                   |
| [INLONG-4408](https://github.com/apache/incubator-inlong/issues/4408) | [Feature][Manager][Sort] Add iceberg sink load node                                                    |
| [INLONG-4405](https://github.com/apache/incubator-inlong/issues/4405) | [Feature][Sort] Support run SQL script                                                                 |
| [INLONG-4394](https://github.com/apache/incubator-inlong/issues/4394) | [Feature][Sort] Add Oracle data load support                                                           |
| [INLONG-4390](https://github.com/apache/incubator-inlong/issues/4390) | [Bug][Sort] Exclude mysql:mysql-connector-java:jar package                                             |
| [INLONG-4385](https://github.com/apache/incubator-inlong/issues/4385) | [Bug][Sort] Exclude or remove the mysql:mysql-connector-java:jar:8.0.21 package                        |
| [INLONG-4383](https://github.com/apache/incubator-inlong/issues/4383) | [Feature][Sort] Add MySQL data load support                                                            |
| [INLONG-4371](https://github.com/apache/incubator-inlong/issues/4371) | [Bug][Sort] Remove null constraint on Hive version                                                     |
| [INLONG-4353](https://github.com/apache/incubator-inlong/issues/4353) | [Improve][Sort] Optimize code structure and shading jar                                                |
| [INLONG-4346](https://github.com/apache/incubator-inlong/issues/4346) | [Improve][Sort] Enhance upsert capability for SqlServer                                                |
| [INLONG-4345](https://github.com/apache/incubator-inlong/issues/4345) | [Feature][Sort] Support sink changelog stream to TDSQL PostgreSQL                                      |
| [INLONG-4342](https://github.com/apache/incubator-inlong/issues/4342) | [Improve][Sort] Duplicate audit-sdk dependency in sort core                                            |
| [INLONG-4339](https://github.com/apache/incubator-inlong/issues/4339) | [Improve][Sort] Rollback debezium-core to 1.5.4                                                        |
| [INLONG-4332](https://github.com/apache/incubator-inlong/issues/4332) | [Feature][Manager][Sort] Support ClickHouse load node                                                  |
| [INLONG-4327](https://github.com/apache/incubator-inlong/issues/4327) | [Bug][Sort] Fix missing some connector table factory error after packaging                             |
| [INLONG-4312](https://github.com/apache/incubator-inlong/issues/4312) | [Feature][Sort] Add SqlServer data load support                                                        |
| [INLONG-4306](https://github.com/apache/incubator-inlong/issues/4306) | [Bug][Sort] Parameter error   connector of file system                                                 |
| [INLONG-4303](https://github.com/apache/incubator-inlong/issues/4303) | [Improve][Sort] Iceberg Load Node add required option                                                  |
| [INLONG-4292](https://github.com/apache/incubator-inlong/issues/4292) | [Improve][Agent][TubeMQ][Sort] Upgrade the property file for all modules from log4j to log4j2          |
| [INLONG-4282](https://github.com/apache/incubator-inlong/issues/4282) | [Improve][Sort] Optimize the sort package structure                                                    |
| [INLONG-4264](https://github.com/apache/incubator-inlong/issues/4264) | [Bug][Sort] Sort lightweight start 'pulsar-to-kafka' task failed                                       |
| [INLONG-4262](https://github.com/apache/incubator-inlong/issues/4262) | [Feature][Sort] Add SqlServer data extract support                                                     |
| [INLONG-4259](https://github.com/apache/incubator-inlong/issues/4259) | [Feature][Sort] Import inlong format json serialize/deserialize                                        |
| [INLONG-4250](https://github.com/apache/incubator-inlong/issues/4250) | [Feature][Sort] Add Elasticsearch load node                                                            |
| [INLONG-4245](https://github.com/apache/incubator-inlong/issues/4245) | [Improve][Manager][Sort] Manager transmit consumer group name of kafka to sort                         |
| [INLONG-4243](https://github.com/apache/incubator-inlong/issues/4243) | [Feature][Sort] Add Oracle data extract support                                                        |
| [INLONG-4228](https://github.com/apache/incubator-inlong/issues/4228) | [Feature][Sort][Manager] Adaptive HDFS Load Node                                                       |
| [INLONG-4227](https://github.com/apache/incubator-inlong/issues/4227) | [Feature][Sort] Sort lightweight support extract data to ClickHouse                                    |
| [INLONG-4224](https://github.com/apache/incubator-inlong/issues/4224) | [Improve][Sort] Add debezium module to connectors                                                      |
| [INLONG-4244](https://github.com/apache/incubator-inlong/issues/4244) | [Improve][Sort] Remove flink-avro relocation                                                           |
| [INLONG-4215](https://github.com/apache/incubator-inlong/issues/4215) | [Improve][Sort] Add license for Hive connector of Sort                                                 |
| [INLONG-4198](https://github.com/apache/incubator-inlong/issues/4198) | [Feature][Sort] Add Postgres load node                                                                 |
| [INLONG-4189](https://github.com/apache/incubator-inlong/issues/4189) | [Feature][Sort] Change the NOTICE to LICENSE                                                           |
| [INLONG-4174](https://github.com/apache/incubator-inlong/issues/4174) | [Feature][Sort] Add Postgres extract node supporting                                                   |
| [INLONG-4171](https://github.com/apache/incubator-inlong/issues/4171) | [Feature][Sort] Add FileSystem extract node                                                            |
| [INLONG-4170](https://github.com/apache/incubator-inlong/issues/4170) | [Feature][Sort] Add FileSystem load node                                                               |
| [INLONG-4169](https://github.com/apache/incubator-inlong/issues/4169) | [Improve][Sort] Add Java docs in sort                                                                  |
| [INLONG-4167](https://github.com/apache/incubator-inlong/issues/4167) | [Feature][Sort] Add MongoDB extract node                                                               |
| [INLONG-4157](https://github.com/apache/incubator-inlong/issues/4157) | [Feature][Sort] Sort lightweight support load data to Iceberg                                          |
| [INLONG-4156](https://github.com/apache/incubator-inlong/issues/4156) | [Feature][Sort] Support HBase load node                                                                |
| [INLONG-4141](https://github.com/apache/incubator-inlong/issues/4141) | [Feature][Sort] Sort lightweight support load data from Pulsar                                         |
| [INLONG-4097](https://github.com/apache/incubator-inlong/issues/4097) | [Improve][Sort] Use javax constrain notNull annotation                                                 |
| [INLONG-4090](https://github.com/apache/incubator-inlong/issues/4090) | [Improve][Sort] Add license for MySQL CDC in Sort single tenant                                        |
| [INLONG-4086](https://github.com/apache/incubator-inlong/issues/4086) | [Improve][Sort] Remove BufferedSocketInputStream class as it is not used                               |
| [INLONG-4081](https://github.com/apache/incubator-inlong/issues/4081) | [Bug][Sort] Fix upsert_kafka constant to upsert-kafka                                                  |
| [INLONG-4066](https://github.com/apache/incubator-inlong/issues/4066) | [Feature][Sort] Add Inlong Msg format for inlong msg                                                   |
| [INLONG-4058](https://github.com/apache/incubator-inlong/issues/4058) | [Improve][Sort] Resolve AVRO format dependency conflict                                                |
| [INLONG-4050](https://github.com/apache/incubator-inlong/issues/4050) | [Bug][Sort] Parameter definition error of HiveLoad                                                     |
| [INLONG-4048](https://github.com/apache/incubator-inlong/issues/4048) | [Bug][Sort] Fix metadata type process error and hive-exec dependency scope error                       |
| [INLONG-4044](https://github.com/apache/incubator-inlong/issues/4044) | [Bug][Sort] Resolve conflict of flink-table-api-java-bridge jar                                        |
| [INLONG-4035](https://github.com/apache/incubator-inlong/issues/4035) | [Bug][Sort] Change the restriction of Hive catalogName from not nullable to nullable                   |
| [INLONG-4030](https://github.com/apache/incubator-inlong/issues/4030) | [Improve][Sort] Import all changelog mode data ingest into Hive                                        |
| [INLONG-4022](https://github.com/apache/incubator-inlong/issues/4022) | [Bug][Sort] Flink table catalog only supports timestamp of precision 9                                 |
| [INLONG-4013](https://github.com/apache/incubator-inlong/issues/4013) | [Feature][Sort] Support write metadata in canal format                                                 |
| [INLONG-4007](https://github.com/apache/incubator-inlong/issues/4007) | [Improve][Sort] Modify default settings of CSV format                                                  |
| [INLONG-4005](https://github.com/apache/incubator-inlong/issues/4005) | [Bug][Sort] Remove some inappropriate comment and code                                                 |
| [INLONG-3996](https://github.com/apache/incubator-inlong/issues/3996) | [Feature][Sort] Support all migrate for database                                                       |
| [INLONG-3979](https://github.com/apache/incubator-inlong/issues/3979) | [Bug][Sort] Fix mysqlExtractNode options append error and conflict of flink jar                        |
| [INLONG-3973](https://github.com/apache/incubator-inlong/issues/3973) | [Feature][Sort] CDC support all migration                                                              |
| [INLONG-3961](https://github.com/apache/incubator-inlong/issues/3961) | [Feature][Sort] Add MySQL CDC append                                                                   |
| [INLONG-3956](https://github.com/apache/incubator-inlong/issues/3956) | [Feature][Sort] Add Hive connector to support CDC                                                      |
| [INLONG-3953](https://github.com/apache/incubator-inlong/issues/3953) | [Feature][Sort] Add MySQL dynamic table implementation -  modified from Flink CDC                      |
| [INLONG-3924](https://github.com/apache/incubator-inlong/issues/3924) | [Feature][Sort] Add MySQL cdc and support multiple meta data                                           |
| [INLONG-3899](https://github.com/apache/incubator-inlong/issues/3899) | [Feature][Sort] Add string regexp replace support for transform                                        |
| [INLONG-3893](https://github.com/apache/incubator-inlong/issues/3893) | [Feature][Sort] Add string delimiting support for transform                                            |
| [INLONG-3890](https://github.com/apache/incubator-inlong/issues/3890) | [Feature][Sort] Add StringConstantParam to enhance support for constant parameters                     |
| [INLONG-3885](https://github.com/apache/incubator-inlong/issues/3885) | [Feature][Sort] Add KafkaExtractNode to support Kafka source                                           |
| [INLONG-3868](https://github.com/apache/incubator-inlong/issues/3868) | [Feature][Sort] Support data from mysql binlog sync to kafka                                           |
| [INLONG-3860](https://github.com/apache/incubator-inlong/issues/3860) | [Improve][Sort] Add some format for ExtractNode and LoadNode                                           |
| [INLONG-3841](https://github.com/apache/incubator-inlong/issues/3841) | [Feature][Sort] Add distinct support based time column for transform                                   |
| [INLONG-3839](https://github.com/apache/incubator-inlong/issues/3839) | [Feature][Sort] Add cascade function support for transform                                             |
| [INLONG-3837](https://github.com/apache/incubator-inlong/issues/3837) | [Feature][Sort] Optimize the time window correlation function format                                   |
| [INLONG-3836](https://github.com/apache/incubator-inlong/issues/3836) | [Feature][Sort] Add join support for transform                                                         |
| [INLONG-3835](https://github.com/apache/incubator-inlong/issues/3835) | [Feature][Sort] Register CascadeFunctionWrapper in the parent interface                                |
| [INLONG-3834](https://github.com/apache/incubator-inlong/issues/3834) | [Feature][Sort] Fix unit test sporadic errors                                                          |
| [INLONG-3831](https://github.com/apache/incubator-inlong/issues/3831) | [Feature][Sort] Add meta field support for sort lightweight                                            |
| [INLONG-3829](https://github.com/apache/incubator-inlong/issues/3829) | [Feature][Sort] Optimize the sort entrance program to support lightweight                              |
| [INLONG-3827](https://github.com/apache/incubator-inlong/issues/3827) | [Feature][Sort] Add functions definition to support transform                                          |
| [INLONG-3826](https://github.com/apache/incubator-inlong/issues/3826) | [Feature][Sort] Enhance field format to support varchar types and timestamp of different precisions    |
| [INLONG-3823](https://github.com/apache/incubator-inlong/issues/3823) | [Feature][Sort] Fix error caused by unregistered custom function                                       |
| [INLONG-3822](https://github.com/apache/incubator-inlong/issues/3822) | [Feature][Sort] Add node relations definition to support transform                                     |
| [INLONG-3817](https://github.com/apache/incubator-inlong/issues/3817) | [Feature][Sort] Fix null point exception in canal-json format                                          |
| [INLONG-3816](https://github.com/apache/incubator-inlong/issues/3816) | [Feature][Sort] Fix NPE in RegexpReplaceFirstFunction                                                  |
| [INLONG-3815](https://github.com/apache/incubator-inlong/issues/3815) | [Feature][Sort] Fix meta field sync error                                                              |
| [INLONG-3805](https://github.com/apache/incubator-inlong/issues/3805) | [Feature][Sort] Add operators definition to support transform                                          |
| [INLONG-3800](https://github.com/apache/incubator-inlong/issues/3800) | [Feature][Sort] Add GroupInfo, StreamInfo definition to support transform                              |
| [INLONG-3794](https://github.com/apache/incubator-inlong/issues/3794) | [Feature][Sort] Add TimeUnitConstantParam definition to support transform                              |
| [INLONG-3793](https://github.com/apache/incubator-inlong/issues/3793) | [Feature][Sort] Add ConstantParam definition to support transform                                      |
| [INLONG-3791](https://github.com/apache/incubator-inlong/issues/3791) | [Feature][Sort] Add WatermarkField definition to support transform                                     |
| [INLONG-3790](https://github.com/apache/incubator-inlong/issues/3790) | [Feature][Sort] Add FieldRelationShip definition to support transform                                  |
| [INLONG-3789](https://github.com/apache/incubator-inlong/issues/3789) | [Feature][Sort] Add NodeRelationShip definition to support transform                                   |
| [INLONG-3788](https://github.com/apache/incubator-inlong/issues/3788) | [Feature][Sort] Add Node interface and derived interface definitions to support transform              |
| [INLONG-3787](https://github.com/apache/incubator-inlong/issues/3787) | [Feature][Sort] Add Function interface and derived interface definitions to support transform          |
| [INLONG-3786](https://github.com/apache/incubator-inlong/issues/3786) | [Feature][Sort] Add Operator interface and derived interface definitions to support transform          |
| [INLONG-3778](https://github.com/apache/incubator-inlong/issues/3778) | [Feature][Sort] FieldInfo enhanced to support transform in the future                                  |
| [INLONG-3777](https://github.com/apache/incubator-inlong/issues/3777) | [Feature][Sort] ExtractNode,LoadNode implementation                                                    |
| [INLONG-3658](https://github.com/apache/incubator-inlong/issues/3658) | [Umbrella][Sort] Data integration lightweight                                                          |
| [INLONG-1823](https://github.com/apache/incubator-inlong/issues/1823) | [Feature][Sort] Support store data to Elasticsearch                                                    |

### Sort-Standalone
| ISSUE                                                                 | Summary                                                                                                |
|:----------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------|
| [INLONG-4453](https://github.com/apache/incubator-inlong/issues/4453) | [Bug][Sort-Standalone] Wrong audit when send to kafka failed                                           |
| [INLONG-3773](https://github.com/apache/incubator-inlong/issues/3773) | [Feature][Sort-Standalone] Support configurable handler to transform data of Kafka                     |
| [INLONG-3667](https://github.com/apache/incubator-inlong/issues/3667) | [Feature][Sort-Standalone] Add manage entry to stop cache consumer before node offline or upgrade      |
| [INLONG-1933](https://github.com/apache/incubator-inlong/issues/1933) | [Feature][Sort-Standalone] Read API support inlong manager commands                                    |

### TubeMQ
| ISSUE                                                                 | Summary                                                                                                |
|:----------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------|
| [INLONG-4486](https://github.com/apache/incubator-inlong/issues/4486) | [Improve][TubeMQ] Adjust the parameter requirements of group consume delete control APIs               |
| [INLONG-4470](https://github.com/apache/incubator-inlong/issues/4470) | [Improve][TubeMQ] The adminQueryBlackGroupInfo method adds query topicName field                       |
| [INLONG-4451](https://github.com/apache/incubator-inlong/issues/4451) | [Bug][TubeMQ] The zookeeper service can not start for TubeMQ Docker Container                          |
| [INLONG-4324](https://github.com/apache/incubator-inlong/issues/4324) | [Improve][TubeMQ] Add Javadoc for methods                                                              |
| [INLONG-4321](https://github.com/apache/incubator-inlong/issues/4321) | [Improve][TubeMQ] Add Javadoc comments for methods                                                     |
| [INLONG-4292](https://github.com/apache/incubator-inlong/issues/4292) | [Improve][Agent][TubeMQ][Sort] Upgrade the property file for all modules from log4j to log4j2          |
| [INLONG-4217](https://github.com/apache/incubator-inlong/issues/4217) | [Improve][TubeMQ] Add the flow control method and filtering method of the consumption group settings   |
| [INLONG-4130](https://github.com/apache/incubator-inlong/issues/4130) | [Improve][TubeMQ] Optimize the broker replication method and topic replication method                  |
| [INLONG-4114](https://github.com/apache/incubator-inlong/issues/4114) | [Bug][TubeMQ] All container can not start successfully                                                 |
| [INLONG-3975](https://github.com/apache/incubator-inlong/issues/3975) | [Improve][TubeMQ] Modify the MasterConfigTest file to configure the incoming parameters                |
| [INLONG-3869](https://github.com/apache/incubator-inlong/issues/3869) | [Improve][TubeMQ] Remove hibernate for tube manager                                                    |
| [INLONG-3475](https://github.com/apache/incubator-inlong/issues/3475) | [Feature][TubeMQ] Add an API for batch deletion of authorized consumer group records                   |

### Other
| ISSUE                                                                 | Summary                                                                                                |
|:----------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------|
| [INLONG-4640](https://github.com/apache/incubator-inlong/issues/4640) | [License] Complete the LICENSE of the third-party dependencies of the Sort connectors                  |
| [INLONG-4633](https://github.com/apache/incubator-inlong/issues/4633) | [License] Add the third-party dependency LICENSE of Sort connectors module                             |
| [INLONG-4628](https://github.com/apache/incubator-inlong/issues/4628) | [License] Update inlong-manager's third-party dependency LICENSE                                       |
| [INLONG-4574](https://github.com/apache/incubator-inlong/issues/4574) | [Release] Bumped version to 1.3.0-incubating-SNAPSHOT                                                  |
| [INLONG-4568](https://github.com/apache/incubator-inlong/issues/4568) | [Release] Bumped version to 1.2.1-incubating-SNAPSHOT                                                  |
| [INLONG-4574](https://github.com/apache/incubator-inlong/issues/4567) | [Release] Update changes log for the 1.2.0 version                                                     |
| [INLONG-4565](https://github.com/apache/incubator-inlong/issues/4565) | [Release] Add the 1.2.0 version option for the bug report                                              |
| [INLONG-4368](https://github.com/apache/incubator-inlong/issues/4368) | [License] Final LICENSE check of all modules                                                           |
| [INLONG-4336](https://github.com/apache/incubator-inlong/issues/4336) | [License] Update inlong-sort's third-party dependency LICENSE                                          |
| [INLONG-4318](https://github.com/apache/incubator-inlong/issues/4318) | [License] Update inlong-manager's third-party dependency LICENSE                                       |
| [INLONG-4314](https://github.com/apache/incubator-inlong/issues/4314) | [License] Update Sort-standalone's third-party dependency LICENSE                                      |
| [INLONG-4305](https://github.com/apache/incubator-inlong/issues/4305) | [License] Update TubeMQ-Manager's third-party dependency LICENSE                                       |
| [INLONG-4296](https://github.com/apache/incubator-inlong/issues/4296) | [License] Update Audit, DataProxy, TubeMQ's third-party dependency LICENSE                             |
| [INLONG-4288](https://github.com/apache/incubator-inlong/issues/4288) | [License] Update Agent's third-party dependency LICENSE                                                |
| [INLONG-3968](https://github.com/apache/incubator-inlong/issues/3968) | [License] Remove the "WIP" label of the DISCLAIMER file                                                |
| [INLONG-3864](https://github.com/apache/incubator-inlong/issues/3864) | [License] Recheck the third-party dependencies by combing each module                                  |
| [INLONG-3849](https://github.com/apache/incubator-inlong/issues/3849) | [License] Update the third-party components LICENSEs for inlong-audit                                  |
| [INLONG-3771](https://github.com/apache/incubator-inlong/issues/3771) | [License] Update the third-party components LICENSEs for inlong-agent                                  |
| [INLONG-3422](https://github.com/apache/incubator-inlong/issues/3422) | [License] Sort out the LICENSEs of the third-party components that the project depends on              |
| [INLONG-4426](https://github.com/apache/incubator-inlong/issues/4426) | [Improve][Office-Website] Update CI Nodejs Version                                                     |
| [INLONG-4150](https://github.com/apache/incubator-inlong/issues/4150) | [Improve][Office-Website] Automatically identify version number sorting                                |
| [INLONG-4455](https://github.com/apache/incubator-inlong/issues/4455) | [Improve][GitHub] Update the Pull Request TEMPLATE to make it more clear                               |
| [INLONG-4267](https://github.com/apache/incubator-inlong/issues/4267) | [Bug][GitHub] The checks pipeline of PR was incorrect                                                  |
| [INLONG-4251](https://github.com/apache/incubator-inlong/issues/4251) | [Improve][GitHub] Improve GitHub configuration in `.asf.yaml`                                          |
| [INLONG-4205](https://github.com/apache/incubator-inlong/issues/4205) | [Bug][GitHub] Error in the greeting workflow                                                           |
| [INLONG-4201](https://github.com/apache/incubator-inlong/issues/4201) | [Improve][GitHub] Improve workflows and documentation                                                  |
| [INLONG-4161](https://github.com/apache/incubator-inlong/issues/4161) | [Bug][GitHub] Incorrect name in stale workflow                                                         |
| [INLONG-4153](https://github.com/apache/incubator-inlong/issues/4153) | [Feature][GitHub] Add support for first interaction                                                    |
| [INLONG-4038](https://github.com/apache/incubator-inlong/issues/4038) | [Improve][GitHub] Enable the alert of dependabot, and disable the automatic update                     |
| [INLONG-4024](https://github.com/apache/incubator-inlong/issues/4024) | [Improve][GitHub] Improve trigger conditions in the stable workflow                                    |
| [INLONG-4019](https://github.com/apache/incubator-inlong/issues/4019) | [Improve][GitHub] Update the dependency from dependabot                                                |
| [INLONG-4001](https://github.com/apache/incubator-inlong/issues/4001) | [Improve][Docker] Modify the MySQL Docker image version to 8.0.28                                      |
| [INLONG-3998](https://github.com/apache/incubator-inlong/issues/3998) | [Improve][GitHub] Improve labeler workflow with docker, k8s and github labels                          |
| [INLONG-3995](https://github.com/apache/incubator-inlong/issues/3995) | [Improve][GitHub] Improve Dependabot configuration                                                     |
| [INLONG-3974](https://github.com/apache/incubator-inlong/issues/3974) | [Feature][GitHub] Add support for Dependabot                                                           |
| [INLONG-3473](https://github.com/apache/incubator-inlong/issues/3473) | [Feature][GitHub][K8s] Add support for helm chart testing                                              |
| [INLONG-4349](https://github.com/apache/incubator-inlong/issues/4349) | [Improve][Doc] Update check style to avoid the Javadoc missing and error                               |
| [INLONG-4256](https://github.com/apache/incubator-inlong/issues/4256) | [Improve][Doc] Add java doc to solve the checkstyle issues                                             |
| [INLONG-4073](https://github.com/apache/incubator-inlong/issues/4073) | [Improve][Doc] Modify the invitation email templates of Committer or PPMC                              |
| [INLONG-3965](https://github.com/apache/incubator-inlong/issues/3965) | [Improve][Doc] Add how-to-maintain-3rd-party-dependencies.md                                           |
| [INLONG-3930](https://github.com/apache/incubator-inlong/issues/3930) | [Improve][Doc] Add more deployment and development guides in Readme                                    |
| [INLONG-3843](https://github.com/apache/incubator-inlong/issues/3843) | [Bug][Doc] The link to "Contribution Guide for Apache InLong" is invalid                               |
| [INLONG-1839](https://github.com/apache/incubator-inlong/issues/1839) | [Improve][Doc] Supplement deployment document                                                          |
| [INLONG-4351](https://github.com/apache/incubator-inlong/issues/4351) | [Improve][Pom] Update the fastjson to solve the CVEs                                                   |
| [INLONG-4484](https://github.com/apache/incubator-inlong/issues/4484) | [Improve][Pom] Upgrade Spring version from 5.3.19 to 5.3.20                                            |
| [INLONG-3940](https://github.com/apache/incubator-inlong/issues/3940) | [Improve][Pom] Bump spring-core from 5.3.18 to 5.3.19                                                  |
| [INLONG-3935](https://github.com/apache/incubator-inlong/issues/3935) | [Feature][Pom] Remove dependency of testng                                                             |
| [INLONG-3780](https://github.com/apache/incubator-inlong/issues/3780) | [Improve][Pom] Upgrade postgresql due to CVEs                                                          |
| [INLONG-4434](https://github.com/apache/incubator-inlong/issues/4434) | [Bug][Docker] Audit Container caught error when create pulsar topic                                    |
| [INLONG-3898](https://github.com/apache/incubator-inlong/issues/3898) | [Bug][Docker] The log paths of Agent, Audit, Dataproxy and TubeMQ Manager containers are incorrect     |
| [INLONG-3744](https://github.com/apache/incubator-inlong/issues/3744) | [Bug][Docker] Docker images are not pushed in release-* branches                                       |
| [INLONG-3553](https://github.com/apache/incubator-inlong/issues/3553) | [Bug][Docker][K8s] TubeMQ pod fails to start                                                           |
| [INLONG-4144](https://github.com/apache/incubator-inlong/issues/4144) | [Bug][K8s] HTTP error appears on the login dashboard when deploying InLong using helm                  |
| [INLONG-3845](https://github.com/apache/incubator-inlong/issues/3845) | [Bug][K8s] The manager pod fails to start                                                              |
| [INLONG-3635](https://github.com/apache/incubator-inlong/issues/3635) | [Improve][JavaDoc] Fix the Javadoc check style problems for all modules                                |


## Release InLong 1.1.0-incubating - Released (as of 2022-04-15)

### Agent
| ISSUE                                                                 | Summary                                                                                      |
|:----------------------------------------------------------------------|:---------------------------------------------------------------------------------------------|
| [INLONG-3699](https://github.com/apache/incubator-inlong/issues/3699) | [Improve][Agent] Exclude mysql-connector-java                                                |
| [INLONG-3692](https://github.com/apache/incubator-inlong/issues/3692) | [Bug][Agent] There are many agent processes after recovering the directory                   |
| [INLONG-3652](https://github.com/apache/incubator-inlong/issues/3652) | [Improve][Agent] Improve TestBinlogOffsetManager unit test                                   |
| [INLONG-3650](https://github.com/apache/incubator-inlong/issues/3650) | [Bug] Agent fix timeoffset npe                                                               |
| [INLONG-3638](https://github.com/apache/incubator-inlong/issues/3638) | [Bug] Agent and DataProxy can not listen to the 8080 port for Prometheus                     |
| [INLONG-3629](https://github.com/apache/incubator-inlong/issues/3629) | [Improve][Agent] Improve TestFileAgent unit test                                             |
| [INLONG-3620](https://github.com/apache/incubator-inlong/issues/3620) | [Improve] Update the file agent guide document                                               |
| [INLONG-3587](https://github.com/apache/incubator-inlong/issues/3587) | [Bug][Agent]Resource leak                                                                    |
| [INLONG-3476](https://github.com/apache/incubator-inlong/issues/3476) | [Bug][Agent] debezium 1.8.1 has npe                                                          |
| [INLONG-3466](https://github.com/apache/incubator-inlong/issues/3466) | [Feature][Agent] Remove protobuf dependency                                                  |
| [INLONG-3463](https://github.com/apache/incubator-inlong/issues/3463) | [Bug][Agent] Fix unit test of TestTaskWrapper                                                |
| [INLONG-3448](https://github.com/apache/incubator-inlong/issues/3448) | [Improve][Manager] Limit the number of Agent pull tasks                                      |
| [INLONG-3437](https://github.com/apache/incubator-inlong/issues/3437) | [Agent] Sort out the LICENSEs of the third-party components of inlong-agent                  |
| [INLONG-3381](https://github.com/apache/incubator-inlong/issues/3381) | [Feature] Agent wait one minute for dataproxy to prepare topic config                        |
| [INLONG-3349](https://github.com/apache/incubator-inlong/issues/3349) | [Feature] Agent add limitation for job number                                                |
| [INLONG-3335](https://github.com/apache/incubator-inlong/issues/3335) | [Bug] fix agent snapshot mode won't work and optimize jvm parameters                         |
| [INLONG-3326](https://github.com/apache/incubator-inlong/issues/3326) | [Improve][Agent] The unit test for TestTaskWrapper was running too long                      |
| [INLONG-3317](https://github.com/apache/incubator-inlong/issues/3317) | [Improve][Agent] Change agent heartbeat/report interval to 10s                               |
| [INLONG-3308](https://github.com/apache/incubator-inlong/issues/3308) | [Bug][Agent] NPE occurred in parsing deliveryTime                                            |
| [INLONG-3306](https://github.com/apache/incubator-inlong/issues/3306) | [Feature][Agent] Use rocksdb as default db in agent                                          |
| [INLONG-3304](https://github.com/apache/incubator-inlong/issues/3304) | [Bug][Agent] Reader cost too much CPU                                                        |
| [INLONG-3299](https://github.com/apache/incubator-inlong/issues/3299) | [Bug][Agent] Report job result rather than task result                                       |
| [INLONG-3298](https://github.com/apache/incubator-inlong/issues/3298) | [Feature][Agent] Remove dbd implementation                                                   |
| [INLONG-3297](https://github.com/apache/incubator-inlong/issues/3297) | [Feature] Add version control in Agent CommandEntity                                         |
| [INLONG-3274](https://github.com/apache/incubator-inlong/issues/3274) | [Bug][Agent] When Kafka topic is deleted                                                     |
| [INLONG-3271](https://github.com/apache/incubator-inlong/issues/3271) | [Bug][Agent] Cannot get localip in docker.sh                                                 |
| [INLONG-3168](https://github.com/apache/incubator-inlong/issues/3168) | [Bug][Agent] Change the deserialization type from String to byte array                       |
| [INLONG-3148](https://github.com/apache/incubator-inlong/issues/3148) | [Bug][Agent] fix avro serialization                                                          |
| [INLONG-3104](https://github.com/apache/incubator-inlong/issues/3104) | [Bug][Agent] Add default value for kafka consumer group                                      |
| [INLONG-3100](https://github.com/apache/incubator-inlong/issues/3100) | [Bug][Agent] Upgrade Kafka to newest version 3.1.0                                           |
| [INLONG-3099](https://github.com/apache/incubator-inlong/issues/3099) | [Bug][Agent] Duplicate send message when agent receive data                                  |
| [INLONG-3083](https://github.com/apache/incubator-inlong/issues/3083) | [Bug][Agent] Upgrade Scala version in Kafka client                                           |
| [INLONG-3077](https://github.com/apache/incubator-inlong/issues/3077) | [Bug][Agent] FileNotFoundException occurred in unit tests                                    |
| [INLONG-3076](https://github.com/apache/incubator-inlong/issues/3076) | [Bug][Agent] MalformedObjectNameException occurred in unit tests                             |
| [INLONG-3050](https://github.com/apache/incubator-inlong/issues/3050) | [Bug][Agent] Update guava version                                                            |
| [INLONG-3045](https://github.com/apache/incubator-inlong/issues/3045) | [Feature][Agent] Add rocksDb implementation                                                  |
| [INLONG-3027](https://github.com/apache/incubator-inlong/issues/3027) | [Feature][Agent] Upgrade snappy version                                                      |
| [INLONG-3022](https://github.com/apache/incubator-inlong/issues/3022) | [Bug] agent pod start failed                                                                 |
| [INLONG-2985](https://github.com/apache/incubator-inlong/issues/2985) | [Bug][Manager] Fix task type and UTF question for agent                                      |
| [INLONG-2974](https://github.com/apache/incubator-inlong/issues/2974) | [Improve][Manager] Support agent to pull tasks without ip and uuid                           |
| [INLONG-2933](https://github.com/apache/incubator-inlong/issues/2933) | [Bug][Agent][Manager] Change the type of the deliveryTime field from Date to String          |
| [INLONG-2908](https://github.com/apache/incubator-inlong/issues/2908) | [Bug][Agent] Delete uuid around space                                                        |
| [INLONG-2894](https://github.com/apache/incubator-inlong/issues/2894) | [Improve][Agent] Adapt the interface and field modification of the Inlong-Manager            |
| [INLONG-2883](https://github.com/apache/incubator-inlong/issues/2883) | [Bug][Agent] ManagerFetcher throws exception when invoke the Gson.fromJson method            |
| [INLONG-2877](https://github.com/apache/incubator-inlong/issues/2877) | [Bug][Agent] Task position manager throws NPE when send dataproxy ack success                |
| [INLONG-2870](https://github.com/apache/incubator-inlong/issues/2870) | [Bug][Agent] Use base64 to encode snapshot instead of using iso-8859-1                       |
| [INLONG-2860](https://github.com/apache/incubator-inlong/issues/2860) | [Feature][Agent] Create file folder when history file set by user does not exist             |
| [INLONG-2859](https://github.com/apache/incubator-inlong/issues/2859) | [Improve][Agent]  Optimize stopping Kafka tasks                                              |
| [INLONG-2857](https://github.com/apache/incubator-inlong/issues/2857) | [Feature][Agent] Support to destroy task                                                     |
| [INLONG-2851](https://github.com/apache/incubator-inlong/issues/2851) | [Feature] Agent change task id string to integer                                             |
| [INLONG-2826](https://github.com/apache/incubator-inlong/issues/2826) | [Bug] Agent mysql connection should set allowPublicKeyRetrieval to true to support mysql 8.0 |
| [INLONG-2818](https://github.com/apache/incubator-inlong/issues/2818) | [Bug] Agent kafka job and binlog job has jar conflict                                        |
| [INLONG-2790](https://github.com/apache/incubator-inlong/issues/2790) | [Bug][Agent] Log4j cannot be output due to jar conflict                                      |
| [INLONG-2788](https://github.com/apache/incubator-inlong/issues/2788) | [Feature] Agent support sync send data to dataproxy when needed (binlog etc.)                |
| [INLONG-2786](https://github.com/apache/incubator-inlong/issues/2786) | [Feature] Agent jetty server support different job type                                      |
| [INLONG-2779](https://github.com/apache/incubator-inlong/issues/2779) | [Feature] Agent support delete job using jetty server                                        |
| [INLONG-2756](https://github.com/apache/incubator-inlong/issues/2756) | [Improve][Agent] Add more logs when sending data to proxy                                    |
| [INLONG-2754](https://github.com/apache/incubator-inlong/issues/2754) | [Feature][Agent] Add strea metric data to Prometheus and JMX                                 |
| [INLONG-2736](https://github.com/apache/incubator-inlong/issues/2736) | [Bug][Manager] Agent get task from manager error                                             |
| [INLONG-2735](https://github.com/apache/incubator-inlong/issues/2735) | [INLONG][Agent] Fix dataprofile properties                                                   |
| [INLONG-2688](https://github.com/apache/incubator-inlong/issues/2688) | [Feature][Agent] Support task freeze and restart when needed                                 |
| [INLONG-2687](https://github.com/apache/incubator-inlong/issues/2687) | [Feature][Agent] Provide binlog reader ability using debezium engine                         |
| [INLONG-2686](https://github.com/apache/incubator-inlong/issues/2686) | [Feature][Agent] Support snapshot for each task                                              |
| [INLONG-2680](https://github.com/apache/incubator-inlong/issues/2680) | [Improve][Common][Agent] Move common class from inlong-agent module to inlong-common         |
| [INLONG-2675](https://github.com/apache/incubator-inlong/issues/2675) | [Feature][Agent] Fix the problem of common dependcy                                          |
| [INLONG-2666](https://github.com/apache/incubator-inlong/issues/2666) | [Feature][Agent] Support kafka collection                                                    |
| [INLONG-2654](https://github.com/apache/incubator-inlong/issues/2654) | [Feature][Agent] Report heartbeat to manager                                                 |
| [INLONG-2530](https://github.com/apache/incubator-inlong/issues/2530) | [Bug] Agent data time never changes                                                          |
| [INLONG-2285](https://github.com/apache/incubator-inlong/issues/2285) | [Feature][Agent] Make berkeleydb-je an optional dependency of InLong-Agent                   |

### DataProxy
| ISSUE                                                                 | Summary                                                                                      |
|:----------------------------------------------------------------------|:---------------------------------------------------------------------------------------------|
| [INLONG-3638](https://github.com/apache/incubator-inlong/issues/3638) | [Bug] Agent and DataProxy can not listen to the 8080 port for Prometheus                     |
| [INLONG-3573](https://github.com/apache/incubator-inlong/issues/3573) | [Feature][Dataproxy][Audit]Tidy up dependencies between dataproxy                            |
| [INLONG-3520](https://github.com/apache/incubator-inlong/issues/3520) | [Improve][DataProxy] Unify the data directory                                                |
| [INLONG-3459](https://github.com/apache/incubator-inlong/issues/3459) | [Bug] DataProxy start error Due to IllegalArgumentException                                  |
| [INLONG-3436](https://github.com/apache/incubator-inlong/issues/3436) | [DataProxy] Sort out the LICENSEs of the third-party components of inlong-dataproxy          |
| [INLONG-3352](https://github.com/apache/incubator-inlong/issues/3352) | [Bug][Dataproxy]  Dataproxy keeps trying to send messages that have send failed              |
| [INLONG-3291](https://github.com/apache/incubator-inlong/issues/3291) | [Bug][Dataproxy] Default channel config for order message didn't work                        |
| [INLONG-3282](https://github.com/apache/incubator-inlong/issues/3282) | [Feature][DataProxy] Add default order message configuration                                 |
| [INLONG-3250](https://github.com/apache/incubator-inlong/issues/3250) | [Bug][DataProxy] Fix duration error of DataProxy metric                                      |
| [INLONG-3231](https://github.com/apache/incubator-inlong/issues/3231) | [Feature][DataProxy] Change the default topic to an optional configuration                   |
| [INLONG-3183](https://github.com/apache/incubator-inlong/issues/3183) | [Bug][Dataproxy] When creating a producer fails                                              |
| [INLONG-3181](https://github.com/apache/incubator-inlong/issues/3181) | [Improve][DataProxy] Optimizing unit tests and code style                                    |
| [INLONG-3161](https://github.com/apache/incubator-inlong/issues/3161) | [Bug][Dataproxy] When sending order messages, no response message is returned to client      |
| [INLONG-3136](https://github.com/apache/incubator-inlong/issues/3136) | [Feature] DataProxy get NOUPDATE"" configuration from Manager when request md5 is same       |"
| [INLONG-3080](https://github.com/apache/incubator-inlong/issues/3080) | [Bug][DataProxy] Fix dataproxy UT bug add mock of MetricRegister                             |
| [INLONG-3067](https://github.com/apache/incubator-inlong/issues/3067) | [Feature][DataProxy] Upgrading the documentation of using the default Pulsar configuration   |
| [INLONG-3060](https://github.com/apache/incubator-inlong/issues/3060) | [Feature][DataProxy] Use Pulsar configuration by default                                     |
| [INLONG-3058](https://github.com/apache/incubator-inlong/issues/3058) | [Feature][DataProxy] Add some configs while creating Pulsar producer                         |
| [INLONG-3047](https://github.com/apache/incubator-inlong/issues/3047) | [Bug][DataProxy] All common.properties configs are overwritten                               |
| [INLONG-3031](https://github.com/apache/incubator-inlong/issues/3031) | [Bug][Dataproxy] Repeated registration jmx metric bean                                       |
| [INLONG-2962](https://github.com/apache/incubator-inlong/issues/2962) | [Bug][UT] Unit tests throw so many error msg for DataProxy                                   |
| [INLONG-2961](https://github.com/apache/incubator-inlong/issues/2961) | [Improve][DataProxy] Check style error in DataProxy                                          |
| [INLONG-2906](https://github.com/apache/incubator-inlong/issues/2906) | [Improve] Fix conflict defined of mq in Dataproxy and Sort                                   |
| [INLONG-2812](https://github.com/apache/incubator-inlong/issues/2812) | [Improve][DataProxy] Modify flume conf and rename MetaSink                                   |
| [INLONG-2805](https://github.com/apache/incubator-inlong/issues/2805) | [Feature][DataProxy] Add stream config log report                                            |
| [INLONG-2802](https://github.com/apache/incubator-inlong/issues/2802) | [Bug][DataProxy] Update mx.properties local file too often                                   |
| [INLONG-2783](https://github.com/apache/incubator-inlong/issues/2783) | [Bug][DataProxy] Port conflict with pulsar port                                              |
| [INLONG-2781](https://github.com/apache/incubator-inlong/issues/2781) | [Feature][DataProxy] Update netty version to 4.x                                             |
| [INLONG-2719](https://github.com/apache/incubator-inlong/issues/2719) | [Bug][DataProxy] Setting multiple topics for the same groupId doesn't work for Pulsar        |
| [INLONG-2711](https://github.com/apache/incubator-inlong/issues/2711) | [Bug][SDK] Dataproxy-SDK get manager ip list error                                           |
| [INLONG-2607](https://github.com/apache/incubator-inlong/issues/2607) | [Feature][DataProxy] Supports prometheus metric report for PulsarSink                        |
| [INLONG-2568](https://github.com/apache/incubator-inlong/issues/2568) | [Feature][Dataproxy] Support dynamically getting TubeMq config from Manager                  |
| [INLONG-2491](https://github.com/apache/incubator-inlong/issues/2491) | [Feature][Dataproxy] update netty version to 4.1.72.Final and log4j to log4j2                |
| [INLONG-2381](https://github.com/apache/incubator-inlong/issues/2381) | [Feature] DataProxy support Tube sink of PB compression cache message protocol.              |
| [INLONG-2379](https://github.com/apache/incubator-inlong/issues/2379) | [Feature] DataProxy support Pulsar sink of PB compression cache message protocol.            |
| [INLONG-2377](https://github.com/apache/incubator-inlong/issues/2377) | [Feature] DataProxy support PB compression protocol format source.                           |

### Manager
| ISSUE                                                                 | Summary                                                                                                |
|:----------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------|
| [INLONG-3716](https://github.com/apache/incubator-inlong/issues/3716) | [Improve][Manager] Decrease the size of manager plugins                                                |
| [INLONG-3712](https://github.com/apache/incubator-inlong/issues/3712) | [Bug][Manager] validation-api dependency conflict                                                      |
| [INLONG-3710](https://github.com/apache/incubator-inlong/issues/3710) | [Bug][Manager] Update the import package for ObjectMapper                                              |
| [INLONG-3704](https://github.com/apache/incubator-inlong/issues/3704) | [Improve][Manager] change the log level for status report                                              |
| [INLONG-3701](https://github.com/apache/incubator-inlong/issues/3701) | [Improve][Manager] Decrease the size of manager client tools                                           |
| [INLONG-3697](https://github.com/apache/incubator-inlong/issues/3697) | [Improve][Manager]  Replenish manager client examples                                                  |
| [INLONG-3686](https://github.com/apache/incubator-inlong/issues/3686) | [Improve][Manager] Remove the check for the serialization type of file source                          |
| [INLONG-3683](https://github.com/apache/incubator-inlong/issues/3683) | [Improve] Add AUTO_PUSH source stream in Manager                                                       |
| [INLONG-3662](https://github.com/apache/incubator-inlong/issues/3662) | [Improve][Manager] Disable ZooKeeper by default and deserialize file source from stream info           |
| [INLONG-3652](https://github.com/apache/incubator-inlong/issues/3652) | [Improve][Agent] Improve TestBinlogOffsetManager unit test                                             |
| [INLONG-3647](https://github.com/apache/incubator-inlong/issues/3647) | [Bug] Manager caught Sniffer : error while sniffing nodes java.net.ConnectException                    |
| [INLONG-3642](https://github.com/apache/incubator-inlong/issues/3642) | [Improve][Manager] Update start script and log configuration                                           |
| [INLONG-3627](https://github.com/apache/incubator-inlong/issues/3627) | [Improve][Manager] Remove deprecated source_file related classes                                       |
| [INLONG-3603](https://github.com/apache/incubator-inlong/issues/3603) | [Improve][Manager] The serialization type cannot be empty for File source                              |
| [INLONG-3601](https://github.com/apache/incubator-inlong/issues/3601) | [Bug][Manager] Should not create Hive resource when sink type is Kafka                                 |
| [INLONG-3599](https://github.com/apache/incubator-inlong/issues/3599) | [Bug][Manager] Null pointer exception occurred when list file sources                                  |
| [INLONG-3596](https://github.com/apache/incubator-inlong/issues/3596) | [Bug] manager can not start successfully                                                               |
| [INLONG-3589](https://github.com/apache/incubator-inlong/issues/3589) | [Feature][Manager] Add Iceberg sink info for Sort                                                      |
| [INLONG-3580](https://github.com/apache/incubator-inlong/issues/3580) | [Improve][Manager] Remove agentIp param in StreamSource                                                |
| [INLONG-3565](https://github.com/apache/incubator-inlong/issues/3565) | [Improve][Manager] Unified the interface for Flink plugin                                              |
| [INLONG-3550](https://github.com/apache/incubator-inlong/issues/3550) | [Improve][Manager] Add file source in Manager Client                                                   |
| [INLONG-3544](https://github.com/apache/incubator-inlong/issues/3544) | [Feature][Manager] Refactor file source request and response                                           |
| [INLONG-3542](https://github.com/apache/incubator-inlong/issues/3542) | [Feature][Manager] Add Iceberg params and change SQL files                                             |
| [INLONG-3538](https://github.com/apache/incubator-inlong/issues/3538) | [Improve][Manager] Adjust mode of getting sort URL                                                     |
| [INLONG-3537](https://github.com/apache/incubator-inlong/issues/3537) | [Improve][Manager] Remove unused APIs and change Kafka sink params                                     |
| [INLONG-3535](https://github.com/apache/incubator-inlong/issues/3535) | [Improve][Manager] Update JDBC configs and maven pom files                                             |
| [INLONG-3510](https://github.com/apache/incubator-inlong/issues/3510) | [Bug][Manager] When deployed                                                                           |
| [INLONG-3507](https://github.com/apache/incubator-inlong/issues/3507) | [Bug][Manager] Upgrade Elasticsearch jar due to cve                                                    |
| [INLONG-3480](https://github.com/apache/incubator-inlong/issues/3480) | [Bug][Manager] Fix null pointer exception when calling sink method in manager client                   |
| [INLONG-3462](https://github.com/apache/incubator-inlong/issues/3462) | [Feature][Manager] Add test module for manager client                                                  |
| [INLONG-3454](https://github.com/apache/incubator-inlong/issues/3454) | [Improve][Manager] Remove the dependency of nimbusds which was unused                                  |
| [INLONG-3451](https://github.com/apache/incubator-inlong/issues/3451) | [Bug][Manager] Got wrong results when querying tube cluster info                                       |
| [INLONG-3448](https://github.com/apache/incubator-inlong/issues/3448) | [Improve][Manager] Limit the number of Agent pull tasks                                                |
| [INLONG-3438](https://github.com/apache/incubator-inlong/issues/3438) | [Manager] Sort out the LICENSEs of the third-party components of inlong-manager                        |
| [INLONG-3428](https://github.com/apache/incubator-inlong/issues/3428) | [Improve][Manager] Set the default value and code refactor                                             |
| [INLONG-3405](https://github.com/apache/incubator-inlong/issues/3405) | [Improve][Manager] Support generic partition configuration for Hive sink                               |
| [INLONG-3398](https://github.com/apache/incubator-inlong/issues/3398) | [Bug][Manager] Fix database error when saving sink field                                               |
| [INLONG-3397](https://github.com/apache/incubator-inlong/issues/3397) | [Bug][Manager] SQL error when saving sink fields                                                       |
| [INLONG-3383](https://github.com/apache/incubator-inlong/issues/3383) | [Bug][Manager] Fix the null pointer caused by sink field not configured with source field              |
| [INLONG-3376](https://github.com/apache/incubator-inlong/issues/3376) | [Improve][Manager] Support custom field format in client                                               |
| [INLONG-3370](https://github.com/apache/incubator-inlong/issues/3370) | [Improve][Manager] Optimize stream source delete logic                                                 |
| [INLONG-3369](https://github.com/apache/incubator-inlong/issues/3369) | [Improve][Manager] Add StreamSource in list API                                                        |
| [INLONG-3367](https://github.com/apache/incubator-inlong/issues/3367) | [Improve][Manager] Support custom field format                                                         |
| [INLONG-3362](https://github.com/apache/incubator-inlong/issues/3362) | [Improve][Manager] Make Group.updateStatus new Transaction                                             |
| [INLONG-3339](https://github.com/apache/incubator-inlong/issues/3339) | [Bug][Manager] Should update status when agent result was failed                                       |
| [INLONG-3337](https://github.com/apache/incubator-inlong/issues/3337) | [Bug][Manager] Fix API error in manger client                                                          |
| [INLONG-3334](https://github.com/apache/incubator-inlong/issues/3334) | [Improve][Manager] Get source field list from stream field table for Sort                              |
| [INLONG-3323](https://github.com/apache/incubator-inlong/issues/3323) | [Improve][Manager] Add APIs in manager client                                                          |
| [INLONG-3312](https://github.com/apache/incubator-inlong/issues/3312) | [Improve][Manager] Optimize the delete operation for stream source                                     |
| [INLONG-3310](https://github.com/apache/incubator-inlong/issues/3310) | [Improve][Manager] Optimize Pessimistic Lock for select stream sources                                 |
| [INLONG-3301](https://github.com/apache/incubator-inlong/issues/3301) | [Improve][Manager] Remove deprecated classes and tables                                                |
| [INLONG-3300](https://github.com/apache/incubator-inlong/issues/3300) | [Improve][Manager] Optimize the interface of the inlong-stream page                                    |
| [INLONG-3294](https://github.com/apache/incubator-inlong/issues/3294) | [Bug][Manager] Protocol of client and manager is inconsistent                                          |
| [INLONG-3293](https://github.com/apache/incubator-inlong/issues/3293) | [Improve][Manager] Add version controller for stream source                                            |
| [INLONG-3287](https://github.com/apache/incubator-inlong/issues/3287) | [Bug][Manager] Fix heartbeat manager init in Manager Service                                           |
| [INLONG-3280](https://github.com/apache/incubator-inlong/issues/3280) | [Bug][Manager][Dashboard] Update and delete datasource failed                                          |
| [INLONG-3269](https://github.com/apache/incubator-inlong/issues/3269) | [Improve][Manager] Change the request method of list query from GET to POST                            |
| [INLONG-3264](https://github.com/apache/incubator-inlong/issues/3264) | [Improve][Manager] MySQL deadlocked when operating stream source                                       |
| [INLONG-3257](https://github.com/apache/incubator-inlong/issues/3257) | [Bug][Manager][Dashboard] Create CommonServerDB return 404                                             |
| [INLONG-3252](https://github.com/apache/incubator-inlong/issues/3252) | [Bug][Manager] Remove transaction in select method                                                     |
| [INLONG-3246](https://github.com/apache/incubator-inlong/issues/3246) | [Improve][Manager] Add config_failed status after suspend/restart/delete failed for inlong group       |
| [INLONG-3242](https://github.com/apache/incubator-inlong/issues/3242) | [Bug][Manager] ConsumptionMqExtBase cannot be cast to ConsumptionPulsarInfo                            |
| [INLONG-3239](https://github.com/apache/incubator-inlong/issues/3239) | [Bug][Manager] Listing inlong group failed in manager client                                           |
| [INLONG-3228](https://github.com/apache/incubator-inlong/issues/3228) | [Bug][Manager] Deadlock found when trying to get lock                                                  |
| [INLONG-3225](https://github.com/apache/incubator-inlong/issues/3225) | [Improve][Manager] Resolves multiple IPs when querying a DataProxy cluster                             |
| [INLONG-3222](https://github.com/apache/incubator-inlong/issues/3222) | [Improve][Manager] Check source state while operate stop / restart / delete                            |
| [INLONG-3210](https://github.com/apache/incubator-inlong/issues/3210) | [Feature] [Manager] Support group batch query in manager client                                        |
| [INLONG-3209](https://github.com/apache/incubator-inlong/issues/3209) | [Improve][Manager] Optimize timeout handling in manager client                                         |
| [INLONG-3208](https://github.com/apache/incubator-inlong/issues/3208) | [Improve][Manager] Support batch query by inlong group name and inlong group id                        |
| [INLONG-3192](https://github.com/apache/incubator-inlong/issues/3192) | [Improve][Manager] Optimize group state collect in Manager Client                                      |
| [INLONG-3190](https://github.com/apache/incubator-inlong/issues/3190) | [Bug][Manager] Sql error in select source list                                                         |
| [INLONG-3188](https://github.com/apache/incubator-inlong/issues/3188) | [Bug][Manager] Inlong group status was not right after approve                                         |
| [INLONG-3179](https://github.com/apache/incubator-inlong/issues/3179) | [Improve][Manager] Check the source name can not be the same when saving stream source                 |
| [INLONG-3178](https://github.com/apache/incubator-inlong/issues/3178) | [Improve][Manager] Add check param for manager-client                                                  |
| [INLONG-3176](https://github.com/apache/incubator-inlong/issues/3176) | [Bug][Manager] Fix duplicate key  exception in manager client                                          |
| [INLONG-3175](https://github.com/apache/incubator-inlong/issues/3175) | [Feature][Manager] SortService check md5                                                               |
| [INLONG-3166](https://github.com/apache/incubator-inlong/issues/3166) | [Improve][Manager] Replace hdfsDefaultFs and warehouseDir of Hive sink with dataPath field             |
| [INLONG-3160](https://github.com/apache/incubator-inlong/issues/3160) | [Bug][Manager] Deleting stream source failed as the status was not allowed to delete                   |
| [INLONG-3156](https://github.com/apache/incubator-inlong/issues/3156) | [Feature][Manager] Add Shiro interfaces for manager authorization                                      |
| [INLONG-3152](https://github.com/apache/incubator-inlong/issues/3152) | [Improve][Manager] Stop and update operation in initializing state                                     |
| [INLONG-3149](https://github.com/apache/incubator-inlong/issues/3149) | [Improve][Manager] Add async method for inlong group stop/restart/delete                               |
| [INLONG-3146](https://github.com/apache/incubator-inlong/issues/3146) | [Improve][Manager] Optimize FieldType enums                                                            |
| [INLONG-3140](https://github.com/apache/incubator-inlong/issues/3140) | [Bug][Manager] Fix NPE in Manager Client                                                               |
| [INLONG-3134](https://github.com/apache/incubator-inlong/issues/3134) | [Bug][Manager] Save stream sink field error                                                            |
| [INLONG-3112](https://github.com/apache/incubator-inlong/issues/3112) | [Feature][Manager] Support metadata in manager and manager client                                      |
| [INLONG-3101](https://github.com/apache/incubator-inlong/issues/3101) | [Improve][Manager] Support user defined properties in StreamSource                                     |
| [INLONG-3095](https://github.com/apache/incubator-inlong/issues/3095) | [Improve][Manager] Update inlong group info in the complete listeners                                  |
| [INLONG-3090](https://github.com/apache/incubator-inlong/issues/3090) | [Improve][Manager] Add TDMQ_PULSAR type in manager                                                     |
| [INLONG-3089](https://github.com/apache/incubator-inlong/issues/3089) | [Bug][Manager] Create group resource faild after approving one inlong group                            |
| [INLONG-3073](https://github.com/apache/incubator-inlong/issues/3073) | [Improve][Manager] Get MQ cluster by the type and mq_set_name                                          |
| [INLONG-3068](https://github.com/apache/incubator-inlong/issues/3068) | [Improve][Manager] Add autoOffsetReset param for Kafka source                                          |
| [INLONG-3065](https://github.com/apache/incubator-inlong/issues/3065) | [Improve][Manager] Support download plugins from remote address                                        |
| [INLONG-3064](https://github.com/apache/incubator-inlong/issues/3064) | [Improve][Manager] Unify field types of sink and source in manager client                              |
| [INLONG-3062](https://github.com/apache/incubator-inlong/issues/3062) | [Improve][Manager] Merge the data_proxy_cluster table and the third_party_cluster table                |
| [INLONG-3053](https://github.com/apache/incubator-inlong/issues/3053) | [Bug][Manager] Push sort config failed as the mqExtInfo is null in workflow form                       |
| [INLONG-3046](https://github.com/apache/incubator-inlong/issues/3046) | [Bug][Manager] The status was incorrect after approving an inlong group                                |
| [INLONG-3042](https://github.com/apache/incubator-inlong/issues/3042) | [Improve][Manager] Supplements of  binlog allMigration stream                                          |
| [INLONG-3039](https://github.com/apache/incubator-inlong/issues/3039) | [Improve][Manager] Add properties in sinkRequest                                                       |
| [INLONG-3037](https://github.com/apache/incubator-inlong/issues/3037) | [Improve][Manager] Add field mapping support for source and sink in manage client                      |
| [INLONG-3024](https://github.com/apache/incubator-inlong/issues/3024) | [Bug][Manager] Save cluster failed as the token field is too long                                      |
| [INLONG-3017](https://github.com/apache/incubator-inlong/issues/3017) | [Bug][Manager] The interface of OpenAPI does not need authentication                                   |
| [INLONG-3012](https://github.com/apache/incubator-inlong/issues/3012) | [Improve][Manager] Support built-in field for source and sink info                                     |
| [INLONG-3000](https://github.com/apache/incubator-inlong/issues/3000) | [Improve][Manager] Add token field for cluster info                                                    |
| [INLONG-2993](https://github.com/apache/incubator-inlong/issues/2993) | [Bug][Manager] Check whether the mq info is NULL to avoid NPE                                          |
| [INLONG-2992](https://github.com/apache/incubator-inlong/issues/2992) | [Feature][Manager] Support the field mapping feature for Sort                                          |
| [INLONG-2985](https://github.com/apache/incubator-inlong/issues/2985) | [Bug][Manager] Fix task type and UTF question for agent                                                |
| [INLONG-2974](https://github.com/apache/incubator-inlong/issues/2974) | [Improve][Manager] Support agent to pull tasks without ip and uuid                                     |
| [INLONG-2973](https://github.com/apache/incubator-inlong/issues/2973) | [Bug][Manager] Fix get pulsar info from third party cluster table                                      |
| [INLONG-2971](https://github.com/apache/incubator-inlong/issues/2971) | [Improve][Manager] Support stream log collecting in manager client                                     |
| [INLONG-2969](https://github.com/apache/incubator-inlong/issues/2969) | [Bug][Manager] Fix interface of open API cluster                                                       |
| [INLONG-2957](https://github.com/apache/incubator-inlong/issues/2957) | [Improve][Manager] Optimize the cluster management interface                                           |
| [INLONG-2944](https://github.com/apache/incubator-inlong/issues/2944) | [Improve][Manager] Should not change the modify_time when updating the source snapshot                 |
| [INLONG-2939](https://github.com/apache/incubator-inlong/issues/2939) | [Improve][Manager] Support sync message transfer in manager client                                     |
| [INLONG-2934](https://github.com/apache/incubator-inlong/issues/2934) | [Bug][Manager] Manager client occured NPE since not check NULL                                         |
| [INLONG-2933](https://github.com/apache/incubator-inlong/issues/2933) | [Bug][Agent][Manager] Change the type of the deliveryTime field from Date to String                    |
| [INLONG-2930](https://github.com/apache/incubator-inlong/issues/2930) | [Feature][Manager] Add ClickHouse sink support in manager-client                                       |
| [INLONG-2913](https://github.com/apache/incubator-inlong/issues/2913) | [Bug][Manager] Fix get data proxy cluster failed and update inlong group failed                        |
| [INLONG-2912](https://github.com/apache/incubator-inlong/issues/2912) | [Improve][Manager] Add fields for the binlog task                                                      |
| [INLONG-2900](https://github.com/apache/incubator-inlong/issues/2900) | [Bug][Manager] Pulsar topics for DataProxy are inconsistent with topics for Sort                       |
| [INLONG-2898](https://github.com/apache/incubator-inlong/issues/2898) | [Bug][Manager] Fix parse Json exception in manager client                                              |
| [INLONG-2892](https://github.com/apache/incubator-inlong/issues/2892) | [Improve][Manager] Update status of StreamSource after approving the InlongGroup or InlongStream       |
| [INLONG-2890](https://github.com/apache/incubator-inlong/issues/2890) | [Feature][Manager] Support query source list in stream/listAll API                                     |
| [INLONG-2888](https://github.com/apache/incubator-inlong/issues/2888) | [Bug][Manager] Stream source was not deleted when calling delete operate                               |
| [INLONG-2886](https://github.com/apache/incubator-inlong/issues/2886) | [Improve][Manager] Check if the URL is valid to avoid network security attacks                         |
| [INLONG-2873](https://github.com/apache/incubator-inlong/issues/2873) | [Bug][Manager] Fix serialization problem                                                               |
| [INLONG-2869](https://github.com/apache/incubator-inlong/issues/2869) | [Feature][Manager] Support config sync send data for agent and sort                                    |
| [INLONG-2867](https://github.com/apache/incubator-inlong/issues/2867) | [Feature][Manager] Support report the task result and get tasks for the agent                          |
| [INLONG-2862](https://github.com/apache/incubator-inlong/issues/2862) | [Feature][Manager] Startup sort task through the ordinary flink cluster                                |
| [INLONG-2856](https://github.com/apache/incubator-inlong/issues/2856) | [Improve][Manager] Support multi-source and multi-sink in one stream                                   |
| [INLONG-2855](https://github.com/apache/incubator-inlong/issues/2855) | [Feature][Manager] Support use other plugin of Authorization                                           |
| [INLONG-2849](https://github.com/apache/incubator-inlong/issues/2849) | [Bug][Manager] Manager client occurred NPE                                                             |
| [INLONG-2845](https://github.com/apache/incubator-inlong/issues/2845) | [Bug][Manager] Manager client occurred NPE when parsing the ext info                                   |
| [INLONG-2841](https://github.com/apache/incubator-inlong/issues/2841) | [Bug][Manager] New Inlong group cannot invoke the related listeners                                    |
| [INLONG-2839](https://github.com/apache/incubator-inlong/issues/2839) | [Improve][Manager] Add intermediate state for Inlong group                                             |
| [INLONG-2837](https://github.com/apache/incubator-inlong/issues/2837) | [Bug][Manager] Loss update Kafka operation when using manager client to update config                  |
| [INLONG-2830](https://github.com/apache/incubator-inlong/issues/2830) | [Improve][Manager] Support more than one source for a pair of group and streams                        |
| [INLONG-2829](https://github.com/apache/incubator-inlong/issues/2829) | [Feature][Manager] Support for migrating all databases in a database server for the inlong-sort module |
| [INLONG-2827](https://github.com/apache/incubator-inlong/issues/2827) | [Feature][Manager] Support configurable plugin when creating Hive table                                |
| [INLONG-2821](https://github.com/apache/incubator-inlong/issues/2821) | [Improve][Manager] Change the status of the source after receiving the task snapshot                   |
| [INLONG-2815](https://github.com/apache/incubator-inlong/issues/2815) | [Improve][Manager] Optimize Inlong domains for manager-client                                          |
| [INLONG-2808](https://github.com/apache/incubator-inlong/issues/2808) | [Feature][Manager] Support kafka sink in manager client                                                |
| [INLONG-2807](https://github.com/apache/incubator-inlong/issues/2807) | [Improve][Manager] Optimize state defined in manager client                                            |
| [INLONG-2794](https://github.com/apache/incubator-inlong/issues/2794) | [Bug] Manager website should not display port when adding agent job                                    |
| [INLONG-2791](https://github.com/apache/incubator-inlong/issues/2791) | [Improve][Manager] Optimize manager client APIs                                                        |
| [INLONG-2768](https://github.com/apache/incubator-inlong/issues/2768) | [Bug][Manager] The middleware_type not same after creating group                                       |
| [INLONG-2764](https://github.com/apache/incubator-inlong/issues/2764) | [Bug][Manager] Key was duplicate when InlongGroup extList already has the same key                     |
| [INLONG-2760](https://github.com/apache/incubator-inlong/issues/2760) | [Bug][Manager] Delete data grouping exception                                                          |
| [INLONG-2759](https://github.com/apache/incubator-inlong/issues/2759) | [Bug][Manager] InlongGroupController update interface status problem                                   |
| [INLONG-2751](https://github.com/apache/incubator-inlong/issues/2751) | [Bug][Manager] Fix the response code while query failing                                               |
| [INLONG-2743](https://github.com/apache/incubator-inlong/issues/2743) | [Improve][Manager] Support getting inlong workflow error for manager-client module                     |
| [INLONG-2741](https://github.com/apache/incubator-inlong/issues/2741) | [Feature][Manager] Inlong client adds an interface for querying inlong group                           |
| [INLONG-2736](https://github.com/apache/incubator-inlong/issues/2736) | [Bug][Manager] Agent get task from manager error                                                       |
| [INLONG-2734](https://github.com/apache/incubator-inlong/issues/2734) | [Improve][Manager] Support multi serialization type for Sort in Manager                                |
| [INLONG-2732](https://github.com/apache/incubator-inlong/issues/2732) | [Feature][Manager] Support more parameters for Kafka source                                            |
| [INLONG-2723](https://github.com/apache/incubator-inlong/issues/2723) | [Bug][Manager] Manager module occurred exception when startup                                          |
| [INLONG-2720](https://github.com/apache/incubator-inlong/issues/2720) | [Bug][Manager] data_source_cmd_config table not exit                                                   |
| [INLONG-2717](https://github.com/apache/incubator-inlong/issues/2717) | [Improve][Manager] Support middleware of NONE                                                          |
| [INLONG-2715](https://github.com/apache/incubator-inlong/issues/2715) | [Feature][Manager] Support more parameters for the StreamSource entity                                 |
| [INLONG-2714](https://github.com/apache/incubator-inlong/issues/2714) | [Bug][Manager] Create stream_source table failed                                                       |
| [INLONG-2711](https://github.com/apache/incubator-inlong/issues/2711) | [Bug][SDK] Dataproxy-SDK get manager ip list error                                                     |
| [INLONG-2707](https://github.com/apache/incubator-inlong/issues/2707) | [Bug][Manager] Table name and field type was inconsistent in SQL and XML file                          |
| [INLONG-2701](https://github.com/apache/incubator-inlong/issues/2701) | [Bug][Manager] Occurred NPE as the data proxy cluster name is null                                     |
| [INLONG-2700](https://github.com/apache/incubator-inlong/issues/2700) | [Bug][Manager] Inlong group status was incorrect                                                       |
| [INLONG-2699](https://github.com/apache/incubator-inlong/issues/2699) | [Bug][Manager] Field ext_params in table data_proxy_cluster not exits                                  |
| [INLONG-2697](https://github.com/apache/incubator-inlong/issues/2697) | [Bug][Manager] Inlong manager occurred NullpointException                                              |
| [INLONG-2694](https://github.com/apache/incubator-inlong/issues/2694) | [Feature][Manager] Implement services of getSortSource interface                                       |
| [INLONG-2693](https://github.com/apache/incubator-inlong/issues/2693) | [Feature][Manager] Define tables and beans for getSortSource interface                                 |
| [INLONG-2690](https://github.com/apache/incubator-inlong/issues/2690) | [Improve][Manager] Optimize group state in workflow                                                    |
| [INLONG-2689](https://github.com/apache/incubator-inlong/issues/2689) | [Feature][Manager] Support report heartbeat for source agent                                           |
| [INLONG-2682](https://github.com/apache/incubator-inlong/issues/2682) | [Feature][Manager] Add metric and config log report interface                                          |
| [INLONG-2678](https://github.com/apache/incubator-inlong/issues/2678) | [Improve][Manager] Update field type in manager sql script                                             |
| [INLONG-2677](https://github.com/apache/incubator-inlong/issues/2677) | [Feature][Manager] Get MQ cluster Info from database                                                   |
| [INLONG-2676](https://github.com/apache/incubator-inlong/issues/2676) | [Improve][Manager] Support stop / restart / finish stream source task                                  |
| [INLONG-2669](https://github.com/apache/incubator-inlong/issues/2669) | [Improve][Manager] Optimizing source module code structure                                             |
| [INLONG-2662](https://github.com/apache/incubator-inlong/issues/2662) | [Bug][Manager] Fix duplicate listener in manager service                                               |
| [INLONG-2660](https://github.com/apache/incubator-inlong/issues/2660) | [Improve][Manager] Optimize manager state machine                                                      |
| [INLONG-2655](https://github.com/apache/incubator-inlong/issues/2655) | [Bug][Manager] Fix non-null limit in sql file                                                          |
| [INLONG-2653](https://github.com/apache/incubator-inlong/issues/2653) | [Feature][Manager] Support Kafka source in Inong Stream                                                |
| [INLONG-2638](https://github.com/apache/incubator-inlong/issues/2638) | [Bug][Manager] apache_inlong_manager.sql file execution exception                                      |
| [INLONG-2636](https://github.com/apache/incubator-inlong/issues/2636) | [Improve][Manager] Enable sort config generate when zookeeper is disabled;                             |
| [INLONG-2617](https://github.com/apache/incubator-inlong/issues/2617) | [Bug][Manager] After approving the new group                                                           |
| [INLONG-2616](https://github.com/apache/incubator-inlong/issues/2616) | [Improve][Manager] Optimize manager client and some APIs                                               |
| [INLONG-2614](https://github.com/apache/incubator-inlong/issues/2614) | [Feature][Sort] Support array and map data structures in Hive sink and ClickHouse sink                 |
| [INLONG-2612](https://github.com/apache/incubator-inlong/issues/2612) | [Improve][Manager] Unify the domain model of the Manager module                                        |
| [INLONG-2610](https://github.com/apache/incubator-inlong/issues/2610) | [Feature][Manager] Plug-in support for StreamSource                                                    |
| [INLONG-2605](https://github.com/apache/incubator-inlong/issues/2605) | [Improve][Manager] Refactor the manager-workflow module                                                |
| [INLONG-2600](https://github.com/apache/incubator-inlong/issues/2600) | [Improve][Manager] Rename the third party cluster class name and table name                            |
| [INLONG-2588](https://github.com/apache/incubator-inlong/issues/2588) | [Feature][Manager] Support cluster management                                                          |
| [INLONG-2586](https://github.com/apache/incubator-inlong/issues/2586) | [Feature][Manager] Support agent to get task from manager                                              |
| [INLONG-2579](https://github.com/apache/incubator-inlong/issues/2579) | [Feature][Manager] Support stream sink to ClickHouse                                                   |
| [INLONG-2574](https://github.com/apache/incubator-inlong/issues/2574) | [Feature][Manager] Add getSortSource interface for Sort                                                |
| [INLONG-2573](https://github.com/apache/incubator-inlong/issues/2573) | [Feature][Manager] Inlong manager support getSortSource interface                                      |
| [INLONG-2571](https://github.com/apache/incubator-inlong/issues/2571) | [Bug][Manager] Fix unit tests bugs                                                                     |
| [INLONG-2558](https://github.com/apache/incubator-inlong/issues/2558) | [Improve][Manager] Optimizing manager test pattern                                                     |
| [INLONG-2529](https://github.com/apache/incubator-inlong/issues/2529) | [Bug][manager] get NumberFormatException when creating a new stream                                    |
| [INLONG-2512](https://github.com/apache/incubator-inlong/issues/2512) | [Improve] [Manager] Add manager client                                                                 |
| [INLONG-2507](https://github.com/apache/incubator-inlong/issues/2507) | [Bug][Manager] Init sort config failed                                                                 |
| [INLONG-2492](https://github.com/apache/incubator-inlong/issues/2492) | [Feature][Manager] Plug-in support for DataStorage                                                     |
| [INLONG-2491](https://github.com/apache/incubator-inlong/issues/2491) | [Feature][Dataproxy] update netty version to 4.1.72.Final and log4j to log4j2                          |
| [INLONG-2490](https://github.com/apache/incubator-inlong/issues/2490) | [Feature][Manager] Support to startup a single tenant sort job                                         |
| [INLONG-2483](https://github.com/apache/incubator-inlong/issues/2483) | [Feature] Manager provide metadata interface to Dataproxy                                              |
| [INLONG-2414](https://github.com/apache/incubator-inlong/issues/2414) | [Manager] Exclude test jar file during the apache-rat-plugin check                                     |
| [INLONG-2410](https://github.com/apache/incubator-inlong/issues/2410) | [Improve] Inlong Manager support business workflow suspend                                             | 
| [INLONG-2353](https://github.com/apache/incubator-inlong/issues/2353) | [Feature] Tube manager cluster adds support for multi-master configuration                             |
| [INLONG-2286](https://github.com/apache/incubator-inlong/issues/2286) | [Feature][Manager] Put inlong group id in dataflow info for Sort                                       |
| [INLONG-2162](https://github.com/apache/incubator-inlong/issues/2162) | [Feature][Manager] Manager support getSortSourceConfig interface                                       |
| [INLONG-1517](https://github.com/apache/incubator-inlong/issues/1517) | [Feature][Manager] Support sink data to ClickHouse                                                     |

### Audit
| ISSUE                                                                 | Summary                                                                         |
|:----------------------------------------------------------------------|:--------------------------------------------------------------------------------|
| [INLONG-3606](https://github.com/apache/incubator-inlong/issues/3606) | [Bug] lack of the guide for Sort to configure Audit and Flink Plugin            |
| [INLONG-3573](https://github.com/apache/incubator-inlong/issues/3573) | [Feature][Dataproxy][Audit]Tidy up dependencies between dataproxy               |
| [INLONG-3543](https://github.com/apache/incubator-inlong/issues/3543) | [Feature][Audit]Upgrade audit netty version to 4.1.72.Final and log4j to log4j2 |
| [INLONG-3528](https://github.com/apache/incubator-inlong/issues/3528) | [Improve] unify the log directory for manager and audit                         |
| [INLONG-3440](https://github.com/apache/incubator-inlong/issues/3440) | [Audit] Sort out the LICENSEs of the third-party components of inlong-audit     |
| [INLONG-3417](https://github.com/apache/incubator-inlong/issues/3417) | [Improve] add audit configuration for other component docker image              |
| [INLONG-3288](https://github.com/apache/incubator-inlong/issues/3288) | [Improve][Audit] Support TubMQ for website                                      |
| [INLONG-3159](https://github.com/apache/incubator-inlong/issues/3159) | [Feature][Audit] Store support TubeMQ                                           |
| [INLONG-3158](https://github.com/apache/incubator-inlong/issues/3158) | [Feature][Audit] Proxy support TubeMQ                                           |
| [INLONG-3013](https://github.com/apache/incubator-inlong/issues/3013) | [Bug][Audit] Error occurred in started container on tencent eks                 |
| [INLONG-2960](https://github.com/apache/incubator-inlong/issues/2960) | [Bug][Audit] Unit tests error when executing mvn test command                   |
| [INLONG-2640](https://github.com/apache/incubator-inlong/issues/2640) | [Bug][K8s] The Audit Configmap can not be handled as a ConfigMap                |
| [INLONG-2623](https://github.com/apache/incubator-inlong/issues/2623) | [Improve][Audit] Add audit image for docker publish script                      |
| [INLONG-2591](https://github.com/apache/incubator-inlong/issues/2591) | [Bug][Audit] Audit proxy and store process with wrong options                   |
| [INLONG-2549](https://github.com/apache/incubator-inlong/issues/2549) | [Improve] [audit] update audit protobuf field type                              |
| [INLONG-2548](https://github.com/apache/incubator-inlong/issues/2548) | [Bug] update version of lombok from 1.18.20  to 1.18.22                         |
| [INLONG-2540](https://github.com/apache/incubator-inlong/issues/2540) | [agent] create db sql collect task by config from manager                       |
| [INLONG-2538](https://github.com/apache/incubator-inlong/issues/2538) | [TubeMQ] Optimize message write cache logic                                     |
| [INLONG-2535](https://github.com/apache/incubator-inlong/issues/2535) | [Improve][dashboard] Audit module display time in reverse order                 |
| [INLONG-2523](https://github.com/apache/incubator-inlong/issues/2523) | [Improve][Audit] Modify package name according to specification                 |
| [INLONG-2468](https://github.com/apache/incubator-inlong/issues/2468) | [Bug][Audit] CommunicationsException occurred in unit tests                     |
| [INLONG-2441](https://github.com/apache/incubator-inlong/issues/2441) | [Improve] [InLong audit] Modify the version of audit protobuf                   |
| [INLONG-2408](https://github.com/apache/incubator-inlong/issues/2408) | [Audit] protobuf-java dependency has security vulnerability                     |

### Sort
| ISSUE                                                                 | Summary                                                                                                          |
|:----------------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------|
| [INLONG-3682](https://github.com/apache/incubator-inlong/issues/3682) | [Improve] Optimize classification for the sort and usage manual guide                                            |
| [INLONG-3672](https://github.com/apache/incubator-inlong/issues/3672) | [Feature][Sort-standalone] Tidy up dependencies.                                                                 |
| [INLONG-3670](https://github.com/apache/incubator-inlong/issues/3670) | [Improve][Dashboard] Add dashboard plugin docs to official website                                               |
| [INLONG-3668](https://github.com/apache/incubator-inlong/issues/3668) | [Sort-standalone] exclude spotbugs-annotations package                                                           |
| [INLONG-3606](https://github.com/apache/incubator-inlong/issues/3606) | [Bug] lack of the guide for Sort to configure Audit and Flink Plugin                                             |
| [INLONG-3591](https://github.com/apache/incubator-inlong/issues/3591) | [Feature] Support multiple SortTask with the one-on-one relationship of Source and SortTask.                     |
| [INLONG-3589](https://github.com/apache/incubator-inlong/issues/3589) | [Feature][Manager] Add Iceberg sink info for Sort                                                                |
| [INLONG-3546](https://github.com/apache/incubator-inlong/issues/3546) | [Feature] Support reporting metrics by audit-sdk in sort-single-tenant                                           |
| [INLONG-3538](https://github.com/apache/incubator-inlong/issues/3538) | [Improve][Manager] Adjust mode of getting sort URL                                                               |
| [INLONG-3523](https://github.com/apache/incubator-inlong/issues/3523) | [Sort-Flink] Remove lzo-core dependency                                                                          |
| [INLONG-3501](https://github.com/apache/incubator-inlong/issues/3501) | [Sort-standalone] Sort out the LICENSEs of the third-party components of inlong-standalone                       |
| [INLONG-3486](https://github.com/apache/incubator-inlong/issues/3486) | [Bug][Sort] Data with Timestamp/Date type are written wrongly when using parquet format                          |
| [INLONG-3483](https://github.com/apache/incubator-inlong/issues/3483) | [Bug][Sort] Wrong date data in ORC formatted output                                                              |
| [INLONG-3457](https://github.com/apache/incubator-inlong/issues/3457) | [Improve][Sort] Exclude partition fields when writing data with parquet/orc format to hive                       |
| [INLONG-3450](https://github.com/apache/incubator-inlong/issues/3450) | [Feature][SDK] Sort-SDK change ack log from info to debug                                                        |
| [INLONG-3426](https://github.com/apache/incubator-inlong/issues/3426) | [Improve][Sort] Unify the value of binlog event type                                                             |
| [INLONG-3418](https://github.com/apache/incubator-inlong/issues/3418) | [Feature][Sort-Standalone] Upgrade protobuf version to 3.19.4                                                    |
| [INLONG-3414](https://github.com/apache/incubator-inlong/issues/3414) | [Improve][Sort] Set the default value of field includeUpdateBefore for DebeziumDeserializationInfo               |
| [INLONG-3408](https://github.com/apache/incubator-inlong/issues/3408) | [Bug][Sort-Standalone] Replace IP and authentication in configuration example                                    |
| [INLONG-3396](https://github.com/apache/incubator-inlong/issues/3396) | [Feature][Sort] Support multiple dataflow to write the same hive table                                           |
| [INLONG-3378](https://github.com/apache/incubator-inlong/issues/3378) | [Feature] Add configuration example of Sort-standalone(Hive+ElasticSearch)                                       |
| [INLONG-3372](https://github.com/apache/incubator-inlong/issues/3372) | [Bug][Sort] The binlog type is always `INSERT` when the output format is Canal                                   |
| [INLONG-3340](https://github.com/apache/incubator-inlong/issues/3340) | [Bug][Sort-Standalone] ClsSink cannot acquire correct IdConfig and type overflow                                 |
| [INLONG-3332](https://github.com/apache/incubator-inlong/issues/3332) | [Bug][Sort-Standalone] NP when init ClsSink and data race problem when first get ClsIdConfig field list          |
| [INLONG-3329](https://github.com/apache/incubator-inlong/issues/3329) | [Bug][Sort] Wrong class mapping of debezium serialization info                                                   |
| [INLONG-3328](https://github.com/apache/incubator-inlong/issues/3328) | [Feature][Sort-Standalone] Support to load sort-sdk configuration from file                                      |
| [INLONG-3321](https://github.com/apache/incubator-inlong/issues/3321) | [Improve][Sort] Set the correspond field in output to null if the field value in input is null                   |
| [INLONG-3316](https://github.com/apache/incubator-inlong/issues/3316) | [Bug][Sort] Change target sort jar in inlong-distribution module                                                 |
| [INLONG-3285](https://github.com/apache/incubator-inlong/issues/3285) | [Bug][Sort] Elasticsearch jar has security issue                                                                 |
| [INLONG-3260](https://github.com/apache/incubator-inlong/issues/3260) | [Improve][Sort] Change the default semantic to at-least-once when using kafka producer                           |
| [INLONG-3256](https://github.com/apache/incubator-inlong/issues/3256) | [Improve][SDK] Improve sort-sdk log info                                                                         |
| [INLONG-3243](https://github.com/apache/incubator-inlong/issues/3243) | [Feature][Sort-Standalone] Support multiple scenes to request configs                                            |
| [INLONG-3237](https://github.com/apache/incubator-inlong/issues/3237) | [Feature][Sort-Standalone] SdkSource support periodiclly update sdk config and remove expire client.             |
| [INLONG-3218](https://github.com/apache/incubator-inlong/issues/3218) | [Bug][SDK] Sort-SDK may creating multiple duplicate consumers                                                    |
| [INLONG-3206](https://github.com/apache/incubator-inlong/issues/3206) | [Improve][Sort] Do not specify uid for kafka sink in case of transactionalId conflict                            |
| [INLONG-3202](https://github.com/apache/incubator-inlong/issues/3202) | [Feature][SDK] Unify SortSourceConfig of Sort-Sdk and Manager                                                    |
| [INLONG-3173](https://github.com/apache/incubator-inlong/issues/3173) | [Bug][Sort-Standalone] Unify SortClusterConfig in manager and sort-standalone                                    |
| [INLONG-3130](https://github.com/apache/incubator-inlong/issues/3130) | [Feature][Sort] Support extract specified metadata from input data with canal format                             |
| [INLONG-3122](https://github.com/apache/incubator-inlong/issues/3122) | [Bug][Sort-Standalone] Missing TASK_NAME parameter when report to Audit                                          |
| [INLONG-3117](https://github.com/apache/incubator-inlong/issues/3117) | [Bug][Sort-Standalone] Parameter error when invoking SortSdk ack method                                          |
| [INLONG-3116](https://github.com/apache/incubator-inlong/issues/3116) | [Bug][Sort-Standalone] SortSdkSource does not specify the manager url                                            |
| [INLONG-3115](https://github.com/apache/incubator-inlong/issues/3115) | [Bug][Sort-Standalone] optimize the sort-standalone                                                              |
| [INLONG-3113](https://github.com/apache/incubator-inlong/issues/3113) | [Bug][Sort] Date and Time related bugs                                                                           |
| [INLONG-3110](https://github.com/apache/incubator-inlong/issues/3110) | [Improve][Sort] Shade flink-avro to avoid potential conflicts                                                    |
| [INLONG-3108](https://github.com/apache/incubator-inlong/issues/3108) | [TubeMQ] Optimize the implementation of KeepAlive Interface                                                      |
| [INLONG-3086](https://github.com/apache/incubator-inlong/issues/3086) | [Improve][Sort] Check style error in Sort                                                                        |
| [INLONG-3078](https://github.com/apache/incubator-inlong/issues/3078) | [Bug][Sort] TubeClientException occurred in unit tests                                                           |
| [INLONG-3055](https://github.com/apache/incubator-inlong/issues/3055) | [Bug][Sort] Fix bugs of deserializing TransformationInfo json string                                             |
| [INLONG-3054](https://github.com/apache/incubator-inlong/issues/3054) | [Bug][Sort] Start sort task failed as parsing the config error                                                   |
| [INLONG-3053](https://github.com/apache/incubator-inlong/issues/3053) | [Bug][Manager] Push sort config failed as the mqExtInfo is null in workflow form                                 |
| [INLONG-3011](https://github.com/apache/incubator-inlong/issues/3011) | [Feature][Sort] Add deploy command of sort-standalone                                                            |
| [INLONG-2910](https://github.com/apache/incubator-inlong/issues/2910) | [Bug][Sort] Deserialization failure of BuiltInField                                                              |
| [INLONG-2872](https://github.com/apache/incubator-inlong/issues/2872) | [Feature][Sort] Support field mapping when transforming                                                          |
| [INLONG-2847](https://github.com/apache/incubator-inlong/issues/2847) | [Feature][Sort] Support whole-database migration from debezium format to canal format                            |
| [INLONG-2820](https://github.com/apache/incubator-inlong/issues/2820) | [Improve][Sort] Improve the deserialization processing of the update event in DebeziumDeserializationSchema      |
| [INLONG-2816](https://github.com/apache/incubator-inlong/issues/2816) | [Bug][Sort-sdk] java.lang.OutOfMemoryError: Java heap space                                                      |
| [INLONG-2793](https://github.com/apache/incubator-inlong/issues/2793) | [Bug][Sort] Bugs related to hive sink                                                                            |
| [INLONG-2785](https://github.com/apache/incubator-inlong/issues/2785) | [Feature][Sort] Support extract metadata from data with debezium format and write them to data with canal format |
| [INLONG-2774](https://github.com/apache/incubator-inlong/issues/2774) | [Bug][Sort] Fix bugs in sort-single-tenant                                                                       |
| [INLONG-2730](https://github.com/apache/incubator-inlong/issues/2730) | [Feature][Sort] Stand-alone CLS sink reduce the number of AsyncProducerClient                                    |
| [INLONG-2728](https://github.com/apache/incubator-inlong/issues/2728) | [TubeMQ] Optimize the content of statistical metrics                                                             |
| [INLONG-2723](https://github.com/apache/incubator-inlong/issues/2723) | [Bug][Manager] Manager module occurred exception when startup                                                    |
| [INLONG-2721](https://github.com/apache/incubator-inlong/issues/2721) | [Bug][Sort] Fix bugs in HiveSinkInfo                                                                             |
| [INLONG-2684](https://github.com/apache/incubator-inlong/issues/2684) | [Bug][SDK] Sort SDK occurred OOM in unit tests                                                                   |
| [INLONG-2667](https://github.com/apache/incubator-inlong/issues/2667) | [Bug][Sort] Bugs occurred when starting up sort-single-tenant                                                    |
| [INLONG-2659](https://github.com/apache/incubator-inlong/issues/2659) | [Bug][Sort] Could not build the program from JAR file.                                                           |
| [INLONG-2651](https://github.com/apache/incubator-inlong/issues/2651) | [Feature][Sort] Add CLS sink                                                                                     |
| [INLONG-2650](https://github.com/apache/incubator-inlong/issues/2650) | [Feature][Sort] Define sort stand-alone CLS context and config bean                                              |
| [INLONG-2649](https://github.com/apache/incubator-inlong/issues/2649) | [Feature][Sort] Implement default IEvent2LogItemHandler interface                                                |
| [INLONG-2642](https://github.com/apache/incubator-inlong/issues/2642) | [Feature][Sort] Use proxy user to write hive                                                                     |
| [INLONG-2634](https://github.com/apache/incubator-inlong/issues/2634) | [Feature][Sort] Support CHDFS filesystem when using Hive sink                                                    |
| [INLONG-2625](https://github.com/apache/incubator-inlong/issues/2625) | [Feature][Sort] Support extracting data time in deserialization                                                  |
| [INLONG-2614](https://github.com/apache/incubator-inlong/issues/2614) | [Feature][Sort] Support array and map data structures in Hive sink and ClickHouse sink                           |
| [INLONG-2572](https://github.com/apache/incubator-inlong/issues/2572) | [Bug][SDK] Sort sdk cause with name javax/management/MBeanServer in unit tests                                   |
| [INLONG-2561](https://github.com/apache/incubator-inlong/issues/2561) | [Feature][Sort] Update deploy settings of InLong-Sort                                                            |
| [INLONG-2554](https://github.com/apache/incubator-inlong/issues/2554) | [Feature][Sort] Support array and map data structures in ORC writer                                              |
| [INLONG-2526](https://github.com/apache/incubator-inlong/issues/2526) | [Feature][Sort] Support serialization and deserialization of debezium-json formatted data                        |
| [INLONG-2524](https://github.com/apache/incubator-inlong/issues/2524) | [Feature][InLong-Sort] Support deserialization of json                                                           |
| [INLONG-2507](https://github.com/apache/incubator-inlong/issues/2507) | [Bug][Manager] Init sort config failed                                                                           |
| [INLONG-2496](https://github.com/apache/incubator-inlong/issues/2496) | [Feature][Sort]Support COS filesystem when using hive sink                                                       |
| [INLONG-2435](https://github.com/apache/incubator-inlong/issues/2435) | [Feature] Fix Sort-standalone UT problem.                                                                        |
| [INLONG-2413](https://github.com/apache/incubator-inlong/issues/2413) | [Feature][Sort]Support non-partitioned table when using hive sink                                                |
| [INLONG-2382](https://github.com/apache/incubator-inlong/issues/2382) | [Feature] Sort-sdk support Pulsar consumer of PB compression cache message protocol.                             |
| [INLONG-2346](https://github.com/apache/incubator-inlong/issues/2346) | [Feature][InLong-Sort] Support avro and canal formats for sort sink                                              |
| [INLONG-1928](https://github.com/apache/incubator-inlong/issues/1928) | [Feature]Inlong-Sort-Standalone support to consume events from Tube cache clusters.                              |
| [INLONG-1896](https://github.com/apache/incubator-inlong/issues/1896) | [Feature]Inlong-Sort-Standalone support to sort the events to Kafka clusters.                                    |

### TubeMQ
| ISSUE                                                                 | Summary                                                                                         |
|:----------------------------------------------------------------------|:------------------------------------------------------------------------------------------------|
| [INLONG-3644](https://github.com/apache/incubator-inlong/issues/3644) | [Feature][TubeMQ]Upgrade netty version and tidy up dependencies.                                |
| [INLONG-3621](https://github.com/apache/incubator-inlong/issues/3621) | [Improve][TubeMQ] Added cluster switching method and delete cluster support to delete master    |
| [INLONG-3598](https://github.com/apache/incubator-inlong/issues/3598) | [Improve][TubeMQ] Add broker modify method                                                      |
| [INLONG-3568](https://github.com/apache/incubator-inlong/issues/3568) | [Improve][TubeMQ] Nodes realize batch online                                                    |
| [INLONG-3560](https://github.com/apache/incubator-inlong/issues/3560) | [Bug][TubeMQ][InLong] The tubemq pod is invalid                                                 |
| [INLONG-3547](https://github.com/apache/incubator-inlong/issues/3547) | [Bug][TubeMQ] curl and ps commands not found in tubemq docker container                         |
| [INLONG-3514](https://github.com/apache/incubator-inlong/issues/3514) | [Improve][TubeMQ] Add name and IP attributes for query cluster API                              |
| [INLONG-3509](https://github.com/apache/incubator-inlong/issues/3509) | [TubeMQ]Optimize the LICENSE file format of inlong-tubemq third-party components                |
| [INLONG-3477](https://github.com/apache/incubator-inlong/issues/3477) | [TubeMQ] Remove direct reference to log4j1.x                                                    |
| [INLONG-3453](https://github.com/apache/incubator-inlong/issues/3453) | [Feature][TubeMQ] Remove hibernete for tube manager                                             |
| [INLONG-3451](https://github.com/apache/incubator-inlong/issues/3451) | [Bug][Manager] Got wrong results when querying tube cluster info                                |
| [INLONG-3445](https://github.com/apache/incubator-inlong/issues/3445) | [Feature][TubeMQ] Remove the hibernate dependency for the tube manager                          |
| [INLONG-3432](https://github.com/apache/incubator-inlong/issues/3432) | [TubeMQ] Sort out the LICENSEs of the third-party components of inlong-tubemq-manager           |
| [INLONG-3431](https://github.com/apache/incubator-inlong/issues/3431) | [TubeMQ] Sort out the LICENSEs of the third-party components of inlong-tubemq                   |
| [INLONG-3429](https://github.com/apache/incubator-inlong/issues/3429) | [TubeMQ] Add missing adminQueryMasterVersion method                                             |
| [INLONG-3363](https://github.com/apache/incubator-inlong/issues/3363) | [TubeMQ]Added how to use optional BDB components and documentation                              |
| [INLONG-3354](https://github.com/apache/incubator-inlong/issues/3354) | [TubeMQ]Update master.ini configuration guidelines document                                     |
| [INLONG-3348](https://github.com/apache/incubator-inlong/issues/3348) | [TubeMQ] Update protobuf-java version to 3.19.4                                                 |
| [INLONG-3324](https://github.com/apache/incubator-inlong/issues/3324) | [TubeMQ]Optimize CliMetaDataBRU class implementation                                            |
| [INLONG-3290](https://github.com/apache/incubator-inlong/issues/3290) | [TubeMQ]Add the query API for finding the consumption group based on the specified topic        |
| [INLONG-3268](https://github.com/apache/incubator-inlong/issues/3268) | [TubeMQ] Fix some bugs when metadata is saved to ZooKeeper                                      |
| [INLONG-3254](https://github.com/apache/incubator-inlong/issues/3254) | [TubeMQ]Replace the call of MetaDataManager with DefaultMetaDataService                         |
| [INLONG-3201](https://github.com/apache/incubator-inlong/issues/3201) | [Improve][TubeMQ] Improve the cluster query function                                            |
| [INLONG-3154](https://github.com/apache/incubator-inlong/issues/3154) | [TubeMQ] Adjust the Master.ini file reading implementation                                      |
| [INLONG-3143](https://github.com/apache/incubator-inlong/issues/3143) | [TubeMQ] Optimize Metadatamanager class implementation                                          |
| [INLONG-3108](https://github.com/apache/incubator-inlong/issues/3108) | [TubeMQ] Optimize the implementation of KeepAlive Interface                                     |
| [INLONG-3105](https://github.com/apache/incubator-inlong/issues/3105) | [TubeMQ] Add MetaStoreMapper related implementation                                             |
| [INLONG-3095](https://github.com/apache/incubator-inlong/issues/3095) | [Improve][Manager] Update inlong group info in the complete listeners                           |
| [INLONG-3093](https://github.com/apache/incubator-inlong/issues/3093) | [TubeMQ] Optimize the AbsXXXMapperImpl implementation classes                                   |
| [INLONG-3079](https://github.com/apache/incubator-inlong/issues/3079) | [Bug][TubeMQ] An NPE was thrown when starting the Tube-Manager                                  |
| [INLONG-3078](https://github.com/apache/incubator-inlong/issues/3078) | [Bug][Sort] TubeClientException occurred in unit tests                                          |
| [INLONG-3072](https://github.com/apache/incubator-inlong/issues/3072) | [TubeMQ] Output the total count of control block in admin_query_cluster_topic_view              |
| [INLONG-3035](https://github.com/apache/incubator-inlong/issues/3035) | [TubeMQ] Optimize the MetaStoreService implementation class                                     |
| [INLONG-3029](https://github.com/apache/incubator-inlong/issues/3029) | [TubeMQ] Adjust the implementation classes under the impl.bdbimpl package                       |
| [INLONG-3020](https://github.com/apache/incubator-inlong/issues/3020) | [Improve] Format the some code of TubeMQ Go SDK                                                 |
| [INLONG-3015](https://github.com/apache/incubator-inlong/issues/3015) | [Feature][TubeMQ] Add configuration to support the number of reloaded machines per batch        |
| [INLONG-2980](https://github.com/apache/incubator-inlong/issues/2980) | [TubeMQ] Modify the code style problems of the metadata classes                                 |
| [INLONG-2979](https://github.com/apache/incubator-inlong/issues/2979) | [Feature] Tubemq cluster delete provides token code                                             |
| [INLONG-2955](https://github.com/apache/incubator-inlong/issues/2955) | [TubeMQ] Adjust the offsetstorage and zookeeper package paths                                   |
| [INLONG-2915](https://github.com/apache/incubator-inlong/issues/2915) | [TubeMQ] Fix code style issues                                                                  |
| [INLONG-2844](https://github.com/apache/incubator-inlong/issues/2844) | [TubeMQ] Implement the ZooKeeper-based metadata Mapper class                                    |
| [INLONG-2813](https://github.com/apache/incubator-inlong/issues/2813) | [Improve] update the docker config for getting TubeMq config dynamically                        |
| [INLONG-2803](https://github.com/apache/incubator-inlong/issues/2803) | [Improve][TubeMQ] Update the Python client package                                              |
| [INLONG-2776](https://github.com/apache/incubator-inlong/issues/2776) | [TubeMQ] Add metadata backup cli script                                                         |
| [INLONG-2728](https://github.com/apache/incubator-inlong/issues/2728) | [TubeMQ] Optimize the content of statistical metrics                                            |
| [INLONG-2620](https://github.com/apache/incubator-inlong/issues/2620) | [TubeMQ] Add direct write to disk control                                                       |
| [INLONG-2609](https://github.com/apache/incubator-inlong/issues/2609) | [TubeMQ] Fix Javadoc related errors                                                             |
| [INLONG-2603](https://github.com/apache/incubator-inlong/issues/2603) | [TubeMQ] Remove obsolete metric codes                                                           |
| [INLONG-2596](https://github.com/apache/incubator-inlong/issues/2596) | [Improve][TubeMQ] Fix param in the client module and the main class of tubeManager pom is error |
| [INLONG-2569](https://github.com/apache/incubator-inlong/issues/2569) | [TubeMQ] Discarded msgTime value when msgType is empty                                          |
| [INLONG-2555](https://github.com/apache/incubator-inlong/issues/2555) | [TubeMQ] Remove slf4j-log4j12                                                                   |
| [INLONG-2552](https://github.com/apache/incubator-inlong/issues/2552) | [TubeMQ] Add Master metric operation APIs                                                       |
| [INLONG-2538](https://github.com/apache/incubator-inlong/issues/2538) | [TubeMQ] Optimize message write cache logic                                                     |
| [INLONG-2518](https://github.com/apache/incubator-inlong/issues/2518) | [TubeMQ] Adjust the client's metric statistics logic                                            |
| [INLONG-2517](https://github.com/apache/incubator-inlong/issues/2517) | [TubeMQ] Adjust the statistical logic of the Master service status                              |
| [INLONG-2516](https://github.com/apache/incubator-inlong/issues/2516) | [TubeMQ] Optimize Broker's JMX metric interface                                                 |
| [INLONG-2508](https://github.com/apache/incubator-inlong/issues/2508) | [TubeMQ] Add Broker metric operation APIs                                                       |
| [INLONG-2505](https://github.com/apache/incubator-inlong/issues/2505) | [TubeMQ] Add MsgStoreStatsHolder class                                                          |
| [INLONG-2488](https://github.com/apache/incubator-inlong/issues/2488) | [TubeMQ] Optimize MsgFileStatisInfo implementation logic                                        |
| [INLONG-2484](https://github.com/apache/incubator-inlong/issues/2484) | [TubeMQ]Optimize MsgMemStatisInfo implementation logic                                          |
| [INLONG-2480](https://github.com/apache/incubator-inlong/issues/2480) | [TubeMQ] Add WebCallStatsHolder class for Web API call statistics                               |
| [INLONG-2478](https://github.com/apache/incubator-inlong/issues/2478) | [TubeMQ] Optimize GroupCountService logic implementation                                        |
| [INLONG-2474](https://github.com/apache/incubator-inlong/issues/2474) | [TubeMQ] Adjust the statistics of Broker's message service part                                 |
| [INLONG-2451](https://github.com/apache/incubator-inlong/issues/2451) | [TubeMQ] Add Histogram implementation classes                                                   |
| [INLONG-2445](https://github.com/apache/incubator-inlong/issues/2445) | [TubeMQ] Add Gauge and Counter implementation classes                                           |
| [INLONG-2433](https://github.com/apache/incubator-inlong/issues/2433) | [TubeMQ] Abstract metrics' interface                                                            |
| [INLONG-2353](https://github.com/apache/incubator-inlong/issues/2353) | [Feature] Tube manager cluster adds support for multi-master configuration                      |
| [INLONG-2282](https://github.com/apache/incubator-inlong/issues/2282) | [Feature][TubeMQ] Add ZooKeeper as the metadata storage component of TubeMQ                     |
| [INLONG-2204](https://github.com/apache/incubator-inlong/issues/2204) | [Improve][TubeMQ] Optimize the collection of metrics for TubeMQ                                 |
| [INLONG-1655](https://github.com/apache/incubator-inlong/issues/1655) | [Improve] TubeMQ Documents should use English pictures                                          |

### Dashboard
| ISSUE                                                                 | Summary                                                                                     |
|:----------------------------------------------------------------------|:--------------------------------------------------------------------------------------------|
| [INLONG-3684](https://github.com/apache/incubator-inlong/issues/3684) | [Improve][Dashboard]  Add tooltip for stream data type                                      |
| [INLONG-3670](https://github.com/apache/incubator-inlong/issues/3670) | [Improve][Dashboard] Add dashboard plugin docs to official website                          |
| [INLONG-3660](https://github.com/apache/incubator-inlong/issues/3660) | [Improve][Dashboard] Change visible for dataType and dataSeparator                          |
| [INLONG-3631](https://github.com/apache/incubator-inlong/issues/3631) | [Improve][Dashboard] Change the ClickHouse sink page to adapt the Manager module            |
| [INLONG-3617](https://github.com/apache/incubator-inlong/issues/3617) | [Improve][Dashboard] Add params for stream source to adapt Manager module                   |
| [INLONG-3556](https://github.com/apache/incubator-inlong/issues/3556) | [Feature][Dashboard] Add Iceberg params to adapt the Manager module                         |
| [INLONG-3518](https://github.com/apache/incubator-inlong/issues/3518) | [Feature][Dashboard] Support Iceberg Sink                                                   |
| [INLONG-3506](https://github.com/apache/incubator-inlong/issues/3506) | [Feature][Dashboard] Support Kafka                                                          |
| [INLONG-3498](https://github.com/apache/incubator-inlong/issues/3498) | [Improve][Dashboard] Adapt manager streams update API                                       |
| [INLONG-3496](https://github.com/apache/incubator-inlong/issues/3496) | [Improve][Dashboard] Update node version                                                    |
| [INLONG-3492](https://github.com/apache/incubator-inlong/issues/3492) | [Improve][Dashboard] Initialize a npmrc file                                                |
| [INLONG-3471](https://github.com/apache/incubator-inlong/issues/3471) | [Improve][Dashboard] Hive sink adapts to new manager API                                    |
| [INLONG-3442](https://github.com/apache/incubator-inlong/issues/3442) | [Dashboard] Sort out the LICENSEs of the third-party components of inlong-dashboard         |
| [INLONG-3423](https://github.com/apache/incubator-inlong/issues/3423) | [Improve][Dashboard] Add user and password config to binlog source                          |
| [INLONG-3394](https://github.com/apache/incubator-inlong/issues/3394) | [Improve][Dashboard] Remove the stream owner parameter and manage it uniformly by group     |
| [INLONG-3390](https://github.com/apache/incubator-inlong/issues/3390) | [Improve][Dashboard] Text form support date and array dataType                              |
| [INLONG-3341](https://github.com/apache/incubator-inlong/issues/3341) | [Feature][Dashboard] Sink supports plug-in configuration                                    |
| [INLONG-3314](https://github.com/apache/incubator-inlong/issues/3314) | [Improve][Dashboard] Support for new data stream API data formats                           |
| [INLONG-3280](https://github.com/apache/incubator-inlong/issues/3280) | [Bug][Manager][Dashboard] Update and delete datasource failed                               |
| [INLONG-3275](https://github.com/apache/incubator-inlong/issues/3275) | [Improve][Dashboard] Change the request method of list query from GET to POST               |
| [INLONG-3257](https://github.com/apache/incubator-inlong/issues/3257) | [Bug][Manager][Dashboard] Create CommonServerDB return 404                                  |
| [INLONG-3220](https://github.com/apache/incubator-inlong/issues/3220) | [Improve][Dashboard] The binlog configuration aligns the parameters of the managerAPI       |
| [INLONG-3128](https://github.com/apache/incubator-inlong/issues/3128) | [Feature][Dashboard] Make binlog as a source type                                           |
| [INLONG-2997](https://github.com/apache/incubator-inlong/issues/2997) | [Improve][Dashboard] The group execution log needs to distinguish between different states  |
| [INLONG-2996](https://github.com/apache/incubator-inlong/issues/2996) | [Bug][Dashboard] An error is reported after closing the group execution log modal           |
| [INLONG-2895](https://github.com/apache/incubator-inlong/issues/2895) | [Feature][Dashboard] The data source in the data stream supports binlog collection          |
| [INLONG-2861](https://github.com/apache/incubator-inlong/issues/2861) | [Feature][Dashboard] Support common data source module                                      |
| [INLONG-2799](https://github.com/apache/incubator-inlong/issues/2799) | [Bug][Dashboard] Some code specification issues                                             |
| [INLONG-2624](https://github.com/apache/incubator-inlong/issues/2624) | [Improve][Dashboard] Modify the interface and parameters to adapt to changes in the Manager |
| [INLONG-2535](https://github.com/apache/incubator-inlong/issues/2535) | [Improve][dashboard] Audit module display time in reverse order                             |
| [INLONG-2500](https://github.com/apache/incubator-inlong/issues/2500) | [Feature][Dashboard] Adapt to Manager's modification of data storage                        |
| [INLONG-2461](https://github.com/apache/incubator-inlong/issues/2461) | [Bug] a number of CVEs exist for NPMs exist in dashboard                                    |

## Release 1.0.0-incubating - Released (as of 2022-2-1)

### FEATURES:
| ISSUE  | Summary  |
| :---- | :------- |
| [INLONG-2347](https://github.com/apache/incubator-inlong/issues/2347) | [Feature]Support hive sink in sort-single-tenant |
| [INLONG-2334](https://github.com/apache/incubator-inlong/issues/2334) | [Feature][inlong-dataproxy]create pulsar client need support config ioThreads |
| [INLONG-2333](https://github.com/apache/incubator-inlong/issues/2333) | [Feature]Support clickhouse sink in sort-single-tenant |
| [INLONG-2266](https://github.com/apache/incubator-inlong/issues/2266) | [Feature]Support reporting metrics by audit-sdk in sort | 
| [INLONG-2250](https://github.com/apache/incubator-inlong/issues/2250) | [Feature][InLong-Sort] Support Kafka sink in InLong-Sort | 
| [INLONG-2247](https://github.com/apache/incubator-inlong/issues/2247) | Read the consume group offset and store to the specified topic | 
| [INLONG-2236](https://github.com/apache/incubator-inlong/issues/2236) | [Feature]Support iceberg sink in sort-single-tenant |
| [INLONG-2232](https://github.com/apache/incubator-inlong/issues/2232) | Add start and end timestamp of segment |
| [INLONG-2218](https://github.com/apache/incubator-inlong/issues/2218) | [Feature][InLong-DataProxy] Inlong-DataProxy support authentication access pulsar |
| [INLONG-2217](https://github.com/apache/incubator-inlong/issues/2217) | [Feature][InLong-DataProxy] Add TCP protocol client demo and config doc feature |
| [INLONG-2216](https://github.com/apache/incubator-inlong/issues/2216) | [Feature][InLong-DataProxy] Add UDP protocol  client demo and config doc |
| [INLONG-2215](https://github.com/apache/incubator-inlong/issues/2215) | [Feature][InLong-DataProxy] Add http protocol  client demo and config doc |
| [INLONG-2326](https://github.com/apache/incubator-inlong/issues/2326) | [Feature] Inlong-Sort-Standalone support to sort the events to ElasticSearch cluster. |
| [INLONG-2322](https://github.com/apache/incubator-inlong/issues/2322) | [Feature][InLong-Sort] Support json format for kafka sink |
| [INLONG-2301](https://github.com/apache/incubator-inlong/issues/2301) | [Feature]  Support Standalone deployment for InLong |
| [INLONG-2207](https://github.com/apache/incubator-inlong/issues/2207) | [Feature][InLong-Website] Add component about charts |  
| [INLONG-2187](https://github.com/apache/incubator-inlong/issues/2187) | [Feature] Website support audit view |                
| [INLONG-2183](https://github.com/apache/incubator-inlong/issues/2183) | [Feature][InLong-Sort] Bump flink version to 1.13.5 | 
| [INLONG-2176](https://github.com/apache/incubator-inlong/issues/2176) | Add histogram metric and client-side metric output |  
| [INLONG-2170](https://github.com/apache/incubator-inlong/issues/2170) | [Feature] add Inlong-Sort-standalone document. |
| [INLONG-2169](https://github.com/apache/incubator-inlong/issues/2169) | [Feature] [Agent] should provide docs for agent db sql collect |
| [INLONG-2167](https://github.com/apache/incubator-inlong/issues/2167) | [Feature] [Agent] support db SQL collect |
| [INLONG-2164](https://github.com/apache/incubator-inlong/issues/2164) | [Feature] Sort-standalone expose metric data using prometheus HttpServer. |
| [INLONG-2161](https://github.com/apache/incubator-inlong/issues/2161) | [Feature][InLong-Manager] Manager support getClusterConfig  |
| [INLONG-2138](https://github.com/apache/incubator-inlong/issues/2138) | [Feature] Agent should provide docs for programmers to customize their own source or sink | 
| [INLONG-2106](https://github.com/apache/incubator-inlong/issues/2106) | [Feature] DataProxy expose metric data using prometheus HttpServer. | 
| [INLONG-2096](https://github.com/apache/incubator-inlong/issues/2096) | [Feature] DataProxy add InlongGroupId+InlongStreamId metric dimensions in TDSDKSource and TubeSink. |
| [INLONG-2077](https://github.com/apache/incubator-inlong/issues/2077) | [Feature]sort-sdk change pulsar consume mode from listener to fetch |
| [INLONG-2076](https://github.com/apache/incubator-inlong/issues/2076) | [Feature] Tube sink of DataProxy support new Message format. |
| [INLONG-2075](https://github.com/apache/incubator-inlong/issues/2075) | [Feature] SDK Source of DataProxy support new Message format. |
| [INLONG-2058](https://github.com/apache/incubator-inlong/issues/2058) | [Feature] The metric of Sort-standalone append a dimension(minute level) of event time, supporting audit reconciliation of minute level.  |
| [INLONG-2056](https://github.com/apache/incubator-inlong/issues/2056) | [Feature]The metric of DataProxy append a dimension(minute level) of event time, supporting audit reconciliation of minute level. |
| [INLONG-2055](https://github.com/apache/incubator-inlong/issues/2055) | [Feature] [InLong audit] Audit SDK Support real-time report  |
| [INLONG-2054](https://github.com/apache/incubator-inlong/issues/2054) | [Feature] [InLong audit] Audit SDK Support disaster tolerance |
| [INLONG-2053](https://github.com/apache/incubator-inlong/issues/2053) | [Feature] [InLong audit] Audit Web Page Display |
| [INLONG-2051](https://github.com/apache/incubator-inlong/issues/2051) | [Feature] [InLong audit] Add Audit API for Manager |
| [INLONG-2050](https://github.com/apache/incubator-inlong/issues/2050) | [Feature] [InLong audit] Audit Strore for Elasticsearch |
| [INLONG-2045](https://github.com/apache/incubator-inlong/issues/2045) | [Feature]sort-sdk support Prometheus monitor |
| [INLONG-2028](https://github.com/apache/incubator-inlong/issues/2028) | [Feature][CI] Add support for docker build on GitHub Actions |
| [INLONG-1992](https://github.com/apache/incubator-inlong/issues/1992) | [Feature]sort-flink support configurable loader of getting configuration. |
| [INLONG-1950](https://github.com/apache/incubator-inlong/issues/1950) | [Feature] DataProxy add supporting to udp protocol for reporting data |
| [INLONG-1949](https://github.com/apache/incubator-inlong/issues/1949) | [Feature] DataProxy sdk add demo  |
| [INLONG-1931](https://github.com/apache/incubator-inlong/issues/1931) | [Feature]Inlong-Sort-Standalone-readapi support to consume events from inlong cache clusters(tube) |
| [INLONG-1895](https://github.com/apache/incubator-inlong/issues/1895) | [Feature]Inlong-Sort-Standalone support to sort the events to Hive cluster. |
| [INLONG-1894](https://github.com/apache/incubator-inlong/issues/1894) | [Feature]Inlong-Sort-Standalone support JMX metrics listener for pushing. |
| [INLONG-1892](https://github.com/apache/incubator-inlong/issues/1892) | [Feature]Inlong-Sort-Standalone support to consume events from Pulsar cache clusters. |
| [INLONG-1738](https://github.com/apache/incubator-inlong/issues/1738) | [Feature]  InLong audit |

### IMPROVEMENTS:
| ISSUE  | Summary  |
| :---- | :------- |
| [INLONG-2373](https://github.com/apache/incubator-inlong/issues/2373) | [Improve] Refactor of CreateBusinessWorkflow |
| [INLONG-2358](https://github.com/apache/incubator-inlong/issues/2358) | [InLong audit] modify audit proxy name of introduction |               
| [INLONG-2352](https://github.com/apache/incubator-inlong/issues/2352) | [InLong audit] add audit introduction |                                
| [INLONG-2349](https://github.com/apache/incubator-inlong/issues/2349) | [inlong-dataproxy] change log file name from flum.log to dataproxy.log |
| [INLONG-2331](https://github.com/apache/incubator-inlong/issues/2331) | [Improve] Extract connector related code to sort-connector module | 
| [INLONG-2329](https://github.com/apache/incubator-inlong/issues/2329) | [Improve][inlong-dataproxy-sdk] asyncSendMessage in sender.java can be optimized to reduce the number of invalid objects |
| [INLONG-2297](https://github.com/apache/incubator-inlong/issues/2297) | [Improve][agent] support audit for source and sink |
| [INLONG-2296](https://github.com/apache/incubator-inlong/issues/2296) | Added lag consumption log |
| [INLONG-2294](https://github.com/apache/incubator-inlong/issues/2294) | Rename the variable BROKER_VERSION to SERVER_VERSION |
| [INLONG-2279](https://github.com/apache/incubator-inlong/issues/2279) | [Improve] Supplement TubeMQ's  Javadoc information |
| [INLONG-2274](https://github.com/apache/incubator-inlong/issues/2274) | [Improve][Manager] Supports configuring whether to create a Hive database or table |
| [INLONG-2271](https://github.com/apache/incubator-inlong/issues/2271) | [Improve] rename the TDMsg to InLongMsg |
| [INLONG-2258](https://github.com/apache/incubator-inlong/issues/2258) | [Improve][dashboard] Audit page support auto select datastream |
| [INLONG-2254](https://github.com/apache/incubator-inlong/issues/2254) | Add historical offset query API |
| [INLONG-2245](https://github.com/apache/incubator-inlong/issues/2245) | [Improve] Supports database-level isolation of audit queries | 
| [INLONG-2229](https://github.com/apache/incubator-inlong/issues/2229) | [Improve] Manager support pulsar authentification  | 
| [INLONG-2225](https://github.com/apache/incubator-inlong/issues/2225) | [Improve][InLong-Dashboard] Audit module support i18n | 
| [INLONG-2220](https://github.com/apache/incubator-inlong/issues/2220) | [Improve] move dataproxy-sdk to inlong-sdk | 
| [INLONG-2210](https://github.com/apache/incubator-inlong/issues/2210) | [Improve] package `inlong-manager-web` as `inlong-manager` | 
| [INLONG-2200](https://github.com/apache/incubator-inlong/issues/2200) | [Feature] DataProxy add supporting to http protocol for reporting data |  
| [INLONG-2196](https://github.com/apache/incubator-inlong/issues/2196) | [Improve] move website to dashboard |         
| [INLONG-2193](https://github.com/apache/incubator-inlong/issues/2193) | [Improve] optimize inlong manager structure | 
| [INLONG-2160](https://github.com/apache/incubator-inlong/issues/2160) | [Improve] Time format conversion using DateTimeFormatter |  
| [INLONG-2151](https://github.com/apache/incubator-inlong/issues/2151) | [Improve] Add time and sort statistics by topic | 
| [INLONG-2133](https://github.com/apache/incubator-inlong/issues/2133) | Update year to 2022 | 
| [INLONG-2126](https://github.com/apache/incubator-inlong/issues/2126) | [Improve]prepare_env.sh can be merged into dataproxy-start.sh，as a InLong beginner maybe forgot this step |
| [INLONG-2122](https://github.com/apache/incubator-inlong/issues/2122) | [Improve] Send a dev notifications email for issue status |
| [INLONG-2119](https://github.com/apache/incubator-inlong/issues/2119) | [Improve][Website][CI] Add support for building inlong website when building or testing project |
| [INLONG-2117](https://github.com/apache/incubator-inlong/issues/2117) | [Improve][agent] optimize class name  |
| [INLONG-2116](https://github.com/apache/incubator-inlong/issues/2116) | [Improve][Website] Improve the README document |
| [INLONG-2107](https://github.com/apache/incubator-inlong/issues/2107) | [Improve] [InLong Manager] remove gson and json-simple from dependency |  
| [INLONG-2103](https://github.com/apache/incubator-inlong/issues/2103) | [Improve] update the definition of Apache InLong | 
| [INLONG-2073](https://github.com/apache/incubator-inlong/issues/2073) | [Improve] [InLong agent] remove spring 2.5.6 from dependencyManagement |
| [INLONG-2072](https://github.com/apache/incubator-inlong/issues/2072) | [Improve] update the deployment guide for sort |
| [INLONG-2070](https://github.com/apache/incubator-inlong/issues/2070) | [Improve] update the default pulsar demo configuration for dataproxy  |
| [INLONG-1944](https://github.com/apache/incubator-inlong/issues/1944) | Bumped version to 0.13.0-incubating-SNAPSHOT for the master branch |

### BUG FIXES:
| ISSUE  | Summary  |
| :---- | :------- |
| [INLONG-2371](https://github.com/apache/incubator-inlong/issues/2371) | [Bug][inlong-dataproxy] monitorIndex should not use msgid for key,it affects performance |
| [INLONG-2361](https://github.com/apache/incubator-inlong/issues/2361) | [Bug] audit have no data |
| [INLONG-2344](https://github.com/apache/incubator-inlong/issues/2344) | [Bug][InLong-Sort] Kafka sink ut failed under multithread compiling | 
| [INLONG-2338](https://github.com/apache/incubator-inlong/issues/2338) | [Bug] agent can not get dataproxy for docker-compose environment |
| [INLONG-2336](https://github.com/apache/incubator-inlong/issues/2336) | [Bug][agent] the manager fetcher thread was shielded |
| [INLONG-2288](https://github.com/apache/incubator-inlong/issues/2288) | [Bug] sort-flink task catches an NPE |
| [INLONG-2264](https://github.com/apache/incubator-inlong/issues/2264) | [Bug] DataProxy get metric value with error JMX ObjectName |
| [INLONG-2263](https://github.com/apache/incubator-inlong/issues/2263) | [Bug] SortStandalone get metric value with error JMX ObjectName |
| [INLONG-2252](https://github.com/apache/incubator-inlong/issues/2252) | [Bug] Remove <> character in sort-standalone quick_start.md. |     
| [INLONG-2242](https://github.com/apache/incubator-inlong/issues/2242) | [BUG][manager] table field incorrect: db_collector_detail_task, 'sql' should be 'sql_statement' |
| [INLONG-2237](https://github.com/apache/incubator-inlong/issues/2237) | [Bug] call audit query interface error |
| [INLONG-2230](https://github.com/apache/incubator-inlong/issues/2230) | [Bug]  manager started get jackson error |    
| [INLONG-2227](https://github.com/apache/incubator-inlong/issues/2227) | [Bug] build failed for dataproxy-sdk |
| [INLONG-2224](https://github.com/apache/incubator-inlong/issues/2224) | [Bug][inlong-DataProxy] Source receive one message will be send to pulsar twice when config both memery channel and file channel | 
| [INLONG-2202](https://github.com/apache/incubator-inlong/issues/2202) | [Bug] add lower version log4j exclusion in sort-standalone pom.xml |   
| [INLONG-2199](https://github.com/apache/incubator-inlong/issues/2199) | [Bug][inlong-audit][audit-source] one message will put tow channel, and store two message | 
| [INLONG-2191](https://github.com/apache/incubator-inlong/issues/2191) | [Bug][inlong-audit][audit-source] requestId is not set in response message |
| [INLONG-2190](https://github.com/apache/incubator-inlong/issues/2190) | [Bug][inlong-audit][audit-store] can not started by start shell |
| [INLONG-2174](https://github.com/apache/incubator-inlong/issues/2174) | [Bug]Clickhouse sink can cause data loss when checkpointing | 
| [INLONG-2155](https://github.com/apache/incubator-inlong/issues/2155) | [Bug][Manager] Some unit tests running failed | 
| [INLONG-2148](https://github.com/apache/incubator-inlong/issues/2148) | [Bug][sort]Pattern used for extracting clickhouse metadata is not compatible with some versions of clickhouse |
| [INLONG-2143](https://github.com/apache/incubator-inlong/issues/2143) | [Bug][sort] caught a NoClassDefFoundError exception |
| [INLONG-2137](https://github.com/apache/incubator-inlong/issues/2137) | [Bug] version 0.12.0 cannot pass UT |
| [INLONG-2130](https://github.com/apache/incubator-inlong/issues/2130) | [Bug] inlong-sort occurs `ClassNotFoundException: og.objenesis..ClassUtils` | 
| [INLONG-2113](https://github.com/apache/incubator-inlong/issues/2113) | [Bug][Docker] Audit docker image build failed |
| [INLONG-2098](https://github.com/apache/incubator-inlong/issues/2098) | [Bug] agent can not restart successfully |
| [INLONG-2097](https://github.com/apache/incubator-inlong/issues/2097) | [Bug][Docker] error while building tubemq image |
| [INLONG-2094](https://github.com/apache/incubator-inlong/issues/2094) | [Bug] summit job failed after enabling Prometheus |
| [INLONG-2089](https://github.com/apache/incubator-inlong/issues/2089) | [Bug]tubemq-manager throws error when starting:   java.lang.ClassNotFoundException: javax.validation.ClockProvider |
| [INLONG-2087](https://github.com/apache/incubator-inlong/issues/2087) | [Bug] Miss a "-p" flag before 2181:2181 in the command "Start Standalone Container" |
| [INLONG-2085](https://github.com/apache/incubator-inlong/issues/2085) | [Bug] Solve the incubator-inlong-website Compilation failure problem |
| [INLONG-2084](https://github.com/apache/incubator-inlong/issues/2084) | [Bug]A bug in the Go SDK demo, and the API result class is not clear enough |
| [INLONG-2082](https://github.com/apache/incubator-inlong/issues/2082) | [Bug] file agent collector file failed |
| [INLONG-2080](https://github.com/apache/incubator-inlong/issues/2080) | [Bug] file agent send file failed |
| [INLONG-2078](https://github.com/apache/incubator-inlong/issues/2078) | [Bug] create pulsar subscription failed |
| [INLONG-2068](https://github.com/apache/incubator-inlong/issues/2068) | [Bug] the class name in dataproxy stop.sh  is wrong |
| [INLONG-2066](https://github.com/apache/incubator-inlong/issues/2066) | Each message will be consumed twice.[Bug] |
| [INLONG-2064](https://github.com/apache/incubator-inlong/issues/2064) | [Bug]master branch, tubemq-manager  module occurs: package Java.validation.constraints not exists |
| [INLONG-2061](https://github.com/apache/incubator-inlong/issues/2061) | [Bug][Office-Website] The homepage structure image error |
| [INLONG-1989](https://github.com/apache/incubator-inlong/issues/1989) | [Bug]some font of " DataProxy-SDK architecture " page  incorrectly |
| [INLONG-1342](https://github.com/apache/incubator-inlong/issues/1342) | [Bug] Create tube consumer group failed where the group exists |  

## Release 0.12.0-incubating - Released (as of 2021-12-22)

### FEATURES:
| ISSUE  | Summary  |
| :---- | :------- |
| [INLONG-1310](https://github.com/apache/incubator-inlong/issues/1310) | [Feature] [Feature] Support Pulsar
| [INLONG-1711](https://github.com/apache/incubator-inlong/issues/1711) | [feature] website support process pulsar dataflow
| [INLONG-1712](https://github.com/apache/incubator-inlong/issues/1712) | [Feature][agent] Add agent metric statistics
| [INLONG-1722](https://github.com/apache/incubator-inlong/issues/1722) | [Feature] Add IssueNavigationLink for IDEA
| [INLONG-1725](https://github.com/apache/incubator-inlong/issues/1725) | [Feature] [InLong-Manager] Modify bid and tid (or dsid) to inlongGroupId and inlongStreamId
| [INLONG-1726](https://github.com/apache/incubator-inlong/issues/1726) | [Feature] [InLong-Website] Adapt the Manager module and modify the field names of bid and dsid
| [INLONG-1732](https://github.com/apache/incubator-inlong/issues/1732) | [Feature] [InLong-Agent] Modify bid and tid to inlongGroupId and inlongStreamId
| [INLONG-1738](https://github.com/apache/incubator-inlong/issues/1738) | [Feature] InLong audit
| [INLONG-1764](https://github.com/apache/incubator-inlong/issues/1764) | [Feature]Use black for code block background style
| [INLONG-1768](https://github.com/apache/incubator-inlong/issues/1768) | [Feature] Adding consume type that allows partition assign from the client
| [INLONG-1785](https://github.com/apache/incubator-inlong/issues/1785) | [Feature] add 0.11.0 release article for blog
| [INLONG-1786](https://github.com/apache/incubator-inlong/issues/1786) | [Feature]Inlong-common provide monitoring indicator reporting mechanism with JMX, user can implement the code that read the metrics and report to user-defined monitor system.
| [INLONG-1791](https://github.com/apache/incubator-inlong/issues/1791) | [Feature][InLong-Manager] Some bid fields have not been modified
| [INLONG-1796](https://github.com/apache/incubator-inlong/issues/1796) | [Feature]DataProxy support monitor indicator with JMX.
| [INLONG-1809](https://github.com/apache/incubator-inlong/issues/1809) | [Feature] Adjust the font style of the official home page
| [INLONG-1814](https://github.com/apache/incubator-inlong/issues/1814) | [Feature] Show document file subdirectories
| [INLONG-1817](https://github.com/apache/incubator-inlong/issues/1817) | [Feature][InLong-Manager] Workflow supports data stream for Pulsar
| [INLONG-1821](https://github.com/apache/incubator-inlong/issues/1821) | [INLONG-810] Sort Module Support store data to ApacheDoris
| [INLONG-1826](https://github.com/apache/incubator-inlong/issues/1826) | [Feature] Use jmx metric defined in inlong-common
| [INLONG-1830](https://github.com/apache/incubator-inlong/issues/1830) | [Feature] Add a star reminder
| [INLONG-1833](https://github.com/apache/incubator-inlong/issues/1833) | [Feature] Add Team button to the navigation bar
| [INLONG-1840](https://github.com/apache/incubator-inlong/issues/1840) | [Feature] add a Welcome committer articles to official website blog
| [INLONG-1847](https://github.com/apache/incubator-inlong/issues/1847) | [Feature][InLong-Manager] Add consumption APIs for Pulsar MQ
| [INLONG-1849](https://github.com/apache/incubator-inlong/issues/1849) | [Feature][InLong-Manager] Push Sort config for Pulsar
| [INLONG-1851](https://github.com/apache/incubator-inlong/issues/1851) | [Feature]TubeMQ supports monitoring indicators with JMX.
| [INLONG-1853](https://github.com/apache/incubator-inlong/issues/1853) | [Feature] Agent should provide docs for jmx metrics
| [INLONG-1854](https://github.com/apache/incubator-inlong/issues/1854) | [Feature] Agent Rmi args should be added in agent-env.sh
| [INLONG-1856](https://github.com/apache/incubator-inlong/issues/1856) | [Feature] Add a news tab on the official website
| [INLONG-1867](https://github.com/apache/incubator-inlong/issues/1867) | [Feature] Add a user column display to the official website
| [INLONG-1873](https://github.com/apache/incubator-inlong/issues/1873) | [Feature] refactor the structure of the document for the official website
| [INLONG-1874](https://github.com/apache/incubator-inlong/issues/1874) | [Feature] Add contact information and common links at the bottom of the homepage of the official website
| [INLONG-1878](https://github.com/apache/incubator-inlong/issues/1878) | [Feature] Optimize user display page layout style
| [INLONG-1901](https://github.com/apache/incubator-inlong/issues/1901) | [Feature] Optimize the layout of the user display page
| [INLONG-1910](https://github.com/apache/incubator-inlong/issues/1910) | [Feature]Inlong-Sort-Standalone-sort-sdk support to consume events from inlong cache clusters(pulsar)
| [INLONG-1926](https://github.com/apache/incubator-inlong/issues/1926) | [Feature]Inlong-Sort-Standalone support JMX metrics listener for pulling.
| [INLONG-1938](https://github.com/apache/incubator-inlong/issues/1938) | [Feature] DataProxy send message to multi-pulsar cluster conf demo
| [INLONG-2002](https://github.com/apache/incubator-inlong/issues/2002) | [Feature]creating data access with pulsar, users should be able to change the ensemble param

### IMPROVEMENTS:
| ISSUE  | Summary  |
| :---- | :------- |
| [INLONG-1708](https://github.com/apache/incubator-inlong/issues/1708) | [Improve] Add restrict of @author and Chinese in java file
| [INLONG-1729](https://github.com/apache/incubator-inlong/issues/1729) | [Improve] Avoid using constant value as version when referencing other modules
| [INLONG-1739](https://github.com/apache/incubator-inlong/issues/1739) | [Improve] Optimization of TubeMQ SDK usage demo
| [INLONG-1740](https://github.com/apache/incubator-inlong/issues/1740) | [Improve] change bid/tid to be more identifiable
| [INLONG-1746](https://github.com/apache/incubator-inlong/issues/1746) | [improve] the log4j properties for dataproxy contains some useless code and some class name are incorrect
| [INLONG-1756](https://github.com/apache/incubator-inlong/issues/1756) | [Improve] Use metadata to manage data sources and flow fields
| [INLONG-1772](https://github.com/apache/incubator-inlong/issues/1772) | [Improve]Adjust the ProcessResult class implementation
| [INLONG-1798](https://github.com/apache/incubator-inlong/issues/1798) | [Improve]RestTemplate does not read configuration from the configuration file
| [INLONG-1802](https://github.com/apache/incubator-inlong/issues/1802) | [Improve] Optimize document version management
| [INLONG-1808](https://github.com/apache/incubator-inlong/issues/1808) | [Improve] Optimize document of DataProxy about monitor metric.
| [INLONG-1810](https://github.com/apache/incubator-inlong/issues/1810) | [Improve] update the architecture for office-website
| [INLONG-1811](https://github.com/apache/incubator-inlong/issues/1811) | [Improve] Modify the architecture diagram of README.md
| [INLONG-1815](https://github.com/apache/incubator-inlong/issues/1815) | [Improve][translation] the blog of the 0.11.0 release should be translated into English
| [INLONG-1819](https://github.com/apache/incubator-inlong/issues/1819) | Optimize GC parameter configuration in TubeMQ's env.sh file
| [INLONG-1822](https://github.com/apache/incubator-inlong/issues/1822) | Optimize the table formatting in some MD documents
| [INLONG-1824](https://github.com/apache/incubator-inlong/issues/1824) | Refine the how-to-vote-a-committer-ppmc.md
| [INLONG-1857](https://github.com/apache/incubator-inlong/issues/1857) | [Improve] Adjust the content of the Disclaimer and Events column
| [INLONG-1859](https://github.com/apache/incubator-inlong/issues/1859) | [Improve][InLong-Manager] Remove duplicate SQL files
| [INLONG-1861](https://github.com/apache/incubator-inlong/issues/1861) | [Improve] Update document for docker-compose
| [INLONG-1863](https://github.com/apache/incubator-inlong/issues/1863) | [Improve][TubeMQ] repHelperHost for master should be exposed in configuration
| [INLONG-1864](https://github.com/apache/incubator-inlong/issues/1864) | [Improve] Agent Website doc contains a typo
| [INLONG-1865](https://github.com/apache/incubator-inlong/issues/1865) | [Improve] There are several errors in TubeMQ's guidance document
| [INLONG-1877](https://github.com/apache/incubator-inlong/issues/1877) | [Improve] improve the document's format for the office website
| [INLONG-1886](https://github.com/apache/incubator-inlong/issues/1886) | [Improve][InLong-Manager] Refactor and delete unused entities
| [INLONG-1916](https://github.com/apache/incubator-inlong/issues/1916) | [Improve][website] modify the Business to InLong Group
| [INLONG-1934](https://github.com/apache/incubator-inlong/issues/1934) | [Improve] update the image of the hive example after the bid changed
| [INLONG-1935](https://github.com/apache/incubator-inlong/issues/1935) | [Improve] package the SQL file for the manager
| [INLONG-1939](https://github.com/apache/incubator-inlong/issues/1939) | [Improve] add basic concepts for InLong
| [INLONG-1952](https://github.com/apache/incubator-inlong/issues/1952) | [Improve] Update the office website structure image
| [INLONG-1987](https://github.com/apache/incubator-inlong/issues/1987) | [Improve] Add function comment information in TubeMQ
| [INLONG-2017](https://github.com/apache/incubator-inlong/issues/2017) | [Improve] Add more guide documents for Pulsar

### BUG FIXES:
| ISSUE  | Summary  |
| :---- | :------- |
| [INLONG-1706](https://github.com/apache/incubator-inlong/issues/1706) | [Bug] there are some incorrect expressions for issues tracking in the how-to-contribute file
| [INLONG-1716](https://github.com/apache/incubator-inlong/issues/1716) | [Bug][manager] can not login successfully
| [INLONG-1731](https://github.com/apache/incubator-inlong/issues/1731) | [Bug] release template has sth wrong with KEY URL
| [INLONG-1745](https://github.com/apache/incubator-inlong/issues/1745) | [Bug]TubeMQ HTTP API download link cannot be opened
| [INLONG-1752](https://github.com/apache/incubator-inlong/issues/1752) | [Bug] The official website action failed to build, it may be that the node version needs to be upgraded
| [INLONG-1754](https://github.com/apache/incubator-inlong/issues/1754) | [Bug] confused navigation in download page
| [INLONG-1755](https://github.com/apache/incubator-inlong/issues/1755) | [Bug] Broken link in the ANNOUNCE email template
| [INLONG-1769](https://github.com/apache/incubator-inlong/issues/1769) | [Bug][TubeMQ]Util function SpitToMap in Go SDK panic
| [INLONG-1771](https://github.com/apache/incubator-inlong/issues/1771) | [Bug] Website readme error
| [INLONG-1776](https://github.com/apache/incubator-inlong/issues/1776) | [Bug] Get error while parse td msg with go client
| [INLONG-1777](https://github.com/apache/incubator-inlong/issues/1777) | [Bug][TubeMQ]Go SDK failed to parse tdmsg v4
| [INLONG-1781](https://github.com/apache/incubator-inlong/issues/1781) | [Bug] Get uncorrect time value of attributes
| [INLONG-1783](https://github.com/apache/incubator-inlong/issues/1783) | [Bug] Topic filters config has't any effects
| [INLONG-1828](https://github.com/apache/incubator-inlong/issues/1828) | [Bug]parse message error: invalid default attr's msg Length
| [INLONG-1876](https://github.com/apache/incubator-inlong/issues/1876) | [Bug] office website build failed
| [INLONG-1897](https://github.com/apache/incubator-inlong/issues/1897) | [Bug][Website] form cannot use chain name
| [INLONG-1898](https://github.com/apache/incubator-inlong/issues/1898) | [Bug][Website] The error of the person responsible for the second edit of the new consumption
| [INLONG-1902](https://github.com/apache/incubator-inlong/issues/1902) | [Bug][Website] Access create params error
| [INLONG-1911](https://github.com/apache/incubator-inlong/issues/1911) | [Bug] Some questions about the metric implementation in the common module
| [INLONG-1915](https://github.com/apache/incubator-inlong/issues/1915) | [Bug] tubemq master can not start
| [INLONG-1919](https://github.com/apache/incubator-inlong/issues/1919) | [Bug] TubeMQ HTTP API xls can not download
| [INLONG-1920](https://github.com/apache/incubator-inlong/issues/1920) | [Bug]Failed to start up MultiSession factory by following the demo code
| [INLONG-1953](https://github.com/apache/incubator-inlong/issues/1953) | [Bug]It can not be submitted when I create data access using file data source
| [INLONG-1954](https://github.com/apache/incubator-inlong/issues/1954) | [Bug]inlong-sort does not support pulsar ???
| [INLONG-1955](https://github.com/apache/incubator-inlong/issues/1955) | [Bug]Source data fields' type are all mapped to tinyint, and can not be modified
| [INLONG-1958](https://github.com/apache/incubator-inlong/issues/1958) | [Bug]Avoid the security risks of log4j package
| [INLONG-1966](https://github.com/apache/incubator-inlong/issues/1966) | [Bug][InLong-Manager] The stream name field is not required, but error occurs when create a data stream with name field not filled
| [INLONG-1967](https://github.com/apache/incubator-inlong/issues/1967) | [Bug][InLong-Manager] Cannot create the Pulsar subscription
| [INLONG-1973](https://github.com/apache/incubator-inlong/issues/1973) | [Bug]with the demo conf of dataproxy, the app can not start rightly
| [INLONG-1975](https://github.com/apache/incubator-inlong/issues/1975) | [Bug]error occurs when deleting a data access
| [INLONG-1978](https://github.com/apache/incubator-inlong/issues/1978) | [Bug]Create multiple file import tasks, and inlong-agent reports an error when registering metric
| [INLONG-1980](https://github.com/apache/incubator-inlong/issues/1980) | [Bug]the content of topics.properties generated incorrectly，and too much backup files generate automatically
| [INLONG-1981](https://github.com/apache/incubator-inlong/issues/1981) | [Bug] When compiling the project, the InLong-audit module reported Warning errors
| [INLONG-1984](https://github.com/apache/incubator-inlong/issues/1984) | [Bug][InLong-Manager] Create pulsar access, modify pulsar related parameters failed
| [INLONG-1995](https://github.com/apache/incubator-inlong/issues/1995) | [Bug] Compile Audit-SDK and report TestNGException
| [INLONG-1996](https://github.com/apache/incubator-inlong/issues/1996) | [Bug] Compile the project and InLong-Agent module throws 3 exceptions
| [INLONG-1997](https://github.com/apache/incubator-inlong/issues/1997) | [Bug]after the compilation of inlong, no lib directory in inlong-dataproxy
| [INLONG-2009](https://github.com/apache/incubator-inlong/issues/2009) | [Bug]Topic obtained through "/api/inlong/manager/openapi/dataproxy/getConfig" is not right
| [INLONG-2012](https://github.com/apache/incubator-inlong/issues/2012) | [Bug] Inlong-agent could not fetch file agent task through api --"/api/inlong/manager/openapi/agent/fileAgent/getTaskConf"
| [INLONG-2014](https://github.com/apache/incubator-inlong/issues/2014) | [Bug]inlong-dataproxy could not identify the groupId and topic format of topics.properties
| [INLONG-2018](https://github.com/apache/incubator-inlong/issues/2018) | [Bug]after approving a data access, some failures happen and the data access is always in the state of configuration
| [INLONG-2020](https://github.com/apache/incubator-inlong/issues/2020) | [Bug] Dependency of "jul-to-slf4j" is missing for pulsar connector
| [INLONG-2023](https://github.com/apache/incubator-inlong/issues/2023) | [Bug] Agent stream id is not passed to proxy
| [INLONG-2026](https://github.com/apache/incubator-inlong/issues/2026) | [Bug] Found Pulsar client create failure when starting Sort
| [INLONG-2030](https://github.com/apache/incubator-inlong/issues/2030) | [Bug]inlong-agent raises NPE error when running
| [INLONG-2032](https://github.com/apache/incubator-inlong/issues/2032) | [Bug]"javax.xml.parsers.FactoryConfigurationError" throwed in flink when starting a inlong-sort job
| [INLONG-2035](https://github.com/apache/incubator-inlong/issues/2035) | [Bug] Agent use wrong tid __ to generate message
| [INLONG-2038](https://github.com/apache/incubator-inlong/issues/2038) | [Bug]inlong-sort abandon data from pulsar due to an ClassCastException
| [INLONG-2043](https://github.com/apache/incubator-inlong/issues/2043) | [Bug] Sort module renames tid to streamId


## Release 0.11.0-incubating - Released (as of 2021-10-25)

### FEATURES:
| ISSUE  | Summary  |
| :---- | :------- |
| [INLONG-624](https://github.com/apache/incubator-inlong/issues/624) | [Feature] Go SDK support for TubeMQ
| [INLONG-1308](https://github.com/apache/incubator-inlong/issues/1308) | [Feature] Support Deploying All Modules on Kubernetes
| [INLONG-1330](https://github.com/apache/incubator-inlong/issues/1330) | [Feature] DataProxy support Pulsar
| [INLONG-1631](https://github.com/apache/incubator-inlong/issues/1631) | [Feature] [office-website] Refactor incubator-inlong-website by docusaurus

### IMPROVEMENTS:
| ISSUE  | Summary  |
| :---- | :------- |
| [INLONG-1324](https://github.com/apache/incubator-inlong/issues/1324) | [Improve] [Manager] The consumption details should be refreshed after editing successfully
| [INLONG-1327](https://github.com/apache/incubator-inlong/issues/1327) | [Improve] [Manager] Supports pagi`ng query for workflow execution log
| [INLONG-1578](https://github.com/apache/incubator-inlong/issues/1570) | [Improve] Go SDK should provide a more elegant way to set the parameter of config`
| [INLONG-1571](https://github.com/apache/incubator-inlong/issues/1571) | [Improve] [CI] Check License Heade
| [INLONG-1584](https://github.com/apache/incubator-inlong/issues/1584) | [Improve] TDMsg Decode Support For Go SDK
| [INLONG-1585](https://github.com/apache/incubator-inlong/issues/1585) | [Improve] Improve issue template with issue forms
| [INLONG-1589](https://github.com/apache/incubator-inlong/issues/1589) | [Improve] [Manager] Manager provide an openapi of DataProxy configuration data for multi-subcluster
| [INLONG-1593](https://github.com/apache/incubator-inlong/issues/1593) | [Improve] Add EmptyLineSeparator java code checkstyle rule
| [INLONG-1595](https://github.com/apache/incubator-inlong/issues/1595) | [Improve] inlong-dataproxy start by the configuration data from inlong-manager
| [INLONG-1623](https://github.com/apache/incubator-inlong/issues/1602) | [Improve] Optimize EntityStatus enum
| [INLONG-1619](https://github.com/apache/incubator-inlong/issues/1619) | [Improve] Add improve suggestion template
| [INLONG-1611](https://github.com/apache/incubator-inlong/issues/1611) | [Improve] Enable Merge Button
| [INLONG-1623](https://github.com/apache/incubator-inlong/issues/1623) | [Improve] add contribution guide document for the main repo
| [INLONG-1626](https://github.com/apache/incubator-inlong/issues/1626) | [Improve] [office-website] Agent introduce a Message filter
| [INLONG-1628](https://github.com/apache/incubator-inlong/issues/1628) | [Improve] Remove commitlint in package.json
| [INLONG-1629](https://github.com/apache/incubator-inlong/issues/1629) | [Improve] Disable merge commit
| [INLONG-1632](https://github.com/apache/incubator-inlong/issues/1632) | [Improve] [office-website] Refactoring of basic projects
| [INLONG-1633](https://github.com/apache/incubator-inlong/issues/1633) | [Improve] [office-website] Migrate modules documentation
| [INLONG-1634](https://github.com/apache/incubator-inlong/issues/1634) | [Improve] [office-website] Migrate download documentation
| [INLONG-1635](https://github.com/apache/incubator-inlong/issues/1635) | [Improve] [office-website] Migrate development documentation
| [INLONG-1636](https://github.com/apache/incubator-inlong/issues/1636) | [Improve] [office-website] Replace the default language selection icon
| [INLONG-1637](https://github.com/apache/incubator-inlong/issues/1637) | [Improve] [office-website] Add docusaurus i18n docs
| [INLONG-1638](https://github.com/apache/incubator-inlong/issues/1638) | [Improve] [office-website] Adapt new github action command
| [INLONG-1641](https://github.com/apache/incubator-inlong/issues/1641) | [Improve] [Agent] Agent introduce a Message filter
| [INLONG-1642](https://github.com/apache/incubator-inlong/issues/1642) | [Improve] [Agent] Agent Use Message filter to get tid from different lines in a file
| [INLONG-1650](https://github.com/apache/incubator-inlong/issues/1650) | [Improve] [TubeMQ] Provide a more elegant way to define config address
| [INLONG-1662](https://github.com/apache/incubator-inlong/issues/1662) | [Improve] [GitHub] Improve issue templates
| [INLONG-1666](https://github.com/apache/incubator-inlong/issues/1666) | [Improve] [TubeMQ] README for Go SDK
| [INLONG-1668](https://github.com/apache/incubator-inlong/issues/1668) | [Improve] [office-website] Adapt quick edit link
| [INLONG-1669](https://github.com/apache/incubator-inlong/issues/1669) | [Improve] [office-website] Adapt docusaurus build command
| [INLONG-1670](https://github.com/apache/incubator-inlong/issues/1670) | [Improve] [Manager] Add H2 in UT
| [INLONG-1680](https://github.com/apache/incubator-inlong/issues/1680) | [Improve] [doc] remove the redundant download links
| [INLONG-1682](https://github.com/apache/incubator-inlong/issues/1682) | [Improve] [TubeMQ] New Go module for Go SDK
| [INLONG-1699](https://github.com/apache/incubator-inlong/issues/1699) | [Improve] [doc] add a correct interpretation for InLong
| [INLONG-1701](https://github.com/apache/incubator-inlong/issues/1701) | [Improve] [InLong-Manager] Adjust unit tests

### BUG FIXES:
| ISSUE  | Summary  |
| :---- | :------- |
| [INLONG-1498](https://github.com/apache/incubator-inlong/issues/1498) | ignore the files with versionsBackup suffix for the bumped version
| [INLONG-1507](https://github.com/apache/incubator-inlong/issues/1507) | Go Client should reconnect to server if the server is shutdown and restarted
| [INLONG-1509](https://github.com/apache/incubator-inlong/issues/1509) | duplicate issues be counted in CHANGES.md
| [INLONG-1511](https://github.com/apache/incubator-inlong/issues/1511) | release guild documents has some errors
| [INLONG-1514](https://github.com/apache/incubator-inlong/issues/1514) | the license header is not correct for inlong-website/nginx.conf
| [INLONG-1525](https://github.com/apache/incubator-inlong/issues/1525) | Go SDK fail to parse SubscribeInfo
| [INLONG-1527](https://github.com/apache/incubator-inlong/issues/1527) | GoSDK should throw error if it fail to connect to master
| [INLONG-1529](https://github.com/apache/incubator-inlong/issues/1529) | Go SDK should reset heartbeat if register to master successfully
| [INLONG-1531](https://github.com/apache/incubator-inlong/issues/1531) | Go SDK should init the flow control item of the partition
| [INLONG-1533](https://github.com/apache/incubator-inlong/issues/1533) | Go SDK should provide more example
| [INLONG-1535](https://github.com/apache/incubator-inlong/issues/1535) | Go SDK should be closed before stopping the event processing goroutine
| [INLONG-1538](https://github.com/apache/incubator-inlong/issues/1538) | TubeMQ reports the error "Topic xxx not publish" when producing data
| [INLONG-1550](https://github.com/apache/incubator-inlong/issues/1550) | Go SDK should obey the flow control rule
| [INLONG-1552](https://github.com/apache/incubator-inlong/issues/1552) | Java SDK should deal with the default flow control rule
| [INLONG-1553](https://github.com/apache/incubator-inlong/issues/1553) | migrate the user manual documents at first class
| [INLONG-1554](https://github.com/apache/incubator-inlong/issues/1554) | remove the Console Introduction for manager
| [INLONG-1555](https://github.com/apache/incubator-inlong/issues/1555) | Go SDK should record the consumer config to the log
| [INLONG-1558](https://github.com/apache/incubator-inlong/issues/1558) | Go SDK should provide a multi goroutine consumer example
| [INLONG-1560](https://github.com/apache/incubator-inlong/issues/1560) | C++ SDK can not return error code of PartInUse and PartWaiting correctly
| [INLONG-1562](https://github.com/apache/incubator-inlong/issues/1562) | [K8s] There are some syntax bugs and configuration bugs in helm chart
| [INLONG-1563](https://github.com/apache/incubator-inlong/issues/1563) | Go SDK can not stop the heartbeat timer after the consumer has been closed
| [INLONG-1566](https://github.com/apache/incubator-inlong/issues/1566) | The user defined partition offset of Go SDK can not take effect
| [INLONG-1568](https://github.com/apache/incubator-inlong/issues/1568) | C++ SDK cant not return the whether the partition has been registered correctly
| [INLONG-1569](https://github.com/apache/incubator-inlong/issues/1569) | The first_registered is not the same with its naming
| [INLONG-1573](https://github.com/apache/incubator-inlong/issues/1573) | Add TDMsg decode logic to TubeMQ's C++ SDK
| [INLONG-1575](https://github.com/apache/incubator-inlong/issues/1575) | Modify the download url of version 0.9.0
| [INLONG-1579](https://github.com/apache/incubator-inlong/issues/1579) | lots of files are not standard License Header
| [INLONG-1581](https://github.com/apache/incubator-inlong/issues/1581) | InLong's website does not work without Javascript
| [INLONG-1587](https://github.com/apache/incubator-inlong/issues/1587) | Fix compile error
| [INLONG-1592](https://github.com/apache/incubator-inlong/issues/1592) | TextFileReader: The cpu utilization rate is very high, nearly 50%
| [INLONG-1600](https://github.com/apache/incubator-inlong/issues/1600) | There are some YAML errors in bug report and feature request issue forms
| [INLONG-1604](https://github.com/apache/incubator-inlong/issues/1604) | Some resultType is wrong in mapper
| [INLONG-1607](https://github.com/apache/incubator-inlong/issues/1607) | The master version should be added in the bug-report.yml
| [INLONG-1614](https://github.com/apache/incubator-inlong/issues/1614) | dataProxyConfigRepository constructor error
| [INLONG-1617](https://github.com/apache/incubator-inlong/issues/1617) | Ignore mysql directory after run docker compose
| [INLONG-1621](https://github.com/apache/incubator-inlong/issues/1621) | RestTemplateConfig cannot load config from properties
| [INLONG-1625](https://github.com/apache/incubator-inlong/issues/1625) | some page links are not available for Contribution Guide
| [INLONG-1645](https://github.com/apache/incubator-inlong/issues/1645) | [Bug] Druid datasource is not used
| [INLONG-1665](https://github.com/apache/incubator-inlong/issues/1665) | Adjust the content of the document title
| [INLONG-1673](https://github.com/apache/incubator-inlong/issues/1673) | some links are not available after office-website refactored
| [INLONG-1676](https://github.com/apache/incubator-inlong/issues/1676) | two recent PRs were overwritten after the office-website refactored
| [INLONG-1677](https://github.com/apache/incubator-inlong/issues/1677) | the architecture picture is lost in README
| [INLONG-1685](https://github.com/apache/incubator-inlong/issues/1685) | the Chinese Quick Start Guide has some incorrect place after the office-webiste refactored
| [INLONG-1694](https://github.com/apache/incubator-inlong/issues/1694) | Build docker mirror error for TubeMQ C++
| [INLONG-1695](https://github.com/apache/incubator-inlong/issues/1695) | [Bug][DataProxy] Build failed

## Release 0.10.0-incubating - Released (as of 2021-09-01)

### IMPROVEMENTS:
| ISSUE  | Summary  |
| :---- | :------- |
| [INLONG-570](https://issues.apache.org/jira/browse/INLONG-570) | Optimizing the implementations of HTTP API for Master
| [INLONG-726](https://issues.apache.org/jira/browse/INLONG-726) | Optimize The Deployment For InLong
| [INLONG-732](https://issues.apache.org/jira/browse/INLONG-732) | Optimize the CI/CD workflow for build and integration test
| [INLONG-733](https://issues.apache.org/jira/browse/INLONG-733) | Unity the Manger-API/Manger-OpenAPI, and change Manager-API to Manager-Web
| [INLONG-744](https://issues.apache.org/jira/browse/INLONG-744) | Error log, should use log4j
| [INLONG-750](https://issues.apache.org/jira/browse/INLONG-750) | Add configuration descriptions for 'defEthName' in 'broker.ini'
| [INLONG-756](https://issues.apache.org/jira/browse/INLONG-756) | package start.sh script executable for agent
| [INLONG-768](https://issues.apache.org/jira/browse/INLONG-768) | add github pull request template
| [INLONG-789](https://issues.apache.org/jira/browse/INLONG-789) | make agent readme more friendly
| [INLONG-792](https://issues.apache.org/jira/browse/INLONG-792) | tubemanager add a cluster after configuration
| [INLONG-800](https://issues.apache.org/jira/browse/INLONG-800) | Fix codestyle of some comments and methods names.
| [INLONG-804](https://issues.apache.org/jira/browse/INLONG-804) | Optimize the ASF Configuration
| [INLONG-805](https://issues.apache.org/jira/browse/INLONG-805) | Migrate InLong Issues from JIRA to GitHub
| [INLONG-808](https://issues.apache.org/jira/browse/INLONG-808) | Missing dataproxy sdk readme
| [INLONG-809](https://issues.apache.org/jira/browse/INLONG-809) | dataproxy readme delete reference url
| [INLONG-1498](https://github.com/apache/incubator-inlong/issues/1498) |  ignore the files with versionsBackup suffix for the bumped version
| [INLONG-1487](https://github.com/apache/incubator-inlong/issues/1487) |  remove the user number limit  when create a new data stream
| [INLONG-1486](https://github.com/apache/incubator-inlong/issues/1486) |  [agent] update the document about configuring the dataprxy address
| [INLONG-1485](https://github.com/apache/incubator-inlong/issues/1485) |  [sort] add the guide documents for using Pulsar
| [INLONG-1484](https://github.com/apache/incubator-inlong/issues/1484) |  Bid and Tid is not well explained in agent and might cause send error
| [INLONG-1464](https://github.com/apache/incubator-inlong/issues/1464) |  Add code CheckStyle rules
| [INLONG-1459](https://github.com/apache/incubator-inlong/issues/1459) |  proxy address configuration is redundant for inlong-agent
| [INLONG-1457](https://github.com/apache/incubator-inlong/issues/1457) |  remove the user limit for creating a new data access
| [INLONG-1455](https://github.com/apache/incubator-inlong/issues/1455) |  add a script to publish docker images
| [INLONG-1443](https://github.com/apache/incubator-inlong/issues/1443) |   Provide management interface SDK
| [INLONG-1439](https://github.com/apache/incubator-inlong/issues/1439) |  Add the port legal check and remove the useless deleteWhen field
| [INLONG-1430](https://github.com/apache/incubator-inlong/issues/1430) |  Go SDK example
| [INLONG-1429](https://github.com/apache/incubator-inlong/issues/1429) |  update the asf config for inlong office website
| [INLONG-1427](https://github.com/apache/incubator-inlong/issues/1427) |  Go SDK return maxOffset and updateTime in ConsumerOffset
| [INLONG-1424](https://github.com/apache/incubator-inlong/issues/1424) |  change the format of the configuration file: make the yaml to properties
| [INLONG-1423](https://github.com/apache/incubator-inlong/issues/1423) |  modify the docker image of the inlong-manager module
| [INLONG-1417](https://github.com/apache/incubator-inlong/issues/1417) |  rename the distribution file for inlong
| [INLONG-1415](https://github.com/apache/incubator-inlong/issues/1415) |  [TubeMQ Docker] expose zookeeper port for other component usages
| [INLONG-1409](https://github.com/apache/incubator-inlong/issues/1409) |  Sort out the LICENSE information of the 3rd-party components that the DataProxy submodule depends on
| [INLONG-1407](https://github.com/apache/incubator-inlong/issues/1407) |  [DataProxy]Adjust the pom dependency of the DataProxy module
| [INLONG-1405](https://github.com/apache/incubator-inlong/issues/1405) |  too many issues mail at dev@inlong mailbox

### BUG FIXES:
| ISSUE  | Summary  |
| :---- | :------- |
| [INLONG-751](https://issues.apache.org/jira/browse/INLONG-751) | InLong Manager start up error
| [INLONG-776](https://issues.apache.org/jira/browse/INLONG-776) | fix the version error for tubemq cpp client docker image
| [INLONG-777](https://issues.apache.org/jira/browse/INLONG-777) | InLong Manager new data stream error
| [INLONG-782](https://issues.apache.org/jira/browse/INLONG-782) | Optimize The PULL_REQUEST_TEMPLATE
| [INLONG-787](https://issues.apache.org/jira/browse/INLONG-787) | The actions "reviewdog/action-setup" is not allowed to be used
| [INLONG-797](https://issues.apache.org/jira/browse/INLONG-797) | the document for deployment DataProxy is not complete
| [INLONG-799](https://issues.apache.org/jira/browse/INLONG-799) | can not find common.properties for dataproxy
| [INLONG-1488](https://github.com/apache/incubator-inlong/issues/1488) |  there are still some chinese characters for website
| [INLONG-1475](https://github.com/apache/incubator-inlong/issues/1475) |  Tube manager compile ch.qos.logback with error
| [INLONG-1474](https://github.com/apache/incubator-inlong/issues/1474) |  the interface of get data proxy configurations got abnormal status result
| [INLONG-1470](https://github.com/apache/incubator-inlong/issues/1470) |  Java.util.ConcurrentModificationException error when rebalance
| [INLONG-1468](https://github.com/apache/incubator-inlong/issues/1468) |  The update interval of dataproxy is quite long and may cause produce error when config is not updated
| [INLONG-1466](https://github.com/apache/incubator-inlong/issues/1466) |  get snappy error when the agent collecting data
| [INLONG-1462](https://github.com/apache/incubator-inlong/issues/1462) |  dataproxy can not create configuration properties successfully in the docker container
| [INLONG-1458](https://github.com/apache/incubator-inlong/issues/1458) |  The http port in agent readme should be 8008 to be consistent with the code
| [INLONG-1453](https://github.com/apache/incubator-inlong/issues/1453) |  agent connect dataproxy fail when using docker-compose
| [INLONG-1448](https://github.com/apache/incubator-inlong/issues/1448) |  The Manager throws an exception when creating a business
| [INLONG-1447](https://github.com/apache/incubator-inlong/issues/1447) |  Fix Group Control API logic bug
| [INLONG-1444](https://github.com/apache/incubator-inlong/issues/1444) |  Fix Web API multiple field search logic bug
| [INLONG-1441](https://github.com/apache/incubator-inlong/issues/1441) |  Repair Broker configuration API bugs
| [INLONG-1436](https://github.com/apache/incubator-inlong/issues/1436) |  [CI] The checkstyle workflow is redundant
| [INLONG-1432](https://github.com/apache/incubator-inlong/issues/1432) |  The manager url of agent and dataproxy need to be updated since manager merged openapi and api into one module
| [INLONG-1403](https://github.com/apache/incubator-inlong/issues/1403) |  fix some error in dataproxy-sdk readme

### SUB-TASK:
| ISSUE  | Summary  |
| :---- | :------- |
| [INLONG-576](https://issues.apache.org/jira/browse/INLONG-576) | Build metadata entity classes
| [INLONG-578](https://issues.apache.org/jira/browse/INLONG-578) | Build implementation classes based on BDB storage
| [INLONG-579](https://issues.apache.org/jira/browse/INLONG-579) | Add structure mapping of BDB and metadata entity classes
| [INLONG-580](https://issues.apache.org/jira/browse/INLONG-580) | Build active and standby keep-alive services
| [INLONG-581](https://issues.apache.org/jira/browse/INLONG-581) | Add data cache in BDB metadata Mapper implementations
| [INLONG-582](https://issues.apache.org/jira/browse/INLONG-582) | Adjust the business logic related to the BdbClusterSettingEntity class
| [INLONG-583](https://issues.apache.org/jira/browse/INLONG-583) | Adjust BrokerConfManager class implementation
| [INLONG-584](https://issues.apache.org/jira/browse/INLONG-584) | Adjust WebMasterInfoHandler class implementation
| [INLONG-593](https://issues.apache.org/jira/browse/INLONG-593) | Add WebGroupConsumeCtrlHandler class implementation
| [INLONG-595](https://issues.apache.org/jira/browse/INLONG-595) | Add WebBrokerConfHandler class implementation
| [INLONG-596](https://issues.apache.org/jira/browse/INLONG-596) | Add WebTopicConfHandler class implementation
| [INLONG-597](https://issues.apache.org/jira/browse/INLONG-597) | Adjust WebTopicCtrlHandler class implementation
| [INLONG-598](https://issues.apache.org/jira/browse/INLONG-598) | Adjust WebTopicCtrlHandler class implementation
| [INLONG-599](https://issues.apache.org/jira/browse/INLONG-599) | Adjust WebParameterUtils.java's static functions
| [INLONG-601](https://issues.apache.org/jira/browse/INLONG-601) | Adjust WebBrokerDefConfHandler class implementation
| [INLONG-602](https://issues.apache.org/jira/browse/INLONG-602) | Add replacement processing after metadata changes
| [INLONG-611](https://issues.apache.org/jira/browse/INLONG-611) | Add FSM for broker configure manage
| [INLONG-617](https://issues.apache.org/jira/browse/INLONG-617) | Add unit tests for WebParameterUtils
| [INLONG-618](https://issues.apache.org/jira/browse/INLONG-618) | Add unit tests for metastore.dao.entity.*
| [INLONG-625](https://issues.apache.org/jira/browse/INLONG-625) | Add unit tests for metamanage.metastore.impl.*
| [INLONG-626](https://issues.apache.org/jira/browse/INLONG-626) | Fix broker and topic confiugre implement bugs
| [INLONG-707](https://issues.apache.org/jira/browse/INLONG-707) | Bumped version to 0.10.0-SNAPSHOT
| [INLONG-740](https://issues.apache.org/jira/browse/INLONG-740) | Merge the changes in INLONG-739 to master and delete the temporary branch
| [INLONG-755](https://issues.apache.org/jira/browse/INLONG-755) | Go SDK Consumer Result
| [INLONG-757](https://issues.apache.org/jira/browse/INLONG-757) | fix the artifactId of dataproxy
| [INLONG-758](https://issues.apache.org/jira/browse/INLONG-758) | remove redundant baseDirectory for manager output files
| [INLONG-759](https://issues.apache.org/jira/browse/INLONG-759) | fix assembly issue for TubeMQ manager
| [INLONG-760](https://issues.apache.org/jira/browse/INLONG-760) | standardize the directories name for the sort sub-module
| [INLONG-761](https://issues.apache.org/jira/browse/INLONG-761) | unify all modules target files to a singe directory
| [INLONG-762](https://issues.apache.org/jira/browse/INLONG-762) | refactor the deployment document
| [INLONG-763](https://issues.apache.org/jira/browse/INLONG-763) | make the inlong-websit be a maven module of InLong project
| [INLONG-764](https://issues.apache.org/jira/browse/INLONG-764) | Fix Go SDK RPC Request bug
| [INLONG-766](https://issues.apache.org/jira/browse/INLONG-766) | Fix Go SDK Codec Bug
| [INLONG-770](https://issues.apache.org/jira/browse/INLONG-770) | update the readme document
| [INLONG-771](https://issues.apache.org/jira/browse/INLONG-771) | Fix Go SDK Authorization Bug
| [INLONG-773](https://issues.apache.org/jira/browse/INLONG-773) | add manager docker image
| [INLONG-774](https://issues.apache.org/jira/browse/INLONG-774) | update TubeMQ docker images to InLong repo
| [INLONG-778](https://issues.apache.org/jira/browse/INLONG-778) | Fix Go SDK Consumer Bug
| [INLONG-779](https://issues.apache.org/jira/browse/INLONG-779) | update tubemq manager docker image
| [INLONG-781](https://issues.apache.org/jira/browse/INLONG-781) | add inlong agent docker image support
| [INLONG-784](https://issues.apache.org/jira/browse/INLONG-784) | Fix Go SDK Heartbeat Bug
| [INLONG-785](https://issues.apache.org/jira/browse/INLONG-785) | Fix Go SDK Metadata Bug
| [INLONG-786](https://issues.apache.org/jira/browse/INLONG-786) | add dataproxy docker image
| [INLONG-788](https://issues.apache.org/jira/browse/INLONG-788) | Fix Go SDK Remote Cache Bug
| [INLONG-791](https://issues.apache.org/jira/browse/INLONG-791) | Go SDK Support multiple topic address
| [INLONG-793](https://issues.apache.org/jira/browse/INLONG-793) | Fix Some Corner Case in Go SDK
| [INLONG-794](https://issues.apache.org/jira/browse/INLONG-794) | add website docker image
| [INLONG-803](https://issues.apache.org/jira/browse/INLONG-803) | add docker requirement for building InLong
| [INLONG-806](https://issues.apache.org/jira/browse/INLONG-806) | open GitHub Issue For InLong
| [INLONG-807](https://issues.apache.org/jira/browse/INLONG-807) | migrate issue history to inlong
| [INLONG-1491](https://github.com/apache/incubator-inlong/issues/1491) |  Add 0.10.0 version release modification to CHANGES.md
| [INLONG-1492](https://github.com/apache/incubator-inlong/issues/1492) |  Bumped version to 0.11.0-incubating-SNAPSHOT
| [INLONG-1493](https://github.com/apache/incubator-inlong/issues/1493) |  Modify download&release notes for 0.10.0
| [INLONG-1494](https://github.com/apache/incubator-inlong/issues/1494) |  Adjust the version information of all pom.xml to 0.10.0-incubating
| [INLONG-1495](https://github.com/apache/incubator-inlong/issues/1495) |  Update Office website content for release 0.10.0
| [INLONG-1496](https://github.com/apache/incubator-inlong/issues/1496) |  Release InLong 0.10.0
| [INLONG-1497](https://github.com/apache/incubator-inlong/issues/1497) |  create a 0.10.0 branch to release
| [INLONG-1502](https://github.com/apache/incubator-inlong/issues/1502) |  publish all 0.10.0 images to docker hub

## Release 0.9.0-incubating - Released (as of 2021-07-11)

### IMPROVEMENTS:
| JIRA  | Summary  | Priority |
| :---- | :------- | :------- |
| [INLONG-594](https://issues.apache.org/jira/browse/INLONG-594) | Trpc-go  tube sdk strongly rely on local config  | Major |
| [INLONG-616](https://issues.apache.org/jira/browse/INLONG-616) | Adjust the content in .asf.yaml according to the new project name  | Major |
| [INLONG-651](https://issues.apache.org/jira/browse/INLONG-651) | Refine the tubemq-client-cpp build description  | Major |
| [INLONG-655](https://issues.apache.org/jira/browse/INLONG-655) | add dependency in tube-manager  | Major |
| [INLONG-656](https://issues.apache.org/jira/browse/INLONG-656) | fix tubemanager start.sh  | Major |
| [INLONG-657](https://issues.apache.org/jira/browse/INLONG-657) | remove some test cases in agent  | Major |
| [INLONG-659](https://issues.apache.org/jira/browse/INLONG-659) | fix unit test in agent  | Major |
| [INLONG-666](https://issues.apache.org/jira/browse/INLONG-666) | change tar name in agent  | Major |
| [INLONG-697](https://issues.apache.org/jira/browse/INLONG-697) | fix decode error in proxySdk  | Major |
| [INLONG-705](https://issues.apache.org/jira/browse/INLONG-705) | add stop.sh in dataproxy  | Major |
| [INLONG-743](https://issues.apache.org/jira/browse/INLONG-743) | Adjust the rat check setting of the pom.xml  | Major |

### BUG FIXES:
| JIRA  | Summary  | Priority |
| :---- | :------- | :------- |
| [INLONG-577](https://issues.apache.org/jira/browse/INLONG-577) | reload status output and topic config output mismatch | Major |
| [INLONG-612](https://issues.apache.org/jira/browse/INLONG-612) | Restful api "admin_snapshot_message" is not compatible with the old version | Major |
| [INLONG-638](https://issues.apache.org/jira/browse/INLONG-638) | Issues About Disk Error recovery | Major |
| [INLONG-688](https://issues.apache.org/jira/browse/INLONG-688) | change additionstr to additionAtr in agent | Major |
| [INLONG-703](https://issues.apache.org/jira/browse/INLONG-703) | Query after adding a consumer group policy and report a null error | Major |
| [INLONG-724](https://issues.apache.org/jira/browse/INLONG-724) | Encountered a symbol not found error when compiling | Major |

### TASK:
| JIRA | Summary | Priority |
|:---- |:---- | :--- |
| [INLONG-613](https://issues.apache.org/jira/browse/INLONG-613) | Adjust the project codes according to the renaming requirements  | Major |

### SUB-TASK:
| JIRA  | Summary  | Priority |
| :---- | :------- | :------- |
| [INLONG-519](https://issues.apache.org/jira/browse/INLONG-519) | Bumped version to 0.9.0-SNAPSHOT | Major |
| [INLONG-565](https://issues.apache.org/jira/browse/INLONG-565) | Replace simple scripts and code implementation | Major |
| [INLONG-571](https://issues.apache.org/jira/browse/INLONG-571) | Adjust WebOtherInfoHandler class implementation | Major |
| [INLONG-573](https://issues.apache.org/jira/browse/INLONG-573) | Adjust WebAdminFlowRuleHandler class implementation | Major |
| [INLONG-574](https://issues.apache.org/jira/browse/INLONG-574) | Adjust WebAdminGroupCtrlHandler class implementation | Major |
| [INLONG-632](https://issues.apache.org/jira/browse/INLONG-632) | Add inlong-manager subdirectory | Major |
| [INLONG-633](https://issues.apache.org/jira/browse/INLONG-633) | Add inlong-sort subdirectory | Major |
| [INLONG-634](https://issues.apache.org/jira/browse/INLONG-634) | Add inlong-tubemq subdirectory | Major |
| [INLONG-635](https://issues.apache.org/jira/browse/INLONG-635) | Add inlong-dataproxy subdirectory | Major |
| [INLONG-636](https://issues.apache.org/jira/browse/INLONG-636) | Add inlong-agent subdirectory | Major |
| [INLONG-640](https://issues.apache.org/jira/browse/INLONG-640) | Adjust the main frame of the InLong project | Major |
| [INLONG-641](https://issues.apache.org/jira/browse/INLONG-641) | Add inlong-common module | Major |
| [INLONG-642](https://issues.apache.org/jira/browse/INLONG-642) | Add inlong-website subdirectory | Major |
| [INLONG-643](https://issues.apache.org/jira/browse/INLONG-643) | Add inlong-dataproxy-sdk subdirectory | Major |
| [INLONG-644](https://issues.apache.org/jira/browse/INLONG-644) | Remove "/dist" from .gitignore and add subdirectory dist in inlong-sort | Major |
| [INLONG-646](https://issues.apache.org/jira/browse/INLONG-646) | Remove time related unit tests until timezone is configurable in inlong-sort | Major |
| [INLONG-647](https://issues.apache.org/jira/browse/INLONG-647) | Adjust the introduction content of the README.md | Major |
| [INLONG-648](https://issues.apache.org/jira/browse/INLONG-648) | Change initial version of inlong-sort to 0.9.0-incubating-SNAPSHOT | Major |
| [INLONG-649](https://issues.apache.org/jira/browse/INLONG-649) | Modify the suffix of the docs/zh-cn/download/release-0.8.0.md file | Major |
| [INLONG-650](https://issues.apache.org/jira/browse/INLONG-650) | Adjust .asf.yaml's label | Major |
| [INLONG-654](https://issues.apache.org/jira/browse/INLONG-654) | Adjust the link in ReadMe.md according to the latest document | Major |
| [INLONG-658](https://issues.apache.org/jira/browse/INLONG-658) | modify the dependency of inlong-manager | Major |
| [INLONG-663](https://issues.apache.org/jira/browse/INLONG-663) | modify the tar package name of inlong-manager | Major |
| [INLONG-667](https://issues.apache.org/jira/browse/INLONG-667) | rename tar in agent | Major |
| [INLONG-673](https://issues.apache.org/jira/browse/INLONG-673) | add tube cluster id in inlong-manager | Major |
| [INLONG-674](https://issues.apache.org/jira/browse/INLONG-674) | adjust HiveSinkInfo in inlong-manager | Major |
| [INLONG-675](https://issues.apache.org/jira/browse/INLONG-675) | modify the npm script of inlong-website | Major |
| [INLONG-676](https://issues.apache.org/jira/browse/INLONG-676) | modify manager-web to manager-api in inlong-manager module | Major |
| [INLONG-677](https://issues.apache.org/jira/browse/INLONG-677) | Support dt as built-in data time partition field in inlong-sort | Major |
| [INLONG-681](https://issues.apache.org/jira/browse/INLONG-681) | modify assembly in agent proxy tubemanager | Major |
| [INLONG-687](https://issues.apache.org/jira/browse/INLONG-687) | set schemaName to m0_day when save business in inlong-manager | Major |
| [INLONG-689](https://issues.apache.org/jira/browse/INLONG-689) | add sort app name | Major |
| [INLONG-691](https://issues.apache.org/jira/browse/INLONG-691) | update properties and scripts for inlong-manager | Major |
| [INLONG-692](https://issues.apache.org/jira/browse/INLONG-692) | remove hive cluster entity in inlong-manager | Major |
| [INLONG-693](https://issues.apache.org/jira/browse/INLONG-693) | change data type to tdmsg | Major |
| [INLONG-694](https://issues.apache.org/jira/browse/INLONG-694) | Add retry mechanism for creating tube consumer group | Major |
| [INLONG-695](https://issues.apache.org/jira/browse/INLONG-695) | update getConfig API in inlong-manager | Major |
| [INLONG-696](https://issues.apache.org/jira/browse/INLONG-696) | modify the status of the entities after approval | Major |
| [INLONG-699](https://issues.apache.org/jira/browse/INLONG-699) | Fix serialization issue of DeserializationInfo 's subType in inlong-sort | Major |
| [INLONG-700](https://issues.apache.org/jira/browse/INLONG-700) | Optimize dependencies in inlong-sort | Major |
| [INLONG-701](https://issues.apache.org/jira/browse/INLONG-701) | Update create resource workflow definition | Major |
| [INLONG-702](https://issues.apache.org/jira/browse/INLONG-702) | sort config field spillter | Major |
| [INLONG-706](https://issues.apache.org/jira/browse/INLONG-706) | Add 0.9.0 version release modification to CHANGES.md | Major |
| [INLONG-709](https://issues.apache.org/jira/browse/INLONG-709) | Adjust the version information of all pom.xml to 0.9.0-incubating | Major |
| [INLONG-712](https://issues.apache.org/jira/browse/INLONG-712) | adjust partition info for hive sink | Major |
| [INLONG-713](https://issues.apache.org/jira/browse/INLONG-713) | add partition filed when create hive table | Major |
| [INLONG-714](https://issues.apache.org/jira/browse/INLONG-714) | Enable checkpointing in inlong-sort | Major |
| [INLONG-715](https://issues.apache.org/jira/browse/INLONG-715) | Make shouldRollOnCheckpoint always return true in DefaultRollingPolicy in inlong-sort | Major |
| [INLONG-716](https://issues.apache.org/jira/browse/INLONG-716) | set terminated symbol when create hive table | Major |
| [INLONG-717](https://issues.apache.org/jira/browse/INLONG-717) | Declare the 3rd party Catagory x LICENSE components in use | Major |


## Release 0.8.0-incubating - Released (as of 2021-01-18)

### IMPROVEMENTS:
| JIRA  | Summary  | Priority |
| :---- | :------- | :------- |
| [INLONG-430](https://issues.apache.org/jira/browse/INLONG-430) | Optimizing the implementation of HTTP API for broke  | Major |
| [INLONG-445](https://issues.apache.org/jira/browse/INLONG-445) | Adjust the status check default sleep interval of pullConsumeReadyChkSliceMs  | Major |
| [INLONG-448](https://issues.apache.org/jira/browse/INLONG-448) | Add Committer and PPMC operation process  | Major |
| [INLONG-449](https://issues.apache.org/jira/browse/INLONG-449) | Adjust Example implementation  | Major |
| [INLONG-452](https://issues.apache.org/jira/browse/INLONG-452) | Optimize rebalance performance | Major |
| [INLONG-467](https://issues.apache.org/jira/browse/INLONG-467) | Add WEB APIs of Master and Broker | Major |
| [INLONG-489](https://issues.apache.org/jira/browse/INLONG-489) | Add the maximum message length parameter setting | Major |
| [INLONG-495](https://issues.apache.org/jira/browse/INLONG-495) | Code implementation adjustment based on SpotBugs check | Major |
| [INLONG-511](https://issues.apache.org/jira/browse/INLONG-511) | Replace the conditional operator (?:) with mid()  | Major |
| [INLONG-512](https://issues.apache.org/jira/browse/INLONG-512) | Add package length control based on Topic  | Major |
| [INLONG-515](https://issues.apache.org/jira/browse/INLONG-515) | Add cluster Topic view web api  | Major |

### BUG FIXES:
| JIRA  | Summary  | Priority |
| :---- | :------- | :------- |
| [INLONG-437](https://issues.apache.org/jira/browse/INLONG-437) | Fix tubemq table source sink factory instance creating problem | Major |
| [INLONG-441](https://issues.apache.org/jira/browse/INLONG-441) | An error occurred when using the Tubemq class to create a sink table | Major |
| [INLONG-442](https://issues.apache.org/jira/browse/INLONG-442) | Modifying the jvm parameters when the broker starts does not take effect  | Major    |
| [INLONG-443](https://issues.apache.org/jira/browse/INLONG-443) | TubemqSourceFunction class prints too many logs problem | Major |
| [INLONG-446](https://issues.apache.org/jira/browse/INLONG-446) | Small bugs fix that do not affect the main logics | Major |
| [INLONG-450](https://issues.apache.org/jira/browse/INLONG-450) | TubeClientException: Generate producer id failed  | Major |
| [INLONG-453](https://issues.apache.org/jira/browse/INLONG-453) | TubemqSourceFunction class prints too many logs problem | Major |
| [INLONG-506](https://issues.apache.org/jira/browse/INLONG-506) | cmakelist error | Major |
| [INLONG-510](https://issues.apache.org/jira/browse/INLONG-510) | Found a bug in MessageProducerExample class | Major |
| [INLONG-518](https://issues.apache.org/jira/browse/INLONG-518) | fix parameter pass error | Major |
| [INLONG-526](https://issues.apache.org/jira/browse/INLONG-526) | Adjust the packaging script and version check list, remove the "-WIP" tag | Major |
| [INLONG-555](https://issues.apache.org/jira/browse/INLONG-555) | short session data can only be written to a specific partition | Major |
| [INLONG-556](https://issues.apache.org/jira/browse/INLONG-556) | Index value is bigger than the actual number of records | Low |


### TASK:
| JIRA | Summary | Priority |
|:---- |:---- | :--- |
| [INLONG-505](https://issues.apache.org/jira/browse/INLONG-505) | Remove the "WIP" label of the DISCLAIMER file  | Major |
| [INLONG-543](https://issues.apache.org/jira/browse/INLONG-543) | Modify the LICENSE statement of multiple files and others  | Major |
| [INLONG-557](https://issues.apache.org/jira/browse/INLONG-557) | Handle the issues mentioned in the 0.8.0-RC2 review  | Major |
| [INLONG-562](https://issues.apache.org/jira/browse/INLONG-562) | Update project contents according to the 0.8.0-RC3 review  | Major |

### SUB-TASK:
| JIRA  | Summary  | Priority |
| :---- | :------- | :------- |
| [INLONG-428](https://issues.apache.org/jira/browse/INLONG-433) | Bumped version to 0.8.0-SNAPSHOT | Major |
| [INLONG-433](https://issues.apache.org/jira/browse/INLONG-433) | add tubemq perf-consumer/producer scripts | Major |
| [INLONG-434](https://issues.apache.org/jira/browse/INLONG-434) | Adjust Broker API mapping | Major |
| [INLONG-435](https://issues.apache.org/jira/browse/INLONG-435) | Create Web field Mapping | Major |
| [INLONG-436](https://issues.apache.org/jira/browse/INLONG-436) | Adjust Broker's HTTP API implementation | Major |
| [INLONG-439](https://issues.apache.org/jira/browse/INLONG-439) | Add Cli field Scheme definition | Major |
| [INLONG-440](https://issues.apache.org/jira/browse/INLONG-440) | Add feature package tube-manager to zip  | Major |
| [INLONG-444](https://issues.apache.org/jira/browse/INLONG-444) | Add consume and produce Cli commands | Major |
| [INLONG-447](https://issues.apache.org/jira/browse/INLONG-447) | Add Broker-Admin Cli | Major |
| [INLONG-451](https://issues.apache.org/jira/browse/INLONG-451) | Replace ConsumeTupleInfo with Tuple2  | Major    |
| [INLONG-457](https://issues.apache.org/jira/browse/INLONG-457) | There is no need to return StringBuilder in Master.java | Major |
| [INLONG-463](https://issues.apache.org/jira/browse/INLONG-463) | Adjust Master rebalance process implementation  | Major |
| [INLONG-464](https://issues.apache.org/jira/browse/INLONG-464) | Add parameter rebalanceParallel in master.ini | Major |
| [INLONG-470](https://issues.apache.org/jira/browse/INLONG-470) | Add query API of TopicName and BrokerId collection  | Major |
| [INLONG-471](https://issues.apache.org/jira/browse/INLONG-471) | Add query API Introduction of TopicName and BrokerId collection | Major |
| [INLONG-472](https://issues.apache.org/jira/browse/INLONG-472) | Adjust Broker's AbstractWebHandler class implementation  | Major |
| [INLONG-475](https://issues.apache.org/jira/browse/INLONG-475) | add the offset clone api of the consume group  | Major |
| [INLONG-482](https://issues.apache.org/jira/browse/INLONG-482) | Add offset query api  | Major |
| [INLONG-484](https://issues.apache.org/jira/browse/INLONG-484) | Add query API for topic publication information  | Major |
| [INLONG-485](https://issues.apache.org/jira/browse/INLONG-485) | Add the batch setting API of consume group offset  | Major |
| [INLONG-486](https://issues.apache.org/jira/browse/INLONG-486) | Add the delete API of consumer group offset  | Major |
| [INLONG-494](https://issues.apache.org/jira/browse/INLONG-494) | Update API interface instruction document | Major |
| [INLONG-499](https://issues.apache.org/jira/browse/INLONG-499) | Add configure store  | Major |
| [INLONG-500](https://issues.apache.org/jira/browse/INLONG-500) | Add setting operate API  | Major |
| [INLONG-501](https://issues.apache.org/jira/browse/INLONG-501) | Adjust max message size check logic  | Major |
| [INLONG-502](https://issues.apache.org/jira/browse/INLONG-502) | Add setting API interface document  | Major |
| [INLONG-504](https://issues.apache.org/jira/browse/INLONG-504) | Adjust the WebMethodMapper class interfaces  | Major |
| [INLONG-508](https://issues.apache.org/jira/browse/INLONG-508) | Optimize Broker's PB parameter check processing logic  | Major |
| [INLONG-509](https://issues.apache.org/jira/browse/INLONG-509) | Adjust the packet length check when data is loaded  | Major |
| [INLONG-522](https://issues.apache.org/jira/browse/INLONG-522) | Add admin_query_cluster_topic_view API document  | Major |
| [INLONG-544](https://issues.apache.org/jira/browse/INLONG-544) | Adjust the LICENSE statement in the client.conf files of Python and C/C++ SDK | Major |
| [INLONG-546](https://issues.apache.org/jira/browse/INLONG-546) | Restore the original license header of the referenced external source files | Major |
| [INLONG-547](https://issues.apache.org/jira/browse/INLONG-547) | Recode the implementation of the *Startup.java classes in the Tool package | Major |
| [INLONG-548](https://issues.apache.org/jira/browse/INLONG-548) | Handle the LICENSE authorization of font files in the resources | Major |
| [INLONG-549](https://issues.apache.org/jira/browse/INLONG-549) | Handling the problem of compilation failure | Major |
| [INLONG-550](https://issues.apache.org/jira/browse/INLONG-550) | Adjust LICENSE file content | Major |
| [INLONG-551](https://issues.apache.org/jira/browse/INLONG-551) | Adjust NOTICE file content | Major |
| [INLONG-558](https://issues.apache.org/jira/browse/INLONG-558) | Adjust the LICENSE of the file header | Major |
| [INLONG-559](https://issues.apache.org/jira/browse/INLONG-559) | Update the LICENSE file according to the 0.8.0-RC2 review | Major |
| [INLONG-560](https://issues.apache.org/jira/browse/INLONG-560) | Remove unprepared modules | Major |


## Release 0.7.0-incubating - Released (as of 2020-11-25)

### New Features:

| JIRA  | Summary  | Priority |
| :---- | :------- | :------- |
| [INLONG-162](https://issues.apache.org/jira/browse/INLONG-162) | Python SDK support in TubeMQ | High |
| [INLONG-336](https://issues.apache.org/jira/browse/INLONG-336) | Propose web portal to manage tube cluster Phase-I | Major |
| [INLONG-390](https://issues.apache.org/jira/browse/INLONG-390)   | support build C++ SDK with docker image | Normal |

### IMPROVEMENTS:

| JIRA  | Summary  | Priority |
| :---- | :------- | :------- |
| [INLONG-369](https://issues.apache.org/jira/browse/INLONG-369) | hope to add an option in the compilation script (like `make lib` etc...)                 | Major    |
| [INLONG-373](https://issues.apache.org/jira/browse/INLONG-373) | Reduce the redundant code of Utils::Split functions             | Major    |
| [INLONG-374](https://issues.apache.org/jira/browse/INLONG-374) | Adjust some coding style issues     | Major    |
| [INLONG-375](https://issues.apache.org/jira/browse/INLONG-375) | Add a section to the README file about how to compile the project| Major    |
| [INLONG-385](https://issues.apache.org/jira/browse/INLONG-385) | update docker images     | Major    |
| [INLONG-393](https://issues.apache.org/jira/browse/INLONG-393) | Optimize the mapping code of WEB API     | Major    |
| [INLONG-406](https://issues.apache.org/jira/browse/INLONG-406) | test_consumer.py works for both Python 2 and 3   | Minor |
| [INLONG-410](https://issues.apache.org/jira/browse/INLONG-410) | install python package and simplify test_consumer.py    | Major    |
| [INLONG-416](https://issues.apache.org/jira/browse/INLONG-416) | support consume from specified position   | Major    |
| [INLONG-417](https://issues.apache.org/jira/browse/INLONG-417) | C++ Client support parse message from binary data for Python SDK    | Major    |
| [INLONG-419](https://issues.apache.org/jira/browse/INLONG-419) | SetMaxPartCheckPeriodMs() negative number, getMessage() still  | Major    |

### BUG FIXES:

| JIRA                                                         | Summary                                                      | Priority |
| :----------------------------------------------------------- | :----------------------------------------------------------- | :------- |
| [INLONG-365](https://issues.apache.org/jira/browse/INLONG-365) | Whether the consumption setting is wrong after the processRequest exception | Major    |
| [INLONG-370](https://issues.apache.org/jira/browse/INLONG-370) | Calling GetCurConsumedInfo API always returns failure      | Major    |
| [INLONG-376](https://issues.apache.org/jira/browse/INLONG-376) | Move pullrequests_status notifications commits mail list | Major    |
| [INLONG-366](https://issues.apache.org/jira/browse/INLONG-366) | Found a nullpointerexception bug in broker | Normal |
| [INLONG-379](https://issues.apache.org/jira/browse/INLONG-379) | Modify the memory cache size default to 3M | Normal |
| [INLONG-380](https://issues.apache.org/jira/browse/INLONG-380) | Cpp client link error when gcc optimization is disabled   | Major    |
| [INLONG-405](https://issues.apache.org/jira/browse/INLONG-405) | python sdk install files lack of the whole cpp configuration | Major |
| [INLONG-401](https://issues.apache.org/jira/browse/INLONG-401) | python sdk readme bug | Minor |
| [INLONG-407](https://issues.apache.org/jira/browse/INLONG-407) | Fix some content in README.md | Trivial |
| [INLONG-418](https://issues.apache.org/jira/browse/INLONG-418) | C++ SDK function SetMaxPartCheckPeriodMs() can't work | Major |

### SUB-TASK:

| JIRA  | Summary  | Priority |
| :---- | :------- | :------- |
| [INLONG-276](https://issues.apache.org/jira/browse/INLONG-276) | add python client encode/decode protobuf message for TubeMQ RPC Protocol                                 | Major    |
| [INLONG-338](https://issues.apache.org/jira/browse/INLONG-338) | web pages for tubemq manager                                     | Major    |
| [INLONG-341](https://issues.apache.org/jira/browse/INLONG-341) | open independent sub-project for tubemq                                | Major    |
| [INLONG-342](https://issues.apache.org/jira/browse/INLONG-342) | abstract backend threads for routine management                              | Major    |
| [INLONG-346](https://issues.apache.org/jira/browse/INLONG-346) | remove chinese comments                                          | Minor |
| [INLONG-355](https://issues.apache.org/jira/browse/INLONG-355) | Add business entity for topic manager                            | Major    |
| [INLONG-361](https://issues.apache.org/jira/browse/INLONG-361) | create topic when getting request             | Major    |
| [INLONG-364](https://issues.apache.org/jira/browse/INLONG-364) | uniform response format for exception state                              | Major    |
| [INLONG-383](https://issues.apache.org/jira/browse/INLONG-383) | document about Installation/API Reference/Example                                   | Major    |
| [INLONG-387](https://issues.apache.org/jira/browse/INLONG-387) | add manager web pages                                       | Major    |
| [INLONG-392](https://issues.apache.org/jira/browse/INLONG-392) | add query rest api for clusters| Major    |
| [INLONG-394](https://issues.apache.org/jira/browse/INLONG-394) | Creating Mapper class from web api to inner handler | Major    |
| [INLONG-395](https://issues.apache.org/jira/browse/INLONG-395) | Create Abstract WebHandler class                            | Major    |
| [INLONG-396](https://issues.apache.org/jira/browse/INLONG-396) | Adjust the WebXXXHandler classes implementation  | Major    |
| [INLONG-397](https://issues.apache.org/jira/browse/INLONG-397) | Add master info and other info web handler   | Major    |
| [INLONG-398](https://issues.apache.org/jira/browse/INLONG-398) | reinit project for using pybind11                            | Major    |
| [INLONG-399](https://issues.apache.org/jira/browse/INLONG-399) | expose C++ SDK method by Pybind11                                         | Major    |
| [INLONG-400](https://issues.apache.org/jira/browse/INLONG-400) | add example for consume message by bypind11                                 | Major    |
| [INLONG-402](https://issues.apache.org/jira/browse/INLONG-402) | add modify rest api for clusters                           | Major    |
| [INLONG-412](https://issues.apache.org/jira/browse/INLONG-402) | tube manager start stop scrrpts                           | Major    |
| [INLONG-415](https://issues.apache.org/jira/browse/INLONG-415) | exclude apache license for front end code  | Major    |

## Release 0.6.0-incubating - Released (as of 2020-09-25)

### New Features:

| JIRA  | Summary  | Priority |
| :---- | :------- | :------- |
| [INLONG-319](https://issues.apache.org/jira/browse/INLONG-319) | In the pull mode, consumers support the  suspension of consumption for a certain partition | Major    |
| [INLONG-3](https://issues.apache.org/jira/browse/INLONG-3)   | C++ SDK support in TubeMQ                                    | Normal   |

### IMPROVEMENTS:

| JIRA  | Summary  | Priority |
| :---- | :------- | :------- |
| [INLONG-311](https://issues.apache.org/jira/browse/INLONG-311) | Feedback more production information                 | Major    |
| [INLONG-312](https://issues.apache.org/jira/browse/INLONG-312) | Feedback more consumption information                | Major    |
| [INLONG-325](https://issues.apache.org/jira/browse/INLONG-325) | Add 406 ~ 408 error code to pullSelect call          | Major    |
| [INLONG-345](https://issues.apache.org/jira/browse/INLONG-345) | Optimize the call logic of getMessage() in Pull mode | Major    |
| [INLONG-352](https://issues.apache.org/jira/browse/INLONG-352) | Set the parameters of the example at startup         | Major    |
| [INLONG-353](https://issues.apache.org/jira/browse/INLONG-353) | Update LICENSE about C/C++ SDK's code reference      | Major    |
| [INLONG-356](https://issues.apache.org/jira/browse/INLONG-356) | C++ SDK Codec decode add requestid                   | Major    |
| [INLONG-327](https://issues.apache.org/jira/browse/INLONG-327) | Fix the concurrency problem in the example           | Normal   |

### BUG FIXES:

| JIRA                                                         | Summary                                                      | Priority |
| :----------------------------------------------------------- | :----------------------------------------------------------- | :------- |
| [INLONG-316](https://issues.apache.org/jira/browse/INLONG-316) | Where the port the port is aleady used, the  process throw the exception, but not exit | Major    |
| [INLONG-317](https://issues.apache.org/jira/browse/INLONG-317) | The Store Manager throws java.lang.NullPointerException      | Major    |
| [INLONG-320](https://issues.apache.org/jira/browse/INLONG-320) | Request for static web contents would get responses with no content | Major    |
| [INLONG-354](https://issues.apache.org/jira/browse/INLONG-354) | Found a dns translate bug in C/C++ sdk                       | Major    |
| [INLONG-306](https://issues.apache.org/jira/browse/INLONG-306) | Raise Nullpointer Exception when create tubemq instance      | Low      |
| [INLONG-359](https://issues.apache.org/jira/browse/INLONG-359) | TubeMQ consume speed dropped to 0 in some partitions, it is a very serious bug | Blocker  |

### SUB-TASK:

| JIRA  | Summary  | Priority |
| :---- | :------- | :------- |
| [INLONG-250](https://issues.apache.org/jira/browse/INLONG-250) | Create C/C++ configure files                                 | Major    |
| [INLONG-251](https://issues.apache.org/jira/browse/INLONG-251) | Create C/C++ Codec utils                                     | Major    |
| [INLONG-252](https://issues.apache.org/jira/browse/INLONG-252) | Create C/C++ Metadata classes                                | Major    |
| [INLONG-262](https://issues.apache.org/jira/browse/INLONG-262) | Create C++ flow control handler                              | Major    |
| [INLONG-263](https://issues.apache.org/jira/browse/INLONG-263) | Create C/C++ ini file read utils                             | Major    |
| [INLONG-266](https://issues.apache.org/jira/browse/INLONG-266) | [INLONG-266] Add Tencent/rapidjson as submodule              | Major    |
| [INLONG-267](https://issues.apache.org/jira/browse/INLONG-267) | Create C/C++ Message class                                   | Major    |
| [INLONG-269](https://issues.apache.org/jira/browse/INLONG-269) | Create C/C++ RmtDataCache class                              | Major    |
| [INLONG-272](https://issues.apache.org/jira/browse/INLONG-272) | Unified C/C++ files's code style                             | Major    |
| [INLONG-274](https://issues.apache.org/jira/browse/INLONG-274) | Support CMake compilation                                    | Major    |
| [INLONG-275](https://issues.apache.org/jira/browse/INLONG-275) | Thread Pool & Timer                                          | Major    |
| [INLONG-280](https://issues.apache.org/jira/browse/INLONG-280) | Create C/C++ subscribe info class                            | Major    |
| [INLONG-281](https://issues.apache.org/jira/browse/INLONG-281) | atomic_def.h use C++11 stdlib class                          | Major    |
| [INLONG-282](https://issues.apache.org/jira/browse/INLONG-282) | Create C/C++ return result class                             | Major    |
| [INLONG-283](https://issues.apache.org/jira/browse/INLONG-283) | Adjust C/C++ some file names: add "tubemq_" prefix           | Major    |
| [INLONG-285](https://issues.apache.org/jira/browse/INLONG-285) | Replace C/C++ pthread's mutex to std::mutex                  | Major    |
| [INLONG-286](https://issues.apache.org/jira/browse/INLONG-286) | Create C/C++ SDK's manager class                             | Major    |
| [INLONG-287](https://issues.apache.org/jira/browse/INLONG-287) | C++ SDK io buffer                                            | Major    |
| [INLONG-288](https://issues.apache.org/jira/browse/INLONG-288) | C++ SDK Codec interface                                      | Major    |
| [INLONG-289](https://issues.apache.org/jira/browse/INLONG-289) | C++ SDK Codec TubeMQ proto support                           | Major    |
| [INLONG-290](https://issues.apache.org/jira/browse/INLONG-290) | C++ SDK TCP Connect                                          | Major    |
| [INLONG-291](https://issues.apache.org/jira/browse/INLONG-291) | C++ SDK Connect Pool                                         | Major    |
| [INLONG-293](https://issues.apache.org/jira/browse/INLONG-293) | C++ SDK Create Future class                                  | Major    |
| [INLONG-296](https://issues.apache.org/jira/browse/INLONG-296) | Adjust the version information of all pom.xml                | Major    |
| [INLONG-300](https://issues.apache.org/jira/browse/INLONG-300) | Update LICENSE                                               | Major    |
| [INLONG-308](https://issues.apache.org/jira/browse/INLONG-308) | Upgrade Jetty 6 (mortbay) => Jetty 9 (eclipse)               | Major    |
| [INLONG-309](https://issues.apache.org/jira/browse/INLONG-309) | Add POST support to WebAPI                                   | Major    |
| [INLONG-326](https://issues.apache.org/jira/browse/INLONG-326) | [website] Added 405 ~ 408 error code definition              | Major    |
| [INLONG-347](https://issues.apache.org/jira/browse/INLONG-347) | C++ SDK Create client API                                    | Major    |
| [INLONG-348](https://issues.apache.org/jira/browse/INLONG-348) | C++SDK Client handler detail                                 | Major    |
| [INLONG-349](https://issues.apache.org/jira/browse/INLONG-349) | C++ SDK Create Thread Pool                                   | Major    |
| [INLONG-350](https://issues.apache.org/jira/browse/INLONG-350) | C++ SDK client code adj                                      | Major    |
| [INLONG-351](https://issues.apache.org/jira/browse/INLONG-351) | C++ SDK example&tests                                        | Major    |
| [INLONG-358](https://issues.apache.org/jira/browse/INLONG-358) | Adjust tubemq-manager, remove it from master, and develop with INLONG-336  branch | Major    |
| [INLONG-268](https://issues.apache.org/jira/browse/INLONG-268) | C++ SDK log module                                           | Normal   |
| [INLONG-292](https://issues.apache.org/jira/browse/INLONG-292) | C++ SDK singleton & executor_pool optimization               | Normal   |
| [INLONG-270](https://issues.apache.org/jira/browse/INLONG-270) | this point c++ SDK class                                     | Minor    |
| [INLONG-271](https://issues.apache.org/jira/browse/INLONG-271) | C++ SDK copy constructor and  assignment constructor         | Minor    |
| [INLONG-273](https://issues.apache.org/jira/browse/INLONG-273) | C++ SDK dir name change inc -> include/tubemq/               | Minor    |

## Release 0.5.0-incubating - released (as of 2020-07-22)

### NEW FEATURES:
| JIRA | Summary | Priority |
|:---- |:---- | :--- |
|[INLONG-122](https://issues.apache.org/jira/browse/INLONG-122) | Increase JAVA version collection of SDK environment |  Major|
|[INLONG-163](https://issues.apache.org/jira/browse/INLONG-163) | Flume sink for TubeMQ |  Major|
|[INLONG-197](https://issues.apache.org/jira/browse/INLONG-197) | Support TubeMQ connector for Apache Flink |  Major|
|[INLONG-238](https://issues.apache.org/jira/browse/INLONG-238) | Support TubeMQ connector for Apache Spark Streaming |  Major|
|[INLONG-239](https://issues.apache.org/jira/browse/INLONG-239) | support deployment on kubernetes |  Major|

### IMPROVEMENTS:
| JIRA | Summary | Priority |
|:---- |:---- | :--- |
| [INLONG-46](https://issues.apache.org/jira/browse/INLONG-46) | Correct some spelling issues |  Low|
| [INLONG-53](https://issues.apache.org/jira/browse/INLONG-53) | fix some typos |  Low|
| [INLONG-55](https://issues.apache.org/jira/browse/INLONG-55) | fix some typos |  Low|
| [INLONG-57](https://issues.apache.org/jira/browse/INLONG-57) | fix some typos & todo |  Low|
| [INLONG-58](https://issues.apache.org/jira/browse/INLONG-58) | fix some typos |  Low|
| [INLONG-60](https://issues.apache.org/jira/browse/INLONG-60) | Remove unnecessary synchronized & using IllegalArgumentException instead of IllegalStateException |  Low|
| [INLONG-61](https://issues.apache.org/jira/browse/INLONG-61) | minor update & fix some typos |  Low|
| [INLONG-64](https://issues.apache.org/jira/browse/INLONG-64) | minor update & fix some typos |  Low|
| [INLONG-67](https://issues.apache.org/jira/browse/INLONG-67) | remove synchronized & fix some typos |  Low|
| [INLONG-71](https://issues.apache.org/jira/browse/INLONG-71) | using IllegalArgumentException & fix some typos |  Low|
| [INLONG-73](https://issues.apache.org/jira/browse/INLONG-73) | remove duplicate codes & some minor updates |  Normal|
| [INLONG-74](https://issues.apache.org/jira/browse/INLONG-74) | minor updates for DefaultBdbStoreService |  Low|
| [INLONG-75](https://issues.apache.org/jira/browse/INLONG-75) | remove unused Logger |  Major|
| [INLONG-76](https://issues.apache.org/jira/browse/INLONG-76) | rename the classes |  Low|
| [INLONG-77](https://issues.apache.org/jira/browse/INLONG-77) | fix typo |  Low|
| [INLONG-79](https://issues.apache.org/jira/browse/INLONG-79) | fix typo |  Major|
| [INLONG-80](https://issues.apache.org/jira/browse/INLONG-80) | Fix some typos |  Low|
| [INLONG-82](https://issues.apache.org/jira/browse/INLONG-82) | Fix some typos & update comments |  Low|
| [INLONG-83](https://issues.apache.org/jira/browse/INLONG-83) | Fix some typos |  Low|
| [INLONG-87](https://issues.apache.org/jira/browse/INLONG-87) | Minor updates |  Low|
| [INLONG-89](https://issues.apache.org/jira/browse/INLONG-89) | Minor updates |  Low|
| [INLONG-90](https://issues.apache.org/jira/browse/INLONG-90) | Remove unused codes in TubeBroker |  Normal|
| [INLONG-91](https://issues.apache.org/jira/browse/INLONG-91) | replace explicit type with <> |  Low|
| [INLONG-93](https://issues.apache.org/jira/browse/INLONG-93) | Substitute the parameterized type for client module & missed server module |  Low|
| [INLONG-94](https://issues.apache.org/jira/browse/INLONG-94) | Substitute the parameterized type for core module |  Low|
| [INLONG-95](https://issues.apache.org/jira/browse/INLONG-95) | Substitute the parameterized type for server module |  Low|
| [INLONG-96](https://issues.apache.org/jira/browse/INLONG-96) | Fix typo & use IllegalArgumentException |  Low|
| [INLONG-98](https://issues.apache.org/jira/browse/INLONG-98) | Fix typo & Simplify 'instanceof' judgment |  Low|
| [INLONG-100](https://issues.apache.org/jira/browse/INLONG-100) | Fix typos & remove unused codes |  Low|
| [INLONG-101](https://issues.apache.org/jira/browse/INLONG-101) | Optimize code & Fix type |  Low|
| [INLONG-103](https://issues.apache.org/jira/browse/INLONG-103) | Substitute Chinese comments with English |  Normal|
| [INLONG-108](https://issues.apache.org/jira/browse/INLONG-108) | About maven jdk version configuration problem |  Minor|
| [INLONG-127](https://issues.apache.org/jira/browse/INLONG-127) | Fixed a bug & minor changes |  Low|
| [INLONG-128](https://issues.apache.org/jira/browse/INLONG-128) | Shorten the log clearup check cycle |  Major|
| [INLONG-138](https://issues.apache.org/jira/browse/INLONG-138) | Optimize core module test case code |  Low|
| [INLONG-141](https://issues.apache.org/jira/browse/INLONG-141) | Remove the requirement to provide localHostIP |  Major|
| [INLONG-152](https://issues.apache.org/jira/browse/INLONG-152) | Modify the master.ini file's annotations |  Normal|
| [INLONG-154](https://issues.apache.org/jira/browse/INLONG-154) | Modify the wrong comment & Minor changes for example module |  Low|
| [INLONG-155](https://issues.apache.org/jira/browse/INLONG-155) | Use enum class for consume position |  Normal|
| [INLONG-156](https://issues.apache.org/jira/browse/INLONG-156) | Update for README.md |  Normal|
| [INLONG-166](https://issues.apache.org/jira/browse/INLONG-166) | Hide `bdbStore` configs in master.ini |  Major|
| [INLONG-167](https://issues.apache.org/jira/browse/INLONG-167) | Change to relative paths in default configs |  Trivial|
| [INLONG-168](https://issues.apache.org/jira/browse/INLONG-168) | Example module: remove localhost IP configuration parameters |  Minor|
| [INLONG-170](https://issues.apache.org/jira/browse/INLONG-170) | improve build/deployment/configuration for quick start |  Major|
| [INLONG-196](https://issues.apache.org/jira/browse/INLONG-196) | use log to print exception |  Low|
| [INLONG-201](https://issues.apache.org/jira/browse/INLONG-201) | [Website] Adjust user guide & fix demo example |  Major|
| [INLONG-202](https://issues.apache.org/jira/browse/INLONG-202) | Add protobuf protocol syntax declaration |  Major|
| [INLONG-213](https://issues.apache.org/jira/browse/INLONG-213) | Optimize code & Minor changes |  Low|
| [INLONG-216](https://issues.apache.org/jira/browse/INLONG-216) | use ThreadUtil.sleep replace Thread.sleep |  Low|
| [INLONG-222](https://issues.apache.org/jira/browse/INLONG-222) | Optimize code: Unnecessary boxing/unboxing conversion |  Normal|
| [INLONG-224](https://issues.apache.org/jira/browse/INLONG-224) | Fixed: Unnecessary conversion to string inspection for server module |  Low|
| [INLONG-226](https://issues.apache.org/jira/browse/INLONG-226) | Add Windows startup scripts |  High|
| [INLONG-227](https://issues.apache.org/jira/browse/INLONG-227) | remove build guide in docker-build readme |  Major|
| [INLONG-232](https://issues.apache.org/jira/browse/INLONG-232) | TubeBroker#register2Master, reconnect and wait |  Low|
| [INLONG-234](https://issues.apache.org/jira/browse/INLONG-234) | Add .asf.yaml to change notifications |  Major|
| [INLONG-235](https://issues.apache.org/jira/browse/INLONG-235) | Add code coverage supporting for pull request created. |  Normal|
| [INLONG-237](https://issues.apache.org/jira/browse/INLONG-237) | add maven module build for docker image |  Major|

### BUG FIXES:
| JIRA | Summary | Priority |
|:---- |:---- | :--- |
| [INLONG-47](https://issues.apache.org/jira/browse/INLONG-47) | Fix some typos |  Major|
| [INLONG-102](https://issues.apache.org/jira/browse/INLONG-102) | Fix question [INLONG-101] [Optimize code] |  Major|
| [INLONG-121](https://issues.apache.org/jira/browse/INLONG-121) | Fix compilation alarm |  Major|
| [INLONG-139](https://issues.apache.org/jira/browse/INLONG-139) | a bug in the equals method of the TubeClientConfig class |  Major|
| [INLONG-157](https://issues.apache.org/jira/browse/INLONG-157) | Optimize Broker disk anomaly check |  Normal|
| [INLONG-158](https://issues.apache.org/jira/browse/INLONG-158) | nextWithAuthInfo2B status should be managed independently according to Broker |  Normal|
| [INLONG-159](https://issues.apache.org/jira/browse/INLONG-159) | Fix some typos |  Normal|
| [INLONG-165](https://issues.apache.org/jira/browse/INLONG-165) | Remove unnecessary fiiles |  Major|
| [INLONG-205](https://issues.apache.org/jira/browse/INLONG-205) | Duplicate dependency of jetty in tuber-server pom file |  Minor|
| [INLONG-206](https://issues.apache.org/jira/browse/INLONG-206) | There are some residual files after executed unit tests |  Major|
| [INLONG-210](https://issues.apache.org/jira/browse/INLONG-210) | Add log4j properties file for unit tests |  Minor|
| [INLONG-217](https://issues.apache.org/jira/browse/INLONG-217) | UPdate the je download path |  Major|
| [INLONG-218](https://issues.apache.org/jira/browse/INLONG-218) | build failed: Too many files with unapproved license |  Major|
| [INLONG-230](https://issues.apache.org/jira/browse/INLONG-230) | TubeMQ run mvn test failed with openjdk version 13.0.2 |  Major|
| [INLONG-236](https://issues.apache.org/jira/browse/INLONG-236) | Can't get dependencies from the maven repository |  Major|
| [INLONG-253](https://issues.apache.org/jira/browse/INLONG-253) | tube-consumer fetch-worker cpu used too high |  Major|
| [INLONG-254](https://issues.apache.org/jira/browse/INLONG-254) | support using different mapping port for standalone mode |  Major|
| [INLONG-265](https://issues.apache.org/jira/browse/INLONG-265) | Unexpected broker disappearance in broker list after updating default broker metadata |  Major|

### TASK:
| JIRA | Summary | Priority |
|:---- |:---- | :--- |
|[INLONG-193](https://issues.apache.org/jira/browse/INLONG-193)  | Update project document content |  Major |

### SUB-TASK:
| JIRA | Summary | Priority |
|:---- |:---- | :--- |
|[INLONG-123](https://issues.apache.org/jira/browse/INLONG-123) | Batch flush data to disk |  Major |
|[INLONG-126](https://issues.apache.org/jira/browse/INLONG-126) | Increase the unflushed data bytes control |  Major |
|[INLONG-140](https://issues.apache.org/jira/browse/INLONG-140) | Remove the SSD auxiliary consumption function |  Major |
|[INLONG-160](https://issues.apache.org/jira/browse/INLONG-160) | Improve the protocol between Broker and Master |  Major |
|[INLONG-169](https://issues.apache.org/jira/browse/INLONG-169) | support build with docker image |  Major |
|[INLONG-171](https://issues.apache.org/jira/browse/INLONG-171) | master and broker support config hostname with “localhost” or "127.0.0.1" or dns address |  Major |
|[INLONG-172](https://issues.apache.org/jira/browse/INLONG-172) | simplify start/stop script |  Major |
|[INLONG-173](https://issues.apache.org/jira/browse/INLONG-173) | change jvm memory parameters for default deployment |  Major |
|[INLONG-174](https://issues.apache.org/jira/browse/INLONG-174) | hange defaule accessing url of web gui to http://your-master-ip:8080 |  Major |
|[INLONG-178](https://issues.apache.org/jira/browse/INLONG-178) | change default IPs configuration to localhost |  Major |
|[INLONG-188](https://issues.apache.org/jira/browse/INLONG-188) | the example for demo topic catch exception |  Major |
|[INLONG-194](https://issues.apache.org/jira/browse/INLONG-194) | [website]Remove SSD auxiliary storage introduction |  Major |
|[INLONG-195](https://issues.apache.org/jira/browse/INLONG-195) | [website] Adjust the content of the Chinese part of the document |  Major |
|[INLONG-198](https://issues.apache.org/jira/browse/INLONG-198) | Support TubeMQ source for flink |  Major |
|[INLONG-199](https://issues.apache.org/jira/browse/INLONG-199) | Support TubeMQ sink for flink |  Major |
|[INLONG-204](https://issues.apache.org/jira/browse/INLONG-204) | Remove document address guideline |  Major |
|[INLONG-221](https://issues.apache.org/jira/browse/INLONG-221) | make quick start doc more easy for reading |  Major |
|[INLONG-240](https://issues.apache.org/jira/browse/INLONG-240) | add status command for broker/master script |  Major |
|[INLONG-241](https://issues.apache.org/jira/browse/INLONG-241) | add helm chart for tubemq |  Major |
|[INLONG-242](https://issues.apache.org/jira/browse/INLONG-242) | Support Table interface for TubeMQ flink connector |  Major |
|[INLONG-244](https://issues.apache.org/jira/browse/INLONG-244) | tubemq web support access using proxy IP |  Major |
|[INLONG-246](https://issues.apache.org/jira/browse/INLONG-246) | support register broker using hostname |  Major |
|[INLONG-295](https://issues.apache.org/jira/browse/INLONG-295) | Modify CHANGES.md to add 0.5.0 version release modification |  Major |
|[INLONG-299](https://issues.apache.org/jira/browse/INLONG-299) | Fix RAT check warnning |  Major |
|[INLONG-300](https://issues.apache.org/jira/browse/INLONG-300) | Update LICENSE |  Major |



## Release 0.3.0-incubating - Released (as of 2020-06-08)

### NEW FEATURES:

| JIRA | Summary | Priority |
|:---- |:---- | :--- |
|[INLONG-42](https://issues.apache.org/jira/browse/INLONG-42) | Add peer information about message received  Major  New Feature |  Major|

### IMPROVEMENTS:
| JIRA | Summary | Priority |
|:---- |:---- | :--- |
| [INLONG-16](https://issues.apache.org/jira/browse/INLONG-16) |Correct BdbStoreService#isPrimaryNodeActived to BdbStoreService#isPrimaryNodeActive|  Low|
| [INLONG-18](https://issues.apache.org/jira/browse/INLONG-18) |Correct TMaster#idGenerater to TMaster#idGenerator|  Low|
| [INLONG-19](https://issues.apache.org/jira/browse/INLONG-19) |Correct parameter names to fit in camel case|  Low|
| [INLONG-20](https://issues.apache.org/jira/browse/INLONG-20) |Correct DefaultLoadBalancer#balance parameter  | Low|
| [INLONG-21](https://issues.apache.org/jira/browse/INLONG-21) |Change version number from x.y-SNAPSHOT to x.y.z-incubating-SNAPSHOT|  Normal|
| [INLONG-22](https://issues.apache.org/jira/browse/INLONG-22) |Correct ClientSubInfo#getTopicProcesser -> ClientSubInfo#getTopicProcessor|  Low|
| [INLONG-23](https://issues.apache.org/jira/browse/INLONG-23) |Improve project README content introduction|  Major|
| [INLONG-24](https://issues.apache.org/jira/browse/INLONG-24) |Add NOTICE and adjust LICENSE  | Major|
| [INLONG-26](https://issues.apache.org/jira/browse/INLONG-26) |correct spelling (difftime-> diffTime)  |Low|
| [INLONG-27](https://issues.apache.org/jira/browse/INLONG-27) |replace StringBuffer with StringBuilder |  Major|
| [INLONG-28](https://issues.apache.org/jira/browse/INLONG-28) |ignore path error  |Major|
| [INLONG-29](https://issues.apache.org/jira/browse/INLONG-29) |Change the package name to org.apache.tubemq.""  |Major|
| [INLONG-33](https://issues.apache.org/jira/browse/INLONG-33) |refactor enum implement from annoymouse inner class  | Major|
| [INLONG-38](https://issues.apache.org/jira/browse/INLONG-38) |Add Broker's running status check  | Major||
| [INLONG-39](https://issues.apache.org/jira/browse/INLONG-39) |Optimize the loadMessageStores() logic  | Nor|mal|
| [INLONG-40](https://issues.apache.org/jira/browse/INLONG-40) |Optimize message disk store classes's logic  | Major|
| [INLONG-43](https://issues.apache.org/jira/browse/INLONG-43) |Add DeletePolicy's value check  | Major|
| [INLONG-44](https://issues.apache.org/jira/browse/INLONG-44) |Remove unnecessary synchronized definition of shutdown () function  | Normal|
| [INLONG-49](https://issues.apache.org/jira/browse/INLONG-49) |setTimeoutTime change to updTimeoutTime  | Major|
| [INLONG-50](https://issues.apache.org/jira/browse/INLONG-50) |Replace fastjson to gson  | Major|
| [INLONG-7](https://issues.apache.org/jira/browse/INLONG-7) | Using StringBuilder instead of StringBuffer in BaseResult  | Low|
| [INLONG-9](https://issues.apache.org/jira/browse/INLONG-9) | Remove some unnecessary code  | Minor |

### BUG FIXES:
| JIRA | Summary | Priority |
|:---- |:---- | :--- |
| [INLONG-10](https://issues.apache.org/jira/browse/INLONG-10) |Fix Javadoc error|Low|
| [INLONG-14](https://issues.apache.org/jira/browse/INLONG-14) |Some compilation errors|Major|
| [INLONG-15](https://issues.apache.org/jira/browse/INLONG-15) |Correct typo in http_access_API_definition.md|Low|
| [INLONG-32](https://issues.apache.org/jira/browse/INLONG-32) |File path not match with package name in tubemq-client module|Major|
| [INLONG-35](https://issues.apache.org/jira/browse/INLONG-35) |check illegal package's field value|Normal|
| [INLONG-36](https://issues.apache.org/jira/browse/INLONG-36) |Remove unnecessary removefirst() function printing|Normal|
| [INLONG-37](https://issues.apache.org/jira/browse/INLONG-37) |Offset is set to 0 when Broker goes offline|Major|
| [INLONG-45](https://issues.apache.org/jira/browse/INLONG-45) |Check groupName with checkHostName function|Major|
| [INLONG-48](https://issues.apache.org/jira/browse/INLONG-48) |No timeout when setting consumer timeout|Major|
| [INLONG-59](https://issues.apache.org/jira/browse/INLONG-59) |Null pointer exception is thrown while constructing ConsumerConfig with MasterInfo|Normal|
| [INLONG-62](https://issues.apache.org/jira/browse/INLONG-62) |consumed and set consumerConfig.setConsumeModel (0) for the first time|Major|
| [INLONG-66](https://issues.apache.org/jira/browse/INLONG-66) |TubeSingleSessionFactory shutdown bug|Normal|
| [INLONG-85](https://issues.apache.org/jira/browse/INLONG-85) |There is NPE when creating PullConsumer with TubeSingleSessionFactory|Major|
| [INLONG-88](https://issues.apache.org/jira/browse/INLONG-88) |Broker does not take effect after the deletePolicy value is changed|Major|
| [INLONG-149](https://issues.apache.org/jira/browse/INLONG-149) |Some of the consumers stop consuming their corresponding partitions and never release the partition to others|Major|
| [INLONG-153](https://issues.apache.org/jira/browse/INLONG-153) |update copyright notices year to 2020|  Major |
| [INLONG-165](https://issues.apache.org/jira/browse/INLONG-165) |Remove unnecessary fiiles|  Major |

### TASK:
| JIRA | Summary | Priority |
|:---- |:---- | :--- |
|[INLONG-12](https://issues.apache.org/jira/browse/INLONG-12)  |Change to use Apache License V2   |  Major |

### SUB-TASK:
| JIRA | Summary | Priority |
|:---- |:---- | :--- |
|[INLONG-130](https://issues.apache.org/jira/browse/INLONG-130) |Generate CHANGES.md and DISCLAIMER-WIP   |  Major |
|[INLONG-133](https://issues.apache.org/jira/browse/INLONG-133) |Add Apache parent pom |  Major |
|[INLONG-134](https://issues.apache.org/jira/browse/INLONG-134) |add maven-source-plugin for generate source jar|  Major |
|[INLONG-135](https://issues.apache.org/jira/browse/INLONG-135) |Refactoring all pom.xml|  Major |
|[INLONG-136](https://issues.apache.org/jira/browse/INLONG-136) |Add LICENSE/NOTICE/DISCLAIMER-WIP to binary package|  Major |
