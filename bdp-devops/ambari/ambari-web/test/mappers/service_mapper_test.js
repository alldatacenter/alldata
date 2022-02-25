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

require('utils/helper');
require('mappers/server_data_mapper');
require('mappers/service_metrics_mapper');
var dateUtils = require('utils/date/date');

describe('App.serviceMetricsMapper', function () {

  describe('#hbaseMapper', function() {

    var tests = [
        {
          components: [
            {
              ServiceComponentInfo: {
                component_name: "HBASE_MASTER",
                RegionsInTransition : [ ]
              },
              host_components: [
                {
                  metrics:{
                    hbase: {
                      master: {
                        IsActiveMaster: 'true',
                        AverageLoad: 1.23456789
                      }
                    },
                    master: {
                      AssignmentManager: {
                        ritCount: 0
                      }
                    }
                  },
                  HostRoles: {
                    host_name: 'host1'
                  }
                }
              ]
            }
          ],
          e: '1.23'
        },
        {
          components: [
            {
              ServiceComponentInfo: {
                component_name: "HBASE_MASTER",
                RegionsInTransition : [ ]
              },
              host_components: [
                {
                  metrics:{
                    hbase: {
                      master: {
                        IsActiveMaster: 'true',
                        AverageLoad: 1.00
                      }
                    },
                    master: {
                      AssignmentManager: {
                        ritCount: 0
                      }
                    }
                  },
                  HostRoles: {
                    host_name: 'host1'
                  }
                }
              ]
            }
          ],
          e: '1.00'
        },
        {
          components: [
            {
              ServiceComponentInfo: {
                component_name: "HBASE_MASTER",
                RegionsInTransition : [ ]
              },
              host_components: [
                {
                  metrics:{
                    hbase: {
                      master: {
                        IsActiveMaster: 'true',
                        AverageLoad: 1
                      }
                    },
                    master: {
                      AssignmentManager: {
                        ritCount: 0
                      }
                    }
                  },
                  HostRoles: {
                    host_name: 'host1'
                  }
                }
              ]
            }
          ],
          e: '1.00'
        },
        {
          components: [
            {
              ServiceComponentInfo: {
                component_name: "HBASE_MASTER",
                RegionsInTransition : [ ]
              },
              host_components: [
                {
                  metrics:{
                    hbase: {
                      master: {
                        IsActiveMaster: 'true',
                        AverageLoad: 1.2
                      }
                    },
                    master: {
                      AssignmentManager: {
                        ritCount: 0
                      }
                    }
                  },
                  HostRoles: {
                    host_name: 'host1'
                  }
                }
              ]
            }
          ],
          e: '1.20'
        }
    ];
    tests.forEach(function(test) {
      it('Round Average Load (' + test.e + ')', function () {
        var result = App.serviceMetricsMapper.hbaseMapper(test);
        expect(result.average_load).to.equal(test.e);
      });
    });
  });

  describe('#stormMapper', function() {
    var tests = [
      {
        stackVersionNumber: '2.2',
        message: 'Storm mapper, stack version 2.2',
        expectedValues: {
          total_executors: 28,
          nimbus_uptime: "15m 1s",
          free_slots: 0,
          used_slots: 2,
          total_slots: 2,
          total_tasks: 28,
          topologies: 1
        },
        components: [
          {
            "ServiceComponentInfo" : {
              "component_name" : "STORM_UI_SERVER",
              "service_name" : "STORM"
            },
            "metrics" : {
              "api" : {
                "v1": {
                  "cluster": {
                    "summary": {
                      "executorsTotal": 28.0,
                      "nimbusUptime": "15m 1s",
                      "slotsFree": 0.0,
                      "slotsTotal": 2.0,
                      "slotsUsed": 2.0,
                      "supervisors": 1.0,
                      "tasksTotal": 28.0
                    }
                  },
                  "topology": {
                    "summary": [
                      {
                        "executorsTotal": 21.0,
                        "uptime": "5m 59s",
                        "schedulerInfo": null,
                        "name": "WordCountida8c06640_date2901141",
                        "workersTotal": 2.0,
                        "status": "ACTIVE",
                        "owner": "",
                        "tasksTotal": 21.0,
                        "id": "WordCountida8c06640_date2901141-2-1412195707"
                      }
                    ]
                  }
                }
              }
            }
          }
        ]
      },
      {
        stackVersionNumber: '2.1',
        message: 'Storm mapper, stack version 2.1',
        expectedValues: {
          total_executors: 2,
          nimbus_uptime: 14250000,
          free_slots: 2,
          used_slots: 0,
          total_slots: 2,
          total_tasks: 21,
          topologies: 0
        },
        components: [
          {
            "ServiceComponentInfo" : {
              "component_name" : "STORM_REST_API",
              "service_name" : "STORM"
            },
            "metrics" : {
              "api" : {
                "cluster" : {
                  "summary" : {
                    "executors.total" : 2.0,
                    "nimbus.uptime" : 14250.0,
                    "slots.free" : 2.0,
                    "slots.total" : 2.0,
                    "slots.used" : 0.0,
                    "supervisors" : 1.0,
                    "tasks.total" : 21.0,
                    "topologies" : 0.0
                  }
                }
              }
            }
          }
        ]
      }
    ];

    beforeEach(function () {
      this.stub = sinon.stub(App, 'get');
      sinon.stub(dateUtils, 'timingFormat', function(arg) {return arg;});
    });

    afterEach(function () {
      App.get.restore();
      dateUtils.timingFormat.restore();
    });

    tests.forEach(function(test) {
      it(test.message, function() {
        this.stub.withArgs('currentStackVersionNumber').returns(test.stackVersionNumber);
        var result = App.serviceMetricsMapper.stormMapper(test);
        expect(result).to.include(test.expectedValues);
      });
    });

  });
});
