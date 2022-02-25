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
require('utils/ajax/ajax_queue');
require('controllers/main/service/info/configs');
require('controllers/wizard/step8_controller');
var installerStep8Controller;
var testHelpers = require('test/helpers');
var fileUtils = require('utils/file_utils');

var configs = Em.A([
  Em.Object.create({filename: 'hdfs-site.xml', name: 'p1', value: 'v1'}),
  Em.Object.create({filename: 'hdfs-site.xml', name: 'p2', value: 'v2'}),
  Em.Object.create({filename: 'hue-site.xml', name: 'p1', value: 'v1'}),
  Em.Object.create({filename: 'hue-site.xml', name: 'p2', value: 'v2'}),
  Em.Object.create({filename: 'mapred-site.xml', name: 'p1', value: 'v1'}),
  Em.Object.create({filename: 'mapred-site.xml', name: 'p2', value: 'v2'}),
  Em.Object.create({filename: 'yarn-site.xml', name: 'p1', value: 'v1'}),
  Em.Object.create({filename: 'yarn-site.xml', name: 'p2', value: 'v2'}),
  Em.Object.create({filename: 'capacity-scheduler.xml', name: 'p1', value: 'v1'}),
  Em.Object.create({filename: 'capacity-scheduler.xml', name: 'p2', value: 'v2'}),
  Em.Object.create({filename: 'mapred-queue-acls.xml', name: 'p1', value: 'v1'}),
  Em.Object.create({filename: 'mapred-queue-acls.xml', name: 'p2', value: 'v2'}),
  Em.Object.create({filename: 'hbase-site.xml', name: 'p1', value: 'v1'}),
  Em.Object.create({filename: 'hbase-site.xml', name: 'p2', value: 'v2'}),
  Em.Object.create({filename: 'oozie-site.xml', name: 'p1', value: 'v1'}),
  Em.Object.create({filename: 'oozie-site.xml', name: 'p2', value: 'v2'}),
  Em.Object.create({filename: 'hive-site.xml', name: 'p1', value: 'v1'}),
  Em.Object.create({filename: 'hive-site.xml', name: 'p2', value: 'v2'}),
  Em.Object.create({filename: 'pig-properties.xml', name: 'p1', value: 'v1'}),
  Em.Object.create({filename: 'webhcat-site.xml', name: 'p1', value: 'v1'}),
  Em.Object.create({filename: 'webhcat-site.xml', name: 'p2', value: 'v2'}),
  Em.Object.create({filename: 'tez-site.xml', name: 'p1', value: 'v1'}),
  Em.Object.create({filename: 'tez-site.xml', name: 'p2', value: 'v2'}),
  Em.Object.create({filename: 'falcon-startup.properties.xml', name: 'p1', value: 'v1'}),
  Em.Object.create({filename: 'falcon-startup.properties.xml', name: 'p2', value: 'v2'}),
  Em.Object.create({filename: 'falcon-runtime.properties.xml', name: 'p1', value: 'v1'}),
  Em.Object.create({filename: 'falcon-runtime.properties.xml', name: 'p2', value: 'v2'}),
  Em.Object.create({filename: 'cluster-env.xml', name: 'p1', value: 'v1'}),
]);

var services = Em.A([
        Em.Object.create({
          serviceName: 's1',
          isSelected: true,
          isInstalled: false,
          displayNameOnSelectServicePage: 's01',
          isClientOnlyService: false,
          serviceComponents: Em.A([
            Em.Object.create({
              isClient: true
            })
          ]),
          configTypes: {
            site1 : [],
            site2 : []
          },
          isHiddenOnSelectServicePage: false
        }),
        Em.Object.create({
          serviceName: 's2',
          isSelected: true,
          isInstalled: false,
          displayNameOnSelectServicePage: 's02',
          serviceComponents: Em.A([
            Em.Object.create({
              isMaster: true
            })
          ]),
          configTypes: {
            site3 : []
          },
          isHiddenOnSelectServicePage: false
        }),
        Em.Object.create({
          serviceName: 's3',
          isSelected: true,
          isInstalled: false,
          displayNameOnSelectServicePage: 's03',
          serviceComponents: Em.A([
            Em.Object.create({
              isHAComponentOnly: true
            })
          ]),
          configTypes: {},
          isHiddenOnSelectServicePage: false
        }),
        Em.Object.create({
          serviceName: 's4',
          isSelected: true,
          isInstalled: false,
          displayNameOnSelectServicePage: 's03',
          isClientOnlyService: true,
          serviceComponents: Em.A([
            Em.Object.create({
              isClient: true
            })
          ]),
          configTypes: {},
          isHiddenOnSelectServicePage: false
        })
]);

function getController() {
  return App.WizardStep8Controller.create({
    configs: configs,
    content: {controllerName: ''}
  });
}

