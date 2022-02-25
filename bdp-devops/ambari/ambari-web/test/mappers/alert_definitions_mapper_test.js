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

var App = require('app');

require('mappers/alert_definitions_mapper');
var testHelpers = require('test/helpers');

describe('App.alertDefinitionsMapper', function () {
  /*eslint-disable mocha-cleanup/asserts-limit */
  describe('#map', function () {

    var json = {
      items: [
        {
          "AlertDefinition" : {
            "component_name" : "RESOURCEMANAGER",
            "enabled" : true,
            "id" : 1,
            "ignore_host" : false,
            "interval" : 5,
            "label" : "ResourceManager RPC Latency",
            "name" : "yarn_resourcemanager_rpc_latency",
            "description" : "some description",
            "scope" : "ANY",
            "service_name" : "YARN",
            "source" : {
              "jmx" : {
                "property_list" : [
                  "Hadoop:service=ResourceManager,name=RpcActivityForPort*/RpcQueueTimeAvgTime",
                  "Hadoop:service=ResourceManager,name=RpcActivityForPort*/RpcProcessingTimeAvgTime"
                ],
                "value" : "{0}"
              },
              "reporting" : {
                "ok" : {
                  "text" : "Average Queue Time:[{0}], Average Processing Time:[{1}]"
                },
                "warning" : {
                  "text" : "Average Queue Time:[{0}], Average Processing Time:[{1}]",
                  "value" : 3000.0
                },
                "critical" : {
                  "text" : "Average Queue Time:[{0}], Average Processing Time:[{1}]",
                  "value" : 5000.0
                }
              },
              "type" : "METRIC",
              "uri" : {
                "http" : "{{yarn-site/yarn.resourcemanager.webapp.address}}",
                "https" : "{{yarn-site/yarn.resourcemanager.webapp.https.address}}",
                "https_property" : "{{yarn-site/yarn.http.policy}}",
                "https_property_value" : "HTTPS_ONLY",
                "default_port" : 0.0
              }
            }
          }
        },
        {
          "AlertDefinition" : {
            "component_name" : "RESOURCEMANAGER",
            "enabled" : true,
            "id" : 2,
            "ignore_host" : false,
            "interval" : 1,
            "label" : "ResourceManager Web UI",
            "name" : "yarn_resourcemanager_webui",
            "description" : "",
            "scope" : "ANY",
            "service_name" : "YARN",
            "source" : {
              "reporting" : {
                "ok" : {
                  "text" : "HTTP {0} response in {2:.4f} seconds"
                },
                "warning" : {
                  "text" : "HTTP {0} response in {2:.4f} seconds"
                },
                "critical" : {
                  "text" : "Connection failed to {1}"
                }
              },
              "type" : "WEB",
              "uri" : {
                "http" : "{{yarn-site/yarn.resourcemanager.webapp.address}}",
                "https" : "{{yarn-site/yarn.resourcemanager.webapp.https.address}}",
                "https_property" : "{{yarn-site/yarn.http.policy}}",
                "https_property_value" : "HTTPS_ONLY",
                "connection_timeout" : 5.0,
                "default_port" : 0.0
              }
            }
          }
        },
        {
          "AlertDefinition" : {
            "component_name" : null,
            "enabled" : true,
            "id" : 3,
            "ignore_host" : false,
            "interval" : 1,
            "label" : "Percent NodeManagers Available",
            "name" : "yarn_nodemanager_webui_percent",
            "description" : null,
            "scope" : "SERVICE",
            "service_name" : "YARN",
            "source" : {
              "alert_name" : "yarn_nodemanager_webui",
              "reporting" : {
                "ok" : {
                  "text" : "affected: [{1}], total: [{0}]"
                },
                "warning" : {
                  "text" : "affected: [{1}], total: [{0}]",
                  "value" : 0.1
                },
                "critical" : {
                  "text" : "affected: [{1}], total: [{0}]",
                  "value" : 0.3
                }
              },
              "type" : "AGGREGATE"
            }
          }
        },
        {
          "AlertDefinition" : {
            "component_name" : "NODEMANAGER",
            "enabled" : true,
            "id" : 4,
            "ignore_host" : false,
            "interval" : 1,
            "label" : "NodeManager Health",
            "name" : "yarn_nodemanager_health",
            "description" : "some description",
            "scope" : "HOST",
            "service_name" : "YARN",
            "source" : {
              "parameters" : [
                {
                  "name" : "connection.timeout",
                  "display_name" : "Connection Timeout",
                  "units" : "seconds",
                  "value" : 5.0,
                  "description" : "The maximum time before this alert is considered to be CRITICAL",
                  "type" : "NUMERIC",
                  "threshold" : "CRITICAL"
                }
              ],
              "path" : "HDP/2.0.6/services/YARN/package/files/alert_nodemanager_health.py",
              "type" : "SCRIPT"
            }
          }
        },
        {
          "AlertDefinition" : {
            "component_name" : "ZOOKEEPER_SERVER",
            "enabled" : true,
            "id" : 5,
            "ignore_host" : false,
            "interval" : 1,
            "label" : "ZooKeeper Server Process",
            "name" : "zookeeper_server_process",
            "description" : "some description",
            "scope" : "ANY",
            "service_name" : "ZOOKEEPER",
            "source" : {
              "default_port" : 2181.0,
              "reporting" : {
                "ok" : {
                  "text" : "TCP OK - {0:.4f} response on port {1}"
                },
                "critical" : {
                  "text" : "Connection failed: {0} to {1}:{2}"
                }
              },
              "type" : "PORT",
              "uri" : "{{zookeeper-env/clientPort}}"
            }
          }
        },
        {
          "AlertDefinition" : {
            "component_name" : "NAMENODE",
            "description" : "This service-level alert is triggered if the NN heap usage deviation has grown beyond the specified threshold within a given time interval.",
            "enabled" : true,
            "help_url" : "http://test.test",
            "id" : 6,
            "ignore_host" : false,
            "interval" : 1,
            "label" : "NameNode Heap Usage (Hourly)",
            "name" : "namenode_free_heap_size_deviation_percentage",
            "repeat_tolerance" : 1,
            "repeat_tolerance_enabled" : true,
            "scope" : "SERVICE",
            "service_name" : "HDFS",
            "source" : {
              "ams" : {
                "metric_list" : [
                  "jvm.JvmMetrics.MemHeapUsedM",
                  "jvm.JvmMetrics.MemHeapMaxM"
                ],
                "value" : "{1} - {0}",
                "interval" : 60.0,
                "compute" : "sample_standard_deviation_percentage",
                "app_id" : "NAMENODE",
                "minimum_value" : 1.0
              },
              "reporting" : {
                "ok" : {
                  "text" : "The sample standard deviation percentage is {0}%"
                },
                "warning" : {
                  "text" : "The sample standard deviation percentage is {0}%",
                  "value" : 20.0
                },
                "critical" : {
                  "text" : "The sample standard deviation percentage is {0}%",
                  "value" : 50.0
                },
                "units" : "%"
              },
              "type" : "AMS",
              "uri" : {
                "http" : "{{ams-site/timeline.metrics.service.webapp.address}}",
                "https" : "{{ams-site/timeline.metrics.service.webapp.address}}",
                "https_property" : "{{ams-site/timeline.metrics.service.http.policy}}",
                "https_property_value" : "HTTPS_ONLY",
                "default_port" : 0.0,
                "connection_timeout" : 5.0
              }
            }
          }
        }
      ]
    };

    beforeEach(function () {

      App.alertDefinitionsMapper.setProperties({
        'model': {},
        'parameterModel': {},
        'reportModel': {},
        'metricsSourceModel': {},
        'metricsUriModel': {},
        'metricsAmsModel': {}
      });

      sinon.stub(App.alertDefinitionsMapper, 'deleteRecord', Em.K);

      sinon.stub(App.store, 'commit', Em.K);
      sinon.stub(App.store, 'loadMany', function (type, content) {
        type.content = content;
      });

      sinon.stub(App.router, 'get', function() {return false;});
      App.cache.previousAlertGroupsMap = {};

      sinon.stub(App.alertDefinitionsMapper, 'setMetricsSourcePropertyLists', Em.K);
      sinon.stub(App.alertDefinitionsMapper, 'setAlertDefinitionsRawSourceData', Em.K);

    });

    afterEach(function () {

      App.store.commit.restore();
      App.store.loadMany.restore();

      App.alertDefinitionsMapper.setProperties({
        'model': App.AlertDefinition,

        'reportModel': App.AlertReportDefinition,
        'metricsSourceModel': App.AlertMetricsSourceDefinition,
        'metricsUriModel': App.AlertMetricsUriDefinition,
        'metricsAmsModel': App.AlertMetricsAmsDefinition
      });

      App.alertDefinitionsMapper.deleteRecord.restore();

      App.router.get.restore();
      App.cache.previousAlertGroupsMap = {};

      App.alertDefinitionsMapper.setMetricsSourcePropertyLists.restore();
      App.alertDefinitionsMapper.setAlertDefinitionsRawSourceData.restore();

    });

    describe('should parse METRIC alertDefinitions', function () {

      var data = {items: [json.items[0]]},
        expected = [{
          id: 1,
          "name": "yarn_resourcemanager_rpc_latency",
          "label": "ResourceManager RPC Latency",
          "description" : "some description",
          "service_id": "YARN",
          "component_name": "RESOURCEMANAGER",
          "enabled": true,
          "scope": "ANY",
          "interval": 5,
          "type": "METRIC",
          "jmx_id": "1jmx",
          "uri_id": "1uri"
        }],
        expectedMetricsSource = [{
          "id":"1jmx",
          "value":"{0}",
          "property_list":[
            "Hadoop:service=ResourceManager,name=RpcActivityForPort*/RpcQueueTimeAvgTime",
            "Hadoop:service=ResourceManager,name=RpcActivityForPort*/RpcProcessingTimeAvgTime"
          ]
        }],
        expectedMetricsUri = [{
          "id":"1uri",
          "http":"{{yarn-site/yarn.resourcemanager.webapp.address}}",
          "https":"{{yarn-site/yarn.resourcemanager.webapp.https.address}}",
          "https_property":"{{yarn-site/yarn.http.policy}}",
          "https_property_value":"HTTPS_ONLY"
        }];


      beforeEach(function () {

        App.alertDefinitionsMapper.map(data);

      });

      it('parsing metrics model', function() {
        testHelpers.nestedExpect(expected, App.alertDefinitionsMapper.get('model.content'));
      });

      it('parse metrics source', function() {
        testHelpers.nestedExpect(expectedMetricsSource, App.alertDefinitionsMapper.get('metricsSourceModel.content'));
      });

      it('parse metrics uri', function() {
        testHelpers.nestedExpect(expectedMetricsUri, App.alertDefinitionsMapper.get('metricsUriModel.content'));
      });

    });

    describe('should parse WEB alertDefinitions', function () {

      var data = {items: [json.items[1]]},
        expected = [
          {
            "id": 2,
            "name": "yarn_resourcemanager_webui",
            "label": "ResourceManager Web UI",
            "description" : "",
            "service_id": "YARN",
            "component_name": "RESOURCEMANAGER",
            "enabled": true,
            "scope": "ANY",
            "interval": 1,
            "type": "WEB",
            "uri_id": "2uri"
          }
        ],
        expectedMetricsUri = [{
          "id":"2uri",
          "http":"{{yarn-site/yarn.resourcemanager.webapp.address}}",
          "https":"{{yarn-site/yarn.resourcemanager.webapp.https.address}}",
          "https_property":"{{yarn-site/yarn.http.policy}}",
          "https_property_value":"HTTPS_ONLY",
          "connection_timeout" : 5.0
        }];

      beforeEach(function () {

        App.alertDefinitionsMapper.map(data);

      });

      it('parsing web model', function() {
        testHelpers.nestedExpect(expected, App.alertDefinitionsMapper.get('model.content'));
      });


      it('parse metrics uri', function() {
        testHelpers.nestedExpect(expectedMetricsUri, App.alertDefinitionsMapper.get('metricsUriModel.content'));
      });

    });

    it('should parse AGGREGATE alertDefinitions', function () {

      var data = {items: [json.items[2]]},
        expected = [
          {
            "id":3,
            "name":"yarn_nodemanager_webui_percent",
            "label":"Percent NodeManagers Available",
            "description" : "",
            "service_id":"YARN",
            "component_name":null,
            "enabled":true,
            "scope":"SERVICE",
            "interval":1,
            "type":"AGGREGATE",
            "alert_name":"yarn_nodemanager_webui"
          }
        ];
      App.alertDefinitionsMapper.map(data);

      testHelpers.nestedExpect(expected, App.alertDefinitionsMapper.get('model.content'));

    });

    describe('should parse SCRIPT alertDefinitions', function () {

      var data = {items: [json.items[3]]},
        expected = [
          {
            "id":4,
            "name":"yarn_nodemanager_health",
            "label":"NodeManager Health",
            "description" : "some description",
            "service_id":"YARN",
            "component_name":"NODEMANAGER",
            "enabled":true,
            "scope":"HOST",
            "interval":1,
            "type":"SCRIPT",
            "location":"HDP/2.0.6/services/YARN/package/files/alert_nodemanager_health.py"
          }
        ];

      var expectedParameters = [{
        "id": "4connection.timeout",
        "name": "connection.timeout",
        "display_name": "Connection Timeout",
        "units": "seconds",
        "value": 5,
        "description": "The maximum time before this alert is considered to be CRITICAL",
        "type": "NUMERIC",
        "threshold": "CRITICAL"
      }];

      beforeEach(function () {
        App.alertDefinitionsMapper.map(data);
      });

      it('should map definition', function () {
        testHelpers.nestedExpect(expected, App.alertDefinitionsMapper.get('model.content'));
      });

      it('should map parameters', function () {
        testHelpers.nestedExpect(expectedParameters, App.alertDefinitionsMapper.get('parameterModel.content'));
      });

    });

    it('should parse PORT alertDefinitions', function () {

      var data = {items: [json.items[4]]},
        expected = [
          {
            "id":5,
            "name":"zookeeper_server_process",
            "label":"ZooKeeper Server Process",
            "description" : "some description",
            "service_id":"ZOOKEEPER",
            "component_name":"ZOOKEEPER_SERVER",
            "enabled":true,
            "scope":"ANY",
            "interval":1,
            "type":"PORT",
            "default_port":2181,
            "port_uri":"{{zookeeper-env/clientPort}}"
          }
        ];
      App.alertDefinitionsMapper.map(data);

      testHelpers.nestedExpect(expected, App.alertDefinitionsMapper.get('model.content'));

    });

    describe('should parse AMS alertDefinitions', function () {

      var data = {items: [json.items[5]]};
      var expected = [
        {
          "id" : 6,
          "interval" : 1,
          "label" : "NameNode Heap Usage (Hourly)",
          "name" : "namenode_free_heap_size_deviation_percentage",
          "repeat_tolerance" : 1,
          "repeat_tolerance_enabled" : true,
          "scope" : "SERVICE",
          "service_name" : "HDFS",
          "component_name" : "NAMENODE",
          "help_url" : "http://test.test"
        }
      ];

      var expectedMetricsUri = [{
        "id":"6uri",
        "http" : "{{ams-site/timeline.metrics.service.webapp.address}}",
        "https" : "{{ams-site/timeline.metrics.service.webapp.address}}",
        "https_property" : "{{ams-site/timeline.metrics.service.http.policy}}",
        "https_property_value" : "HTTPS_ONLY",
        "connection_timeout" : 5.0
      }];

      var expectedAms = [{
        "id": "6ams",
        "value": "{1} - {0}",
        "minimal_value": 1,
        "interval": 60
      }];

      beforeEach(function () {
        App.alertDefinitionsMapper.map(data);
      });

      it('should map definition', function () {
        testHelpers.nestedExpect(expected, App.alertDefinitionsMapper.get('model.content'));
      });

      it('parse metrics uri', function() {
        testHelpers.nestedExpect(expectedMetricsUri, App.alertDefinitionsMapper.get('metricsUriModel.content'));
      });

      it('parse ams parameters', function () {
        testHelpers.nestedExpect(expectedAms, App.alertDefinitionsMapper.get('metricsAmsModel.content'));
      });

    });

    /*eslint-disable mocha-cleanup/complexity-it */
    it('should set groups from App.cache.previousAlertGroupsMap', function () {

      App.cache.previousAlertGroupsMap = {
        1: [5,1],
        2: [4,3],
        3: [3,2],
        4: [2,5],
        5: [1,4]
      };

      App.alertDefinitionsMapper.map(json);

      expect(App.alertDefinitionsMapper.get('model.content')[0].groups).to.eql([5, 1]);
      expect(App.alertDefinitionsMapper.get('model.content')[1].groups).to.eql([4, 3]);
      expect(App.alertDefinitionsMapper.get('model.content')[2].groups).to.eql([3, 2]);
      expect(App.alertDefinitionsMapper.get('model.content')[3].groups).to.eql([2, 5]);
      expect(App.alertDefinitionsMapper.get('model.content')[4].groups).to.eql([1, 4]);


    });
    /*eslint-enable mocha-cleanup/complexity-it */

    describe('should delete not existing definitions', function () {

      var definitions = [
        Em.Object.create({id: 100500, type: 'PORT'})
      ];

      beforeEach(function () {

        sinon.stub(App.AlertDefinition, 'find', function () {
          return definitions;
        });

      });

      afterEach(function() {
        App.AlertDefinition.find.restore();
      });

      it('should delete PORT alert definition with id 100500', function () {

        App.alertDefinitionsMapper.map(json);
        expect(App.alertDefinitionsMapper.deleteRecord.calledOnce).to.be.true;
        expect(App.alertDefinitionsMapper.deleteRecord.args[0][0].id).to.equal(100500);
      });

    });

  });
  /*eslint-enable mocha-cleanup/asserts-limit */

});