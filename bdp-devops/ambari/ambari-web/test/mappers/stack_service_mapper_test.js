/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/*eslint-disable */

var App = require('app');

require('mappers/stack_service_mapper');

describe('App.stackServiceMapper', function () {

  describe('#map', function () {

    var data = {
        "items": [
          {
            "StackServices" : {
              "comments" : "A high-throughput distributed messaging system",
              "custom_commands" : [ ],
              "display_name" : "Kafka",
              "required_services" : [
                "ZOOKEEPER"
              ],
              "service_check_supported" : true,
              "support_delete_via_ui" : false,
              "service_name" : "KAFKA",
              "service_version" : "0.8.1.2.2",
              "stack_name" : "HDP",
              "stack_version" : "2.2",
              "config_types" : {
                "kafka-broker" : {
                  "supports" : {
                    "adding_forbidden" : "false",
                    "do_not_extend" : "false",
                    "final" : "false"
                  }
                },
                "kafka-env" : {
                  "supports" : {
                    "adding_forbidden" : "false",
                    "do_not_extend" : "false",
                    "final" : "false"
                  }
                },
                "kafka-log4j" : {
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
                "StackServiceComponents" : {
                  "cardinality" : "1+",
                  "component_category" : "MASTER",
                  "component_name" : "KAFKA_BROKER",
                  "custom_commands" : [ ],
                  "display_name" : "Kafka Broker",
                  "is_client" : false,
                  "is_master" : true,
                  "service_name" : "KAFKA",
                  "stack_name" : "HDP",
                  "stack_version" : "2.2"
                },
                "dependencies" : [
                  {
                    "Dependencies" : {
                      "component_name" : "ZOOKEEPER_SERVER",
                      "dependent_component_name" : "KAFKA_BROKER",
                      "dependent_service_name" : "KAFKA",
                      "scope" : "cluster",
                      "stack_name" : "HDP",
                      "stack_version" : "2.2"
                    }
                  }
                ]
              }
            ],
            "artifacts" : [
              {
                "Artifacts" : {
                  "artifact_name": "widget_descriptor"
                }
              }
            ]
          },
          {
            "StackServices" : {
              "service_name" : "ZOOKEEPER",
              "support_delete_via_ui" : true
            },
            "components" : [ ],
            "artifacts" : [ ]
          },
          {
            "StackServices" : {
              "service_name" : "KERBEROS"
            },
            "components" : [ ],
            "artifacts" : [ ]
          },
          {
            "StackServices" : {
              "service_name" : "HDFS"
            },
            "components" : [
              {
                "StackServiceComponents" : {
                  "cardinality" : "1+",
                  "component_category" : "SLAVE",
                  "component_name" : "DATANODE",
                  "custom_commands" : [ ],
                  "decommission_allowed" : true,
                  "bulk_commands_display_name" : "DataNodes",
                  "bulk_commands_master_component_name" : "NAMENODE",
                  "has_bulk_commands_definition" : true,
                  "reassign_allowed" : true,
                  "display_name" : "DataNode",
                  "is_client" : false,
                  "is_master" : true,
                  "service_name" : "HDFS",
                  "stack_name" : "HDP"
                },
                "dependencies" : []
              }
            ],
            "artifacts" : [ ]
          },
          {
            "StackServices" : {
              "service_name" : "ACCUMULO"
            },
            "components" : [ ],
            "artifacts" : [ ]
          },
          {
            "StackServices" : {
              "service_name" : "HIVE"
            },
            "components" : [
              {
                "StackServiceComponents" : {
                  "component_name" : "MYSQL_SERVER",
                  "custom_commands" : [
                    "CLEAN"
                  ]
                },
                "dependencies" : [ ]
              }
            ],
            "artifacts" : [ ]
          }
        ]
      },
      sortedServiceNames = ["HDFS", "HIVE", "ZOOKEEPER", "ACCUMULO", "KAFKA", "KERBEROS"],
      serviceResult = {
        id: "KAFKA",
        serviceName: "KAFKA",
        displayName: "Kafka",
        configTypes: {
          "kafka-broker" : {
            "supports" : {
              "adding_forbidden" : "false",
              "do_not_extend" : "false",
              "final" : "false"
            }
          },
          "kafka-env" : {
            "supports" : {
              "adding_forbidden" : "false",
              "do_not_extend" : "false",
              "final" : "false"
            }
          },
          "kafka-log4j" : {
            "supports" : {
              "adding_forbidden" : "false",
              "do_not_extend" : "false",
              "final" : "false"
            }
          }
        },
        comments: "A high-throughput distributed messaging system",
        serviceVersion: "0.8.1.2.2",
        stackName: "HDP",
        isInstalled: false,
        isInstallable: true,
        isServiceWithWidgets: false,
        serviceCheckSupported: true,
        supportDeleteViaUi : false,
        requiredServices: ["ZOOKEEPER"]
      },
      componentResult = {
        id: "KAFKA_BROKER",
        componentName: "KAFKA_BROKER",
        displayName: "Kafka Broker",
        cardinality: "1+",
        customCommands: [],
        serviceName: "KAFKA",
        componentCategory: "MASTER",
        isMaster: true,
        isClient: false,
        stackName: "HDP",
        stackVersion: "2.2",
        dependencies: [
          {
            componentName: "ZOOKEEPER_SERVER",
            scope : "cluster"
          }
        ]
      };

    beforeEach(function () {
      App.stackServiceMapper.clearStackModels();
    });

    it('should sort and map data about services with their components', function () {
      App.stackServiceMapper.map(data);
      var services = App.StackService.find(),
        components = App.StackServiceComponent.find(),
        kafkaService = services.findProperty('serviceName', 'KAFKA');
      expect(services.mapProperty('serviceName')).to.eql(sortedServiceNames);
      expect(kafkaService.get('serviceComponents.length')).to.equal(1);
      Em.keys(serviceResult).forEach(function (key) {
        expect(kafkaService.get(key)).to.eql(serviceResult[key]);
      });
      Em.keys(componentResult).forEach(function (key) {
        expect(kafkaService.get('serviceComponents').toArray()[0].get(key)).to.eql(componentResult[key]);
      });
      Em.keys(componentResult).forEach(function (key) {
        expect(components.findProperty('componentName', 'KAFKA_BROKER').get(key)).to.eql(componentResult[key]);
      });
      expect(services.findProperty('serviceName', 'KERBEROS').get('isInstallable')).to.be.false;
      expect(services.findProperty('serviceName', 'KERBEROS').get('isSelected')).to.be.false;
      expect(components.findProperty('componentName', 'MYSQL_SERVER').get('customCommands')).to.be.empty;

      expect(components.findProperty('componentName', 'DATANODE').get('hasBulkCommandsDefinition')).to.be.true;
      expect(components.findProperty('componentName', 'DATANODE').get('bulkCommandsDisplayName')).to.eql("DataNodes");
      expect(components.findProperty('componentName', 'DATANODE').get('bulkCommandsMasterComponentName')).to.eql("NAMENODE");
      expect(components.findProperty('componentName', 'DATANODE').get('decommissionAllowed')).to.be.true;
      expect(components.findProperty('componentName', 'DATANODE').get('reassignAllowed')).to.be.true;

      expect(services.findProperty('serviceName', 'ZOOKEEPER').get('supportDeleteViaUi')).to.be.true;
    });

  });

});