describe('App.WizardStep8Controller', function () {

  beforeEach(function () {
    installerStep8Controller = getController();
  });

  App.TestAliases.testAsComputedFilterBy(getController(), 'installedServices', 'content.services', 'isInstalled', true);

  App.TestAliases.testAsComputedEqual(getController(), 'isManualKerberos', 'App.router.mainAdminKerberosController.kdc_type', 'none');

  App.TestAliases.testAsComputedAlias(getController(), 'clusterName', 'content.cluster.name', 'string');

  describe('#createSelectedServicesData', function () {

    var tests = Em.A([
      {selectedServices: Em.A(['MAPREDUCE2']), e: 2},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN']), e: 5},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE']), e: 7},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE']), e: 9},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE']), e: 12},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE']), e: 13},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE']), e: 14},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE', 'PIG']), e: 15},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE', 'PIG', 'FALCON']), e: 17},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE', 'PIG', 'FALCON', 'STORM']), e: 18},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE', 'PIG', 'FALCON', 'STORM', 'TEZ']), e: 19},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE', 'PIG', 'FALCON', 'STORM', 'TEZ', 'ZOOKEEPER']), e: 21}
    ]);

    tests.forEach(function (test) {
      it(test.selectedServices.join(','), function () {
        var mappedServices = test.selectedServices.map(function (serviceName) {
          return Em.Object.create({isSelected: true, isInstalled: false, serviceName: serviceName});
        });
        installerStep8Controller = App.WizardStep8Controller.create({
          content: {controllerName: 'addServiceController', services: mappedServices},
          configs: configs
        });
        var serviceData = installerStep8Controller.createSelectedServicesData();
        expect(serviceData.mapProperty('ServiceInfo.service_name')).to.eql(test.selectedServices.toArray());
        installerStep8Controller.clearStep();
      });
    });

  });

  describe('#getRegisteredHosts', function () {

    var tests = Em.A([
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'REGISTERED', name: 'h1'}),
          h2: Em.Object.create({bootStatus: 'OTHER', name: 'h2'})
        },
        e: ['h1'],
        m: 'Two hosts, one registered'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'OTHER', name: 'h1'}),
          h2: Em.Object.create({bootStatus: 'OTHER', name: 'h2'})
        },
        e: [],
        m: 'Two hosts, zero registered'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'REGISTERED', name: 'h1'}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2'})
        },
        e: ['h1', 'h2'],
        m: 'Two hosts, two registered'
      }
    ]);

    tests.forEach(function (test) {
      it(test.m, function () {
        installerStep8Controller.set('content', Em.Object.create({hosts: test.hosts}));
        var registeredHosts = installerStep8Controller.getRegisteredHosts();
        expect(registeredHosts.mapProperty('hostName').toArray()).to.eql(test.e);
      });
    });

  });

  describe('#createRegisterHostData', function () {

    var tests = Em.A([
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'REGISTERED', name: 'h1', isInstalled: false}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2', isInstalled: false})
        },
        e: ['h1', 'h2'],
        m: 'two registered, two isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'OTHER', name: 'h1', isInstalled: false}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2', isInstalled: false})
        },
        e: ['h2'],
        m: 'one registered, two isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'OTHER', name: 'h1', isInstalled: true}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2', isInstalled: false})
        },
        e: ['h2'],
        m: 'one registered, one isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'REGISTERED', name: 'h1', isInstalled: true}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2', isInstalled: false})
        },
        e: ['h2'],
        m: 'two registered, one isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'OTHER', name: 'h1', isInstalled: false}),
          h2: Em.Object.create({bootStatus: 'OTHER', name: 'h2', isInstalled: false})
        },
        e: [],
        m: 'zero registered, two isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'REGISTERED', name: 'h1', isInstalled: true}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2', isInstalled: true})
        },
        e: [],
        m: 'two registered, zeto insInstalled false'
      }
    ]);

    tests.forEach(function (test) {
      it(test.m, function () {
        installerStep8Controller.set('content', Em.Object.create({hosts: test.hosts}));
        var registeredHostData = installerStep8Controller.createRegisterHostData();
        expect(registeredHostData.mapProperty('Hosts.host_name').toArray()).to.eql(test.e);
      });
    });

  });

  describe('#loadServices', function () {

    beforeEach(function () {
      var selectedServices = services.filterProperty('isSelected');
      var slaveComponentHosts = Em.A([
        Em.Object.create({
          componentName: 'CLIENT',
          hostName: 'h1',
          hosts: Em.A([
            Em.Object.create({hostName: 'h1', isInstalled: true}),
            Em.Object.create({hostName: 'h2', isInstalled: false})
          ])
        })
      ]);
      var content = Em.Object.create({
        services: services,
        selectedServices: selectedServices,
        slaveComponentHosts: slaveComponentHosts,
        hosts: Em.A([
          Em.Object.create({hostName: 'h1', isInstalled: true}),
          Em.Object.create({hostName: 'h2', isInstalled: false})
        ]),
        masterComponentHosts: []
      });
      installerStep8Controller.set('content', content);
      installerStep8Controller.set('services', Em.A([]));
      installerStep8Controller.reopen({selectedServices: selectedServices});
      installerStep8Controller.loadServices();
    });

    it('should load services', function () {
      var expected = [
        {
          "service_name": "s1",
          "display_name": "s01",
          "service_components": []
        },
        {
          "service_name": "s2",
          "display_name": "s02",
          "service_components": []
        },
        {
          "service_name": "s3",
          "display_name": "s03",
          "service_components": []
        },
        {
          "service_name": "s4",
          "display_name": "s03",
          "service_components": [
            {
              "component_name": "CLIENT",
              "display_name": "Clients",
              "component_value": "2 hosts"
            }
          ]
        }
      ];
      var result = JSON.parse(JSON.stringify(installerStep8Controller.get('services')));
      expect(result).to.be.eql(expected);
    });
  });

  describe('#removeClientsFromList', function () {

    beforeEach(function () {
      installerStep8Controller.set('content', Em.Object.create({
        hosts: Em.Object.create({
          h1: Em.Object.create({
            hostName: 'h1',
            isInstalled: true,
            hostComponents: Em.A([Em.Object.create({HostRoles: Em.Object.create({component_name: "h1"})})])
          }),
          h2: Em.Object.create({
            hostName: 'h2',
            isInstalled: true,
            hostComponents: Em.A([Em.Object.create({HostRoles: Em.Object.create({component_name: "h2"})})])
          })
        })
      }));
    });

    it('should remove h1', function () {
      var hostList = Em.A(['h1','h2']);
      installerStep8Controller.removeClientsFromList('h1', hostList);
      expect(JSON.parse(JSON.stringify(hostList))).to.eql(["h2"]);
    });
  });

  describe('#createSlaveAndClientsHostComponents', function () {

    beforeEach(function () {
      installerStep8Controller.set('content', Em.Object.create({
        masterComponentHosts: Em.A([
          Em.Object.create({
            componentName: 'CLIENT',
            component: 'HBASE_MASTER',
            hostName: 'h1'
          })
        ]),
        slaveComponentHosts: Em.A([
          Em.Object.create({
            componentName: 'CLIENT',
            hostName: 'h1',
            hosts: Em.A([
              Em.Object.create({hostName: 'h1', isInstalled: true}),
              Em.Object.create({hostName: 'h2', isInstalled: false})
            ])
          }),
          Em.Object.create({
            componentName: 'CLIENT1',
            hostName: 'h1',
            hosts: Em.A([
              Em.Object.create({hostName: 'h1', isInstalled: true}),
              Em.Object.create({hostName: 'h2', isInstalled: false})
            ])

          })
        ]),
        clients: Em.A([
          Em.Object.create({
            isInstalled: false
          })
        ]),
        services: Em.A([
          Em.Object.create({
            isInstalled: true,
            serviceName: "name",
            isClient: true
          })
        ]),
        hosts: Em.Object.create({
          h1: Em.Object.create({
            hostName: 'h1',
            isInstalled: true,
            hostComponents: Em.A([Em.Object.create({})])
          }),
          h2: Em.Object.create({
            hostName: 'h2',
            isInstalled: false,
            hostComponents: Em.A([Em.Object.create({})])
          })
        }),
        additionalClients: Em.A([{hostNames: "name", componentName: "client"}])
      }));
      installerStep8Controller.set('ajaxRequestsQueue', App.ajaxQueue.create());
      installerStep8Controller.get('ajaxRequestsQueue').clear();
    });

    it('should return non install object', function () {
      installerStep8Controller.createSlaveAndClientsHostComponents();
      expect(installerStep8Controller.get('content.clients')[0].isInstalled).to.be.false;
    });
  });

  describe('#createAdditionalClientComponents', function () {

    beforeEach(function () {
      installerStep8Controller.set('content', Em.Object.create({
        masterComponentHosts: Em.A([
          Em.Object.create({
            componentName: 'CLIENT',
            component: 'HBASE_MASTER',
            hostName: 'h1'
          })
        ]),
        slaveComponentHosts: Em.A([
          Em.Object.create({
            componentName: 'CLIENT',
            hostName: 'h1',
            hosts: Em.A([
              Em.Object.create({hostName: 'h1', isInstalled: true}),
              Em.Object.create({hostName: 'h2', isInstalled: false})
            ])
          })
        ]),
        clients: Em.A([
          Em.Object.create({
            isInstalled: false
          })
        ]),
        services: Em.A([
          Em.Object.create({
            isInstalled: true,
            serviceName: "name",
            isClient: true
          })
        ]),
        hosts: Em.Object.create({
          h1: Em.Object.create({
            hostName: 'h1',
            isInstalled: true,
            hostComponents: Em.A([Em.Object.create({})])
          }),
          h2: Em.Object.create({
            hostName: 'h2',
            isInstalled: false,
            hostComponents: Em.A([Em.Object.create({})])
          })
        }),
        additionalClients: Em.A([{hostNames: "name", componentName: "client"}])
      }));
      installerStep8Controller.set('ajaxRequestsQueue', App.ajaxQueue.create());
      installerStep8Controller.get('ajaxRequestsQueue').clear();
      installerStep8Controller.createAdditionalClientComponents();
    });

    it('should bes equal to content.cluster.name', function () {

      var result = [
        {
          "hostNames": "name",
          "componentName": "client"
        }
      ];
      var expected = installerStep8Controller.get('content.additionalClients');
      expect(JSON.parse(JSON.stringify(expected))).to.eql(result);
    });
  });

  describe('#assignComponentHosts', function () {
    it('should return host name', function () {
      var component = Em.Object.create({
        isMaster: true,
        componentName: 'HBASE_MASTER',
        hostName: 'h1'
      });
      installerStep8Controller.set('content', Em.Object.create({
        masterComponentHosts:Em.A([
          Em.Object.create({component: 'HBASE_MASTER', hostName: 'h1'})
      ])}));
      var res = installerStep8Controller.assignComponentHosts(component);
      expect(res).to.equal("h1");
    });
    it('should return number of hosts', function () {
      var component = Em.Object.create({
        componentName: 'HBASE_MASTER',
        isClient: false,
        hostName: 'h1'
      });
      installerStep8Controller.set('content', Em.Object.create({
        slaveComponentHosts:Em.A([
          Em.Object.create({
            componentName: 'HBASE_MASTER',
            hostName: 'h1',
            hosts: [
              {hostName: 'h1'},
              {hostName: 'h2'}
            ]
          })
      ])}));
      var res = installerStep8Controller.assignComponentHosts(component);
      expect(res).to.equal("2 hosts");
    });
  });

  describe('#loadClusterInfo', function () {
    beforeEach(function () {
      sinon.stub(App.Stack, 'find', function(){
        return Em.A([
          Em.Object.create({isSelected: false, hostName: 'h1'}),
          Em.Object.create({
            isSelected: true,
            hostName: 'h2',
            operatingSystems: Em.A([Em.Object.create({
              name:'windows',
              isSelected: true,
              repositories: Em.A([Em.Object.create({
                baseUrl: "url",
                osType: "2",
                repoId: "3"
              })])
            })])
          }),
          Em.Object.create({isSelected: false, hostName: 'h3'})
        ]);
      });
    });
    afterEach(function () {
      App.Stack.find.restore();
    });
    it('should return config with display_name', function () {
      installerStep8Controller.set('clusterInfo', Em.A([]));
      installerStep8Controller.loadClusterInfo();
      var res = [{
        "config_name":"cluster",
        "display_name":"Cluster Name"
      },{
        "config_name":"hosts",
        "display_name":"Total Hosts",
        "config_value":"0 (0 new)"
      }];
      var calcRes = JSON.parse(JSON.stringify(installerStep8Controller.get('clusterInfo')));
      expect(calcRes).to.eql(res);
    });
  });

  describe('#loadStep', function () {
    beforeEach(function () {
      sinon.stub(installerStep8Controller, 'clearStep', Em.K);
      sinon.stub(installerStep8Controller, 'formatProperties', Em.K);
      sinon.stub(installerStep8Controller, 'loadConfigs', Em.K);
      sinon.stub(installerStep8Controller, 'loadClusterInfo', Em.K);
      sinon.stub(installerStep8Controller, 'loadServices', Em.K);
      installerStep8Controller.set('content', {controllerName: 'installerController'});
    });
    afterEach(function () {
      installerStep8Controller.clearStep.restore();
      installerStep8Controller.formatProperties.restore();
      installerStep8Controller.loadConfigs.restore();
      installerStep8Controller.loadClusterInfo.restore();
      installerStep8Controller.loadServices.restore();
    });
    it('should call clearStep', function () {
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.clearStep.calledOnce).to.equal(true);
    });
    it('should call loadClusterInfo', function () {
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.loadClusterInfo.calledOnce).to.equal(true);
    });
    it('should call loadServices', function () {
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.loadServices.calledOnce).to.equal(true);
    });
    it('should call formatProperties if content.serviceConfigProperties is true', function () {
      installerStep8Controller.set('content.serviceConfigProperties', true);
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.loadServices.calledOnce).to.equal(true);
    });
    it('should call loadConfigs if content.serviceConfigProperties is true', function () {
      installerStep8Controller.set('content.serviceConfigProperties', true);
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.loadConfigs.calledOnce).to.equal(true);
    });
    it('should set isSubmitDisabled to false', function () {
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.get('isSubmitDisabled')).to.equal(false);
    });
    it('should set isBackBtnDisabled to false', function () {
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.get('isBackBtnDisabled')).to.equal(false);
    });
  });

  describe('#getRegisteredHosts', function() {
    Em.A([
        {
          hosts: {},
          m: 'no content.hosts',
          e: []
        },
        {
          hosts: {
            h1:{bootStatus: ''},
            h2:{bootStatus: ''}
          },
          m: 'no registered hosts',
          e: []
        },
        {
          hosts: {
            h1:{bootStatus: 'REGISTERED', hostName: '', name: 'n1'},
            h2:{bootStatus: 'REGISTERED', hostName: '', name: 'n2'}
          },
          m: 'registered hosts available',
          e: ['n1', 'n2']
        }
      ]).forEach(function(test) {
        it(test.m, function() {
          installerStep8Controller.set('content', {hosts: test.hosts});
          var hosts = installerStep8Controller.getRegisteredHosts();
          expect(hosts.mapProperty('hostName')).to.eql(test.e);
        });
      });
  });

  describe('#loadRepoInfo', function() {

    beforeEach(function () {
      var stubForGet = sinon.stub(App, 'get').withArgs('currentStackName').returns('HDP');
      this.mockRepo = sinon.stub(App.RepositoryVersion, 'find');
    });

    afterEach(function () {
      App.get.restore();
      App.RepositoryVersion.find.restore();
    });
    it('should return repo', function() {
      this.mockRepo.returns([Em.Object.create({stackVersionType: 'HDP', isCurrent: true, isStandard: true, repositoryVersion: '2.3.0.0-2208'})])
      installerStep8Controller.loadRepoInfo();
      var args = testHelpers.findAjaxRequest('name', 'cluster.load_repo_version');
      expect(args[0].data).to.eql({stackName: 'HDP', repositoryVersion: '2.3.0.0-2208'});
    });
  });

  describe('#loadRepoInfoSuccessCallback', function () {
    beforeEach(function () {
      installerStep8Controller.set('clusterInfo', Em.Object.create({}));
    });

    it('should assert error if no data returned from server', function () {
      expect(function () {
        installerStep8Controller.loadRepoInfoSuccessCallback({items: []});
      }).to.throw(Error);
    });

    Em.A([
      {
        m: 'Normal JSON',
        e: {
          base_url: ['baseurl1', 'baseurl2'],
          os_type: ['redhat6', 'suse11'],
          repo_id: ['HDP-2.3', 'HDP-UTILS-1.1.0.20']
        },
        items: [
          {
            repository_versions: [
              {
                operating_systems: [
                  {
                    OperatingSystems: {
                      ambari_managed_repositories: true
                    },
                    repositories: [
                      {
                        Repositories: {
                          base_url: 'baseurl1',
                          os_type: 'redhat6',
                          repo_id: 'HDP-2.3'
                        }
                      }
                    ]
                  },
                  {
                    OperatingSystems: {
                      ambari_managed_repositories: true
                    },
                    repositories: [
                      {
                        Repositories: {
                          base_url: 'baseurl2',
                          os_type: 'suse11',
                          repo_id: 'HDP-UTILS-1.1.0.20'
                        }
                      }
                    ]
                  }
                ]
              }
            ]
          }
        ]
      }
    ]).forEach(function (test) {

      it(test.m, function () {
        installerStep8Controller.loadRepoInfoSuccessCallback({items: test.items});
        expect(installerStep8Controller.get('clusterInfo.repoInfo').mapProperty('base_url')).to.eql(test.e.base_url);
        expect(installerStep8Controller.get('clusterInfo.repoInfo').mapProperty('os_type')).to.eql(test.e.os_type);
        expect(installerStep8Controller.get('clusterInfo.repoInfo').mapProperty('repo_id')).to.eql(test.e.repo_id);
      });

    });

    /*Em.A([
        {
          items: [
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'redhat5',
                    base_url: 'url1'
                  }
                }
              ],
              OperatingSystems: {
                is_type: ''
              }
            }
          ],
          m: 'only redhat5',
          e: {
            base_url: ['url1'],
            os_type: ['redhat5']
          }
        },
        {
          items: [
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'redhat5',
                    base_url: 'url1'
                  }
                }
              ],
              OperatingSystems: {
                is_type: ''
              }
            },
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'redhat6',
                    base_url: 'url2'
                  }
                }
              ],
              OperatingSystems: {
                is_type: ''
              }
            }
          ],
          m: 'redhat5, redhat6',
          e: {
            base_url: ['url1', 'url2'],
            os_type: ['redhat5', 'redhat6']
          }
        },
        {
          items: [
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'redhat5',
                    base_url: 'url1'
                  }
                }
              ],
              OperatingSystems: {
                is_type: ''
              }
            },
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'redhat6',
                    base_url: 'url2'
                  }
                }
              ],
              OperatingSystems: {
                is_type: ''
              }
            },
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'sles11',
                    base_url: 'url3'
                  }
                }
              ],
              OperatingSystems: {
                is_type: ''
              }
            }
          ],
          m: 'redhat5, redhat6, sles11',
          e: {
            base_url: ['url1', 'url2', 'url3'],
            os_type: ['redhat5', 'redhat6', 'sles11']
          }
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          installerStep8Controller.loadRepoInfoSuccessCallback({items: test.items});
          expect(installerStep8Controller.get('clusterInfo.repoInfo').mapProperty('base_url')).to.eql(test.e.base_url);
          expect(installerStep8Controller.get('clusterInfo.repoInfo').mapProperty('os_type')).to.eql(test.e.os_type);
        });
      });*/
  });

  describe('#loadRepoInfoErrorCallback', function() {
    it('should set [] to repoInfo', function() {
      installerStep8Controller.set('clusterInfo', Em.Object.create({repoInfo: [{}, {}]}));
      installerStep8Controller.loadRepoInfoErrorCallback({});
      expect(installerStep8Controller.get('clusterInfo.repoInfo.length')).to.be.equal(0);
    });
  });

  describe('#loadHbaseMasterValue', function () {
    Em.A([
        {
          masterComponentHosts: [{component: 'HBASE_MASTER', hostName: 'h1'}],
          component: Em.Object.create({component_name: 'HBASE_MASTER'}),
          m: 'one host',
          e: 'h1'
        },
        {
          masterComponentHosts: [{component: 'HBASE_MASTER', hostName: 'h1'}, {component: 'HBASE_MASTER', hostName: 'h2'}, {component: 'HBASE_MASTER', hostName: 'h3'}],
          component: Em.Object.create({component_name: 'HBASE_MASTER'}),
          m: 'many hosts',
          e: 'h1 ' + Em.I18n.t('installer.step8.other').format(2)
        }
      ]).forEach(function (test) {
        it(test.m, function() {
          installerStep8Controller.set('content', {masterComponentHosts: test.masterComponentHosts});
          installerStep8Controller.loadHbaseMasterValue(test.component);
          expect(test.component.component_value).to.equal(test.e);
        });
      });
  });

  describe('#loadZkServerValue', function() {
    Em.A([
        {
          masterComponentHosts: [{component: 'ZOOKEEPER_SERVER'}],
          component: Em.Object.create({component_name: 'ZOOKEEPER_SERVER'}),
          m: '1 host',
          e: '1 host'
        },
        {
          masterComponentHosts: [{component: 'ZOOKEEPER_SERVER'},{component: 'ZOOKEEPER_SERVER'},{component: 'ZOOKEEPER_SERVER'}],
          component: Em.Object.create({component_name: 'ZOOKEEPER_SERVER'}),
          m: 'many hosts',
          e: '3 hosts'
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          installerStep8Controller.set('content', {masterComponentHosts: test.masterComponentHosts});
          installerStep8Controller.loadZkServerValue(test.component);
          expect(test.component.component_value).to.equal(test.e);
        });
      });
  });

  describe('#loadDbValue', function() {

    beforeEach(function() {
      installerStep8Controller.set('wizardController', Em.Object.create({
        getDBProperty: Em.K
      }));
    });

    afterEach(function() {
    });

    var tests = [
    {
       it: "Hive test for Existing Oracle Database",
       serviceConfigProperties: [
         {name: 'hive_database', value: 'Existing Oracle Database'}
       ],
       serviceName: 'HIVE',
       result: 'Existing Oracle Database'
     },
     {
       it: "Oozie test for New Derby Database",
       serviceConfigProperties: [
         {name: 'oozie_database', value: 'New Derby Database'}
       ],
       serviceName: 'OOZIE',
       result: 'New Derby Database'
     }
    ];

    tests.forEach(function(test) {
      it(test.it, function() {
        installerStep8Controller.set('content.serviceConfigProperties', test.serviceConfigProperties);
        var dbComponent = installerStep8Controller.loadDbValue(test.serviceName);
        expect(dbComponent).to.equal(test.result);
      });
    });
  });

  describe('#submit', function() {
    beforeEach(function() {
      sinon.stub(installerStep8Controller, 'submitProceed', Em.K);
      sinon.stub(installerStep8Controller, 'showRestartWarnings').returns($.Deferred().resolve().promise());
      sinon.stub(App.get('router.mainAdminKerberosController'), 'getKDCSessionState', Em.K);
    });
    afterEach(function() {
      installerStep8Controller.submitProceed.restore();
      installerStep8Controller.showRestartWarnings.restore();
      App.set('isKerberosEnabled', false);
      App.get('router.mainAdminKerberosController').getKDCSessionState.restore();
    });
    it('AddServiceController Kerberos enabled', function () {
      installerStep8Controller.reopen({
        isSubmitDisabled: false,
        content: {controllerName: 'addServiceController'}
      });
      installerStep8Controller.submit();
      expect(App.get('router.mainAdminKerberosController').getKDCSessionState.called).to.equal(true);
    });
    it('shouldn\'t do nothing if isSubmitDisabled is true', function() {
      installerStep8Controller.reopen({isSubmitDisabled: true});
      installerStep8Controller.submit();
      expect(App.get('router.mainAdminKerberosController').getKDCSessionState.called).to.equal(false);
      expect(installerStep8Controller.submitProceed.called).to.equal(false);
    });
  });

  describe('#getExistingClusterNamesSuccessCallBack', function() {
    it('should set clusterNames received from server', function() {
      var data = {
          items:[
            {Clusters: {cluster_name: 'c1'}},
            {Clusters: {cluster_name: 'c2'}},
            {Clusters: {cluster_name: 'c3'}}
          ]
        },
        clasterNames = ['c1','c2','c3'];
      installerStep8Controller.getExistingClusterNamesSuccessCallBack(data);
      expect(installerStep8Controller.get('clusterNames')).to.eql(clasterNames);
    });
  });

  describe('#getExistingClusterNamesErrorCallback', function() {
    it('should set [] to clusterNames', function() {
      installerStep8Controller.set('clusterNames', ['c1', 'c2']);
      installerStep8Controller.getExistingClusterNamesErrorCallback();
      expect(installerStep8Controller.get('clusterNames')).to.eql([]);
    });
  });

  describe('#deleteClusters', function() {

    describe('should call App.ajax.send for each provided clusterName', function() {
      var clusterNames = ['h1', 'h2', 'h3'];
      var args;
      beforeEach(function () {
        installerStep8Controller.deleteClusters(clusterNames);
        args = testHelpers.filterAjaxRequests('name', 'common.delete.cluster');
      });

      it('args', function () {
        expect(args).to.have.property('length').equal(clusterNames.length);
      });

      clusterNames.forEach(function(n, i) {
        it(n, function () {
          expect(args[i][0].data).to.eql({name: n, isLast: i === clusterNames.length - 1});
        });
      });
    });

    it('should clear cluster delete error popup body views', function () {
      installerStep8Controller.deleteClusters([]);
      expect(installerStep8Controller.get('clusterDeleteErrorViews')).to.eql([]);
    });

  });

  describe('#ajaxQueueFinished', function() {

    beforeEach(function () {
      sinon.stub(App.router, 'send', Em.K);
    });

    afterEach(function () {
      App.router.send.restore();
    });

    it('should call App.router.next', function() {
      installerStep8Controller.ajaxQueueFinished();
      expect(App.router.send.calledWith('next')).to.equal(true);
    });
  });

  describe('#addRequestToAjaxQueue', function() {

    describe('testMode = false', function() {
      it('should add request', function() {
        var clusterName = 'c1';
        installerStep8Controller.reopen({clusterName: clusterName});
        installerStep8Controller.set('ajaxRequestsQueue', App.ajaxQueue.create());
        installerStep8Controller.get('ajaxRequestsQueue').clear();
        installerStep8Controller.addRequestToAjaxQueue({name:'name', data:{}});
        var request = installerStep8Controller.get('ajaxRequestsQueue.queue.firstObject');
        expect(request.error).to.equal('ajaxQueueRequestErrorCallback');
        expect(request.data.cluster).to.equal(clusterName);
      });
    });
  });

  describe('#ajaxQueueRequestErrorCallback', function() {
    var obj = Em.Object.create({
      registerErrPopup: Em.K,
      setStepsEnable: Em.K
    });
    beforeEach(function() {
      sinon.stub(App.router, 'get', function() {
        return obj;
      });
      sinon.spy(obj, 'registerErrPopup');
      sinon.spy(obj, 'setStepsEnable');
    });
    afterEach(function() {
      App.router.get.restore();
      obj.registerErrPopup.restore();
      obj.setStepsEnable.restore();
    });
    it('should set hasErrorOccurred true', function () {
      installerStep8Controller.set('hasErrorOccurred', false);
      installerStep8Controller.ajaxQueueRequestErrorCallback({responseText: '{"message": ""}'});
      expect(installerStep8Controller.get('hasErrorOccurred')).to.equal(true);
    });
    it('should set isSubmitDisabled false', function () {
      installerStep8Controller.set('isSubmitDisabled', true);
      installerStep8Controller.ajaxQueueRequestErrorCallback({responseText: '{"message": ""}'});
      expect(installerStep8Controller.get('isSubmitDisabled')).to.equal(false);
    });
    it('should set isBackBtnDisabled false', function () {
      installerStep8Controller.set('isBackBtnDisabled', true);
      installerStep8Controller.ajaxQueueRequestErrorCallback({responseText: '{"message": ""}'});
      expect(installerStep8Controller.get('isBackBtnDisabled')).to.equal(false);
    });
    it('should call setStepsEnable', function () {
      installerStep8Controller.ajaxQueueRequestErrorCallback({responseText: '{"message": ""}'});
      expect(obj.setStepsEnable.calledOnce).to.equal(true);
    });
    it('should call registerErrPopup', function () {
      installerStep8Controller.ajaxQueueRequestErrorCallback({responseText: '{"message": ""}'});
      expect(obj.registerErrPopup.calledOnce).to.equal(true);
    });
  });

  describe('#removeInstalledServicesConfigurationGroups', function() {
    beforeEach(function() {
      sinon.stub(installerStep8Controller, 'deleteConfigurationGroup', Em.K);
    });
    afterEach(function() {
      installerStep8Controller.deleteConfigurationGroup.restore();
    });
    it('should call App.config.deleteConfigGroup for each received group', function() {
      var groups = [{}, {}, {}];
      installerStep8Controller.removeInstalledServicesConfigurationGroups(groups);
      expect(installerStep8Controller.deleteConfigurationGroup.callCount).to.equal(groups.length);
    });
  });

  describe('#getExistingClusterNames', function() {

    it('should do ajax request', function() {
      installerStep8Controller.getExistingClusterNames();
      var args = testHelpers.findAjaxRequest('name', 'wizard.step8.existing_cluster_names');
      expect(args).exists;
    });
  });

  describe('Queued requests', function() {

    beforeEach(function() {
      installerStep8Controller.clearStep();
      sinon.spy(installerStep8Controller, 'addRequestToAjaxQueue');
    });

    afterEach(function() {
      installerStep8Controller.addRequestToAjaxQueue.restore();
    });

    describe('#createCluster', function() {
      before(function () {
        sinon.stub(App.Stack, 'find').returns([
          Em.Object.create({
            id: "HDP-2.3-2.3.4.0-1234",
            isSelected: false,
            repositoryVersion: "2.3.4.0-1234"
          }),
          Em.Object.create({
            id: "HDP-2.3-2.3.4.1-1234",
            isSelected: false,
            repositoryVersion: "2.3.4.1-1234"
          }),
          Em.Object.create({
            id: "HDP-2.3-2.3.4.4-1234",
            isSelected: true,
            repositoryVersion: "2.3.4.4-1234"
          })
        ]);
      });
      after(function () {
        App.Stack.find.restore();
      });

      it('App.currentStackVersion should be changed if localRepo selected', function() {
        App.set('currentStackVersion', 'HDP-2.3');
        installerStep8Controller.reopen({content: {controllerName: 'installerController', installOptions: {localRepo: true}}});
        var data = {
          data: JSON.stringify({ "Clusters": {"version": 'HDPLocal-2.3'}})
        };
        installerStep8Controller.createCluster();
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data).to.equal(data.data);
      });

      it('App.currentStackVersion shouldn\'t be changed if localRepo ins\'t selected', function() {
        App.set('currentStackVersion', 'HDP-2.3');
        installerStep8Controller.reopen({content: {controllerName: 'installerController', installOptions: {localRepo: false}}});
        var data = {
          data: JSON.stringify({ "Clusters": {"version": 'HDP-2.3'}})
        };
        installerStep8Controller.createCluster();
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data).to.eql(data.data);
      });
    });

    describe('#createSelectedServices', function() {

      var data;

      beforeEach(function () {
        sinon.stub(installerStep8Controller, 'createSelectedServicesData', function () {
          return data;
        });
      });

      afterEach(function () {
        installerStep8Controller.createSelectedServicesData.restore();
      });

      it('shouldn\'t do nothing if no data', function() {
        data = [];
        installerStep8Controller.createSelectedServices();
        expect(installerStep8Controller.addRequestToAjaxQueue.called).to.equal(false);
      });

      it('should call addRequestToAjaxQueue with computed data', function() {
        data = [
          {"ServiceInfo": { "service_name": 's1' }},
          {"ServiceInfo": { "service_name": 's2' }},
          {"ServiceInfo": { "service_name": 's3' }}
        ];
        installerStep8Controller.createSelectedServices();
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data).to.equal(JSON.stringify(data));
      });

    });

    describe('#registerHostsToCluster', function() {
      var data;
      beforeEach(function () {
        sinon.stub(installerStep8Controller, 'createRegisterHostData', function () {
          return data;
        });
      });

      afterEach(function () {
        installerStep8Controller.createRegisterHostData.restore();
      });

      it('shouldn\'t do nothing if no data', function() {
        data = [];
        installerStep8Controller.registerHostsToCluster();
        expect(installerStep8Controller.addRequestToAjaxQueue.called).to.equal(false);
      });
      it('should call addRequestToAjaxQueue with computed data', function() {
        data = [
          {"Hosts": { "host_name": 'h1'}},
          {"Hosts": { "host_name": 'h3'}}
        ];
        installerStep8Controller.registerHostsToCluster();
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data).to.equal(JSON.stringify(data));
      });
    });

    describe('#registerHostsToComponent', function() {

      it('shouldn\'t do request if no hosts provided', function() {
        installerStep8Controller.registerHostsToComponent([]);
        expect(installerStep8Controller.addRequestToAjaxQueue.called).to.equal(false);
      });

      it('should do request if hostNames are provided', function() {
        var hostNames = ['h1', 'h2'],
          componentName = 'c1';
        installerStep8Controller.registerHostsToComponent(hostNames, componentName);
        var data = JSON.parse(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data);
        expect(data.RequestInfo.query).to.equal('Hosts/host_name=h1|Hosts/host_name=h2');
        expect(data.Body.host_components[0].HostRoles.component_name).to.equal('c1');
      });

    });

    describe('#applyConfigurationsToCluster', function() {
      it('should call addRequestToAjaxQueue', function() {
        var serviceConfigTags = [
            {
              type: 'hdfs',
              tag: 'tag1',
              properties: {
                'prop1': 'value1'
              }
            }
          ],
          data = '['+JSON.stringify({
            Clusters: {
              desired_config: [serviceConfigTags[0]]
            }
          })+']';
        installerStep8Controller.reopen({
          installedServices: [
              Em.Object.create({
                isSelected: true,
                isInstalled: false,
                configTypesRendered: {hdfs:'tag1'}
              })
            ], selectedServices: []
        });
        installerStep8Controller.applyConfigurationsToCluster(serviceConfigTags);
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data).to.equal(data);
      });
    });

    describe('#newServiceComponentErrorCallback', function() {

      it('should add request for new component', function() {
        var serviceName = 's1',
          componentName = 'c1';
        installerStep8Controller.newServiceComponentErrorCallback({}, {}, '', {}, {serviceName: serviceName, componentName: componentName});
        var data = JSON.parse(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data);
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.serviceName).to.equal(serviceName);
        expect(data.components[0].ServiceComponentInfo.component_name).to.equal(componentName);
      });

    });

    describe('#createAdditionalHostComponents', function() {

      beforeEach(function() {
        sinon.stub(installerStep8Controller, 'registerHostsToComponent', Em.K);
      });

      afterEach(function() {
        installerStep8Controller.registerHostsToComponent.restore();
      });

      describe('should add components with isRequiredOnAllHosts == true (1)', function() {

        beforeEach(function () {
          installerStep8Controller.reopen({
            getRegisteredHosts: function() {
              return [{hostName: 'h1'}, {hostName: 'h2'}];
            },
            content: {
              services: [
                Em.Object.create({
                  serviceName: 'GANGLIA', isSelected: true, isInstalled: false, serviceComponents: [
                    Em.Object.create({
                      componentName: 'GANGLIA_MONITOR',
                      isRequiredOnAllHosts: true
                    }),
                    Em.Object.create({
                      componentName: 'GANGLIA_SERVER',
                      isRequiredOnAllHosts: false
                    })
                  ]
                })
              ]
            }
          });
          installerStep8Controller.createAdditionalHostComponents();
        });

        it('registerHostsToComponent is called once', function () {
          expect(installerStep8Controller.registerHostsToComponent.calledOnce).to.equal(true);
        });
        it('hosts are ["h1", "h2"]', function () {
          expect(installerStep8Controller.registerHostsToComponent.args[0][0]).to.eql(['h1', 'h2']);
        });
        it('component is GANGLIA_MONITOR', function () {
          expect(installerStep8Controller.registerHostsToComponent.args[0][1]).to.equal('GANGLIA_MONITOR');
        });

      });

      describe('should add components with isRequiredOnAllHosts == true (2)', function() {

        beforeEach(function () {
          installerStep8Controller.reopen({
            getRegisteredHosts: function() {
              return [{hostName: 'h1', isInstalled: true}, {hostName: 'h2', isInstalled: false}];
            },
            content: {
              services: [
                Em.Object.create({
                  serviceName: 'GANGLIA', isSelected: true, isInstalled: true, serviceComponents: [
                    Em.Object.create({
                      componentName: 'GANGLIA_MONITOR',
                      isRequiredOnAllHosts: true
                    }),
                    Em.Object.create({
                      componentName: 'GANGLIA_SERVER',
                      isRequiredOnAllHosts: false
                    })
                  ]
                })
              ]
            }
          });
          installerStep8Controller.createAdditionalHostComponents();
        });

        it('registerHostsToComponent is called once', function () {
          expect(installerStep8Controller.registerHostsToComponent.calledOnce).to.equal(true);
        });
        it('hosts are ["h2"]', function () {
          expect(installerStep8Controller.registerHostsToComponent.args[0][0]).to.eql(['h2']);
        });
        it('component is GANGLIA_MONITOR', function () {
          expect(installerStep8Controller.registerHostsToComponent.args[0][1]).to.equal('GANGLIA_MONITOR');
        });

      });

      var newDatabases = [
        {
          name: 'New MySQL Database',
          component: 'MYSQL_SERVER',
          expectedHosts: ['h2']
        },
        {
          name: 'New PostgreSQL Database',
          component: 'POSTGRESQL_SERVER',
          expectedHosts: ['h2']
        }
      ];

      newDatabases.forEach(function (db) {
        describe('should add {0}'.format(db.component), function() {

          beforeEach(function () {
            installerStep8Controller.reopen({
              getRegisteredHosts: function() {
                return [{hostName: 'h1'}, {hostName: 'h2'}];
              },
              content: {
                masterComponentHosts: [
                  {component: 'HIVE_METASTORE', hostName: 'h1'},
                  {component: 'HIVE_SERVER', hostName: 'h2'}
                ],
                services: [
                  Em.Object.create({serviceName: 'HIVE', isSelected: true, isInstalled: false, serviceComponents: []})
                ],
                serviceConfigProperties: [
                  {name: 'hive_database', value: db.name}
                ]
              }
            });
            installerStep8Controller.createAdditionalHostComponents();
          });

          it('registerHostsToComponent is called once', function () {
            expect(installerStep8Controller.registerHostsToComponent.calledOnce).to.equal(true);
          });
          it('hosts are ' + db.expectedHosts, function () {
            expect(installerStep8Controller.registerHostsToComponent.args[0][0]).to.eql(db.expectedHosts);
          });
          it('component is ' + db.component, function () {
            expect(installerStep8Controller.registerHostsToComponent.args[0][1]).to.equal(db.component);
          });
        });

      });

    });

  describe('#createAdditionalHostComponentsOnAllHosts', function () {

      beforeEach(function() {
        sinon.stub(installerStep8Controller, 'registerHostsToComponent', Em.K);
      });

      afterEach(function() {
        installerStep8Controller.registerHostsToComponent.restore();
      });

      describe('should add components with isRequiredOnAllHosts == true (1)', function() {

        beforeEach(function () {
          installerStep8Controller.reopen({
            getRegisteredHosts: function() {
              return [{hostName: 'h1'}, {hostName: 'h2'}];
            },
            content: {
              services: Em.A([
                Em.Object.create({
                  serviceName: 'ANYSERVICE', isSelected: true, isInstalled: false, serviceComponents: [
                    // set isRequiredOnAllHosts = true for slave and client
                    Em.Object.create({
                      componentName: 'ANYSERVICE_MASTER',
                      isMaster: true,
                      isRequiredOnAllHosts: false
                    }),
                    Em.Object.create({
                      componentName: 'ANYSERVICE_SLAVE',
                      isSlave: true,
                      isRequiredOnAllHosts: true
                    }),
                    Em.Object.create({
                      componentName: 'ANYSERVICE_SLAVE2',
                      isSlave: true,
                      isRequiredOnAllHosts: true
                    }),
                    Em.Object.create({
                      componentName: 'ANYSERVICE_CLIENT',
                      isClient: true,
                      isRequiredOnAllHosts: true
                    })
                  ]
                })
              ]),
              masterComponentHosts: Em.A([
                Em.Object.create({
                  componentName: 'ANYSERVICE_MASTER',
                  component: 'ANYSERVICE_MASTER',
                  hosts: Em.A([
                    Em.Object.create({hostName: 'h1', isInstalled: true})
                  ])
                })
              ]),
              slaveComponentHosts: Em.A([
                Em.Object.create({
                  componentName: 'ANYSERVICE_SLAVE',
                  hosts: Em.A([
                    Em.Object.create({hostName: 'h1', isInstalled: false}),
                    Em.Object.create({hostName: 'h2', isInstalled: false})
                  ])
                }),
                Em.Object.create({
                  componentName: 'ANYSERVICE_SLAVE2',
                  hosts: Em.A([
                    Em.Object.create({hostName: 'h1', isInstalled: false}),
                    Em.Object.create({hostName: 'h2', isInstalled: false})
                  ])
                }),
                Em.Object.create({
                  componentName: 'CLIENT',
                  hosts: Em.A([
                    Em.Object.create({hostName: 'h1', isInstalled: false}),
                    Em.Object.create({hostName: 'h2', isInstalled: false})
                  ])
                })
              ]),
              clients: Em.A([
                Em.Object.create({
                  component_name: 'ANYSERVICE_CLIENT',
                  isInstalled: false,
                  hosts: Em.A([
                    Em.Object.create({hostName: 'h1', isInstalled: false}),
                    Em.Object.create({hostName: 'h2', isInstalled: false})
                  ])
                })
              ])
            }
          });
          installerStep8Controller.set('ajaxRequestsQueue', App.ajaxQueue.create());
          installerStep8Controller.get('ajaxRequestsQueue').clear();
          installerStep8Controller.createAdditionalHostComponents();
        });

        // Any component with isRequiredOnAllHosts = true implies that
        // registerHostsToComponent would be done via
        // createAdditionalHostComponents() BUT NOT
        // createMasterHostComponents() or createSlaveAndClientsHostComponents()
        // or createAdditionalClientComponents()
        it('registerHostsToComponent 1st call', function () {
          expect(installerStep8Controller.registerHostsToComponent.args[0][0]).to.eql(['h1', 'h2']);
          expect(installerStep8Controller.registerHostsToComponent.args[0][1]).to.equal('ANYSERVICE_SLAVE');
        });
        it('registerHostsToComponent 2nd call', function () {
          expect(installerStep8Controller.registerHostsToComponent.args[1][0]).to.eql(['h1', 'h2']);
          expect(installerStep8Controller.registerHostsToComponent.args[1][1]).to.equal('ANYSERVICE_SLAVE2');
        });
        it('registerHostsToComponent 3rd call', function () {
          expect(installerStep8Controller.registerHostsToComponent.args[2][0]).to.eql(['h1', 'h2']);
          expect(installerStep8Controller.registerHostsToComponent.args[2][1]).to.equal('ANYSERVICE_CLIENT');
        });
      });

      describe('should add components with isRequiredOnAllHosts == true (2)', function() {

        beforeEach(function () {
          installerStep8Controller.reopen({
            getRegisteredHosts: function() {
              return [{hostName: 'h1'}, {hostName: 'h2'}];
            },
            content: {
              services: Em.A([
                Em.Object.create({
                  serviceName: 'ANYSERVICE', isSelected: true, isInstalled: false, serviceComponents: [
                    // set isRequiredOnAllHosts = true for master
                    Em.Object.create({
                      componentName: 'ANYSERVICE_MASTER',
                      isMaster: true,
                      isRequiredOnAllHosts: true
                    }),
                    Em.Object.create({
                      componentName: 'ANYSERVICE_SLAVE',
                      isSlave: true,
                      isRequiredOnAllHosts: false
                    }),
                    Em.Object.create({
                      componentName: 'ANYSERVICE_SLAVE2',
                      isSlave: true,
                      isRequiredOnAllHosts: false
                    }),
                    Em.Object.create({
                      componentName: 'ANYSERVICE_CLIENT',
                      isClient: true,
                      isRequiredOnAllHosts: false
                    })
                  ]
                })
              ]),
              masterComponentHosts: Em.A([
                Em.Object.create({
                  componentName: 'ANYSERVICE_MASTER',
                  component: 'ANYSERVICE_MASTER',
                  hosts: Em.A([
                    Em.Object.create({hostName: 'h1', isInstalled: true})
                  ])
                })
              ]),
              slaveComponentHosts: Em.A([
                Em.Object.create({
                  componentName: 'ANYSERVICE_SLAVE',
                  hosts: Em.A([
                    Em.Object.create({hostName: 'h1', isInstalled: false}),
                    Em.Object.create({hostName: 'h2', isInstalled: false})
                  ])
                }),
                Em.Object.create({
                  componentName: 'ANYSERVICE_SLAVE2',
                  hosts: Em.A([
                    Em.Object.create({hostName: 'h1', isInstalled: false}),
                    Em.Object.create({hostName: 'h2', isInstalled: false})
                  ])
                }),
                Em.Object.create({
                  componentName: 'CLIENT',
                  hosts: Em.A([
                    Em.Object.create({hostName: 'h1', isInstalled: false}),
                    Em.Object.create({hostName: 'h2', isInstalled: false})
                  ])
                })
              ]),
              clients: Em.A([
                Em.Object.create({
                  component_name: 'ANYSERVICE_CLIENT',
                  isInstalled: false,
                  hosts: Em.A([
                    Em.Object.create({hostName: 'h1', isInstalled: false}),
                    Em.Object.create({hostName: 'h2', isInstalled: false})
                  ])
                })
              ])
            }
          });
          installerStep8Controller.set('ajaxRequestsQueue', App.ajaxQueue.create());
          installerStep8Controller.get('ajaxRequestsQueue').clear();
          installerStep8Controller.createMasterHostComponents();
          installerStep8Controller.createAdditionalHostComponents();
        });

        // master component with isRequiredOnAllHosts = true implies that
        // registerHostsToComponent would be done via
        // createAdditionalHostComponents() BUT NOT
        // createMasterHostComponents()
        it('registerHostsToComponent 1st call', function () {
          expect(installerStep8Controller.registerHostsToComponent.args[0][0]).to.eql(['h1', 'h2']);
          expect(installerStep8Controller.registerHostsToComponent.args[0][1]).to.equal('ANYSERVICE_MASTER');
          expect(installerStep8Controller.registerHostsToComponent.callCount).to.equal(1);
        });
      });

      describe('should not add components with isRequiredOnAllHosts == false (3)', function() {

        beforeEach(function () {
          installerStep8Controller.reopen({
            getRegisteredHosts: function() {
              return [{hostName: 'h1'}, {hostName: 'h2'}];
            },
            content: {
              services: Em.A([
                Em.Object.create({
                  serviceName: 'ANYSERVICE', isSelected: true, isInstalled: false, serviceComponents: [
                    // set isRequiredOnAllHosts = false for all components
                    Em.Object.create({
                      componentName: 'ANYSERVICE_MASTER',
                      isMaster: true,
                      isRequiredOnAllHosts: false
                    }),
                    Em.Object.create({
                      componentName: 'ANYSERVICE_SLAVE',
                      isSlave: true,
                      isRequiredOnAllHosts: false
                    }),
                    Em.Object.create({
                      componentName: 'ANYSERVICE_SLAVE2',
                      isSlave: true,
                      isRequiredOnAllHosts: false
                    }),
                    Em.Object.create({
                      componentName: 'ANYSERVICE_CLIENT',
                      isClient: true,
                      isRequiredOnAllHosts: false
                    })
                  ]
                })
              ]),
              masterComponentHosts: Em.A([
                Em.Object.create({
                  componentName: 'ANYSERVICE_MASTER',
                  component: 'ANYSERVICE_MASTER',
                  hosts: Em.A([
                    Em.Object.create({hostName: 'h1', isInstalled: true})
                  ])
                })
              ]),
              slaveComponentHosts: Em.A([
                Em.Object.create({
                  componentName: 'ANYSERVICE_SLAVE',
                  hosts: Em.A([
                    Em.Object.create({hostName: 'h1', isInstalled: false}),
                    Em.Object.create({hostName: 'h2', isInstalled: false})
                  ])
                }),
                Em.Object.create({
                  componentName: 'ANYSERVICE_SLAVE2',
                  hosts: Em.A([
                    Em.Object.create({hostName: 'h1', isInstalled: false}),
                    Em.Object.create({hostName: 'h2', isInstalled: false})
                  ])
                }),
                Em.Object.create({
                  componentName: 'CLIENT',
                  hosts: Em.A([
                    Em.Object.create({hostName: 'h1', isInstalled: false}),
                    Em.Object.create({hostName: 'h2', isInstalled: false})
                  ])
                })
              ]),
              clients: Em.A([
                Em.Object.create({
                  component_name: 'ANYSERVICE_CLIENT',
                  isInstalled: false,
                  hosts: Em.A([
                    Em.Object.create({hostName: 'h1', isInstalled: false}),
                    Em.Object.create({hostName: 'h2', isInstalled: false})
                  ])
                })
              ])
            }
          });
          installerStep8Controller.set('ajaxRequestsQueue', App.ajaxQueue.create());
          installerStep8Controller.get('ajaxRequestsQueue').clear();
          installerStep8Controller.createAdditionalHostComponents();
        });

        it('registerHostsToComponent is not called', function () {
          // isRequiredOnAllHosts = false for all components, implies that
          // registerHostsToComponent would be done via
          // createMasterHostComponents() or createSlaveAndClientsHostComponents()
          // or createAdditionalClientComponents()
          // BUT NOT createAdditionalHostComponents()
          expect(installerStep8Controller.registerHostsToComponent.callCount).to.equal(0);
        });

      });

  });

    describe('#createNotification', function () {

      beforeEach(function () {
        installerStep8Controller.clearStep();
        installerStep8Controller.set('content', {controllerName: 'installerController'});
        installerStep8Controller.set('configs', [
          {name: 'create_notification', value: 'yes', serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'ambari.dispatch.recipients', value: 'to@f.c', serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'mail.smtp.host', value: 'h', serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'mail.smtp.port', value: '25', serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'mail.smtp.from', value: 'from@f.c', serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'mail.smtp.starttls.enable', value: true, serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'mail.smtp.startssl.enable', value: false, serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'smtp_use_auth', value: 'true', serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'ambari.dispatch.credential.username', value: 'usr', serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'ambari.dispatch.credential.password', value: 'pwd', serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'some_p', value: 'some_v', serviceName: 'MISC', filename: 'alert_notification'}
        ]);
        installerStep8Controller.get('ajaxRequestsQueue').clear();
      });

      it('should add request to queue', function () {
        installerStep8Controller.createNotification();
        expect(installerStep8Controller.get('ajaxRequestsQueue.queue.length')).to.equal(1);
        installerStep8Controller.get('ajaxRequestsQueue').runNextRequest();
        var args = testHelpers.findAjaxRequest('name', 'alerts.create_alert_notification');
        expect(args).exists;
      });

      describe('sent data should be valid', function () {
        var data;
        beforeEach(function () {
          installerStep8Controller.createNotification();
          data = installerStep8Controller.get('ajaxRequestsQueue.queue')[0].data.data.AlertTarget;
        });

        it('global is true', function () {
          expect(data.global).to.be.true;
        });
        it('notification_type is EMAIL', function () {
          expect(data.notification_type).to.equal('EMAIL');
        });
        it('alert_states are valid', function () {
          expect(data.alert_states).to.eql(['OK', 'WARNING', 'CRITICAL', 'UNKNOWN']);
        });
        it('ambari.dispatch.recipients is valid', function () {
          expect(data.properties['ambari.dispatch.recipients']).to.eql(['to@f.c']);
        });
        it('mail.smtp.host is valid', function () {
          expect(data.properties['mail.smtp.host']).to.equal('h');
        });
        it('mail.smtp.port is valid', function () {
          expect(data.properties['mail.smtp.port']).to.equal('25');
        });
        it('mail.smtp.from is valid', function () {
          expect(data.properties['mail.smtp.from']).to.equal('from@f.c');
        });
        it('mail.smtp.starttls.enable is true', function () {
          expect(data.properties['mail.smtp.starttls.enable']).to.equal(true);
        });
        it('mail.smtp.startssl.enable is false', function () {
          expect(data.properties['mail.smtp.startssl.enable']).to.equal(false);
        });
        it('ambari.dispatch.credential.username is valid', function () {
          expect(data.properties['ambari.dispatch.credential.username']).to.equal('usr');
        });
        it('ambari.dispatch.credential.password is valid', function () {
          expect(data.properties['ambari.dispatch.credential.password']).to.equal('pwd');
        });
        it('custom property is valid', function () {
          expect(data.properties.some_p).to.equal('some_v');
        });

      });

    });

  });

  App.TestAliases.testAsComputedEqualProperties(getController(), 'isAllClusterDeleteRequestsCompleted', 'clusterDeleteRequestsCompleted', 'clusterNames.length');

  describe('#deleteClusterSuccessCallback', function () {

    beforeEach(function () {
      sinon.stub(installerStep8Controller, 'showDeleteClustersErrorPopup', Em.K);
      sinon.stub(installerStep8Controller, 'getExistingVersions', Em.K);
      installerStep8Controller.setProperties({
        clusterDeleteRequestsCompleted: 0,
        clusterNames: ['c0', 'c1'],
        clusterDeleteErrorViews: []
      });
      installerStep8Controller.deleteClusterSuccessCallback();
    });

    afterEach(function () {
      installerStep8Controller.showDeleteClustersErrorPopup.restore();
      installerStep8Controller.getExistingVersions.restore();
    });

    describe('no failed requests', function () {
      it('before Delete Cluster request', function () {
        expect(installerStep8Controller.get('clusterDeleteRequestsCompleted')).to.equal(1);
        expect(installerStep8Controller.showDeleteClustersErrorPopup.called).to.be.false;
        expect(installerStep8Controller.getExistingVersions.called).to.be.false;
      });
      it('after Delete Cluster request', function () {
        installerStep8Controller.deleteClusterSuccessCallback();
        expect(installerStep8Controller.get('clusterDeleteRequestsCompleted')).to.equal(2);
        expect(installerStep8Controller.showDeleteClustersErrorPopup.called).to.be.false;
        expect(installerStep8Controller.getExistingVersions.calledOnce).to.be.true;
      });
    });

    it('one request failed', function () {
      installerStep8Controller.deleteClusterErrorCallback({}, null, null, {});
      expect(installerStep8Controller.get('clusterDeleteRequestsCompleted')).to.equal(2);
      expect(installerStep8Controller.showDeleteClustersErrorPopup.calledOnce).to.be.true;
      expect(installerStep8Controller.getExistingVersions.called).to.be.false;
    });

  });

  describe('#deleteClusterErrorCallback', function () {

    var request = {
        status: 500,
        responseText: '{"message":"Internal Server Error"}'
      },
      ajaxOptions = 'error',
      error = 'Internal Server Error',
      opt = {
        url: 'api/v1/clusters/c0',
        type: 'DELETE'
      };

    beforeEach(function () {
      installerStep8Controller.setProperties({
        clusterDeleteRequestsCompleted: 0,
        clusterNames: ['c0', 'c1'],
        clusterDeleteErrorViews: []
      });
      sinon.stub(installerStep8Controller, 'showDeleteClustersErrorPopup', Em.K);
      installerStep8Controller.deleteClusterErrorCallback(request, ajaxOptions, error, opt);
    });

    afterEach(function () {
      installerStep8Controller.showDeleteClustersErrorPopup.restore();
    });

    describe('should show error popup only if all requests are completed', function () {
      it('Before Delete Cluster request fail', function () {
        expect(installerStep8Controller.get('clusterDeleteRequestsCompleted')).to.equal(1);
        expect(installerStep8Controller.showDeleteClustersErrorPopup.called).to.be.false;
      });
      it('After Delete Cluster request is failed', function () {
        installerStep8Controller.deleteClusterErrorCallback(request, ajaxOptions, error, opt);
        expect(installerStep8Controller.get('clusterDeleteRequestsCompleted')).to.equal(2);
        expect(installerStep8Controller.showDeleteClustersErrorPopup.calledOnce).to.be.true;
      });
    });

    describe('should create error popup body view', function () {
      it('One failed request', function () {
        expect(installerStep8Controller.get('clusterDeleteErrorViews')).to.have.length(1);
      });
      it('failed request url is valid', function () {
        expect(installerStep8Controller.get('clusterDeleteErrorViews.firstObject.url')).to.equal('api/v1/clusters/c0');
      });
      it('failed request type is valid', function () {
        expect(installerStep8Controller.get('clusterDeleteErrorViews.firstObject.type')).to.equal('DELETE');
      });
      it('failed request status is valid', function () {
        expect(installerStep8Controller.get('clusterDeleteErrorViews.firstObject.status')).to.equal(500);
      });
      it('failed request message is valid', function () {
        expect(installerStep8Controller.get('clusterDeleteErrorViews.firstObject.message')).to.equal('Internal Server Error');
      });
    });

  });

  describe('#showDeleteClustersErrorPopup', function () {

    beforeEach(function () {
      installerStep8Controller.setProperties({
        isSubmitDisabled: true,
        isBackBtnDisabled: true
      });
      installerStep8Controller.showDeleteClustersErrorPopup();
    });

    it('should show error popup and unlock navigation', function () {
      expect(installerStep8Controller.get('isSubmitDisabled')).to.be.false;
      expect(installerStep8Controller.get('isBackBtnDisabled')).to.be.false;
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });

  });

  describe('#_startDeploy', function () {

    var stubbedNames = ['createCluster', 'createSelectedServices', 'createConfigurations',
        'applyConfigurationsToCluster', 'createComponents', 'registerHostsToCluster', 'createConfigurationGroups',
        'createMasterHostComponents', 'createSlaveAndClientsHostComponents', 'createAdditionalClientComponents',
        'createAdditionalHostComponents'],
      cases = [
        {
          controllerName: 'installerController',
          notExecuted: ['createAdditionalClientComponents'],
          fileNamesToUpdate: [],
          title: 'Installer, no configs to update'
        },
        {
          controllerName: 'addHostController',
          notExecuted: ['createConfigurations', 'applyConfigurationsToCluster', 'createAdditionalClientComponents'],
          title: 'Add Host Wizard'
        },
        {
          controllerName: 'addServiceController',
          notExecuted: ['updateConfigurations'],
          fileNamesToUpdate: [],
          title: 'Add Service Wizard, no configs to update'
        },
        {
          controllerName: 'addServiceController',
          notExecuted: [],
          fileNamesToUpdate: [''],
          title: 'Add Service Wizard, some configs to be updated'
        }
      ];

    beforeEach(function () {
      sinon.stub(App, 'get').withArgs('isKerberosEnabled').returns(false);
      sinon.stub(App.Stack, 'find').returns([
        Em.Object.create({
          id: "HDP-2.3-2.3.4.4-1234",
          isSelected: true,
          repositoryVersion: "2.3.4.4-1234"
        })
      ]);
      stubbedNames.forEach(function (name) {
        sinon.stub(installerStep8Controller, name, Em.K);
      });
      installerStep8Controller.setProperties({
        serviceConfigTags: [],
        content: {
          controllerName: null
        }
      });
    });

    afterEach(function () {
      App.get.restore();
      App.Stack.find.restore();
      stubbedNames.forEach(function (name) {
        installerStep8Controller[name].restore();
      });
      installerStep8Controller.get.restore();
    });

    cases.forEach(function (item) {
      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(installerStep8Controller, 'get', function (key) {
            if (key === 'ajaxRequestsQueue') {
              return {start: Em.K};
            }
            if (key === 'ajaxRequestsQueue.queue.length') {
              return 1;
            }
            if (key === 'wizardController') {
              return {
                getDBProperty: function () {
                  return item.fileNamesToUpdate;
                }
              };
            }
            return Em.get(this, key);
          });
          installerStep8Controller.set('content.controllerName', item.controllerName);
          installerStep8Controller._startDeploy();
        });

        stubbedNames.forEach(function (name) {
          it(name, function () {
            expect(installerStep8Controller[name].called).to.equal(!item.notExecuted.contains(name));
          });
        });

      });
    });

  });

  describe('#getClientsMap', function () {

    var cases = [
      {
        flag: 'isMaster',
        result: {
          c8: ['c1', 'c2'],
          c9: ['c1', 'c2']
        },
        title: 'dependencies for masters'
      },
      {
        flag: 'isSlave',
        result: {
          c8: ['c5', 'c6'],
          c9: ['c5', 'c6']
        },
        title: 'dependencies for slaves'
      },
      {
        flag: 'isClient',
        result: {
          c8: ['c9', 'c10'],
          c9: ['c9', 'c10']
        },
        title: 'dependencies for clients'
      },
      {
        flag: null,
        result: {
          c8: ['c1', 'c2', 'c5', 'c6', 'c9', 'c10'],
          c9: ['c1', 'c2', 'c5', 'c6', 'c9', 'c10']
        },
        title: 'dependencies for all components'
      }
    ];

    before(function () {
      var mock = sinon.stub(App.StackServiceComponent, 'find');
      var components = [
        App.StackServiceComponent.createRecord({
          componentName: 'c0',
          isMaster: true,
          dependencies: [
            {
              componentName: 'c1'
            },
            {
              componentName: 'c2'
            },
            {
              componentName: 'c4'
            },
            {
              componentName: 'c5'
            }
          ]
        }),
        App.StackServiceComponent.createRecord({
          componentName: 'c1',
          isMaster: true,
          dependencies: [
            {
              componentName: 'c4'
            },
            {
              componentName: 'c5'
            },
            {
              componentName: 'c8'
            },
            {
              componentName: 'c9'
            }
          ]
        }),
        App.StackServiceComponent.createRecord({
          componentName: 'c2',
          isMaster: true,
          dependencies: [
            {
              componentName: 'c1'
            },
            {
              componentName: 'c2'
            },
            {
              componentName: 'c8'
            },
            {
              componentName: 'c9'
            }
          ]
        }),
        App.StackServiceComponent.createRecord({
          componentName: 'c3',
          isMaster: true,
          dependencies: []
        }),
        App.StackServiceComponent.createRecord({
          componentName: 'c4',
          componentCategory: 'SLAVE',
          dependencies: [
            {
              componentName: 'c1'
            },
            {
              componentName: 'c2'
            },
            {
              componentName: 'c4'
            },
            {
              componentName: 'c5'
            }
          ]
        }),
        App.StackServiceComponent.createRecord({
          componentName: 'c5',
          componentCategory: 'SLAVE',
          dependencies: [
            {
              componentName: 'c4'
            },
            {
              componentName: 'c5'
            },
            {
              componentName: 'c8'
            },
            {
              componentName: 'c9'
            }
          ]
        }),
        App.StackServiceComponent.createRecord({
          componentName: 'c6',
          componentCategory: 'SLAVE',
          dependencies: [
            {
              componentName: 'c1'
            },
            {
              componentName: 'c2'
            },
            {
              componentName: 'c8'
            },
            {
              componentName: 'c9'
            }
          ]
        }),
        App.StackServiceComponent.createRecord({
          componentName: 'c7',
          componentCategory: 'SLAVE',
          dependencies: []
        }),
        App.StackServiceComponent.createRecord({
          componentName: 'c8',
          isClient: true,
          dependencies: [
            {
              componentName: 'c1'
            },
            {
              componentName: 'c2'
            },
            {
              componentName: 'c4'
            },
            {
              componentName: 'c5'
            }
          ]
        }),
        App.StackServiceComponent.createRecord({
          componentName: 'c9',
          isClient: true,
          dependencies: [
            {
              componentName: 'c4'
            },
            {
              componentName: 'c5'
            },
            {
              componentName: 'c8'
            },
            {
              componentName: 'c9'
            }
          ]
        }),
        App.StackServiceComponent.createRecord({
          componentName: 'c10',
          isClient: true,
          dependencies: [
            {
              componentName: 'c1'
            },
            {
              componentName: 'c2'
            },
            {
              componentName: 'c8'
            },
            {
              componentName: 'c9'
            }
          ]
        }),
        App.StackServiceComponent.createRecord({
          componentName: 'c11',
          isClient: true,
          dependencies: []
        })
      ];
      components.forEach(function(component) {
        mock.withArgs(component.get('componentName')).returns(component);
      });
      mock.returns(components);
    });

    after(function () {
      App.StackServiceComponent.find.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(installerStep8Controller.getClientsMap(item.flag)).to.eql(item.result);
      });
    });

  });

  describe('#showLoadingIndicator', function() {


    it('if popup doesn\'t exist should create another', function() {
      installerStep8Controller.set('isSubmitDisabled', true);
      installerStep8Controller.showLoadingIndicator();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });

  });

  describe('#updateKerberosDescriptor', function () {

    var requestData = {artifactName: 'kerberos_descriptor',
      data: {
        artifact_data: 1234
      }
    };

    beforeEach(function () {
      sinon.stub(installerStep8Controller, 'addRequestToAjaxQueue', Em.K);
      sinon.stub(installerStep8Controller, 'get').withArgs('wizardController').returns(Em.Object.create({
        getDBProperty: function(key) {
          if (key === 'kerberosDescriptorConfigs') return 1234;
          if (key === 'isClusterDescriptorExists') return true;
          return App.db.get(this.get('dbNamespace'), key);
        }
      }));
      sinon.stub(installerStep8Controller, 'removeIdentityReferences').returns(1234);
    });

    afterEach(function () {
      installerStep8Controller.addRequestToAjaxQueue.restore();
      installerStep8Controller.get.restore();
      installerStep8Controller.removeIdentityReferences.restore();
    });

    it('should send request instantly', function () {
      installerStep8Controller.updateKerberosDescriptor(true);
      var args = testHelpers.findAjaxRequest('name', 'admin.kerberos.cluster.artifact.update');
      expect(args[0]).exists;
      expect(args[0].data).to.be.eql(requestData);
      expect(installerStep8Controller.addRequestToAjaxQueue.called).to.be.false;
    });

    it('should add request to the queue', function () {
      installerStep8Controller.updateKerberosDescriptor(false);
      var args = testHelpers.findAjaxRequest('name', 'admin.kerberos.cluster.artifact.update');
      expect(args).not.exists;
      expect(installerStep8Controller.addRequestToAjaxQueue.calledOnce).to.be.true;
      expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data).to.be.eql(requestData);
    });

  });

  describe('#generateBlueprint', function () {

     beforeEach(function () {
       var configsForTest = Em.A([
         Em.Object.create({filename: 'cluster-env.xml', name: 'p0', value: 'v0'}),
         Em.Object.create({filename: 'site1.xml', name: 'p11', value: 'v11'}),
         Em.Object.create({filename: 'site1.xml', name: 'p12', value: 'v12'}),
         Em.Object.create({filename: 'site2.xml', name: 'p21', value: 'v21'}),
         Em.Object.create({filename: 'site2.xml', name: 'p22', value: 'v22'}),
         Em.Object.create({filename: 'site3.xml', name: 'p31', value: 'v31'}),
         Em.Object.create({filename: 'site3.xml', name: 'p32', value: 'v32'})
       ]);

       var hostComponents1 = Em.A([
         Em.Object.create({componentName: 'NAMENODE'}),
         Em.Object.create({componentName: 'DATANODE'})
       ]);
       var hostComponents2 = Em.A([
         Em.Object.create({componentName: 'JOURNALNODE'}),
         Em.Object.create({componentName: 'DATANODE'})
       ]);
       var hosts = Em.A([
         Em.Object.create({bootStatus: 'REGISTERED', name: 'h1', hostName: 'h1', isInstalled: false, hostComponents: hostComponents1, fqdn: 'h1'}),
         Em.Object.create({bootStatus: 'REGISTERED', name: 'h2', hostName: 'h2', isInstalled: false, hostComponents: hostComponents1, fqdn: 'h2'}),
         Em.Object.create({bootStatus: 'REGISTERED', name: 'h3', hostName: 'h3', isInstalled: false, hostComponents: hostComponents2, fqdn: 'h3'}),
         Em.Object.create({bootStatus: 'REGISTERED', name: 'h4', hostName: 'h4', isInstalled: false, hostComponents: hostComponents2, fqdn: 'h4'})
       ]);
       var configGroupProperties = Em.A([
         Em.Object.create({filename: 'site1.xml', name: 'p11', value: 'v11_overriden'})
       ]);
       var configGroups = Em.A([
         Em.Object.create({is_default : true, properties: [], hosts: [] }),
         Em.Object.create({is_default : false, properties: [], hosts: [] }),
         Em.Object.create({name: 'hdfs_custom_group', is_default : false, properties: configGroupProperties, hosts: [hosts[0].fqdn,hosts[1].fqdn] })
       ]);

       installerStep8Controller = getController();
       installerStep8Controller.set('configs', configsForTest);
       installerStep8Controller.set('allHosts', hosts);
       installerStep8Controller.set('content.services', services.filterProperty('isSelected'));
       installerStep8Controller.set('content.hosts', hosts);
       installerStep8Controller.set('content.configGroups', configGroups);
       installerStep8Controller.set('selectedServices', services.filterProperty('isSelected'));
       sinon.spy(installerStep8Controller, 'getConfigurationDetailsForConfigType');
       sinon.spy(installerStep8Controller, 'hostInExistingHostGroup');
       sinon.spy(installerStep8Controller, 'hostInChildHostGroup');
       sinon.stub(fileUtils, 'downloadFilesInZip');
     });
     afterEach(function() {
       fileUtils.downloadFilesInZip.restore();
     });
     it('should call generateBlueprint', function() {
       installerStep8Controller.generateBlueprint();
       expect(installerStep8Controller.hostInExistingHostGroup.calledAfter(installerStep8Controller.getConfigurationDetailsForConfigType)).to.be.true;
       sinon.assert.callCount(installerStep8Controller.getConfigurationDetailsForConfigType, 4);
       sinon.assert.callCount(installerStep8Controller.hostInExistingHostGroup, 4);
       sinon.assert.callCount(installerStep8Controller.hostInChildHostGroup, 1);
       expect(fileUtils.downloadFilesInZip.calledOnce).to.be.true;
     });
 });
});
