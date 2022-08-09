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
var testHelpers = require('test/helpers');

describe('App.serviceMetricsMapper', function () {

  describe('#computeAdditionalRelations', function () {

    var tests = [
      {
        message: 'if both namenodes are standby then `display_name_advanced` for both should be `Standby NameNode`',
        haStateForNn1: 'standby',
        haStateForNn2: 'standby',
        expectedNameForNn1: 'Standby NameNode',
        expectedNameForNn2: 'Standby NameNode'
      },
      {
        message: 'if one namenode is active and another is standby then they should be shown as  `Active NameNode` and `Standby NameNode` respectively',
        haStateForNn1: 'active',
        haStateForNn2: 'standby',
        expectedNameForNn1: 'Active NameNode',
        expectedNameForNn2: 'Standby NameNode'
      },
      {
        message: 'if one namenode is active and another is unknown then they should be shown as  `Active NameNode` and `Standby NameNode` respectively',
        haStateForNn1: 'active',
        haStateForNn2: undefined,
        expectedNameForNn1: 'Active NameNode',
        expectedNameForNn2: 'Standby NameNode'
      },
      {
        message: 'if both namenodes state are unknown then `display_name_advanced` for both should be null (NN will be shown with display name as `NameNode`)',
        haStateForNn1: undefined,
        haStateForNn2: undefined,
        expectedNameForNn1: null,
        expectedNameForNn2: null
      }
    ];

    var services = [
      {
        ServiceInfo: {
          service_name: "HDFS"
        },
        components: [
          {
            ServiceComponentInfo: {
              component_name: "NAMENODE",
              service_name: "HDFS"
            },
            host_components: [
              {
                HostRoles: {
                  component_name: "NAMENODE",
                  host_name: "h1"
                },
                metrics: {
                  dfs: {
                    FSNamesystem: {
                      HAState: ""
                    }
                  }
                }
              },
              {
                HostRoles: {
                  component_name: "NAMENODE",
                  host_name: "h2"
                },
                metrics: {
                  dfs: {
                    FSNamesystem: {
                      HAState: ""
                    }
                  }
                }
              }
            ]
          }
        ]
      }
    ];

    var hostComponents = [
      {
        component_name: "NAMENODE",
        host_id: "h1",
        service_id: "HDFS"
      },
      {
        component_name: "NAMENODE",
        host_id: "h2",
        service_id: "HDFS"
      }
    ];

    tests.forEach(function (test) {
      it(test.message, function () {
        services[0].components[0].host_components[0].metrics.dfs.FSNamesystem.HAState = test.haStateForNn1;
        services[0].components[0].host_components[1].metrics.dfs.FSNamesystem.HAState = test.haStateForNn2;
        App.serviceMetricsMapper.computeAdditionalRelations(hostComponents, services);
        expect(hostComponents[0].display_name_advanced).to.equal(test.expectedNameForNn1);
        expect(hostComponents[1].display_name_advanced).to.equal(test.expectedNameForNn2);
      });
    });
  });

  describe('#yarnMapper', function () {

    it('should set ACTIVE RM first in any cases (if RM HA enabled)', function() {
      var item = {
          components: [
            {
              ServiceComponentInfo: {
                component_name: 'RESOURCEMANAGER'
              },
              host_components: [
                {
                  HostRoles: {
                    ha_state: null,
                    host_name : 'h1'
                  }
                },
                {
                  HostRoles: {
                    ha_state: 'ACTIVE',
                    host_name : 'h2'
                  },
                  metrics: {
                    yarn: {
                      Queue: {
                        root: {
                          default: {}
                        }
                      }
                    }
                  }
                }
              ]
            }
          ]
        },
        result = App.serviceMetricsMapper.yarnMapper(item);
      expect(result.queue).to.equal("{\"root\":{\"default\":{}}}");
    });
  });

  describe("#isHostComponentPresent()", function () {
    var testCases = [
      {
        title: 'component is empty',
        data: {
          component: {},
          name: 'C1'
        },
        result: false
      },
      {
        title: 'component name does not match',
        data: {
          component: {
            ServiceComponentInfo: {
              component_name: ''
            }
          },
          name: 'C1'
        },
        result: false
      },
      {
        title: 'host_components is undefined',
        data: {
          component: {
            ServiceComponentInfo: {
              component_name: 'C1'
            }
          },
          name: 'C1'
        },
        result: false
      },
      {
        title: 'host_components is empty',
        data: {
          component: {
            ServiceComponentInfo: {
              component_name: 'C1'
            },
            host_components: []
          },
          name: 'C1'
        },
        result: false
      },
      {
        title: 'host_components has component',
        data: {
          component: {
            ServiceComponentInfo: {
              component_name: 'C1'
            },
            host_components: [{}]
          },
          name: 'C1'
        },
        result: true
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        expect(App.serviceMetricsMapper.isHostComponentPresent(test.data.component, test.data.name)).to.equal(test.result);
      });
    });
  });

  var hostNames = ['hostName1', 'hostName2'];
  var interactiveComponents = hostNames.map(function (hostName) {
    return {host_name: hostName}
  });
  describe('#getHostNameIpMap', function () {
    it ('should send ajax request with hostNames param and generate hostNameApiMap on success', function () {
      App.ajax.send.restore();
      sinon.stub(App.ajax, 'send', function () {
        return {success: function (callback) {
            callback({items: [{
              Hosts: {
                host_name: 'hostName1',
                ip: '192.168.1.1'
              }
              }, {
              Hosts: {
                host_name: 'hostName2',
                ip: '192.168.1.2'
              }
            }]});
          }
        };
      });
      App.serviceMetricsMapper.getHostNameIpMap(hostNames);
      var args = testHelpers.findAjaxRequest('name', 'hosts.ips');
      expect(args[0]).exists;
      expect(args[0].data).to.be.eql({
        hostNames: hostNames
      });
      expect(App.serviceMetricsMapper.get('hostNameIpMap')).to.be.eql({
        'hostName1': '192.168.1.1',
        'hostName2': '192.168.1.2'
      });
    });
  });

  describe('#loadInteractiveServerStatuses', function () {
    var configurationController = App.router.get('configurationController');
    beforeEach(function () {
      sinon.stub(configurationController, 'getCurrentConfigsBySites', function () {
        return {done: function (callback) {
          return callback([
            {type: 'hive-interactive-site', properties: {'hive.server2.webui.port': '1'}}
          ]);
        }};
      });
      sinon.stub(App.serviceMetricsMapper, 'getHostNameIpMap', function () {
        return {
          done: function () {
          }
        }
      });
    });

    afterEach(function () {
      configurationController.getCurrentConfigsBySites.restore();
      App.serviceMetricsMapper.getHostNameIpMap.restore();
    });

    it('should not call load ips method', function () {
      App.serviceMetricsMapper.loadInteractiveServerStatuses(interactiveComponents);
      expect(App.serviceMetricsMapper.getHostNameIpMap.calledOnce).to.be.false;
      expect(App.serviceMetricsMapper.getHostNameIpMap.calledWith(hostNames)).to.be.false;
    });

    it('should call load ips method', function () {
      App.serviceMetricsMapper.set('hostNameIpMap', {});
      App.serviceMetricsMapper.loadInteractiveServerStatuses(interactiveComponents);
      expect(App.serviceMetricsMapper.getHostNameIpMap.calledOnce).to.be.true;
      expect(App.serviceMetricsMapper.getHostNameIpMap.calledWith(interactiveComponents, '1')).to.be.false;
    });
  });

  describe('#getHiveServersInteractiveStatus', function () {
    beforeEach(function () {
      App.ajax.send.restore();
      sinon.stub(App.ajax, 'send', function () {
        return {success: function (callback) {
            callback(true);
          }
        };
      });

      sinon.stub(App.HostComponent, 'find', function () {
        return hostNames.map(function (hostName) {
          return Em.Object.create({hostName: hostName, componentName: 'HIVE_SERVER_INTERACTIVE'});
        });
      });
    });
    afterEach(function () {
      App.HostComponent.find.restore();
    });

    it('should do several ajax requests depends on count of components count', function () {
      App.serviceMetricsMapper.getHiveServersInteractiveStatus(interactiveComponents, '1');
      expect(App.ajax.send.calledTwice).to.be.true;
    });
  });
});
