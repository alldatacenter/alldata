/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

module.exports = {
  "items" : [
    {
      "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/FALCON",
      "StackServices" : {
        "comments" : "Data management and processing platform",
        "custom_commands" : [ ],
        "service_check_supported" : true,
        "service_name" : "FALCON",
        "display_name" : "Falcon",
        "service_version" : "0.5.0.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1",
        "user_name" : null,
        "config_types" : {
          "falcon-env" : {
            "supports" : {
              "final" : "false"
            }
          },
          "falcon-runtime.properties" : {
            "supports" : {
              "final" : "false"
            }
          },
          "falcon-startup.properties" : {
            "supports" : {
              "final" : "false"
            }
          },
          "oozie-site" : {
            "supports" : {
              "final" : "true"
            }
          }
        },
        "required_services" : [
          "OOZIE"
        ]
      },
      "components" : [
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/FALCON/components/FALCON_CLIENT",
          "StackServiceComponents" : {
            "cardinality" : "1+",
            "component_category" : "CLIENT",
            "component_name" : "FALCON_CLIENT",
            "display_name" : "Falcon Client",
            "custom_commands" : [ ],
            "is_client" : true,
            "is_master" : false,
            "service_name" : "FALCON",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/FALCON/components/FALCON_SERVER",
          "StackServiceComponents" : {
            "cardinality" : "1",
            "component_category" : "MASTER",
            "component_name" : "FALCON_SERVER",
            "display_name" : "Falcon Server",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : true,
            "service_name" : "FALCON",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/FALCON/components/FALCON_SERVER/dependencies/OOZIE_CLIENT",
              "Dependencies" : {
                "component_name" : "OOZIE_CLIENT",
                "dependent_component_name" : "FALCON_SERVER",
                "dependent_service_name" : "FALCON",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            },
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/FALCON/components/FALCON_SERVER/dependencies/OOZIE_SERVER",
              "Dependencies" : {
                "component_name" : "OOZIE_SERVER",
                "dependent_component_name" : "FALCON_SERVER",
                "dependent_service_name" : "FALCON",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            }
          ]
        }
      ],
      artifacts: []
    },
    {
      "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/FLUME",
      "StackServices" : {
        "comments" : "Data management and processing platform",
        "custom_commands" : [ ],
        "service_check_supported" : true,
        "service_name" : "FLUME",
        "display_name" : "Flume",
        "service_version" : "1.4.0.2.1.1.0",
        "stack_name" : "HDP",
        "stack_version" : "2.1",
        "user_name" : null,
        "config_types" : {
          "flume-conf" : {
            "supports" : {
              "final" : "false"
            }
          },
          "flume-env" : {
            "supports" : {
              "final" : "false"
            }
          },
          "flume-log4j" : {
            "supports" : {
              "final" : "false"
            }
          }
        },
        "required_services" : [
          "HDFS"
        ]
      },
      "components" : [
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/FLUME/components/FLUME_HANDLER",
          "StackServiceComponents" : {
            "cardinality" : "0+",
            "component_category" : "SLAVE",
            "component_name" : "FLUME_HANDLER",
            "display_name" : "Flume",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : false,
            "service_name" : "FLUME",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        }
      ],
      artifacts: []
    },
    {
      "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/GANGLIA",
      "StackServices" : {
        "comments" : "Ganglia Metrics Collection system (<a href=\"http://oss.oetiker.ch/rrdtool/\" target=\"_blank\">RRDTool</a> will be installed too)",
        "custom_commands" : [ ],
        "service_check_supported" : false,
        "service_name" : "GANGLIA",
        "display_name" : "Ganglia",
        "service_version" : "3.5.0",
        "stack_name" : "HDP",
        "stack_version" : "2.1",
        "user_name" : null,
        "required_services" : null,
        "config_types" : {
          "ganglia-env" : {
            "supports" : {
              "final" : "false"
            }
          }
        }
      },
      "components" : [
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/GANGLIA/components/GANGLIA_MONITOR",
          "StackServiceComponents" : {
            "cardinality" : "ALL",
            "component_category" : "SLAVE",
            "component_name" : "GANGLIA_MONITOR",
            "display_name" : "Ganglia Monitor",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : false,
            "service_name" : "GANGLIA",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "auto_deploy" : {
            "enabled" : true
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/GANGLIA/components/GANGLIA_SERVER",
          "StackServiceComponents" : {
            "cardinality" : "1",
            "component_category" : "MASTER",
            "component_name" : "GANGLIA_SERVER",
            "display_name" : "Ganglia Server",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : true,
            "service_name" : "GANGLIA",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        }
      ],
      artifacts: []
    },
    {
      "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HBASE",
      "StackServices" : {
        "comments" : "Non-relational distributed database and centralized service for configuration management &\n        synchronization\n      ",
        "custom_commands" : [ ],
        "service_check_supported" : true,
        "service_name" : "HBASE",
        "display_name" : "HBase",
        "service_version" : "0.98.0.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1",
        "user_name" : null,
        "required_services" : [
          "ZOOKEEPER",
          "HDFS"
        ],
        "config_types" : {
          "hbase-env" : {
            "supports" : {
              "final" : "false"
            }
          },
          "hbase-log4j" : {
            "supports" : {
              "final" : "false"
            }
          },
          "hbase-policy" : {
            "supports" : {
              "final" : "true"
            }
          },
          "hbase-site" : {
            "supports" : {
              "final" : "true"
            }
          }
        }
      },
      "components" : [
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HBASE/components/HBASE_CLIENT",
          "StackServiceComponents" : {
            "cardinality" : "1+",
            "component_category" : "CLIENT",
            "component_name" : "HBASE_CLIENT",
            "display_name" : "HBase Client",
            "custom_commands" : [ ],
            "is_client" : true,
            "is_master" : false,
            "service_name" : "HBASE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HBASE/components/HBASE_MASTER",
          "StackServiceComponents" : {
            "cardinality" : "1+",
            "component_category" : "MASTER",
            "component_name" : "HBASE_MASTER",
            "display_name" : "HBase Master",
            "custom_commands" : [
              "DECOMMISSION"
            ],
            "is_client" : false,
            "is_master" : true,
            "service_name" : "HBASE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HBASE/components/HBASE_MASTER/dependencies/HDFS_CLIENT",
              "Dependencies" : {
                "component_name" : "HDFS_CLIENT",
                "dependent_component_name" : "HBASE_MASTER",
                "dependent_service_name" : "HBASE",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            },
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HBASE/components/HBASE_MASTER/dependencies/ZOOKEEPER_SERVER",
              "Dependencies" : {
                "component_name" : "ZOOKEEPER_SERVER",
                "dependent_component_name" : "HBASE_MASTER",
                "dependent_service_name" : "HBASE",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            }
          ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HBASE/components/HBASE_REGIONSERVER",
          "StackServiceComponents" : {
            "cardinality" : "1+",
            "component_category" : "SLAVE",
            "component_name" : "HBASE_REGIONSERVER",
            "display_name" : "RegionServer",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : false,
            "service_name" : "HBASE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        }
      ],
      artifacts: []
    },
    {
      "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HDFS",
      "StackServices" : {
        "comments" : "Apache Hadoop Distributed File System",
        "custom_commands" : [ ],
        "service_check_supported" : true,
        "service_name" : "HDFS",
        "display_name" : "HDFS",
        "service_version" : "2.4.0.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1",
        "user_name" : null,
        "required_services" : [
          "ZOOKEEPER"
        ],
        "config_types" : {
          "core-site" : {
            "supports" : {
              "final" : "true"
            }
          },
          "hadoop-env" : {
            "supports" : {
              "final" : "false"
            }
          },
          "hadoop-policy" : {
            "supports" : {
              "final" : "true"
            }
          },
          "hdfs-log4j" : {
            "supports" : {
              "final" : "false"
            }
          },
          "hdfs-site" : {
            "supports" : {
              "final" : "true"
            }
          }
        }
      },
      "components" : [
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HDFS/components/DATANODE",
          "StackServiceComponents" : {
            "cardinality" : "1+",
            "component_category" : "SLAVE",
            "component_name" : "DATANODE",
            "display_name" : "DataNode",
            "custom_commands" : [ ],
            "decommission_allowed" : true,
            "is_client" : false,
            "is_master" : false,
            "service_name" : "HDFS",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HDFS/components/HDFS_CLIENT",
          "StackServiceComponents" : {
            "cardinality" : "1+",
            "component_category" : "CLIENT",
            "component_name" : "HDFS_CLIENT",
            "display_name" : "HDFS Client",
            "custom_commands" : [ ],
            "is_client" : true,
            "is_master" : false,
            "service_name" : "HDFS",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HDFS/components/JOURNALNODE",
          "StackServiceComponents" : {
            "cardinality" : "0+",
            "component_category" : "SLAVE",
            "component_name" : "JOURNALNODE",
            "display_name" : "JournalNode",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : false,
            "service_name" : "HDFS",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HDFS/components/NAMENODE",
          "StackServiceComponents" : {
            "cardinality" : "1-2",
            "component_category" : "MASTER",
            "component_name" : "NAMENODE",
            "display_name" : "NameNode",
            "custom_commands" : [
              "DECOMMISSION"
            ],
            "is_client" : false,
            "is_master" : true,
            "service_name" : "HDFS",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HDFS/components/SECONDARY_NAMENODE",
          "StackServiceComponents" : {
            "cardinality" : "1",
            "component_category" : "MASTER",
            "component_name" : "SECONDARY_NAMENODE",
            "display_name" : "SNameNode",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : true,
            "service_name" : "HDFS",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HDFS/components/ZKFC",
          "StackServiceComponents" : {
            "cardinality" : "0+",
            "component_category" : "SLAVE",
            "component_name" : "ZKFC",
            "display_name" : "ZKFailoverController",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : false,
            "service_name" : "HDFS",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        }
      ],
      artifacts: []
    },
    {
      "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HIVE",
      "StackServices" : {
        "comments" : "Data warehouse system for ad-hoc queries & analysis of large datasets and table & storage management service",
        "custom_commands" : [ ],
        "service_check_supported" : true,
        "service_name" : "HIVE",
        "display_name" : "Hive",
        "service_version" : "0.13.0.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1",
        "user_name" : null,
        "required_services" : [
          "ZOOKEEPER",
          "YARN"
        ],
        "config_types" : {
          "hive-env" : {
            "supports" : {
              "final" : "false"
            }
          },
          "hive-exec-log4j" : {
            "supports" : {
              "final" : "false"
            }
          },
          "hive-log4j" : {
            "supports" : {
              "final" : "false"
            }
          },
          "hive-site" : {
            "supports" : {
              "final" : "true"
            }
          },
          "tez-site" : {
            "supports" : {
              "final" : "false"
            }
          }
        }
      },
      "components" : [
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HIVE/components/HIVE_CLIENT",
          "StackServiceComponents" : {
            "cardinality" : "1+",
            "component_category" : "CLIENT",
            "component_name" : "HIVE_CLIENT",
            "display_name" : "Hive Client",
            "custom_commands" : [ ],
            "is_client" : true,
            "is_master" : false,
            "service_name" : "HIVE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HIVE/components/HIVE_METASTORE",
          "StackServiceComponents" : {
            "cardinality" : "1+",
            "component_category" : "MASTER",
            "component_name" : "HIVE_METASTORE",
            "display_name" : "Hive Metastore",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : true,
            "reassign_allowed" : true,
            "service_name" : "HIVE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "auto_deploy" : {
            "enabled" : true,
            "location" : "HIVE/HIVE_SERVER"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HIVE/components/HIVE_SERVER",
          "StackServiceComponents" : {
            "cardinality" : "1+",
            "component_category" : "MASTER",
            "component_name" : "HIVE_SERVER",
            "display_name" : "HiveServer2",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : true,
            "reassign_allowed" : true,
            "service_name" : "HIVE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HIVE/components/HIVE_SERVER/dependencies/MAPREDUCE2_CLIENT",
              "Dependencies" : {
                "component_name" : "MAPREDUCE2_CLIENT",
                "dependent_component_name" : "HIVE_SERVER",
                "dependent_service_name" : "HIVE",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            },
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HIVE/components/HIVE_SERVER/dependencies/TEZ_CLIENT",
              "Dependencies" : {
                "component_name" : "TEZ_CLIENT",
                "dependent_component_name" : "HIVE_SERVER",
                "dependent_service_name" : "HIVE",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            },
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HIVE/components/HIVE_SERVER/dependencies/YARN_CLIENT",
              "Dependencies" : {
                "component_name" : "YARN_CLIENT",
                "dependent_component_name" : "HIVE_SERVER",
                "dependent_service_name" : "HIVE",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            },
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HIVE/components/HIVE_SERVER/dependencies/ZOOKEEPER_SERVER",
              "Dependencies" : {
                "component_name" : "ZOOKEEPER_SERVER",
                "dependent_component_name" : "HIVE_SERVER",
                "dependent_service_name" : "HIVE",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            }
          ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/WEBHCAT/components/WEBHCAT_SERVER",
          "StackServiceComponents" : {
            "cardinality" : "1",
            "component_category" : "MASTER",
            "component_name" : "WEBHCAT_SERVER",
            "display_name" : "WebHCat Server",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : true,
            "service_name" : "HIVE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HIVE/components/WEBHCAT_SERVER/dependencies/HDFS_CLIENT",
              "Dependencies" : {
                "component_name" : "HDFS_CLIENT",
                "dependent_component_name" : "WEBHCAT_SERVER",
                "dependent_service_name" : "WEBHCAT",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            },
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HIVE/components/WEBHCAT_SERVER/dependencies/MAPREDUCE2_CLIENT",
              "Dependencies" : {
                "component_name" : "MAPREDUCE2_CLIENT",
                "dependent_component_name" : "WEBHCAT_SERVER",
                "dependent_service_name" : "WEBHCAT",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            },
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/WEBHCAT/components/WEBHCAT_SERVER/dependencies/YARN_CLIENT",
              "Dependencies" : {
                "component_name" : "YARN_CLIENT",
                "dependent_component_name" : "WEBHCAT_SERVER",
                "dependent_service_name" : "WEBHCAT",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            },
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/WEBHCAT/components/WEBHCAT_SERVER/dependencies/ZOOKEEPER_CLIENT",
              "Dependencies" : {
                "component_name" : "ZOOKEEPER_CLIENT",
                "dependent_component_name" : "WEBHCAT_SERVER",
                "dependent_service_name" : "WEBHCAT",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            },
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/WEBHCAT/components/WEBHCAT_SERVER/dependencies/ZOOKEEPER_SERVER",
              "Dependencies" : {
                "component_name" : "ZOOKEEPER_SERVER",
                "dependent_component_name" : "WEBHCAT_SERVER",
                "dependent_service_name" : "WEBHCAT",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            }
          ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HIVE/components/MYSQL_SERVER",
          "StackServiceComponents" : {
            "cardinality" : "0-1",
            "component_category" : "MASTER",
            "component_name" : "MYSQL_SERVER",
            "display_name" : "MySQL Server",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : true,
            "service_name" : "HIVE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/HIVE/components/HCAT",
          "StackServiceComponents" : {
            "cardinality" : null,
            "component_category" : "CLIENT",
            "component_name" : "HCAT",
            "display_name" : "HCat",
            "custom_commands" : [ ],
            "is_client" : true,
            "is_master" : false,
            "service_name" : "HIVE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        }
      ],
      artifacts: []
    },
    {
      "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/MAPREDUCE2",
      "StackServices" : {
        "comments" : "Apache Hadoop NextGen MapReduce (YARN)",
        "custom_commands" : [ ],
        "service_check_supported" : true,
        "service_name" : "MAPREDUCE2",
        "display_name" : "MapReduce2",
        "service_version" : "2.1.0.2.0.6.0",
        "stack_name" : "HDP",
        "stack_version" : "2.1",
        "user_name" : null,
        "required_services" : [
          "YARN"
        ],
        "config_types" : {
          "core-site" : {
            "supports" : {
              "final" : "true"
            }
          },
          "mapred-env" : {
            "supports" : {
              "final" : "false"
            }
          },
          "mapred-queue-acls" : {
            "supports" : {
              "final" : "true"
            }
          },
          "mapred-site" : {
            "supports" : {
              "final" : "true"
            }
          }
        }
      },
      "components" : [
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/MAPREDUCE2/components/HISTORYSERVER",
          "StackServiceComponents" : {
            "cardinality" : "1",
            "component_category" : "MASTER",
            "component_name" : "HISTORYSERVER",
            "display_name" : "History Server",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : true,
            "service_name" : "MAPREDUCE2",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "auto_deploy" : {
            "enabled" : true,
            "location" : "YARN/RESOURCEMANAGER"
          },
          "dependencies" : [
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/MAPREDUCE2/components/HISTORYSERVER/dependencies/HDFS_CLIENT",
              "Dependencies" : {
                "component_name" : "HDFS_CLIENT",
                "dependent_component_name" : "HISTORYSERVER",
                "dependent_service_name" : "MAPREDUCE2",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            }
          ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/MAPREDUCE2/components/MAPREDUCE2_CLIENT",
          "StackServiceComponents" : {
            "cardinality" : "0+",
            "component_category" : "CLIENT",
            "component_name" : "MAPREDUCE2_CLIENT",
            "display_name" : "MapReduce2 Client",
            "custom_commands" : [ ],
            "is_client" : true,
            "is_master" : false,
            "service_name" : "MAPREDUCE2",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        }
      ],
      artifacts: []
    },
    {
      "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/OOZIE",
      "StackServices" : {
        "comments" : "System for workflow coordination and execution of Apache Hadoop jobs.  This also includes the installation of the optional Oozie Web Console which relies on and will install the <a target=\"_blank\" href=\"http://www.sencha.com/legal/open-source-faq/\">ExtJS</a> Library.\n      ",
        "custom_commands" : [ ],
        "service_check_supported" : true,
        "service_name" : "OOZIE",
        "display_name" : "Oozie",
        "service_version" : "4.0.0.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1",
        "user_name" : null,
        "required_services" : [
          "YARN"
        ],
        "config_types" : {
          "oozie-env" : {
            "supports" : {
              "final" : "false"
            }
          },
          "oozie-log4j" : {
            "supports" : {
              "final" : "false"
            }
          },
          "oozie-site" : {
            "supports" : {
              "final" : "true"
            }
          },
          "yarn-site" : {
            "supports" : {
              "final" : "false"
            }
          }
        }
      },
      "components" : [
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/OOZIE/components/OOZIE_CLIENT",
          "StackServiceComponents" : {
            "cardinality" : "1+",
            "component_category" : "CLIENT",
            "component_name" : "OOZIE_CLIENT",
            "display_name" : "Oozie Client",
            "custom_commands" : [ ],
            "is_client" : true,
            "is_master" : false,
            "service_name" : "OOZIE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/OOZIE/components/OOZIE_CLIENT/dependencies/HDFS_CLIENT",
              "Dependencies" : {
                "component_name" : "HDFS_CLIENT",
                "dependent_component_name" : "OOZIE_CLIENT",
                "dependent_service_name" : "OOZIE",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            },
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/OOZIE/components/OOZIE_CLIENT/dependencies/MAPREDUCE2_CLIENT",
              "Dependencies" : {
                "component_name" : "MAPREDUCE2_CLIENT",
                "dependent_component_name" : "OOZIE_CLIENT",
                "dependent_service_name" : "OOZIE",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            }
          ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/OOZIE/components/OOZIE_SERVER",
          "StackServiceComponents" : {
            "cardinality" : "1",
            "component_category" : "MASTER",
            "component_name" : "OOZIE_SERVER",
            "display_name" : "Oozie Server",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : true,
            "service_name" : "OOZIE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/OOZIE/components/OOZIE_SERVER/dependencies/HDFS_CLIENT",
              "Dependencies" : {
                "component_name" : "HDFS_CLIENT",
                "dependent_component_name" : "OOZIE_SERVER",
                "dependent_service_name" : "OOZIE",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            },
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/OOZIE/components/OOZIE_SERVER/dependencies/MAPREDUCE2_CLIENT",
              "Dependencies" : {
                "component_name" : "MAPREDUCE2_CLIENT",
                "dependent_component_name" : "OOZIE_SERVER",
                "dependent_service_name" : "OOZIE",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            },
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/OOZIE/components/OOZIE_SERVER/dependencies/YARN_CLIENT",
              "Dependencies" : {
                "component_name" : "YARN_CLIENT",
                "dependent_component_name" : "OOZIE_SERVER",
                "dependent_service_name" : "OOZIE",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            }
          ]
        }
      ],
      artifacts: []
    },
    {
      "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/PIG",
      "StackServices" : {
        "comments" : "Scripting platform for analyzing large datasets",
        "custom_commands" : [ ],
        "service_check_supported" : true,
        "service_name" : "PIG",
        "display_name" : "Pig",
        "service_version" : "0.12.1.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1",
        "user_name" : null,
        "required_services" : [
          "YARN"
        ],
        "config_types" : {
          "pig-env" : {
            "supports" : {
              "final" : "false"
            }
          },
          "pig-log4j" : {
            "supports" : {
              "final" : "false"
            }
          },
          "pig-properties" : {
            "supports" : {
              "final" : "false"
            }
          }
        }
      },
      "components" : [
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/PIG/components/PIG",
          "StackServiceComponents" : {
            "cardinality" : "0+",
            "component_category" : "CLIENT",
            "component_name" : "PIG",
            "display_name" : "Pig",
            "custom_commands" : [ ],
            "is_client" : true,
            "is_master" : false,
            "service_name" : "PIG",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        }
      ],
      artifacts: []
    },
    {
      "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/SQOOP",
      "StackServices" : {
        "comments" : "Tool for transferring bulk data between Apache Hadoop and\n        structured data stores such as relational databases\n      ",
        "custom_commands" : [ ],
        "service_check_supported" : true,
        "service_name" : "SQOOP",
        "display_name" : "Sqoop",
        "service_version" : "1.4.4.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1",
        "user_name" : null,
        "required_services" : [
          "HDFS"
        ],
        "config_types" : {
          "sqoop-env" : {
            "supports" : {
              "final" : "false"
            }
          }
        }
      },
      "components" : [
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/SQOOP/components/SQOOP",
          "StackServiceComponents" : {
            "cardinality" : "1+",
            "component_category" : "CLIENT",
            "component_name" : "SQOOP",
            "display_name" : "Sqoop",
            "custom_commands" : [ ],
            "is_client" : true,
            "is_master" : false,
            "service_name" : "SQOOP",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/SQOOP/components/SQOOP/dependencies/HDFS_CLIENT",
              "Dependencies" : {
                "component_name" : "HDFS_CLIENT",
                "dependent_component_name" : "SQOOP",
                "dependent_service_name" : "SQOOP",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            },
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/SQOOP/components/SQOOP/dependencies/MAPREDUCE2_CLIENT",
              "Dependencies" : {
                "component_name" : "MAPREDUCE2_CLIENT",
                "dependent_component_name" : "SQOOP",
                "dependent_service_name" : "SQOOP",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            }
          ]
        }
      ],
      artifacts: []
    },
    {
      "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/STORM",
      "StackServices" : {
        "comments" : "Apache Hadoop Stream processing framework",
        "custom_commands" : [ ],
        "service_check_supported" : true,
        "service_name" : "STORM",
        "display_name" : "Storm",
        "service_version" : "0.9.1.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1",
        "user_name" : null,
        "required_services" : [
          "ZOOKEEPER"
        ],
        "config_types" : {
          "storm-env" : {
            "supports" : {
              "final" : "false"
            }
          },
          "storm-site" : {
            "supports" : {
              "final" : "true"
            }
          }
        }
      },
      "components" : [
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/STORM/components/DRPC_SERVER",
          "StackServiceComponents" : {
            "cardinality" : "1",
            "component_category" : "MASTER",
            "component_name" : "DRPC_SERVER",
            "display_name" : "DRPC Server",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : true,
            "service_name" : "STORM",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/STORM/components/NIMBUS",
          "StackServiceComponents" : {
            "cardinality" : "1",
            "component_category" : "MASTER",
            "component_name" : "NIMBUS",
            "display_name" : "Nimbus",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : true,
            "service_name" : "STORM",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [
            {
              "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/STORM/components/NIMBUS/dependencies/ZOOKEEPER_SERVER",
              "Dependencies" : {
                "component_name" : "ZOOKEEPER_SERVER",
                "dependent_component_name" : "NIMBUS",
                "dependent_service_name" : "STORM",
                "stack_name" : "HDP",
                "stack_version" : "2.1"
              }
            }
          ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/STORM/components/STORM_REST_API",
          "StackServiceComponents" : {
            "cardinality" : "1",
            "component_category" : "MASTER",
            "component_name" : "STORM_REST_API",
            "display_name" : "Storm REST API Server",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : true,
            "service_name" : "STORM",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/STORM/components/STORM_UI_SERVER",
          "StackServiceComponents" : {
            "cardinality" : "1",
            "component_category" : "MASTER",
            "component_name" : "STORM_UI_SERVER",
            "display_name" : "Storm UI Server",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : true,
            "service_name" : "STORM",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/STORM/components/SUPERVISOR",
          "StackServiceComponents" : {
            "cardinality" : "1+",
            "component_category" : "SLAVE",
            "component_name" : "SUPERVISOR",
            "display_name" : "Supervisor",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : false,
            "service_name" : "STORM",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        }
      ],
      artifacts: []
    },
    {
      "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/TEZ",
      "StackServices" : {
        "comments" : "Tez is the next generation Hadoop Query Processing framework written on top of YARN.",
        "custom_commands" : [ ],
        "service_check_supported" : false,
        "service_name" : "TEZ",
        "display_name" : "Tez",
        "service_version" : "0.4.0.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1",
        "user_name" : null,
        "required_services" : [
          "YARN"
        ],
        "config_types" : {
          "tez-env" : {
            "supports" : {
              "final" : "false"
            }
          },
          "tez-site" : {
            "supports" : {
              "final" : "true"
            }
          }
        }
      },
      "components" : [
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/TEZ/components/TEZ_CLIENT",
          "StackServiceComponents" : {
            "cardinality" : "1+",
            "component_category" : "CLIENT",
            "component_name" : "TEZ_CLIENT",
            "display_name" : "Tez Client",
            "custom_commands" : [ ],
            "is_client" : true,
            "is_master" : false,
            "service_name" : "TEZ",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        }
      ],
      artifacts: []
    },
    {
      "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/YARN",
      "StackServices" : {
        "comments" : "Apache Hadoop NextGen MapReduce (YARN)",
        "custom_commands" : [ ],
        "service_check_supported" : true,
        "service_name" : "YARN",
        "display_name" : "YARN",
        "service_version" : "2.4.0.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1",
        "user_name" : null,
        "required_services" : [
          "HDFS",
          "TEZ"
        ],
        "config_types" : {
          "capacity-scheduler" : {
            "supports" : {
              "final" : "true"
            }
          },
          "core-site" : {
            "supports" : {
              "final" : "true"
            }
          },
          "yarn-env" : {
            "supports" : {
              "final" : "false"
            }
          },
          "yarn-log4j" : {
            "supports" : {
              "final" : "false"
            }
          },
          "yarn-site" : {
            "supports" : {
              "final" : "true"
            }
          }
        }
      },
      "components" : [
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/YARN/components/APP_TIMELINE_SERVER",
          "StackServiceComponents" : {
            "cardinality" : "0-1",
            "component_category" : "MASTER",
            "component_name" : "APP_TIMELINE_SERVER",
            "display_name" : "App Timeline Server",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : true,
            "reassign_allowed" : true,
            "service_name" : "YARN",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/YARN/components/NODEMANAGER",
          "StackServiceComponents" : {
            "cardinality" : "1+",
            "component_category" : "SLAVE",
            "component_name" : "NODEMANAGER",
            "display_name" : "NodeManager",
            "custom_commands" : [ ],
            "decommission_allowed" : false,
            "is_client" : false,
            "is_master" : false,
            "service_name" : "YARN",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/YARN/components/RESOURCEMANAGER",
          "StackServiceComponents" : {
            "cardinality" : "1-2",
            "component_category" : "MASTER",
            "component_name" : "RESOURCEMANAGER",
            "display_name" : "ResourceManager",
            "custom_commands" : [
              "DECOMMISSION",
              "REFRESHQUEUES"
            ],
            "is_client" : false,
            "is_master" : true,
            "service_name" : "YARN",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/YARN/components/YARN_CLIENT",
          "StackServiceComponents" : {
            "cardinality" : "1+",
            "component_category" : "CLIENT",
            "component_name" : "YARN_CLIENT",
            "display_name" : "YARN Client",
            "custom_commands" : [ ],
            "is_client" : true,
            "is_master" : false,
            "service_name" : "YARN",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        }
      ],
      artifacts: []
    },
    {
      "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/ZOOKEEPER",
      "StackServices" : {
        "comments" : "Centralized service which provides highly reliable distributed\n        coordination.",
        "custom_commands" : [ ],
        "service_check_supported" : true,
        "service_name" : "ZOOKEEPER",
        "display_name" : "ZooKeeper",
        "service_version" : "3.4.5.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1",
        "user_name" : null,
        "required_services" : [ ],
        "config_types" : {
          "zoo.cfg" : {
            "supports" : {
              "final" : "false"
            }
          },
          "zookeeper-env" : {
            "supports" : {
              "final" : "false"
            }
          },
          "zookeeper-log4j" : {
            "supports" : {
              "final" : "false"
            }
          }
        }
      },
      "components" : [
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/ZOOKEEPER/components/ZOOKEEPER_CLIENT",
          "StackServiceComponents" : {
            "cardinality" : "1+",
            "component_category" : "CLIENT",
            "component_name" : "ZOOKEEPER_CLIENT",
            "display_name" : "ZooKeeper Client",
            "custom_commands" : [ ],
            "is_client" : true,
            "is_master" : false,
            "service_name" : "ZOOKEEPER",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.1/services/ZOOKEEPER/components/ZOOKEEPER_SERVER",
          "StackServiceComponents" : {
            "cardinality" : "1+",
            "component_category" : "MASTER",
            "component_name" : "ZOOKEEPER_SERVER",
            "display_name" : "ZooKeeper Server",
            "custom_commands" : [ ],
            "is_client" : false,
            "is_master" : true,
            "service_name" : "ZOOKEEPER",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          },
          "dependencies" : [ ]
        }
      ],
      artifacts: []
    },
    {
      "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.2/services/RANGER",
      "StackServices" : {
        "comments" : "Comprehensive security for Hadoop",
        "custom_commands" : [ ],
        "display_name" : "Ranger",
        "required_services" : [ ],
        "service_check_supported" : true,
        "service_name" : "RANGER",
        "service_version" : "0.4.0",
        "stack_name" : "HDP",
        "stack_version" : "2.2",
        "user_name" : null,
        "config_types" : {
          "admin-properties" : {
            "supports" : {
              "adding_forbidden" : "false",
              "do_not_extend" : "false",
              "final" : "false"
            }
          },
          "ranger-env" : {
            "supports" : {
              "adding_forbidden" : "false",
              "do_not_extend" : "false",
              "final" : "true"
            }
          },
          "ranger-site" : {
            "supports" : {
              "adding_forbidden" : "false",
              "do_not_extend" : "false",
              "final" : "false"
            }
          },
          "usersync-properties" : {
            "supports" : {
              "adding_forbidden" : "false",
              "do_not_extend" : "false",
              "final" : "false"
            }
          }
        }
      },
      "components" : [
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.2/services/RANGER/components/RANGER_ADMIN",
          "StackServiceComponents" : {
            "component_name" : "RANGER_ADMIN",
            "service_name" : "RANGER",
            "stack_name" : "HDP",
            "stack_version" : "2.2"
          },
          "dependencies" : [ ]
        },
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.2/services/RANGER/components/RANGER_USERSYNC",
          "StackServiceComponents" : {
            "component_name" : "RANGER_USERSYNC",
            "service_name" : "RANGER",
            "stack_name" : "HDP",
            "stack_version" : "2.2"
          },
          "dependencies" : [ ]
        }
      ],
      artifacts: []
    },
    {
      "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/services/RANGER_KMS",
      "StackServices" : {
        "comments" : "Key Management Server",
        "custom_commands" : [ ],
        "display_name" : "Ranger KMS",
        "required_services" : [
          "RANGER",
          "HDFS"
        ],
        "service_check_supported" : true,
        "service_name" : "RANGER_KMS",
        "service_version" : "0.5.0.2.3",
        "stack_name" : "HDP",
        "stack_version" : "2.3",
        "user_name" : null,
        "config_types" : {
          "dbks-site" : {
            "supports" : {
              "adding_forbidden" : "false",
              "do_not_extend" : "false",
              "final" : "false"
            }
          },
          "kms-env" : {
            "supports" : {
              "adding_forbidden" : "false",
              "do_not_extend" : "false",
              "final" : "false"
            }
          },
          "kms-log4j" : {
            "supports" : {
              "adding_forbidden" : "false",
              "do_not_extend" : "false",
              "final" : "false"
            }
          },
          "kms-properties" : {
            "supports" : {
              "adding_forbidden" : "false",
              "do_not_extend" : "false",
              "final" : "false"
            }
          },
          "kms-site" : {
            "supports" : {
              "adding_forbidden" : "false",
              "do_not_extend" : "false",
              "final" : "false"
            }
          },
          "ranger-kms-audit" : {
            "supports" : {
              "adding_forbidden" : "false",
              "do_not_extend" : "false",
              "final" : "false"
            }
          },
          "ranger-kms-policymgr-ssl" : {
            "supports" : {
              "adding_forbidden" : "false",
              "do_not_extend" : "false",
              "final" : "false"
            }
          },
          "ranger-kms-security" : {
            "supports" : {
              "adding_forbidden" : "false",
              "do_not_extend" : "false",
              "final" : "false"
            }
          },
          "ranger-kms-site" : {
            "supports" : {
              "adding_forbidden" : "false",
              "do_not_extend" : "false",
              "final" : "false"
            }
          }
        }
      },
      "themes" : [ ],
      "components" : [
        {
          "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/services/RANGER_KMS/components/RANGER_KMS_SERVER",
          "StackServiceComponents" : {
            "component_name" : "RANGER_KMS_SERVER",
            "service_name" : "RANGER_KMS",
            "stack_name" : "HDP",
            "stack_version" : "2.3"
          },
          "dependencies" : [ ]
        }
      ],
      artifacts: []
    }
  ]
};
