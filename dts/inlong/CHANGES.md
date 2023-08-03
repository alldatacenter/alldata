
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

# Release InLong 1.8.0 - Released (as of 2023-07-09)
### Agent
|                            ISSUE                            | Summary                                                                    |
|:-----------------------------------------------------------:|:---------------------------------------------------------------------------|
| [INLONG-8176](https://github.com/apache/inlong/issues/8176) | [Improve][Agent] Upgrade rocksdb version                                   |
| [INLONG-8180](https://github.com/apache/inlong/issues/8180) | [Improve][Agent] Improve the efficiency and safety of log file reading     | 
| [INLONG-8183](https://github.com/apache/inlong/issues/8183) | [Improve][Agent] Optimize agent UT                                         |
| [INLONG-8244](https://github.com/apache/inlong/issues/8244) | [Bug][Agent] Thread leaks after the job is finished                        | 
| [INLONG-8251](https://github.com/apache/inlong/issues/8251) | [Improve][Agent] Add global memory limit for file collect                  | 
| [INLONG-8334](https://github.com/apache/inlong/issues/8334) | [Improve][Agent] Optimize the file collection UT                           | 
| [INLONG-8339](https://github.com/apache/inlong/issues/8339) | [Improve][DataProxy][Agent] Enable audit by default in DataProxy and Agent | 
| [INLONG-8347](https://github.com/apache/inlong/issues/8347) | [Improve][Agent] Optimize  the agent UT of testTimeOffset                  |
| [INLONG-8352](https://github.com/apache/inlong/issues/8352) | [Improve][Agent] Optimize the agent UT of testRestartTriggerJobRestore     |
| [INLONG-8376](https://github.com/apache/inlong/issues/8376) | [Improve][Agent] Optimize the agent UT of TestTriggerManager               |

### DataProxy
|                            ISSUE                            | Summary                                                                                                   |
|:-----------------------------------------------------------:|:----------------------------------------------------------------------------------------------------------|
| [INLONG-4961](https://github.com/apache/inlong/issues/4961) | [Feature][DataProxy] Golang SDK                                                                           |
| [INLONG-7194](https://github.com/apache/inlong/issues/7194) | [Improve][DataProxy] Migrate index log statistics for the new mq layer                                    |
| [INLONG-7766](https://github.com/apache/inlong/issues/7766) | [Bug][SDK] Adjusted frame length exceeds occurred when reporting data through the HTTP protocol           |
| [INLONG-7950](https://github.com/apache/inlong/issues/7950) | [Improve][DataProxy] Optimize the implementation logic of the Source                                      |
| [INLONG-8049](https://github.com/apache/inlong/issues/8049) | [Improve][DataProxy] Add CIDR configuration in the BlackList and WhiteList                                |
| [INLONG-8073](https://github.com/apache/inlong/issues/8073) | [Improved][DataProxy]Add HTTP message processing logic in source2                                         |
| [INLONG-8106](https://github.com/apache/inlong/issues/8106) | [Improve][DataProxy] Optimize ConfigManager implementation ( part one )                                   |
| [INLONG-8132](https://github.com/apache/inlong/issues/8132) | [Improve][DataProxy] Fix Golang SDK typo errors in readme.md and options.go                               |
| [INLONG-8161](https://github.com/apache/inlong/issues/8161) | [Improve][DataProxy] Optimize BatchPackProfile related classes implementation                             |
| [INLONG-8163](https://github.com/apache/inlong/issues/8163) | [Improve][Manager][DataProxy] Make DataProxy config interface compatible with old versions                |
| [INLONG-8167](https://github.com/apache/inlong/issues/8167) | [Improve][DataProxy]Update Golang SDK dependent packages to fix dependabot alerts                         |
| [INLONG-8192](https://github.com/apache/inlong/issues/8192) | [Bug][DataProxy] The topic name generated by dataproxy is incorrect                                       |
| [INLONG-8212](https://github.com/apache/inlong/issues/8212) | [Improve][DataProxy] Improve HTTP related message handling                                                |
| [INLONG-8228](https://github.com/apache/inlong/issues/8228) | [Improve][DataProxy] Optimize the implementation of the index output to files                             |
| [INLONG-8252](https://github.com/apache/inlong/issues/8252) | [Improve][DataProxy] Adjust default Topic settings from Source to Sink                                    |
| [INLONG-8267](https://github.com/apache/inlong/issues/8267) | [Improve][DataProxy] Add the control of whether to retry and the count of retries for the failure message |
| [INLONG-8284](https://github.com/apache/inlong/issues/8284) | [Improve][DataProxy] Unify the message encoding definition of DataProxy                                   |
| [INLONG-8294](https://github.com/apache/inlong/issues/8294) | [Improve][DataProxy] Optimize the log output in the Sink module                                           |
| [INLONG-8305](https://github.com/apache/inlong/issues/8305) | [Improve][DataProxy] Optimize HttpPost object creation                                                    |
| [INLONG-8311](https://github.com/apache/inlong/issues/8311) | [Improve][DataProxy] Add event handling support for FlumeEvent type                                       |
| [INLONG-8318](https://github.com/apache/inlong/issues/8318) | [Improve][DataProxy] Change notification synchronization through condition variables and locks            |
| [INLONG-8323](https://github.com/apache/inlong/issues/8323) | [Improve][DataProxy] Add Topic detailed information output when Producer is null                          |
| [INLONG-8332](https://github.com/apache/inlong/issues/8332) | [Improve][DataProxy] Return original content for MSG_ORIGINAL_RETURN type messages                        |
| [INLONG-8339](https://github.com/apache/inlong/issues/8339) | [Improve][DataProxy][Agent] Enable audit by default in DataProxy and Agent                                |
| [INLONG-8356](https://github.com/apache/inlong/issues/8356) | [Improve][DataProxy] Replace source2 to source                                                            |
| [INLONG-8368](https://github.com/apache/inlong/issues/8368) | [Bug][DataProxy] Sink does not have audit data                                                            |
| [INLONG-8385](https://github.com/apache/inlong/issues/8385) | [Improve][DataProxy] Add take method in BufferQueue class                                                 |
| [INLONG-8459](https://github.com/apache/inlong/issues/8459) | [Improve][DataProxy] Fix code scanning alert - Implicit narrowing conversion in compound assignment       |

### TubeMQ
|                            ISSUE                            | Summary                                                                   |
|:-----------------------------------------------------------:|:--------------------------------------------------------------------------|
| [INLONG-4968](https://github.com/apache/inlong/issues/4968) | [Feature][TubeMQ] Golang SDK for Producing Message                        |
| [INLONG-8122](https://github.com/apache/inlong/issues/8122) | [Feature][TubeMQ] Add "Heartbeat" method for GO SDK                       |
| [INLONG-8165](https://github.com/apache/inlong/issues/8165) | [Feature][TubeMQ] Add "SendMessage" method for GO SDK                     |
| [INLONG-8286](https://github.com/apache/inlong/issues/8286) | [Improve][TubeMQ] Supports the return package type when querying messages |
| [INLONG-8321](https://github.com/apache/inlong/issues/8321) | [Improve][TubeMQ] Improve the precision of tube consumer id               |

### Manager
|                            ISSUE                            | Summary                                                                                                     |
|:-----------------------------------------------------------:|:------------------------------------------------------------------------------------------------------------|
| [INLONG-7914](https://github.com/apache/inlong/issues/7914) | [Feature][Manager] Support multi-tenancy                                                                    |
| [INLONG-8024](https://github.com/apache/inlong/issues/8024) | [Improve][Manager] Add extended properties when getting the status of the sort task info                    |
| [INLONG-8035](https://github.com/apache/inlong/issues/8035) | [Bug][Manager] Non-file tasks cannot be recovered from the heartbeat timeout state                          |
| [INLONG-8039](https://github.com/apache/inlong/issues/8039) | [Improve][Manager] Optimize the transform interface                                                         |
| [INLONG-8047](https://github.com/apache/inlong/issues/8047) | [Improve][Dashboard][Manager][Sort] Rename lightweight to DataSync                                          |
| [INLONG-8066](https://github.com/apache/inlong/issues/8066) | [Improve][Manager] Add sort extended properties when getting the status info of the InlongGroup             |
| [INLONG-8068](https://github.com/apache/inlong/issues/8068) | [Feature][Manager] Support repeatable read for http request                                                 |
| [INLONG-8072](https://github.com/apache/inlong/issues/8072) | [Bug][Manager] NPE when sort_task_name of stream_sink table is empty or null                                |
| [INLONG-8080](https://github.com/apache/inlong/issues/8080) | [Bug][Manager] The total parameter found on the page is different from the actual value                     |
| [INLONG-8087](https://github.com/apache/inlong/issues/8087) | [Feature][Manager] Add definition of Inlong tenant table                                                    |
| [INLONG-8088](https://github.com/apache/inlong/issues/8088) | [Improve][Manager] The heartbeat timeout interval can be configured                                         |
| [INLONG-8091](https://github.com/apache/inlong/issues/8091) | [Bug][Manager] Unsupported FieldType is reported when the source and sink are both PostgreSQL               |
| [INLONG-8093](https://github.com/apache/inlong/issues/8093) | [Feature][Manager] Add inlong tenant related APIs                                                           |
| [INLONG-8098](https://github.com/apache/inlong/issues/8098) | [Feature][Manager] Support Inlong user permission control                                                   |
| [INLONG-8108](https://github.com/apache/inlong/issues/8108) | [Bug][Manager] WorkflowApprover API Permissions Optimization                                                |
| [INLONG-8114](https://github.com/apache/inlong/issues/8114) | [Bug][Manager] Appeared NPE when building properties                                                        |
| [INLONG-8118](https://github.com/apache/inlong/issues/8118) | [Feature][Manager] Support tenant user permission control                                                   |
| [INLONG-8121](https://github.com/apache/inlong/issues/8121) | [Improve][Manager] Supports cluster node status management in the case of multiple manager nodes            |
| [INLONG-8127](https://github.com/apache/inlong/issues/8127) | [Bug][Manager] UT failed in HeartbeatManagerTest.testReportHeartbeat                                        |
| [INLONG-8129](https://github.com/apache/inlong/issues/8129) | [Improve][Manager] Add encoding check to the MySQL JDBC URL                                                 |
| [INLONG-8136](https://github.com/apache/inlong/issues/8136) | [Improve][Manager] Support obtaining resource information used by the current group                         |
| [INLONG-8148](https://github.com/apache/inlong/issues/8148) | [Bug][Manager] The method of querying cluster nodes is not idempotent                                       |
| [INLONG-8150](https://github.com/apache/inlong/issues/8150) | [Improve][Manager] Not throw an exception when getting cluster nodes                                        |
| [INLONG-8152](https://github.com/apache/inlong/issues/8152) | [Improve][Manager] Paging queries remove the restriction that groupid is not empty                          |
| [INLONG-8154](https://github.com/apache/inlong/issues/8154) | [Improve][Manager] Optimize the way of creating ExtractNodes to make it easy to expand and maintain         |
| [INLONG-8159](https://github.com/apache/inlong/issues/8159) | [Improve][Manager] Rename "tenant" in InlongPulsarInfo to "pulsarTenant"                                    |
| [INLONG-8163](https://github.com/apache/inlong/issues/8163) | [Improve][Manager][DataProxy] Make DataProxy config interface compatible with old versions                  |
| [INLONG-8169](https://github.com/apache/inlong/issues/8169) | [Feature][Manager] Add default tenant "public" if the request does not specify one                          |
| [INLONG-8171](https://github.com/apache/inlong/issues/8171) | [Improve][Manager] Optimize the way of creating LoadNodes to make it easy to expand and maintain            |
| [INLONG-8188](https://github.com/apache/inlong/issues/8188) | [Improve][Manager] Support querying audit information by sink id                                            |
| [INLONG-8197](https://github.com/apache/inlong/issues/8197) | [Improve][Manager] Optimize the ClickHouse query for the Audit interface                                    |
| [INLONG-8199](https://github.com/apache/inlong/issues/8199) | [Improve][Manager] Supports get brief information of inlong stream                                          |
| [INLONG-8200](https://github.com/apache/inlong/issues/8200) | [Feature][Manager] Fliter out requests which user has no permission to the specific tenant                  |
| [INLONG-8202](https://github.com/apache/inlong/issues/8202) | [Improve][Manager] Extract public parseFormat method to optimize NodeProvider                               |
| [INLONG-8222](https://github.com/apache/inlong/issues/8222) | [Feature][Manager] Support different data types mapping for different data sources by the strategy pattern  |
| [INLONG-8226](https://github.com/apache/inlong/issues/8226) | [Improve][Manager] Change the type of partitionNum for KafkaSink                                            |
| [INLONG-8231](https://github.com/apache/inlong/issues/8231) | [Feature][Manager] Add tenant into param and body of each request                                           |
| [INLONG-8247](https://github.com/apache/inlong/issues/8247) | [Improve][Manager] Removes the restriction that only the admin user can create DataNodes                    |
| [INLONG-8260](https://github.com/apache/inlong/issues/8260) | [Bug][Manager] Source field is empty for data sync                                                          |
| [INLONG-8266](https://github.com/apache/inlong/issues/8266) | [Improve][Manager] Optimize PostgreSQL field type mapping strategy with the customized configuration file   |
| [INLONG-8275](https://github.com/apache/inlong/issues/8275) | [Umbrella][Manager]  All resources support multi-tenancy                                                    |
| [INLONG-8276](https://github.com/apache/inlong/issues/8276) | [Feature][Manager] InlongGroup support multi-tenancy                                                        |
| [INLONG-8278](https://github.com/apache/inlong/issues/8278) | [Feature][Manager] Add tenant into the login user info                                                      |
| [INLONG-8281](https://github.com/apache/inlong/issues/8281) | [Feature][Manager] Manager client support multi-tenancy                                                     |
| [INLONG-8282](https://github.com/apache/inlong/issues/8282) | [Improve][Manager] Increase the length of the partial ID or name field                                      |
| [INLONG-8288](https://github.com/apache/inlong/issues/8288) | [Feature][Manager] Add MySQL field type mapping strategy to improve usability                               |
| [INLONG-8290](https://github.com/apache/inlong/issues/8290) | [Improve][Manager] Support SQL interceptor to add tenant into each query                                    |
| [INLONG-8292](https://github.com/apache/inlong/issues/8292) | [Improve][Manager][Dashboard] Add task types on the approval management page                                |
| [INLONG-8296](https://github.com/apache/inlong/issues/8296) | [Improve][Manager] Move LoginUserUtils to pojo to avoid cycle dependency                                    |
| [INLONG-8300](https://github.com/apache/inlong/issues/8300) | [Feature][Manager] Support previewing data of Pulsar                                                        |
| [INLONG-8313](https://github.com/apache/inlong/issues/8313) | [Feature][Manager] Add Oracle field type mapping strategy to improve usability                              |
| [INLONG-8327](https://github.com/apache/inlong/issues/8327) | [Feature][Manager] Add SQLServer field type mapping strategy to improve usability                           |
| [INLONG-8330](https://github.com/apache/inlong/issues/8330) | [Bug][Manager] The information returned by the getAllConfig interface is incorrect                          |
| [INLONG-8345](https://github.com/apache/inlong/issues/8345) | [Improve][Manager][Sort] Flink multi-version adaptation directory adjustment and version unified management |
| [INLONG-8349](https://github.com/apache/inlong/issues/8349) | [Feature][Manager] DataNode support multi-tenancy                                                           |
| [INLONG-8354](https://github.com/apache/inlong/issues/8354) | [Improve][Manager] Support previewing data of TubeMQ                                                        |
| [INLONG-8361](https://github.com/apache/inlong/issues/8361) | [Bug][Manager] Group restart fail                                                                           |
| [INLONG-8365](https://github.com/apache/inlong/issues/8365) | [Feature][Manager] InlongCluster support multi-tenancy                                                      |
| [INLONG-8369](https://github.com/apache/inlong/issues/8369) | [Feature][Manager] Add MongoDB field type mapping strategy to improve usability                             |
| [INLONG-8378](https://github.com/apache/inlong/issues/8378) | [Feature][Manager] InlongClusterTag support multi-tenancy                                                   |
| [INLONG-8380](https://github.com/apache/inlong/issues/8380) | [Feature][Manager] InlongConsume support multi-tenancy                                                      |
| [INLONG-8389](https://github.com/apache/inlong/issues/8389) | [Bug][Manager] Tenant interception failure when authentication is disable                                   |
| [INLONG-8394](https://github.com/apache/inlong/issues/8394) | [Improve][Manager] TenantRole list interface support fuzzy match                                            |
| [INLONG-8396](https://github.com/apache/inlong/issues/8396) | [Improve][Manager] Support for querying audit data with average delay                                       |
| [INLONG-8398](https://github.com/apache/inlong/issues/8398) | [Improve][Manager] Optimize multiple tenant related logs                                                    |
| [INLONG-8404](https://github.com/apache/inlong/issues/8404) | [Feature][Manager] Workflow support multi-tenancy                                                           |
| [INLONG-8405](https://github.com/apache/inlong/issues/8405) | [Improve][Manager] Dynamically configure ClickHouse source                                                  |
| [INLONG-8421](https://github.com/apache/inlong/issues/8421) | [Bug][Manager] NPE when disable OpenApi auth                                                                |
| [INLONG-8423](https://github.com/apache/inlong/issues/8423) | [Feature][Manager] Manager client support tenant operation                                                  |
| [INLONG-8425](https://github.com/apache/inlong/issues/8425) | [Bug][Manager] Error in obtaining audit information when sink is not configured                             |
| [INLONG-8434](https://github.com/apache/inlong/issues/8434) | [Bug][Manager] Error converting null to string when converting JSON string                                  |
| [INLONG-8440](https://github.com/apache/inlong/issues/8440) | [Feature][Manager] Support list tenant info by user or given tenant list                                    |
| [INLONG-8442](https://github.com/apache/inlong/issues/8442) | [Feature][Manager] Support delete tenant and tenant role                                                    |
| [INLONG-8443](https://github.com/apache/inlong/issues/8443) | [Feature][Manager] Add ClickHouse field type mapping strategy to improve usability                          |
| [INLONG-8449](https://github.com/apache/inlong/issues/8449) | [Bug][Manager] Abnormal growth of self increasing primary key in heartbeat table                            |
| [INLONG-8462](https://github.com/apache/inlong/issues/8462) | [Bug][Manager] Attribute was overwritten by the default value error when modifying the group                |
| [INLONG-8469](https://github.com/apache/inlong/issues/8469) | [Bug][Manager] The ext_params is incorrectly set to null                                                    |

### Sort
|                            ISSUE                            | Summary                                                                                                                       |
|:-----------------------------------------------------------:|:------------------------------------------------------------------------------------------------------------------------------|
| [INLONG-6545](https://github.com/apache/inlong/issues/6545) | [Improve][Sort] Accurately parse the schema type and completely match the missing precision information                       |
| [INLONG-7853](https://github.com/apache/inlong/issues/7853) | [Feature][Sort] Add common handle for schema-change in sink                                                                   |
| [INLONG-7882](https://github.com/apache/inlong/issues/7882) | [Improve][Sort] Oracle CDC reduces the number of session connections                                                          |
| [INLONG-7959](https://github.com/apache/inlong/issues/7959) | [Improve][Sort] Dynamic schema evolution support delete and update columns when sink to Iceberg                               |
| [INLONG-7990](https://github.com/apache/inlong/issues/7990) | [Improve][Sort] Fix license header for InLongFixedPartitionPartitionerTest                                                    |
| [INLONG-7994](https://github.com/apache/inlong/issues/7994) | [Improve][Sort] Add UT for all migration  to MongoDB CDC                                                                      |
| [INLONG-8034](https://github.com/apache/inlong/issues/8034) | [Improve][Sort] Bump hudi version to 0.12.3                                                                                   |
| [INLONG-8038](https://github.com/apache/inlong/issues/8038) | [Feature][Sort] Optimize MySQL CDC chunk splitting logic                                                                      |
| [INLONG-8047](https://github.com/apache/inlong/issues/8047) | [Improve][Dashboard][Manager][Sort] Rename lightweight to DataSync                                                            |
| [INLONG-8054](https://github.com/apache/inlong/issues/8054) | [Improve][Sort]  Update document information of Sort                                                                          |
| [INLONG-8062](https://github.com/apache/inlong/issues/8062) | [Feature][Sort] Add PostgreSQL source connector on flink 1.15                                                                 |
| [INLONG-8065](https://github.com/apache/inlong/issues/8065) | [Feature][Sort] Add StarRocks connector on Flink 1.15                                                                         |
| [INLONG-8092](https://github.com/apache/inlong/issues/8092) | [Feature][Sort] Support all database and multiple tables transmission for Hive                                                |
| [INLONG-8099](https://github.com/apache/inlong/issues/8099) | [Umbrella][Sort] Sort support Flink multi-version                                                                             |
| [INLONG-8101](https://github.com/apache/inlong/issues/8101) | [Feature][Sort] Support multi-version packaging of sort-connectors                                                            |
| [INLONG-8110](https://github.com/apache/inlong/issues/8110) | [Improve][Sort] Only whole database migration need table level metric                                                         |
| [INLONG-8116](https://github.com/apache/inlong/issues/8116) | [Improve][Sort] Support table api config setting                                                                              |
| [INLONG-8125](https://github.com/apache/inlong/issues/8125) | [Improve][Sort] Optimizing the speed of transitioning from snapshot to binlog                                                 |
| [INLONG-8140](https://github.com/apache/inlong/issues/8140) | [Feature][Sort] Support data inference schema change type                                                                     |
| [INLONG-8143](https://github.com/apache/inlong/issues/8143) | [Feature][Sort] Kafka support DDL                                                                                             |
| [INLONG-8173](https://github.com/apache/inlong/issues/8173) | [Bug][Sort] Fix the NPE problem when adding new columns                                                                       |
| [INLONG-8175](https://github.com/apache/inlong/issues/8175) | [Improve][Sort] MySQL CDC support read data from specific timestamp                                                           |
| [INLONG-8177](https://github.com/apache/inlong/issues/8177) | [Improve][Sort] Improve jdbc connector object calculation and Fix filesystem connector report dirty data metrics error        |
| [INLONG-8217](https://github.com/apache/inlong/issues/8217) | [Improve][Sort] Sort-core should support running on flink-1.15                                                                |
| [INLONG-8218](https://github.com/apache/inlong/issues/8218) | [Bug][Sort] Kafka connector reader data byte calculation error                                                                |
| [INLONG-8220](https://github.com/apache/inlong/issues/8220) | [Improve][Sort] Add PostgreSQL connector for Flink 1.15 in distribution                                                       |
| [INLONG-8233](https://github.com/apache/inlong/issues/8233) | [Improve][Sort] Support running tests on both Flink 1.13 and Flink 1.15                                                       |
| [INLONG-8239](https://github.com/apache/inlong/issues/8239) | [Improve][Sort] Support sort format to flink-1.15                                                                             |
| [INLONG-8307](https://github.com/apache/inlong/issues/8307) | [Bug][Sort] Job restart failed from savepoint When set 'scan.startup.mode' = 'timestamp'                                      |
| [INLONG-8337](https://github.com/apache/inlong/issues/8337) | [Improve][Sort] Support getting accurate precision and scale of decimal type for Iceberg                                      |
| [INLONG-8341](https://github.com/apache/inlong/issues/8341) | [Improve][Sort] MySQL cdc connector cannot get scale for decimal field                                                        |
| [INLONG-8345](https://github.com/apache/inlong/issues/8345) | [Improve][Manager][Sort] Flink multi-version adaptation directory adjustment and version unified management                   |
| [INLONG-8363](https://github.com/apache/inlong/issues/8363) | [Improve][Sort] MySQL connector captures binlog  in snapshot phase all the time even if tables have been removed in flink sql |
| [INLONG-8366](https://github.com/apache/inlong/issues/8366) | [Bug][Sort] Lost data in Iceberg when restoring checkpoint                                                                    |
| [INLONG-8372](https://github.com/apache/inlong/issues/8372) | [Improve][Sort] MySQL connector supports uploading flink job delay metrics                                                    |
| [INLONG-8382](https://github.com/apache/inlong/issues/8382) | [Improve][Sort] Provide unsupported operation for ddl that is not parseable                                                   |
| [INLONG-8411](https://github.com/apache/inlong/issues/8411) | [Bug][Sort] The artifactId of the dependency of sort-format-json is wrong                                                     |
| [INLONG-8429](https://github.com/apache/inlong/issues/8429) | [Bug][Sort] Shield the missing part of the Flink 1.15 connector pipeline failure error                                        |

### Audit
|                            ISSUE                            | Summary                                                                              |
|:-----------------------------------------------------------:|:-------------------------------------------------------------------------------------|
| [INLONG-8224](https://github.com/apache/inlong/issues/8224) | [Bug][Audit] Fix audit-proxy memory leak                                             |
| [INLONG-8315](https://github.com/apache/inlong/issues/8315) | [Improve][Audit] Filter out invalid data that is more than 7 days old (configurable) |

### Dashboard
|                            ISSUE                            | Summary                                                                                              |
|:-----------------------------------------------------------:|:-----------------------------------------------------------------------------------------------------|
| [INLONG-8043](https://github.com/apache/inlong/issues/8043) | [Improve][Dashboard] The label displays the cluster name as displayName					                         | 
| [INLONG-8046](https://github.com/apache/inlong/issues/8046) | [Improve][Dashboard] Support batch import of sink fields					                                        | 
| [INLONG-8047](https://github.com/apache/inlong/issues/8047) | [Improve][Dashboard][Manager][Sort] Rename lightweight to DataSync					                              | 
| [INLONG-8051](https://github.com/apache/inlong/issues/8051) | [Improve][Dashboard] Optimize the display effect of the batch parse dialog					                      | 
| [INLONG-8078](https://github.com/apache/inlong/issues/8078) | [Bug][Dashboard] Source and sink list pagination does not work					                                  | 
| [INLONG-8134](https://github.com/apache/inlong/issues/8134) | [Feature][Dashboard] Inlong Group supports viewing resource details					                             | 
| [INLONG-8190](https://github.com/apache/inlong/issues/8190) | [Improve][Dashboard] Optimize the param display name for PostgreSQL					                             | 
| [INLONG-8204](https://github.com/apache/inlong/issues/8204) | [Improve][Dashboard] Support querying audit information by sink id					                              | 
| [INLONG-8254](https://github.com/apache/inlong/issues/8254) | [Improve][Dashboard] Supports get brief information of inlong stream					                            | 
| [INLONG-8257](https://github.com/apache/inlong/issues/8257) | [Improve][Dashboard] Optimize the data node to display normally					                                 | 
| [INLONG-8262](https://github.com/apache/inlong/issues/8262) | [Improve][Dashboard] Optimize the group log component					                                           | 
| [INLONG-8269](https://github.com/apache/inlong/issues/8269) | [Feature][Dashboard] Navigation bar increased data synchronization					                              | 
| [INLONG-8298](https://github.com/apache/inlong/issues/8298) | [Improve][Dashboard] Stream supports data preview					                                               | 
| [INLONG-8302](https://github.com/apache/inlong/issues/8302) | [Improve][Dashboard] Data synchronization source and sink type optimization					                     | 
| [INLONG-8316](https://github.com/apache/inlong/issues/8316) | [Improve][Dashboard] Update length limit for partial ID or name field					                           | 
| [INLONG-8343](https://github.com/apache/inlong/issues/8343) | [Improve][Dashboard] Increase the length of the field for creating MySQL varchar types to 16383					 | 
| [INLONG-8350](https://github.com/apache/inlong/issues/8350) | [Feature][Dashboard] Dashboard plugin support light					                                             | 
| [INLONG-8384](https://github.com/apache/inlong/issues/8384) | [Improve][Dashboard] Data Integration ---> Data Ingestion					                                       | 
| [INLONG-8431](https://github.com/apache/inlong/issues/8431) | [Feature][Dashboard] Support tenant management and tenant role management					                       |
| [INLONG-8461](https://github.com/apache/inlong/issues/8461) | [Improve][Dashboard] Cluster management tag optimization                                             |
| [INLONG-8467](https://github.com/apache/inlong/issues/8467) | [Improve][Dashboard] Audit query condition optimization					                                         | 

### Other
|                            ISSUE                            | Summary                                                                                 |
|:-----------------------------------------------------------:|:----------------------------------------------------------------------------------------|
| [INLONG-8005](https://github.com/apache/inlong/issues/8005) | [Bug] Fix duplicate split request when add new table in mysql connector                 |
| [INLONG-8042](https://github.com/apache/inlong/issues/8042) | [Improve][CI] Update the workflow to avoid the network being unreachable                |
| [INLONG-8060](https://github.com/apache/inlong/issues/8060) | [Improve] Let mysql source reader throws runtimeException when connect times out or OOM |
| [INLONG-8082](https://github.com/apache/inlong/issues/8082) | [Bug][CI] workflow ci_build.yml ci_greeting.yml syntax error                            |
| [INLONG-8103](https://github.com/apache/inlong/issues/8103) | [Improve][CI] Format the order of imports by the spotless plugin                        |
| [INLONG-8157](https://github.com/apache/inlong/issues/8157) | [Bug][CI] Failed to compile code in submodule                                           |
| [INLONG-8249](https://github.com/apache/inlong/issues/8249) | [Improve][CVE] Upgrade org.springframework:spring-boot-autoconfigure to 2.6.15          |
| [INLONG-8274](https://github.com/apache/inlong/issues/8274) | [Bug][Sort]  Mysql connector will throw exception when synchronizing incremental data   |
| [INLONG-8309](https://github.com/apache/inlong/issues/8309) | [Improve][CVE] Upgrade org.xerial.snappy:snappy-java to version 1.1.10. 1               |
| [INLONG-8326](https://github.com/apache/inlong/issues/8326) | [Improve][Build] Remove duplicate dependency declarations of "jsqlparser"               |
| [INLONG-8407](https://github.com/apache/inlong/issues/8407) | [Improve] Optimize mongodb cdc for verifying type DDL types                             |
| [INLONG-8413](https://github.com/apache/inlong/issues/8413) | [Improve][Doc] Update the description about data Ingestion and Synchronization          |
| [INLONG-8419](https://github.com/apache/inlong/issues/8419) | [Improve][Doc] Update the description about InLong                                      |
