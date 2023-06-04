
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

# Release InLong 1.7.0 - Released (as of 2023-05-16)
### Agent
|                            ISSUE                            | Summary                                                                                                                                            |
|:-----------------------------------------------------------:|:---------------------------------------------------------------------------------------------------------------------------------------------------|
| [INLONG-7847](https://github.com/apache/inlong/issues/7847) | [Bug][Agent] Failed to create MySQL reader                                                                                                         |
| [INLONG-7783](https://github.com/apache/inlong/issues/7783) | [Feature][Agent] Support sink data tor Kafka                                                                                                       |
| [INLONG-7752](https://github.com/apache/inlong/issues/7752) | [Bug][Agent] PulsarSink threadPool throw reject exception                                                                                          |                         
| [INLONG-7976](https://github.com/apache/inlong/issues/7976) | [Bug][Agent] The data collected by the agent is incomplete                                                                                         |
| [INLONG-8026](https://github.com/apache/inlong/issues/8026) | [Improve][Agent] Improve the Agent performance                                                                                                     |

### DataProxy
|                            ISSUE                            | Summary                                                                                                  |
|:-----------------------------------------------------------:|:---------------------------------------------------------------------------------------------------------|
| [INLONG-7931](https://github.com/apache/inlong/issues/7931) | [Improve][DataProxy] Optimize common.properties related control mechanism                                |
| [INLONG-7898](https://github.com/apache/inlong/issues/7898) | [Improve][DataProxy] Clean up useless configuration files in the ConfigManager class                     |
| [INLONG-7769](https://github.com/apache/inlong/issues/7769) | [Bug][DataProxy] NPE when request Inlong Manager failed                                                  |
| [INLONG-7512](https://github.com/apache/inlong/issues/7512) | [Improve][DataProxy] Update the metrics log level to avoid the log file increasing quickly               |
| [INLONG-7766](https://github.com/apache/inlong/issues/7766) | [Bug][DataProxySDK] Adjusted frame length exceeds occurred when reporting data through the HTTP protocol |

### TubeMQ
|                            ISSUE                            | Summary                                                              |
|:-----------------------------------------------------------:|:---------------------------------------------------------------------|
| [INLONG-7926](https://github.com/apache/inlong/issues/7926) | [Feature][TubeMQ] Add ""Register2Master"" method for GO SDK "        |

### Manager
|                            ISSUE                            | Summary                                                                                        |
|:-----------------------------------------------------------:|:-----------------------------------------------------------------------------------------------|
| [INLONG-8035](https://github.com/apache/inlong/issues/8035) | [Bug][Manager] Non-file tasks cannot be recovered from the heartbeat timeout state             |
| [INLONG-8021](https://github.com/apache/inlong/issues/8021) | [Improve][Manager] Periodically delete sources with inconsistent states                        |
| [INLONG-8006](https://github.com/apache/inlong/issues/8006) | [Improve][Manager] Set displayname for the auto-registered cluster                             |
| [INLONG-7999](https://github.com/apache/inlong/issues/7999) | [Improve][Manager] Support PostgreSQL data node                                                |
| [INLONG-7996](https://github.com/apache/inlong/issues/7996) | [Improve][Manager] Support issued kafka consumer group to sort                                 |
| [INLONG-7987](https://github.com/apache/inlong/issues/7987) | [Improve][Manager] Add a heartbeat timeout status to the source                                |
| [INLONG-7981](https://github.com/apache/inlong/issues/7981) | [Bug][Manager] Failed to stop source correctly when suspend a group                            |
| [INLONG-7948](https://github.com/apache/inlong/issues/7948) | [Improve][Manager] Add user authentication when operate inlong consume                         |
| [INLONG-7946](https://github.com/apache/inlong/issues/7946) | [Improve][Manager] Add user authentication when bind clusterTag                                |
| [INLONG-7941](https://github.com/apache/inlong/issues/7941) | [Improve][Manager][Dashborad] Query group to distinguish lightweight                           |
| [INLONG-7940](https://github.com/apache/inlong/issues/7940) | [Bug][Manager][Sort] Use Pulsar subscriptions in Pulsar connector                              |
| [INLONG-7938](https://github.com/apache/inlong/issues/7938) | [Bug][Manager] The consume list interface does not filter by request                           |
| [INLONG-7936](https://github.com/apache/inlong/issues/7936) | [Improve][Manager] Support issued pulsar subscriptions to sort                                 |
| [INLONG-7934](https://github.com/apache/inlong/issues/7934) | [Improve][Manager] Optimize the serializationType to support debezium json                     |
| [INLONG-7912](https://github.com/apache/inlong/issues/7912) | [Improve][Manager] Only response DataProxy nodes in normal status                              |
| [INLONG-7895](https://github.com/apache/inlong/issues/7895) | [Feature][Manager] Support field description when parsing field by SQL                         |
| [INLONG-7893](https://github.com/apache/inlong/issues/7893) | [Feature][Manager] Support field description when parsing field by JSON                        |
| [INLONG-7890](https://github.com/apache/inlong/issues/7890) | [Improve][Manager] Add checks for unmodifiable data_node_name and cluster_name                 |
| [INLONG-7888](https://github.com/apache/inlong/issues/7888) | [Bug][Manager] Failed to create lightweight task                                               |
| [INLONG-7883](https://github.com/apache/inlong/issues/7883) | [Improve][Manager] Invalidate user session when deleting user                                  |
| [INLONG-7867](https://github.com/apache/inlong/issues/7867) | [Feature][Manager] Support data validation when importing Excel file                           |
| [INLONG-7844](https://github.com/apache/inlong/issues/7844) | [Improve][Manager] Support to set cluster when create table for clickhouse                     |
| [INLONG-7843](https://github.com/apache/inlong/issues/7843) | [Feature][Manager] Creating the schema of StreamSource by importing an Excel file              |
| [INLONG-7841](https://github.com/apache/inlong/issues/7841) | [Feature][Manager] Support title style and font when exporting Excel file                      |
| [INLONG-7839](https://github.com/apache/inlong/issues/7839) | [Improve][Manager] Bump version of apache-poi:poi to 5.2.3                                     |
| [INLONG-7837](https://github.com/apache/inlong/issues/7837) | [Bug][Manager] The Admin user cannot modify streamSource when it is not the responsible person |
| [INLONG-7835](https://github.com/apache/inlong/issues/7835) | [Improve][Manager] The permission is removed when a user is deleted                            |
| [INLONG-7823](https://github.com/apache/inlong/issues/7823) | [Improve][Manager] Supports creating clickhouse tables using the ReplicatedMergeTree engine    |
| [INLONG-7820](https://github.com/apache/inlong/issues/7820) | [Feature][Manager] Support style and font when exporting Excel file                            |
| [INLONG-7816](https://github.com/apache/inlong/issues/7816) | [Feature][Manager] Support validation rules when exporting Excel file                          |
| [INLONG-7810](https://github.com/apache/inlong/issues/7810) | [Bug][Manager] No token field in the return result of getAllConfig                             |
| [INLONG-7804](https://github.com/apache/inlong/issues/7804) | [Improve][Manager] Limit the length of user password                                           |
| [INLONG-7800](https://github.com/apache/inlong/issues/7800) | [Bug][Manager] Update redis data node failed                                                   |
| [INLONG-7798](https://github.com/apache/inlong/issues/7798) | [Improve][Manager] Add user authentication when operate workflow                               |
| [INLONG-7792](https://github.com/apache/inlong/issues/7792) | [Feature][Manager] Support export Excel template file of StreamSource                          |
| [INLONG-7778](https://github.com/apache/inlong/issues/7778) | [Feature][Manager] Optimize the create command for ease of use                                 |
| [INLONG-7774](https://github.com/apache/inlong/issues/7774) | [Improve][Manager] Add permission verification for streamSource                                |
| [INLONG-7760](https://github.com/apache/inlong/issues/7760) | [Bug][Manager] Parse fields failed for streamSink and InlongStream                             |
| [INLONG-7730](https://github.com/apache/inlong/issues/7730) | [Feature][Manager] Support node management for Redis                                           |
| [INLONG-7722](https://github.com/apache/inlong/issues/7722) | [Bug][Manager] The task status is inconsistent with the returned result                        |
| [INLONG-7720](https://github.com/apache/inlong/issues/7720) | [Umbrella][Manager] Creating schema of StreamSource by Excel                                   |
| [INLONG-7719](https://github.com/apache/inlong/issues/7719) | [Feature][Manager] Support test connection for Redis in NodeManagement                         |
| [INLONG-7713](https://github.com/apache/inlong/issues/7713) | [Feature][Manager] Support test connection for Apache Kudu                                     |
| [INLONG-7711](https://github.com/apache/inlong/issues/7711) | [Feature][Sort][Manager] Support specifying parameters for the kudu client                     |
| [INLONG-7706](https://github.com/apache/inlong/issues/7706) | [Bug][Manager] sink is always in the configuration after being saved                           |
| [INLONG-7701](https://github.com/apache/inlong/issues/7701) | [Feature][Manager] Support test connection for hudi in NodeManagement                          |
| [INLONG-7690](https://github.com/apache/inlong/issues/7690) | [Feature][Manager] Creating schema of StreamSource by CSV                                      |
| [INLONG-7688](https://github.com/apache/inlong/issues/7688) | [Feature][Manager] Creating schema of StreamSource by SQL                                      |
| [INLONG-7686](https://github.com/apache/inlong/issues/7686) | [Feature][Manager] Support node management for Apache Kudu                                     |
| [INLONG-7678](https://github.com/apache/inlong/issues/7678) | [Improve][Manager] Rename FROZEN to STOP for source status                                     |
| [INLONG-7675](https://github.com/apache/inlong/issues/7675) | [Improve][Manager] Check OrderType when calling listAll to prevent sql injection               |
| [INLONG-7673](https://github.com/apache/inlong/issues/7673) | [Improve][Manager] Remove whitespace when saving or updating URLs                              |
| [INLONG-7670](https://github.com/apache/inlong/issues/7670) | [Improve][Manager] Filter out sinks with empty cluster                                         |
| [INLONG-7666](https://github.com/apache/inlong/issues/7666) | [Improve][Manager] Support to freezing and restarting streamSource                             |
| [INLONG-7658](https://github.com/apache/inlong/issues/7658) | [Improve][Manager] Optimized table index                                                       |
| [INLONG-7655](https://github.com/apache/inlong/issues/7655) | [Improve][Manager] Remove the dependency of hudi-flink1.13-bundle                              |
| [INLONG-7651](https://github.com/apache/inlong/issues/7651) | [Bug][Manager] StreamSource in normal state cannot be modified                                 |
| [INLONG-7648](https://github.com/apache/inlong/issues/7648) | [Improve][Manager] Support list streams by a specific group principal                          |
| [INLONG-7632](https://github.com/apache/inlong/issues/7632) | [Improve][Manager] Mask the audit information that Sort sent successfully                      |
| [INLONG-7625](https://github.com/apache/inlong/issues/7625) | [Improve][Manager] Make data encoding type as a common property of StreamSink                  |
| [INLONG-7619](https://github.com/apache/inlong/issues/7619) | [Improve][Manager] Support update and retry MySQL sources after updating MySQLDataNode         |
| [INLONG-7004](https://github.com/apache/inlong/issues/7004) | [Feature][Dashboard][Manager] Creating schema of  StreamSource by Statement                    |
| [INLONG-6672](https://github.com/apache/inlong/issues/6672) | [Feature][Manager] Add `inlongctl` status management for the group                             |

### Sort
|                            ISSUE                            | Summary                                                                                                                                            |
|:-----------------------------------------------------------:|:---------------------------------------------------------------------------------------------------------------------------------------------------|
| [INLONG-7958](https://github.com/apache/inlong/issues/7958) | [Bug][Sort] MongoDB's schema becomes unordered after extracting the row data                                                                       |
| [INLONG-7957](https://github.com/apache/inlong/issues/7957) | [Bug][Sort] The canaljson's type of Oracle CDC and MongoDB CDC is inconsistent with MySQL CDC                                                      |
| [INLONG-7952](https://github.com/apache/inlong/issues/7952) | [Improve][Sort]  Mask sensitive message of Flink SQL in the logs                                                                                   |
| [INLONG-7945](https://github.com/apache/inlong/issues/7945) | [Bug][Sort] MongoDB CDC unable to output the 'replace' records                                                                                     |
| [INLONG-7940](https://github.com/apache/inlong/issues/7940) | [Bug][Manager][Sort] Use Pulsar subscriptions in Pulsar connector                                                                                  |
| [INLONG-7906](https://github.com/apache/inlong/issues/7906) | [Improve][Sort] Improve logic of calculation object byte size                                                                                      |
| [INLONG-7858](https://github.com/apache/inlong/issues/7858) | [Bug][Sort] Oracle CDC uploaded two different db name metrics for the same table                                                                   |
| [INLONG-7857](https://github.com/apache/inlong/issues/7857) | [Bug][Sort] Duplicated chunk happens when open scan-newly-added-table in mysql-cdc                                                                 |
| [INLONG-7855](https://github.com/apache/inlong/issues/7855) | [Bug][Sort] Hang up reader in snapshot phase when reducing paralleism                                                                              |
| [INLONG-7850](https://github.com/apache/inlong/issues/7850) | [Feature][Sort] Add support for extracting ddl statement and operation from origin data                                                            |
| [INLONG-7831](https://github.com/apache/inlong/issues/7831) | [Improve][Sort] Using spilling disk map to reduce memory loss for buffer per parititon data                                                        |
| [INLONG-7829](https://github.com/apache/inlong/issues/7829) | [Improve][Sort] Add mini-batch pre aggregate by partition  when ingesting data into iceberg                                                        |
| [INLONG-7825](https://github.com/apache/inlong/issues/7825) | [Bug][Sort] MySQL CDC cannot capture truncate ddl statement                                                                                        |
| [INLONG-7813](https://github.com/apache/inlong/issues/7813) | [Bug][Sort] ES6 multiple sink initialize error                                                                                                     |
| [INLONG-7809](https://github.com/apache/inlong/issues/7809) | [Improve][Sort] ES multiple sink support dirty data runtime strategies                                                                             |
| [INLONG-7790](https://github.com/apache/inlong/issues/7790) | [Bug][Sort] Setting ""scanNewlyAddedTableEnabled=true"" and ""scan.startup.mode=latest-offset"" in MySQL CDC failed to capture newly added tables" |
| [INLONG-7787](https://github.com/apache/inlong/issues/7787) | [Bug][Sort] Fix cannot output drop statement                                                                                                       |
| [INLONG-7767](https://github.com/apache/inlong/issues/7767) | [Bug][Sort]Doris connector does not real delete record because of columns header losing                                                            |
| [INLONG-7762](https://github.com/apache/inlong/issues/7762) | [Bug][Sort] Fix capturing unrelated tables when open DDL change detection                                                                          |
| [INLONG-7731](https://github.com/apache/inlong/issues/7731) | [Bug][Sort] Fix the problem of calling getAndSetPkFromErrMsg method parameter order                                                                |
| [INLONG-7724](https://github.com/apache/inlong/issues/7724) | [Improve][Sort] Add rate limit for ingesting into iceberg                                                                                          |
| [INLONG-7715](https://github.com/apache/inlong/issues/7715) | [Bug][Sort] Fix single table metric invalid                                                                                                        |
| [INLONG-7711](https://github.com/apache/inlong/issues/7711) | [Feature][Sort][Manager] Support specifying parameters for the kudu client                                                                         |
| [INLONG-7708](https://github.com/apache/inlong/issues/7708) | [Bug][Sort] Fix the error that occurs when adding a table during whole database synchronization in Oracle.                                         |
| [INLONG-7700](https://github.com/apache/inlong/issues/7700) | [Improve][Sort] Update some copy class file when update mongo-cdc version to 2.3                                                                   |
| [INLONG-7695](https://github.com/apache/inlong/issues/7695) | [Bug][Sort] NPE occurs when running Oracle all database migration jobs                                                                             |
| [INLONG-7693](https://github.com/apache/inlong/issues/7693) | [Improve][Sort] MySQL CDC Connector supports specifying field synchronization                                                                      |
| [INLONG-7683](https://github.com/apache/inlong/issues/7683) | [Bug][Sort] Unit test error for Oracle connector                                                                                                   |
| [INLONG-7680](https://github.com/apache/inlong/issues/7680) | [Improve][Sort] MySQL CDC supports unsigned zerofill data type                                                                                     |
| [INLONG-7660](https://github.com/apache/inlong/issues/7660) | [Feature][Sort] Support DDL model for MySQL connector when running in all migrate mode                                                             |
| [INLONG-7653](https://github.com/apache/inlong/issues/7653) | [Feature][Sort] Support archiving dirty data and metrics for Iceberg connector                                                                     |
| [INLONG-7635](https://github.com/apache/inlong/issues/7635) | [Feature][Sort] MongoDB CDC supports metrics with enable incremental snapshots                                                                     |
| [INLONG-7581](https://github.com/apache/inlong/issues/7581) | [Feature][Sort] Support multiple-sink migration for Elasticsearch                                                                                  |
| [INLONG-7554](https://github.com/apache/inlong/issues/7554) | [Feature][Sort] MySQL CDC supports parsing gh-ost records                                                                                          |
| [INLONG-7553](https://github.com/apache/inlong/issues/7553) | [Feature][Sort] Mysql CDC support output DDL model in all migrate                                                                                  |
| [INLONG-7249](https://github.com/apache/inlong/issues/7249) | [Feature][Sort] JDBC accurate dirty data archive and metric calculation                                                                            |
| [INLONG-6668](https://github.com/apache/inlong/issues/6668) | [Improve][Sort] Unify the constants in `inlong-common`  and `sort-connectors`                                                                      |
| [INLONG-7970](https://github.com/apache/inlong/issues/7970) | [Bug][Sort] java.lang.ClassCastException: java.lang.String cannot be cast to org.apache.flink.table.data.StringData                                |
| [INLONG-7747](https://github.com/apache/inlong/issues/7747) | [Umbrella][Sort] Improve memory stability of data ingesting into iceberg                                                                           |
| [INLONG-6545](https://github.com/apache/inlong/issues/6545) | [Improve][Sort] Accurately parse the schema type and completely match the missing precision information                                            |
| [INLONG-8029](https://github.com/apache/inlong/issues/8029) | [Improve][Sort] Use fixed subscription name when start a pulsar reader                                                                             |

### Audit
|                            ISSUE                            | Summary                                                                                   |
|:-----------------------------------------------------------:|:------------------------------------------------------------------------------------------|
| [INLONG-7646](https://github.com/apache/inlong/issues/7646) | [Bug][Audit] NPE when mq configuration is not registered                                  |
| [INLONG-7641](https://github.com/apache/inlong/issues/7641) | [Feature][Script] Modify script for Audit standalone deployment                           |
| [INLONG-7636](https://github.com/apache/inlong/issues/7636) | [Feature][Docker] Modify the Kafka config for Audit docker deployment                     |
| [INLONG-7413](https://github.com/apache/inlong/issues/7413) | [Feature][Audit] Proxy and Store get MQ address from the Manager service                  |

### Dashboard
|                            ISSUE                            | Summary                                                                                                        |
|:-----------------------------------------------------------:|:---------------------------------------------------------------------------------------------------------------|
| [INLONG-7955](https://github.com/apache/inlong/issues/7955) | [Improve][Dashboard] Change the consumption query method from get to post                                      |
| [INLONG-7929](https://github.com/apache/inlong/issues/7929) | [Bug][Dashboard] The select box cannot be triggered when the form is not entered                               |
| [INLONG-7918](https://github.com/apache/inlong/issues/7918) | [Improve][Dashboard] File source cluster name is displayed as displayName                                      |
| [INLONG-7915](https://github.com/apache/inlong/issues/7915) | [Improve][Dashboard] Clickhouse sink engine field optimization                                                 |
| [INLONG-7871](https://github.com/apache/inlong/issues/7871) | [Improve][Dashboard] MySQL source supports filling in the database name whitelist                              |
| [INLONG-7852](https://github.com/apache/inlong/issues/7852) | [Improve][Dashboard] Clickhouse supports setting cluster when creating a table                                 |
| [INLONG-7833](https://github.com/apache/inlong/issues/7833) | [Improve][Dashboard] Process optimization for creating source and sink                                         |
| [INLONG-7826](https://github.com/apache/inlong/issues/7826) | [Improve][Dashboard] Supports filling in the life cycle when the Clickhouse sink engine is ReplicatedMergeTree |
| [INLONG-7802](https://github.com/apache/inlong/issues/7802) | [Improve][Dashboard] Requirements for optimizing data sources                                                  |
| [INLONG-7796](https://github.com/apache/inlong/issues/7796) | [Improve][Dashboard] Add icon to menu                                                                          |
| [INLONG-7789](https://github.com/apache/inlong/issues/7789) | [Feature][Dashboard] Support create stream fields by statement                                                 |
| [INLONG-7785](https://github.com/apache/inlong/issues/7785) | [Improve][Dashboard] Clickhouse sink engine defaults to MergeTree                                              |
| [INLONG-7772](https://github.com/apache/inlong/issues/7772) | [Improve][Dashboard] Redis sentinel master name normalization in NodeManagement                                |
| [INLONG-7758](https://github.com/apache/inlong/issues/7758) | [Bug][Dashboard] Login page display error                                                                      |
| [INLONG-7756](https://github.com/apache/inlong/issues/7756) | [Bug][Dashboard] Group logs can not display                                                                    |
| [INLONG-7749](https://github.com/apache/inlong/issues/7749) | [Improve][Dashboard] Support rate limit for pulsar mq mark-delete operation                                    |
| [INLONG-7738](https://github.com/apache/inlong/issues/7738) | [Improve][Dashboard] The name and type of cluster and node cannot be modified                                  |
| [INLONG-7734](https://github.com/apache/inlong/issues/7734) | [Feature][Dashboard] Update Dashbaord layout & folder design                                                   |
| [INLONG-7728](https://github.com/apache/inlong/issues/7728) | [Feature][Dashboard] Extract the cluster properties of Redis NodeManagement                                    |
| [INLONG-7702](https://github.com/apache/inlong/issues/7702) | [Improve][Dashboard] Add Chinese name for partitionKeys properties of Hudi LoadNode                            |
| [INLONG-7672](https://github.com/apache/inlong/issues/7672) | [Feature][Dashboard] Support kudu node management                                                              |
| [INLONG-7668](https://github.com/apache/inlong/issues/7668) | [Feature][Dashboard] InLong source supports restart and freeze operations                                      |
| [INLONG-7643](https://github.com/apache/inlong/issues/7643) | [Feature][Dashboard] Support specifying buckets when creating kudu resource                                    |
| [INLONG-7630](https://github.com/apache/inlong/issues/7630) | [Bug][Dashboard] The page flickers when the route is switched                                                  |
| [INLONG-7004](https://github.com/apache/inlong/issues/7004) | [Feature][Dashboard][Manager] Creating schema of  StreamSource by Statement                                    |
| [INLONG-7971](https://github.com/apache/inlong/issues/7971) | [Feature][Dashboard] Support batch import fields by Excel                                                      |
| [INLONG-8001](https://github.com/apache/inlong/issues/8001) | [Feature][Dashboard] Support postgreSQL node management                                                        |
| [INLONG-8011](https://github.com/apache/inlong/issues/8011) | [Improve][Dashboard] Cluster name and node name can be modified when editing                                   |
| [INLONG-8011](https://github.com/apache/inlong/issues/8022) | [Improve][Dashboard] Node management title text optimization                                                   |

### Other
|                            ISSUE                            | Summary                                                                                                 |
|:-----------------------------------------------------------:|:--------------------------------------------------------------------------------------------------------|
| [INLONG-7909](https://github.com/apache/inlong/issues/7909) | [Bug]Capture changes made by connector user & document that SYS/SYSTEM changes are not captured         |
| [INLONG-7892](https://github.com/apache/inlong/issues/7892) | [bug] multiple times generating approval slips                                                          |
| [INLONG-7876](https://github.com/apache/inlong/issues/7876) | [Improve][CVE] Upgrade org.springframework:spring-core to 5.3.27                                        |
| [INLONG-7781](https://github.com/apache/inlong/issues/7781) | [Bug] [InLong-SDK] Solve the problem of cpp-sdk send  data by  HTTP protocol                            |
| [INLONG-7745](https://github.com/apache/inlong/issues/7745) | [Improve][CVE] Spring Framework vulnerable to denial of service via specially crafted SpEL expression   |
| [INLONG-7742](https://github.com/apache/inlong/issues/7742) | [Improve][Tool] Only support inlong-dev-toolkit on MacOS and Linux                                      |
| [INLONG-7698](https://github.com/apache/inlong/issues/7698) | [Bug][Docker] docker images build faild for tubemq-build                                                |
| [INLONG-7697](https://github.com/apache/inlong/issues/7697) | [Feature] Reduce the memory usage of JM when split table chunks                                         |
| [INLONG-7986](https://github.com/apache/inlong/issues/7986) | [Bug] Collect file to Postgresql failed when using docker image for test                                |
| [INLONG-7986](https://github.com/apache/inlong/issues/8009) | [Improve] Exclude the useless dependency for pulsar-client to decrease the distribution package size    |