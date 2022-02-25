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
require('controllers/wizard');
require('controllers/main/service/add_controller');
var addServiceController = null;
var testHelpers = require('test/helpers');

describe('App.AddServiceController', function() {

  beforeEach(function () {
    addServiceController = App.AddServiceController.create({});
  });

  describe('#generateDataForInstallServices', function() {
    var tests = [{
      selected: ["YARN","HBASE"],
      res: {
        "context": Em.I18n.t('requestInfo.installServices'),
        "ServiceInfo": {"state": "INSTALLED"},
        "urlParams": "ServiceInfo/service_name.in(YARN,HBASE)"
      }
    },
    {
      selected: ['OOZIE'],
      res: {
        "context": Em.I18n.t('requestInfo.installServices'),
        "ServiceInfo": {"state": "INSTALLED"},
        "urlParams": "ServiceInfo/service_name.in(OOZIE,HDFS,YARN,MAPREDUCE2)"
      }
    }];
    tests.forEach(function(t){
      it('should generate data with ' + t.selected.join(","), function () {
        expect(addServiceController.generateDataForInstallServices(t.selected)).to.be.eql(t.res);
      });
    });
  });

  describe('#saveServices', function() {
    beforeEach(function() {
      sinon.stub(addServiceController, 'setDBProperty', Em.K);
    });

    afterEach(function() {
      addServiceController.setDBProperty.restore();
    });

    var tests = [
      {
        appService: [
          Em.Object.create({ serviceName: 'HDFS' }),
          Em.Object.create({ serviceName: 'KERBEROS' })
        ],
        stepCtrlContent: Em.Object.create({
          content: Em.A([
            Em.Object.create({ serviceName: 'HDFS', isInstalled: true, isSelected: true }),
            Em.Object.create({ serviceName: 'YARN', isInstalled: false, isSelected: true })
          ])
        }),
        e: {
          selected: ['YARN'],
          installed: ['HDFS', 'KERBEROS']
        }
      },
      {
        appService: [
          Em.Object.create({ serviceName: 'HDFS' }),
          Em.Object.create({ serviceName: 'STORM' })
        ],
        stepCtrlContent: Em.Object.create({
          content: Em.A([
            Em.Object.create({ serviceName: 'HDFS', isInstalled: true, isSelected: true }),
            Em.Object.create({ serviceName: 'YARN', isInstalled: false, isSelected: true }),
            Em.Object.create({ serviceName: 'MAPREDUCE2', isInstalled: false, isSelected: true })
          ])
        }),
        e: {
          selected: ['YARN', 'MAPREDUCE2'],
          installed: ['HDFS', 'STORM']
        }
      }
    ];

    var message = '{0} installed, {1} selected. Installed list should be {2} and selected - {3}';
    tests.forEach(function(test) {

      var installed = test.appService.mapProperty('serviceName');
      var selected = test.stepCtrlContent.get('content').filterProperty('isSelected', true)
        .filterProperty('isInstalled', false).mapProperty('serviceName');

      describe(message.format(installed, selected, test.e.installed, test.e.selected), function() {

        beforeEach(function () {
          sinon.stub(App.Service, 'find').returns(test.appService);
          addServiceController.saveServices(test.stepCtrlContent);
          this.savedServices = addServiceController.setDBProperty.withArgs('services').args[0][1];
        });

        afterEach(function () {
          App.Service.find.restore();
        });

        it(JSON.stringify(test.e.selected) + ' are in the selectedServices', function () {
          expect(this.savedServices.selectedServices).to.have.members(test.e.selected);
        });

        it(JSON.stringify(test.e.installed) + ' are in the installedServices', function () {
          expect(this.savedServices.installedServices).to.have.members(test.e.installed);
        });

      });
    });
  });

  describe('#loadHosts', function () {

    var cases = [
      {
        hosts: {},
        isAjaxRequestSent: false,
        title: 'hosts are already loaded'
      },
      {
        areHostsLoaded: false,
        isAjaxRequestSent: true,
        title: 'hosts aren\'t yet loaded'
      }
    ];

    afterEach(function () {
      addServiceController.getDBProperty.restore();
    });

    cases.forEach(function (item) {
      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(addServiceController, 'getDBProperty').withArgs('hosts').returns(item.hosts);
          addServiceController.loadHosts();
          this.args = testHelpers.findAjaxRequest('name', 'hosts.confirmed');
        });

        it('request is ' + (item.isAjaxRequestSent ? '' : 'not') + ' sent', function () {
          expect(Em.isNone(this.args)).to.be.equal(!item.isAjaxRequestSent);
        });
      });
    });

  });

  describe('#loadServices', function() {
    var mock = {
      db: {}
    };
    beforeEach(function() {
      this.controller = App.AddServiceController.create({});
      this.mockGetDBProperty = sinon.stub(this.controller, 'getDBProperty');
      sinon.stub(this.controller, 'setDBProperty', function(key, value) {
        mock.db = value;
      });
      sinon.stub(this.controller, 'hasDependentSlaveComponent');
      sinon.stub(App.store, 'fastCommit', Em.K);
      this.mockStackService = sinon.stub(App.StackService, 'find');
      this.mockService = sinon.stub(App.Service, 'find');
    });

    afterEach(function() {
      this.mockGetDBProperty.restore();
      this.controller.setDBProperty.restore();
      this.controller.hasDependentSlaveComponent.restore();
      this.mockStackService.restore();
      this.mockService.restore();
      App.store.fastCommit.restore();
    });

    var tests = [
      {
        appStackService: [
          Em.Object.create({ id: 'HDFS', serviceName: 'HDFS', coSelectedServices: []}),
          Em.Object.create({ id: 'YARN', serviceName: 'YARN', coSelectedServices: ['MAPREDUCE2']}),
          Em.Object.create({ id: 'MAPREDUCE2', serviceName: 'MAPREDUCE2', coSelectedServices: []}),
          Em.Object.create({ id: 'FALCON', serviceName: 'FALCON', coSelectedServices: []}),
          Em.Object.create({ id: 'STORM', serviceName: 'STORM', coSelectedServices: []})
        ],
        appService: [
          Em.Object.create({ id: 'HDFS', serviceName: 'HDFS'}),
          Em.Object.create({ id: 'STORM', serviceName: 'STORM'})
        ],
        servicesFromDB: false,
        serviceToInstall: 'MAPREDUCE2',
        e: {
          selectedServices: ['HDFS', 'YARN', 'MAPREDUCE2', 'STORM'],
          installedServices: ['HDFS', 'STORM']
        },
        m: 'MapReduce selected on Admin -> Stack Versions Page, Yarn service should be selected because it coselected'
      },
      {
        appStackService: [
          Em.Object.create({ id: 'HDFS', serviceName: 'HDFS', coSelectedServices: []}),
          Em.Object.create({ id: 'YARN', serviceName: 'YARN', coSelectedServices: ['MAPREDUCE2']}),
          Em.Object.create({ id: 'HBASE', serviceName: 'HBASE', coSelectedServices: []}),
          Em.Object.create({ id: 'STORM', serviceName: 'STORM', coSelectedServices: []})
        ],
        appService: [
          Em.Object.create({ id: 'HDFS', serviceName: 'HDFS'}),
          Em.Object.create({ id: 'STORM', serviceName: 'STORM'})
        ],
        servicesFromDB: {
          selectedServices: ['HBASE'],
          installedServices: ['HDFS', 'STORM']
        },
        serviceToInstall: null,
        e: {
          selectedServices: ['HDFS', 'HBASE', 'STORM'],
          installedServices: ['HDFS', 'STORM']
        },
        m: 'HDFS and STORM are installed. Select HBASE'
      }
    ];

    tests.forEach(function(test) {
      describe(test.m, function() {

        beforeEach(function () {
          this.mockStackService.returns(test.appStackService);
          this.mockService.returns(test.appService);
          this.mockGetDBProperty.withArgs('services').returns(test.servicesFromDB);
          this.controller.set('serviceToInstall', test.serviceToInstall);
          this.controller.loadServices();
        });

        if (test.servicesFromDB) {
          // verify values for App.StackService
          it(JSON.stringify(test.e.selectedServices) + ' are selected', function () {
            expect(test.appStackService.filterProperty('isSelected', true).mapProperty('serviceName')).to.be.eql(test.e.selectedServices);
          });
          it(JSON.stringify(test.e.installedServices) + ' are installed', function () {
            expect(test.appStackService.filterProperty('isInstalled', true).mapProperty('serviceName')).to.be.eql(test.e.installedServices);
          });
        }
        else {
          // verify saving to local db on first enter to the wizard
          it('selectedServices are saced', function () {
            expect(mock.db.selectedServices).to.be.eql(test.e.selectedServices);
          });
          it('installedServices are saved', function () {
            expect(mock.db.installedServices).to.be.eql(test.e.installedServices);
          });

        }

        it('serviceToInstall is null', function () {
          expect(this.controller.get('serviceToInstall')).to.be.null;
        });

      });
    }, this);
  });

  describe('#loadServiceConfigGroups', function () {

    var dbMock,
      dbMock2,
      cases = [
        {
          serviceConfigGroups: null,
          areInstalledConfigGroupsLoaded: false,
          title: 'config groups not yet loaded'
        },
        {
          serviceConfigGroups: [],
          areInstalledConfigGroupsLoaded: true,
          title: 'config groups already loaded'
        }
      ];

    beforeEach(function () {
      dbMock = sinon.stub(addServiceController, 'getDBProperties');
      dbMock2 = sinon.stub(addServiceController, 'getDBProperty');
    });

    afterEach(function () {
      dbMock.restore();
      dbMock2.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        dbMock.withArgs(['serviceConfigGroups', 'hosts']).returns({
          hosts: {},
          serviceConfigGroups: item.serviceConfigGroups
        });
        dbMock2.withArgs('hosts').returns({}).
          withArgs('serviceConfigGroups').returns(item.serviceConfigGroups);
        addServiceController.loadServiceConfigGroups();
        expect(addServiceController.get('areInstalledConfigGroupsLoaded')).to.equal(item.areInstalledConfigGroupsLoaded);
      });
    });

  });

  describe('#clearStorageData', function () {
    it('areInstalledConfigGroupsLoaded should be false', function () {
      addServiceController.set('areInstalledConfigGroupsLoaded', true);
      addServiceController.clearStorageData();
      expect(addServiceController.get('areInstalledConfigGroupsLoaded')).to.be.false;
    });
  });

  describe('#loadClients', function () {

    var cases = [
      {
        clients: null,
        contentClients: [],
        saveClientsCallCount: 1,
        title: 'no clients info in local db'
      },
      {
        clients: [{}],
        contentClients: [{}],
        saveClientsCallCount: 0,
        title: 'clients info saved in local db'
      }
    ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(addServiceController, 'getDBProperty').withArgs('clientInfo').returns(item.clients);
          sinon.stub(addServiceController, 'saveClients', Em.K);
          addServiceController.set('content.clients', []);
          addServiceController.loadClients();
        });

        afterEach(function () {
          addServiceController.getDBProperty.restore();
          addServiceController.saveClients.restore();
        });

        it('content.clients', function () {
          expect(addServiceController.get('content.clients', [])).to.eql(item.contentClients);
        });

        it('saveClients call', function () {
          expect(addServiceController.saveClients.callCount).to.equal(item.saveClientsCallCount);
        });

      });

    });

  });

  describe('#getServicesBySelectedSlaves', function () {

    beforeEach(function () {
      sinon.stub(App.StackServiceComponent, 'find').returns([
        Em.Object.create({
          componentName: 'c1',
          serviceName: 's1'
        }),
        Em.Object.create({
          componentName: 'c2',
          serviceName: 's2'
        }),
        Em.Object.create({
          componentName: 'c3',
          serviceName: 's3'
        }),
        Em.Object.create({
          componentName: 'c4',
          serviceName: 's1'
        })
      ]);
    });

    [
      {
        title: 'should return empty array',
        sch: [],
        expect: []
      },
      {
        title: 'should return empty array if component is absent in StackServiceComponent model',
        sch: [
          {
            componentName: 'c5',
            hosts: [
              {
                isInstalled: false
              },
              {
                isInstalled: true
              }
            ]
          },
        ],
        expect: []
      },
      {
        title: 'should return services for not installed slaves',
        sch: [
          {
            componentName: 'c1',
            hosts: [
              {
                isInstalled: false
              },
              {
                isInstalled: true
              }
            ]
          },
          {
            componentName: 'c2',
            hosts: [
              {
                isInstalled: false
              },
              {
                isInstalled: true
              }
            ]
          },
          {
            componentName: 'c4',
            hosts: [
              {
                isInstalled: false
              },
              {
                isInstalled: true
              }
            ]
          }
        ],
        expect: ['s1', 's2']
      }
    ].forEach(function (test) {
          describe(test.title, function () {
            it(function () {
              addServiceController.set('content.slaveComponentHosts', test.sch);
              expect(addServiceController.getServicesBySelectedSlaves()).to.eql(test.expect);
            });
          })
        });

  });
  describe('#setCurrentStep', function() {
    beforeEach(function() {
      sinon.stub(App.clusterStatus, 'setClusterStatus');
    });
    afterEach(function() {
      App.clusterStatus.setClusterStatus.restore();
    });

    it('setClusterStatus should be called', function() {
      addServiceController.setCurrentStep();
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe('#loadCurrentHostGroups', function() {
    beforeEach(function() {
      sinon.stub(addServiceController, 'getDBProperty').returns({});
    });
    afterEach(function() {
      addServiceController.getDBProperty.restore();
    });

    it('should set recommendationsHostGroups', function() {
      addServiceController.loadCurrentHostGroups();
      expect(addServiceController.get('content.recommendationsHostGroups')).to.be.eql({});
    });
  });

  describe('#saveMasterComponentHosts', function() {
    var stepController = Em.Object.create({
      selectedServicesMasters: [
        {
          component_name: 'C1',
          display_name: 'c1',
          selectedHost: 'host1',
          serviceId: 'S1'
        }
      ]
    });
    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'C1',
          workStatus: 'INSTALLED'
        })
      ]);
      sinon.stub(addServiceController, 'setDBProperty');
      addServiceController.saveMasterComponentHosts(stepController);
    });
    afterEach(function() {
      App.HostComponent.find.restore();
      addServiceController.setDBProperty.restore();
    });

    it('setDBProperty should be called', function() {
      expect(addServiceController.setDBProperty.calledWith(
        'masterComponentHosts',
        [
          {
            display_name: 'c1',
            component: 'C1',
            hostName: 'host1',
            serviceId: 'S1',
            isInstalled: true,
            workStatus: 'INSTALLED'
          }
        ]
      )).to.be.true;
    });
    it('masterComponentHosts should be set', function() {
      expect(addServiceController.get('content.masterComponentHosts')).to.be.eql([
        {
          display_name: 'c1',
          component: 'C1',
          hostName: 'host1',
          serviceId: 'S1',
          isInstalled: true,
          workStatus: 'INSTALLED'
        }
      ]);
    });
    it('skipMasterStep should be true when all components installed', function() {
      expect(addServiceController.get('content.skipMasterStep')).to.be.true;
    });
    it('second step should be disabled when skipMasterStep is true', function() {
      expect(addServiceController.get('isStepDisabled').findProperty('step', 2).get('value')).to.be.true;
    });
  });

  describe('#isServiceNotConfigurable', function() {
    beforeEach(function() {
      sinon.stub(App, 'get').returns(['S1']);
    });
    afterEach(function() {
      App.get.restore();
    });

    it('should return true when service does not have configs', function() {
      expect(addServiceController.isServiceNotConfigurable('S1')).to.be.true;
    });
  });

  describe('#skipConfigStep', function() {
    beforeEach(function() {
      sinon.stub(addServiceController, 'isServiceNotConfigurable').returns(false);
    });
    afterEach(function() {
      addServiceController.isServiceNotConfigurable.restore();
    });

    it('should return false when there is configurable service', function() {
      addServiceController.set('content.services', [
        Em.Object.create({
          isSelected: true,
          isInstalled: false,
          serviceName: 'S1'
        })
      ]);
      expect(addServiceController.skipConfigStep()).to.be.false;
    });
  });

  describe('#loadServiceConfigProperties', function() {
    beforeEach(function() {
      sinon.stub(addServiceController, 'loadServices');
      sinon.stub(addServiceController, 'skipConfigStep').returns(true);
    });
    afterEach(function() {
      addServiceController.loadServices.restore();
      addServiceController.skipConfigStep.restore();
    });

    it('loadServices should be called when no service found', function() {
      addServiceController.set('content.services', null);
      addServiceController.loadServiceConfigProperties();
      expect(addServiceController.loadServices.calledOnce).to.be.true;
    });

    it('step 4 should be disabled when skipConfigStep is true', function() {
      addServiceController.reopen({
        currentStep: 5
      });
      addServiceController.loadServiceConfigProperties();
      expect(addServiceController.get('content.skipConfigStep')).to.be.true;
      expect(addServiceController.get('isStepDisabled').findProperty('step', 4).get('value')).to.be.true;
    });
  });

  describe('#loadKerberosDescriptorConfigs', function() {
    beforeEach(function() {
      sinon.stub(addServiceController, 'loadClusterDescriptorStackConfigs').returns({
        then: Em.clb
      });
      sinon.stub(addServiceController, 'loadClusterDescriptorConfigs').returns({
        then: function(clb) {
          clb([{}]);
          return {
            always: Em.clb
          }
        }
      });
      sinon.stub(App, 'get').returns(true);
      sinon.stub(addServiceController, 'mergeDescriptorStackWithConfigs').returns([{}]);
    });
    afterEach(function() {
      addServiceController.loadClusterDescriptorStackConfigs.restore();
      addServiceController.loadClusterDescriptorConfigs.restore();
      addServiceController.mergeDescriptorStackWithConfigs.restore();
      App.get.restore();
    });

    it('loadClusterDescriptorStackConfigs should be called', function() {
      addServiceController.loadKerberosDescriptorConfigs();
      expect(addServiceController.loadClusterDescriptorStackConfigs.calledOnce).to.be.true;
    });
    it('loadClusterDescriptorConfigs should be called', function() {
      addServiceController.loadKerberosDescriptorConfigs();
      expect(addServiceController.loadClusterDescriptorConfigs.calledOnce).to.be.true;
    });
    it('kerberosDescriptorConfigs should be set', function() {
      addServiceController.loadKerberosDescriptorConfigs();
      expect(addServiceController.get('kerberosDescriptorConfigs')).to.be.eql([{}]);
    });
    it('kerberosDescriptorData should be set', function() {
      addServiceController.loadKerberosDescriptorConfigs();
      expect(addServiceController.get('kerberosDescriptorData')).to.be.eql([{}]);
    });
  });

  describe('#saveServiceConfigProperties', function() {
    beforeEach(function() {
      sinon.stub(addServiceController, 'skipConfigStep').returns(true);
    });
    afterEach(function() {
      addServiceController.skipConfigStep.restore();
    });

    it('step 4 should be disabled when skipConfigStep is true', function() {
      addServiceController.reopen({
        currentStep: 2
      });
      addServiceController.saveServiceConfigProperties(Em.Object.create({
        stepConfigs: []
      }));
      expect(addServiceController.get('content.skipConfigStep')).to.be.true;
      expect(addServiceController.get('isStepDisabled').findProperty('step', 4).get('value')).to.be.true;
    });
  });

  describe('#loadSlaveComponentHosts', function() {
    beforeEach(function() {
      sinon.stub(addServiceController, 'getDBProperties').returns({
        hosts: {
          host1: {
            id: 'host1'
          }
        },
        slaveComponentHosts: [
          {
            hosts: [
              {
                host_id: 'host1'
              }
            ]
          }
        ]
      });
      sinon.stub(addServiceController, 'getDBProperty').returns({
        host1: {
          id: 'host1'
        }
      });
    });
    afterEach(function() {
      addServiceController.getDBProperties.restore();
      addServiceController.getDBProperty.restore();
    });

    it('should set installedHosts from db', function() {
      addServiceController.loadSlaveComponentHosts();
      expect(addServiceController.get('content.installedHosts')).to.be.eql({
        host1: {
          id: 'host1'
        }
      });
    });
    it('should set slaveComponentHosts', function() {
      addServiceController.loadSlaveComponentHosts();
      expect(addServiceController.get('content.slaveComponentHosts')).to.be.eql([
        {
          hosts: [
            {
              host_id: 'host1',
              hostName: 'host1'
            }
          ]
        }
      ]);
    });
  });

  describe('#saveClients', function() {
    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns([
        Em.Object.create({
          isClient: true,
          componentName: 'C1',
          displayName: 'c1',
          serviceName: 'S1'
        })
      ]);
      sinon.stub(addServiceController, 'setDBProperty');
    });
    afterEach(function() {
      App.StackServiceComponent.find.restore();
      addServiceController.setDBProperty.restore();
    });

    it('should save client components', function() {
      addServiceController.set('content.services', [
        Em.Object.create({
          isSelected: true,
          isInstalled: false,
          serviceName: 'S1'
        })
      ]);
      addServiceController.saveClients();
      expect(addServiceController.get('content.clients')).to.be.eql([
        {
          component_name: 'C1',
          display_name: 'c1',
          isInstalled: false
        }
      ]);
      expect(addServiceController.setDBProperty.calledWith('clientInfo', [
        {
          component_name: 'C1',
          display_name: 'c1',
          isInstalled: false
        }
      ])).to.be.true;
    });
  });
  
  describe('#clearAllSteps', function() {
    beforeEach(function() {
      sinon.stub(addServiceController, 'clearInstallOptions');
      sinon.stub(addServiceController, 'getCluster').returns({});
    });
    afterEach(function() {
      addServiceController.clearInstallOptions.restore();
      addServiceController.getCluster.restore();
    });
    
    it('clearInstallOptions should be called', function() {
      addServiceController.clearAllSteps();
      expect(addServiceController.clearInstallOptions.calledOnce).to.be.true;
      expect(addServiceController.get('content.cluster')).to.be.eql({});
    });
  });
  
  describe('#finish', function() {
    beforeEach(function() {
      sinon.stub(addServiceController, 'clearAllSteps');
      sinon.stub(addServiceController, 'clearStorageData');
      sinon.stub(addServiceController, 'clearServiceConfigProperties');
      sinon.stub(addServiceController, 'resetDbNamespace');
      addServiceController.finish();
    });
    afterEach(function() {
      addServiceController.clearAllSteps.restore();
      addServiceController.clearStorageData.restore();
      addServiceController.clearServiceConfigProperties.restore();
      addServiceController.resetDbNamespace.restore();
    });
    
    it('clearAllSteps should be called', function() {
      expect(addServiceController.clearAllSteps.calledOnce).to.be.true;
    });
    it('clearStorageData should be called', function() {
      expect(addServiceController.clearStorageData.calledOnce).to.be.true;
    });
    it('clearServiceConfigProperties should be called', function() {
      expect(addServiceController.clearServiceConfigProperties.calledOnce).to.be.true;
    });
    it('resetDbNamespace should be called', function() {
      expect(addServiceController.resetDbNamespace.calledOnce).to.be.true;
    });
  });
  
  describe('#installServices', function() {
    beforeEach(function() {
      sinon.stub(addServiceController, 'installAdditionalClients').returns({
        done: Em.clb
      });
      sinon.stub(addServiceController, 'installSelectedServices');
      addServiceController.installServices();
    });
    afterEach(function() {
      addServiceController.installAdditionalClients.restore();
      addServiceController.installSelectedServices.restore();
    });
    
    it('installAdditionalClients should be called', function() {
      expect(addServiceController.installAdditionalClients.calledOnce).to.be.true;
    });
    it('installSelectedServices should be called', function() {
      expect(addServiceController.installSelectedServices.calledOnce).to.be.true;
    });
    it('oldRequestsId should be empty', function() {
      expect(addServiceController.get('content.cluster.oldRequestsId')).to.be.eql([]);
    });
  });
  
  describe('#installSelectedServices', function() {
    beforeEach(function() {
      sinon.stub(addServiceController, 'getServicesBySelectedSlaves').returns([]);
      sinon.stub(addServiceController, 'generateDataForInstallServices').returns([]);
      sinon.stub(addServiceController, 'installServicesRequest');
    });
    afterEach(function() {
      addServiceController.getServicesBySelectedSlaves.restore();
      addServiceController.generateDataForInstallServices.restore();
      addServiceController.installServicesRequest.restore();
    });
    
    it('installServicesRequest should be called', function() {
      addServiceController.installSelectedServices(Em.K);
      expect(addServiceController.installServicesRequest.calledWith('common.services.update', [])).to.be.true;
    });
  });
  
  describe('#installServicesRequest', function() {
   
    it('request should be made', function() {
      addServiceController.installServicesRequest('wizard.step3.host_info');
      expect(testHelpers.findAjaxRequest('name', 'wizard.step3.host_info')).to.exist;
    });
  });
  
  describe('#getServicesBySelectedSlaves', function() {
    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.Object.create({
        isLoaded: true,
        serviceName: 'S1'
      }));
    });
    afterEach(function() {
      App.StackServiceComponent.find.restore();
    });
    
    it('should return selected services list', function() {
      addServiceController.set('content.slaveComponentHosts', [
        {
          hosts: [{isInstalled: false}],
          componentName: 'C1'
        }
      ]);
      expect(addServiceController.getServicesBySelectedSlaves()).to.be.eql(['S1']);
    });
  });
  
  describe('#installAdditionalClients', function() {
    var mock = {
      addRequest: sinon.spy(),
      start: sinon.spy(),
      queue: [{}]
    };
    
    it('addRequest should be called', function() {
      addServiceController.set('content.additionalClients', [
        {
          componentName: 'C1',
          hostNames: ['host1']
        }
      ]);
      addServiceController.set('installClietsQueue', mock);
      addServiceController.installAdditionalClients();
      expect(mock.addRequest.calledOnce).to.be.true;
    });
    it('start should be called', function() {
      addServiceController.set('content.additionalClients', [
        {
          componentName: 'C1',
          hostNames: ['host1']
        }
      ]);
      addServiceController.set('installClietsQueue', mock);
      addServiceController.installAdditionalClients();
      expect(mock.start.called).to.be.true;
    });
  });
  
  describe('#installClientSuccess', function() {
    it('deferred should be resolved', function() {
      var params = {
        counter: 1,
        deferred: {
          resolve: sinon.spy()
        }
      };
      addServiceController.set('installClientQueueLength', 2);
      addServiceController.installClientSuccess({}, {}, params);
      expect(params.deferred.resolve.calledOnce).to.be.true;
    });
  });
  
  describe('#installClientError', function() {
    it('deferred should be resolved', function() {
      var params = {
        counter: 1,
        deferred: {
          resolve: sinon.spy()
        }
      };
      addServiceController.set('installClientQueueLength', 2);
      addServiceController.installClientError({}, {}, null, null, params);
      expect(params.deferred.resolve.calledOnce).to.be.true;
    });
  });

});
