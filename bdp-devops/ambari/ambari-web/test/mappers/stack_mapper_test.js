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

var App = require('app');
require('mappers/server_data_mapper');
require('mappers/stack_mapper');
require('models/stack');
require('models/operating_system');
require('models/repository');
require('models/stack_version/service_simple');

describe('App.stackMapper', function () {
	describe("#map", function() {
    var testData = {
      "items" : [
      {
        "VersionDefinition" : {
          "id" : "HDP-2.3",
          "show_available": true,
          "stack_name" : "HDP",
          "stack_version" : "2.3",
          "repository_version" : "2.3",
          "version_url" : "file:/Users/ncole/src/hwx/ambari/contrib/version-builder/version_234-3396.xml",
          "release" : {
            "build" : "3396",
            "compatible_with" : "2.3.[0-3].0",
            "notes" : "http://example.com",
            "version" : "2.3.4.0"
          },
          "stack_services" : [
            {
              "name" : "HDFS",
              "display_name" : "HDFS",
              "comment" : "Data warehouse system for ad-hoc queries & analysis of large datasets and table & storage management service",
              "versions" : [
                "2.7.1.2.3396"
              ]
            },
            {
              "name" : "YARN",
              "display_name" : "YARN",
              "comment" : "",
              "versions" : [
                "1.7.3.3396"
              ]
            },
            {
              "name" : "ZOOKEEPER",
              "display_name" : "ZooKeeper",
              "comment" : "",
              "versions" : [
                "1.7.3.3396"
              ]
            }
          ]
        },
        "operating_systems" : [
          {
            "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/debian7",
            "OperatingSystems" : {
              "os_type" : "debian7",
              "repository_version_id" : 1,
              "stack_name" : "HDP",
              "stack_version" : "2.3"
            },
            "repositories" : [
              {
                "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/debian7/repositories/HDP-2.3",
                "Repositories" : {
                  "base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/debian7/2.x/BUILDS/2.3.4.0-3396",
                  "default_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/debian7/2.x/BUILDS/2.3.4.0-3396",
                  "latest_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/debian7/2.x/BUILDS/2.3.4.0-3396",
                  "mirrors_list" : "",
                  "os_type" : "debian7",
                  "repo_id" : "HDP-2.3",
                  "repo_name" : "HDP",
                  "repository_version_id" : 1,
                  "stack_name" : "HDP",
                  "stack_version" : "2.3"
                }
              },
              {
                "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/debian7/repositories/HDP-UTILS-1.1.0.20",
                "Repositories" : {
                  "base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/debian7",
                  "default_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/debian7",
                  "latest_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/debian7",
                  "mirrors_list" : "",
                  "os_type" : "debian7",
                  "repo_id" : "HDP-UTILS-1.1.0.20",
                  "repo_name" : "HDP-UTILS",
                  "repository_version_id" : 1,
                  "stack_name" : "HDP",
                  "stack_version" : "2.3"
                }
              }
            ]
          },
          {
            "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/redhat6",
            "OperatingSystems" : {
              "os_type" : "redhat6",
              "repository_version_id" : 1,
              "stack_name" : "HDP",
              "stack_version" : "2.3"
            },
            "repositories" : [
              {
                "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/redhat6/repositories/HDP-2.3",
                "Repositories" : {
                  "base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.3.4.0-3396",
                  "default_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.3.4.0-3396",
                  "latest_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.3.4.0-3396",
                  "mirrors_list" : "",
                  "os_type" : "redhat6",
                  "repo_id" : "HDP-2.3",
                  "repo_name" : "HDP",
                  "repository_version_id" : 1,
                  "stack_name" : "HDP",
                  "stack_version" : "2.3"
                }
              },
              {
                "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/redhat6/repositories/HDP-UTILS-1.1.0.20",
                "Repositories" : {
                  "base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/centos6",
                  "default_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/centos6",
                  "latest_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/centos6",
                  "mirrors_list" : "",
                  "os_type" : "redhat6",
                  "repo_id" : "HDP-UTILS-1.1.0.20",
                  "repo_name" : "HDP-UTILS",
                  "repository_version_id" : 1,
                  "stack_name" : "HDP",
                  "stack_version" : "2.3"
                }
              }
            ]
          }
        ]
      },

      {
        "VersionDefinition" : {
          "id" : "HDP-2.3-2.3.4.0",
          "stack_name" : "HDP",
          "stack_version" : "2.3",
          "show_available": true,
          "repository_version" : "2.3.4.0",
          "version_url" : "file:/Users/ncole/src/hwx/ambari/contrib/version-builder/version_234-3397.xml",
          "release" : {
            "build" : "3397",
            "compatible_with" : "2.3.[0-3].0",
            "notes" : "http://example.com",
            "version" : "2.3.4.0"
          },
          "stack_services" : [
            {
              "name" : "HDFS",
              "display_name" : "HDFS",
              "comment" : "Data warehouse system for ad-hoc queries & analysis of large datasets and table & storage management service",
              "versions" : [
                "2.7.1.2-3397"
              ]
            },
            {
              "name" : "YARN",
              "display_name" : "YARN",
              "comment" : "",
              "versions" : [
                "1.7.3-3397"
              ]
            },
            {
              "name" : "HBase",
              "display_name" : "HBase",
              "comment" : "",
              "versions" : [
                "1.7.3-3397"
              ]
            },
            {
              "name" : "ZOOKEEPER",
              "display_name" : "ZooKeeper",
              "comment" : "",
              "versions" : [
                "1.7.3-3397"
              ]
            },
            {
              "name" : "Hive",
              "display_name" : "Hive",
              "comment" : "",
              "versions" : [
                "1.1.0-3397"
              ]
            }
          ]
        },
        "operating_systems" : [
          {
            "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/debian7",
            "OperatingSystems" : {
              "os_type" : "debian7",
              "repository_version_id" : 1,
              "stack_name" : "HDP",
              "stack_version" : "2.3"
            },
            "repositories" : [
              {
                "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/debian7/repositories/HDP-2.3",
                "Repositories" : {
                  "base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/debian7/2.x/BUILDS/2.3.4.0-3397",
                  "default_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/debian7/2.x/BUILDS/2.3.4.0-3397",
                  "latest_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/debian7/2.x/BUILDS/2.3.4.0-3397",
                  "mirrors_list" : "",
                  "os_type" : "debian7",
                  "repo_id" : "HDP-2.3",
                  "repo_name" : "HDP",
                  "repository_version_id" : 1,
                  "stack_name" : "HDP",
                  "stack_version" : "2.3"
                }
              },
              {
                "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/debian7/repositories/HDP-UTILS-1.1.0.20",
                "Repositories" : {
                  "base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/debian7",
                  "default_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/debian7",
                  "latest_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/debian7",
                  "mirrors_list" : "",
                  "os_type" : "debian7",
                  "repo_id" : "HDP-UTILS-1.1.0.20",
                  "repo_name" : "HDP-UTILS",
                  "repository_version_id" : 1,
                  "stack_name" : "HDP",
                  "stack_version" : "2.3"
                }
              }
            ]
          },
          {
            "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/redhat6",
            "OperatingSystems" : {
              "os_type" : "redhat6",
              "repository_version_id" : 1,
              "stack_name" : "HDP",
              "stack_version" : "2.3"
            },
            "repositories" : [
              {
                "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/redhat6/repositories/HDP-2.3",
                "Repositories" : {
                  "base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.3.4.0-3397",
                  "default_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.3.4.0-3397",
                  "latest_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.3.4.0-3397",
                  "mirrors_list" : "",
                  "os_type" : "redhat6",
                  "repo_id" : "HDP-2.3",
                  "repo_name" : "HDP",
                  "repository_version_id" : 1,
                  "stack_name" : "HDP",
                  "stack_version" : "2.3"
                }
              },
              {
                "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/redhat6/repositories/HDP-UTILS-1.1.0.20",
                "Repositories" : {
                  "base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/centos6",
                  "default_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/centos6",
                  "latest_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/centos6",
                  "mirrors_list" : "",
                  "os_type" : "redhat6",
                  "repo_id" : "HDP-UTILS-1.1.0.20",
                  "repo_name" : "HDP-UTILS",
                  "repository_version_id" : 1,
                  "stack_name" : "HDP",
                  "stack_version" : "2.3"
                }
              }
            ]
          }
        ]
      },

      {
        "VersionDefinition" : {
          "id" : "HDP-2.3-2.3.6.0",
          "stack_name" : "HDP",
          "stack_version" : "2.3",
          "show_available": true,
          "repository_version" : "2.3.6.0",
          "version_url" : "file:/Users/ncole/src/hwx/ambari/contrib/version-builder/version_234-3646.xml",
          "release" : {
            "build" : "3646",
            "compatible_with" : "2.3.[0-6].0",
            "notes" : "http://example.com",
            "version" : "2.3.6.0"
          },
          "stack_services" : [
            {
              "name" : "HDFS",
              "display_name" : "HDFS",
              "comment" : "Data warehouse system for ad-hoc queries & analysis of large datasets and table & storage management service",
              "versions" : [
                "2.7.1.2-3646"
              ]
            },
            {
              "name" : "YARN",
              "display_name" : "YARN",
              "comment" : "",
              "versions" : [
                "1.7.3-3646"
              ]
            },
            {
              "name" : "HBase",
              "display_name" : "HBase",
              "comment" : "",
              "versions" : [
                "1.7.3-3646"
              ]
            },
            {
              "name" : "ZOOKEEPER",
              "display_name" : "ZooKeeper",
              "comment" : "",
              "versions" : [
                "1.7.3-3646"
              ]
            },
            {
              "name" : "Hive",
              "display_name" : "Hive",
              "comment" : "",
              "versions" : [
                "1.1.0-3646"
              ]
            }
          ]
        },
        "operating_systems" : [
          {
            "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/debian7",
            "OperatingSystems" : {
              "os_type" : "debian7",
              "repository_version_id" : 1,
              "stack_name" : "HDP",
              "stack_version" : "2.3"
            },
            "repositories" : [
              {
                "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/debian7/repositories/HDP-2.3",
                "Repositories" : {
                  "base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/debian7/2.x/BUILDS/2.3.6.0-3646",
                  "default_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/debian7/2.x/BUILDS/2.3.6.0-3646",
                  "latest_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/debian7/2.x/BUILDS/2.3.6.0-3646",
                  "mirrors_list" : "",
                  "os_type" : "debian7",
                  "repo_id" : "HDP-2.3",
                  "repo_name" : "HDP",
                  "repository_version_id" : 1,
                  "stack_name" : "HDP",
                  "stack_version" : "2.3"
                }
              },
              {
                "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/debian7/repositories/HDP-UTILS-1.1.0.20",
                "Repositories" : {
                  "base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/debian7",
                  "default_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/debian7",
                  "latest_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/debian7",
                  "mirrors_list" : "",
                  "os_type" : "debian7",
                  "repo_id" : "HDP-UTILS-1.1.0.20",
                  "repo_name" : "HDP-UTILS",
                  "repository_version_id" : 1,
                  "stack_name" : "HDP",
                  "stack_version" : "2.3"
                }
              }
            ]
          },
          {
            "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/redhat6",
            "OperatingSystems" : {
              "os_type" : "redhat6",
              "repository_version_id" : 1,
              "stack_name" : "HDP",
              "stack_version" : "2.3"
            },
            "repositories" : [
              {
                "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/redhat6/repositories/HDP-2.3",
                "Repositories" : {
                  "base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.3.6.0-3646",
                  "default_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.3.6.0-3646",
                  "latest_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.3.6.0-3646",
                  "mirrors_list" : "",
                  "os_type" : "redhat6",
                  "repo_id" : "HDP-2.3",
                  "repo_name" : "HDP",
                  "repository_version_id" : 1,
                  "stack_name" : "HDP",
                  "stack_version" : "2.3"
                }
              },
              {
                "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/redhat6/repositories/HDP-UTILS-1.1.0.20",
                "Repositories" : {
                  "base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/centos6",
                  "default_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/centos6",
                  "latest_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/centos6",
                  "mirrors_list" : "",
                  "os_type" : "redhat6",
                  "repo_id" : "HDP-UTILS-1.1.0.20",
                  "repo_name" : "HDP-UTILS",
                  "repository_version_id" : 1,
                  "stack_name" : "HDP",
                  "stack_version" : "2.3"
                }
              }
            ]
          }
        ]
      },


      {
        "VersionDefinition" : {
          "id" : "HDP-2.4-2.4.0.0",
          "stack_name" : "HDP",
          "stack_version" : "2.4",
          "show_available": true,
          "repository_version" : "2.4.0.0",
          "version_url" : "file:/Users/ncole/src/hwx/ambari/contrib/version-builder/version_169.xml",
          "release" : {
            "build" : "169",
            "compatible_with" : "2.4.[0-3].0",
            "notes" : "http://example.com",
            "version" : "2.4.0.0"
          },
          "stack_services" : [
            {
              "name" : "HDFS",
              "display_name" : "HDFS",
              "comment" : "Data warehouse system for ad-hoc queries & analysis of large datasets and table & storage management service",
              "versions" : [
                "2.7.1.2-169"
              ]
            },
            {
              "name" : "YARN",
              "display_name" : "YARN",
              "comment" : "",
              "versions" : [
                "1.7.3-169"
              ]
            },
            {
              "name" : "HBase",
              "display_name" : "HBase",
              "comment" : "",
              "versions" : [
                "1.7.3-169"
              ]
            },
            {
              "name" : "ZOOKEEPER",
              "display_name" : "ZooKeeper",
              "comment" : "",
              "versions" : [
                "1.7.3-169"
              ]
            },
            {
              "name" : "Hive",
              "display_name" : "Hive",
              "comment" : "",
              "versions" : [
                "1.1.0-169"
              ]
            },
            {
              "name" : "MAPREDUCE2",
              "display_name" : "MapReduce2",
              "comment" : "service",
              "versions" : [
                "2.7.1.2-169"
              ]
            },
            {
              "name" : "Slider",
              "display_name" : "Slider",
              "comment" : "service",
              "versions" : [
                "2.7.1.2-169"
              ]
            },
            {
              "name" : "Pig",
              "display_name" : "Pig",
              "comment" : "service",
              "versions" : [
                "2.7.1.2-169"
              ]
            },
            {
              "name" : "Sqoop",
              "display_name" : "Sqoop",
              "comment" : "service",
              "versions" : [
                "2.7.1.2-169"
              ]
            }
          ]
        },
        "operating_systems" : [
          {
            "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/debian7",
            "OperatingSystems" : {
              "os_type" : "debian7",
              "repository_version_id" : 1,
              "stack_name" : "HDP",
              "stack_version" : "2.4"
            },
            "repositories" : [
              {
                "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/debian7/repositories/HDP-2.3",
                "Repositories" : {
                  "base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/debian7/2.x/BUILDS/2.4.0.0-169",
                  "default_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/debian7/2.x/BUILDS/2.4.0.0-169",
                  "latest_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP/debian7/2.x/BUILDS/2.4.0.0-169",
                  "mirrors_list" : "",
                  "os_type" : "debian7",
                  "repo_id" : "HDP-2.4",
                  "repo_name" : "HDP",
                  "repository_version_id" : 1,
                  "stack_name" : "HDP",
                  "stack_version" : "2.4"
                }
              },
              {
                "href" : "http://c6401.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.3/repository_versions/1/operating_systems/debian7/repositories/HDP-UTILS-1.1.0.20",
                "Repositories" : {
                  "base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/debian7",
                  "default_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/debian7",
                  "latest_base_url" : "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/debian7",
                  "mirrors_list" : "",
                  "os_type" : "debian7",
                  "repo_id" : "HDP-UTILS-1.1.0.20",
                  "repo_name" : "HDP-UTILS",
                  "repository_version_id" : 1,
                  "stack_name" : "HDP",
                  "stack_version" : "2.4"
                }
              }
            ]
          }
        ]
      }
    ]
    };
    beforeEach(function () {
      App.resetDsStoreTypeMap(App.Repository);
      App.resetDsStoreTypeMap(App.OperatingSystem);
      App.resetDsStoreTypeMap(App.Stack);
      App.resetDsStoreTypeMap(App.ServiceSimple);
      sinon.stub(App.store, 'commit', Em.K);
      testData.items.sortProperty('VersionDefinition.stack_version').reverse().forEach(function (versionDefinition) {
        App.stackMapper.map(versionDefinition);
      });
    });
    afterEach(function(){
      App.store.commit.restore();
    });

    it ('should map all Stack data', function() {
      expect(App.Stack.find().get('length')).to.equal(4);
    });

    it ('all stacks are showAvailable', function() {
      expect(App.Stack.find().everyProperty('showAvailable')).to.equal(true);
    });

    it ('no one stack is selected', function() {
      expect(App.Stack.find().everyProperty('isSelected')).to.equal(false);
    });

    it ('7 OSes are mapped', function() {
      expect(App.OperatingSystem.find().get('length')).to.equal(7);
    });

    it ('OSes have valid ids', function() {
      expect(App.OperatingSystem.find().mapProperty('id')).to.eql(
        ['HDP-2.4-2.4.0.0-debian7', 'HDP-2.3-2.3.6.0-debian7', 'HDP-2.3-2.3.6.0-redhat6', 'HDP-2.3-2.3.4.0-debian7',
        'HDP-2.3-2.3.4.0-redhat6', 'HDP-2.3-debian7', 'HDP-2.3-redhat6']);
    });

    it ('14 repositories are mapped', function() {
      expect(App.Repository.find().get('length')).to.equal(14);
    });

    it ('Repositories ids are valid', function() {
      expect(App.Repository.find().mapProperty('id')).to.eql(
        ['HDP-2.4-2.4.0.0-debian7-HDP-2.4', 'HDP-2.4-2.4.0.0-debian7-HDP-UTILS-1.1.0.20',
          'HDP-2.3-2.3.6.0-debian7-HDP-2.3', 'HDP-2.3-2.3.6.0-debian7-HDP-UTILS-1.1.0.20',
          'HDP-2.3-2.3.6.0-redhat6-HDP-2.3','HDP-2.3-2.3.6.0-redhat6-HDP-UTILS-1.1.0.20',
          'HDP-2.3-2.3.4.0-debian7-HDP-2.3','HDP-2.3-2.3.4.0-debian7-HDP-UTILS-1.1.0.20',
          'HDP-2.3-2.3.4.0-redhat6-HDP-2.3', 'HDP-2.3-2.3.4.0-redhat6-HDP-UTILS-1.1.0.20',
          'HDP-2.3-debian7-HDP-2.3', 'HDP-2.3-debian7-HDP-UTILS-1.1.0.20',
          'HDP-2.3-redhat6-HDP-2.3', 'HDP-2.3-redhat6-HDP-UTILS-1.1.0.20'
        ]);
    });
  });
});
