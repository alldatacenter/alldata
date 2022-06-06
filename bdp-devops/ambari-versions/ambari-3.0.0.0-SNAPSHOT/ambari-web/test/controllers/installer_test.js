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
require('models/cluster');
require('controllers/wizard');
require('controllers/installer');

describe('App.InstallerController', function () {

  var installerController = App.InstallerController.create();

  after(function () {
    installerController.destroy();
  });

  describe('#init', function () {
    var c;
    beforeEach(function () {
      c = App.InstallerController.create({});
    });
    it('all steps are disabled by default', function () {
      expect(c.get('isStepDisabled.length')).to.be.above(0);
      expect(c.get('isStepDisabled').everyProperty('value', true)).to.be.ok;
    });
  });

  describe('#getCluster', function() {
    it ('Should return merged clusterStatusTemplate', function() {
      installerController.set('clusterStatusTemplate', {
        name: 'template'
      });
      expect(installerController.getCluster()).to.eql({
        name: 'template'
      });
    });
  });

  describe('#getHosts', function() {
    it ('Should return empty array', function() {
      expect(installerController.getHosts()).to.eql([]);
    });
  });

  describe('#loadServices', function() {
    it ('Should resolve nothing', function() {
      var res = installerController.loadServices();
      res.then(function(data){
        expect(data).to.be.undefined;
      });
    });
  });

  describe('#cancelInstall', function() {
    var mock = {
      goToAdminView: sinon.spy()
    };
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
    });
    afterEach(function() {
      App.router.get.restore();
    });

    it('goToAdminView should be called', function() {
      var popup = installerController.cancelInstall();
      popup.onPrimary();
      expect(mock.goToAdminView.calledOnce).to.be.true;
    });
  });

  describe('#checkRepoURL', function() {
    var stacks = Em.A([
      Em.Object.create({
        isSelected: false
      }),
      Em.Object.create({
        isSelected: true,
        reload: false,
        id: 'nn-cc',
        stackNameVersion: 'nn-cc',
        repositories: Em.A([
          Em.Object.create({
            isSelected: true,
            isEmpty: false
          })
        ]),
        operatingSystems: Em.A([
          Em.Object.create({
            isSelected: true,
            isEmpty: false,
            repositories: Em.A([
              Em.Object.create({
                isEmpty: false,
                errorTitle: '1',
                errorContent: '1',
                validation: '',
                showRepo: true
              })
            ])
          })
        ])
      })
    ]);
    var wizard = Em.Object.create({
      skipValidationChecked: true
    });
    it ('Should reload installed stacks', function() {

      installerController.set('content.stacks', stacks);
      installerController.checkRepoURL(wizard);

      var expected = [
        {
          "isSelected": false
        },
        {
          "isSelected": true,
          "reload": true,
          "id": "nn-cc",
          "stackNameVersion": 'nn-cc',
          "repositories": [
            {
              "isSelected": true,
              "isEmpty": false
            }
          ],
          "operatingSystems": [
            {
              "isSelected": true,
              "isEmpty": false,
              "repositories": [
                {
                  "isEmpty": false,
                  "errorTitle": "",
                  "errorContent": "",
                  "validation": "INPROGRESS",
                  "showRepo": true
                }
              ]
            }
          ]
        }
      ];

      var res = JSON.parse(JSON.stringify(installerController.get('content.stacks')));

      expect(res).to.be.eql(expected);
    });
  });

  describe('#checkRepoURLSuccessCallback', function() {
    var stacks = Em.A([
      Em.Object.create({
        isSelected: false
      }),
      Em.Object.create({
        isSelected: true,
        reload: false,
        id: 'nn-cc',
        repositories: Em.A([
          Em.Object.create({
            repoId: 11,
            isSelected: true,
            isEmpty: false
          })
        ]),
        operatingSystems: Em.A([
          Em.Object.create({
            isSelected: true,
            isEmpty: false,
            id: 1,
            repositories: Em.A([
              Em.Object.create({
                repoId: 11,
                isEmpty: false,
                errorTitle: '1',
                errorContent: '1',
                validation: '',
                showRepo: true
              })
            ])
          })
        ])
      })
    ]);
    var resolve = false;
    var data = {
      osId: 1,
      repoId: 11,
      dfd: {
        resolve: function() {
          resolve = true;
        }
      }
    };
    it ('Should check stacks for success', function() {

      installerController.set('content.stacks', stacks);
      installerController.checkRepoURLSuccessCallback(null,null,data);

      var expected = [
        {
          "isSelected": false
        },
        {
          "isSelected": true,
          "reload": false,
          "id": "nn-cc",
          "repositories": [
            {
              "repoId": 11,
              "isSelected": true,
              "isEmpty": false
            }
          ],
          "operatingSystems": [
            {
              "isSelected": true,
              "isEmpty": false,
              "id": 1,
              "repositories": [
                {
                  "repoId": 11,
                  "isEmpty": false,
                  "errorTitle": "1",
                  "errorContent": "1",
                  "validation": "OK",
                  "showRepo": true
                }
              ]
            }
          ]
        }
      ];

      var res = JSON.parse(JSON.stringify(installerController.get('content.stacks')));
      expect(resolve).to.be.true;
      expect(res).to.be.eql(expected);
    });
  });

  describe('#checkRepoURLErrorCallback', function() {
    var stacks = Em.A([
      Em.Object.create({
        isSelected: false
      }),
      Em.Object.create({
        isSelected: true,
        reload: false,
        id: 'nn-cc',
        repositories: Em.A([
          Em.Object.create({
            repoId: 11,
            isSelected: true
          })
        ]),
        operatingSystems: Em.A([
          Em.Object.create({
            isSelected: true,
            id: 1,
            repositories: Em.A([
              Em.Object.create({
                repoId: 11,
                errorTitle: '1',
                errorContent: '1',
                validation: ''
              })
            ])
          })
        ])
      })
    ]);
    var resolve = false;
    var data = {
      osId: 1,
      repoId: 11,
      dfd: {
        reject: function() {
          resolve = true;
        }
      }
    };
    it ('Should check stacks for error', function() {

      var req = {
        status: 500,
        statusText: 'error'
      };
      installerController.set('content.stacks', stacks);
      installerController.checkRepoURLErrorCallback(req,{},{},{},data);

      var expected = [
        {
          "isSelected": false
        },
        {
          "isSelected": true,
          "reload": false,
          "id": "nn-cc",
          "repositories": [
            {
              "repoId": 11,
              "isSelected": true
            }
          ],
          "operatingSystems": [
            {
              "isSelected": true,
              "id": 1,
              "repositories": [
                {
                  "repoId": 11,
                  "errorTitle": "500:error",
                  "errorContent": "",
                  "validation": "INVALID"
                }
              ]
            }
          ]
        }
      ];

      var res = JSON.parse(JSON.stringify(installerController.get('content.stacks')));
      expect(resolve).to.be.true;
      expect(res).to.be.eql(expected);
    });
  });

  describe('#setLowerStepsDisable', function() {

    beforeEach(function () {
      var steps = Em.A([
        Em.Object.create({
          step: 0,
          value: false
        }),
        Em.Object.create({
          step: 1,
          value: false
        }),
        Em.Object.create({
          step: 2,
          value: false
        }),
        Em.Object.create({
          step: 3,
          value: false
        }),
        Em.Object.create({
          step: 4,
          value: false
        })
      ]);
      installerController.set('isStepDisabled', steps);
      installerController.setLowerStepsDisable(3);
    });

    it ('Should disable lower steps', function() {
      var expected = [
        {
          "step": 0,
          "value": true
        },
        {
          "step": 1,
          "value": true
        },
        {
          "step": 2,
          "value": true
        },
        {
          "step": 3,
          "value": false
        },
        {
          "step": 4,
          "value": false
        }
      ];
      var res = JSON.parse(JSON.stringify(installerController.get('isStepDisabled')));
      expect(res).to.eql(expected);
    });
  });

  describe('#setStepsEnable', function() {

    beforeEach(function () {
      var steps = Em.A([
        Em.Object.create({
          step: 0,
          value: false
        }),
        Em.Object.create({
          step: 1,
          value: false
        }),
        Em.Object.create({
          step: 2,
          value: false
        }),
        Em.Object.create({
          step: 3,
          value: false
        }),
        Em.Object.create({
          step: 4,
          value: false
        })
      ]);
      installerController.set('isStepDisabled', steps);
      installerController.totalSteps = steps.length - 1;
      installerController.set('currentStep',2);
    });

    it ('Should enable next steps', function() {
      var expected = [
        {
          "step": 0,
          "value": false
        },
        {
          "step": 1,
          "value": true
        },
        {
          "step": 2,
          "value": true
        },
        {
          "step": 3,
          "value": true
        },
        {
          "step": 4,
          "value": true
        }
      ];
      var res = JSON.parse(JSON.stringify(installerController.get('isStepDisabled')));
      expect(res).to.eql(expected);
    });
  });

  describe('#loadMap', function() {

    describe('Should load cluster', function() {
      var loadCluster = false;
      var checker = {
        load: function() {
          loadCluster = true;
        }
      };

      beforeEach(function () {
        installerController.loadMap['0'][0].callback.call(checker);
      });

      it('cluster info is loaded', function () {
        expect(loadCluster).to.be.true;
      });
    });

    describe('Should load stacks', function() {
      var loadStacks = false;
      var checker = {
        loadStacks: function() {
          return {
            done: function(callback) {
              callback(true);
            }
          };
        }
      };

      beforeEach(function () {
        sinon.spy(checker, 'loadStacks');
        installerController.loadMap['1'][0].callback.call(checker);
      });

      afterEach(function() {
        checker.loadStacks.restore();
      });

      it('should call loadStacks, stack info not loaded', function () {
        expect(checker.loadStacks.calledOnce).to.be.true;
      });
    });

    describe('Should load stacks async', function() {
      var checker = {
        loadStacksVersions: Em.K
      };

      beforeEach(function () {
        sinon.stub(checker, 'loadStacksVersions').returns({
          done: Em.clb
        });
      });

      afterEach(function() {
        checker.loadStacksVersions.restore();
      });

      it('stack versions are loaded', function () {
        installerController.loadMap['1'][1].callback.call(checker, true).then(function(data){
          expect(data).to.be.true;
        });
        expect(checker.loadStacksVersions.called).to.be.false;
      });

      it('should call loadStacksVersions, stack versions not loaded', function () {
        installerController.loadMap['1'][1].callback.call(checker, false).then(function(data){
          expect(data).to.be.true;
        });
        expect(checker.loadStacksVersions.calledOnce).to.be.true;
      });
    });

    describe('Should load installOptions', function() {
      var installOptions = false;
      var checker = {
        load: function() {
          installOptions = true;
        }
      };

      beforeEach(function () {
        installerController.loadMap['2'][0].callback.call(checker);
      });

      it('install option are loaded', function () {
        expect(installOptions).to.be.true;
      });
    });

    describe('Should load loadConfirmedHosts', function() {
      var loadConfirmedHosts = false;
      var checker = {
        loadConfirmedHosts: function() {
          loadConfirmedHosts = true;
        }
      };

      beforeEach(function () {
        installerController.loadMap['3'][0].callback.call(checker);
      });

      it('confirmed hosts are loaded', function () {
        expect(loadConfirmedHosts).to.be.true;
      });
    });

    describe('Should load loadServices', function() {
      var loadServices = false;
      var checker = {
        loadServices: function() {
          loadServices = true;
        }
      };

      beforeEach(function () {
        installerController.loadMap['4'][0].callback.call(checker);
      });

      it('services are loaded', function () {
        expect(loadServices).to.be.true;
      });
    });

    describe('Should load loadServices (2)', function() {
      var setSkipSlavesStep = false;
      var loadMasterComponentHosts = false;
      var loadConfirmedHosts = false;
      var loadComponentsFromConfigs = false;
      var loadRecommendations = false;

      var checker = {
        setSkipSlavesStep: function() {
          setSkipSlavesStep = true;
        },
        loadMasterComponentHosts: function() {
          loadMasterComponentHosts = true;
        },
        loadConfirmedHosts: function() {
          loadConfirmedHosts = true;
        },
        loadComponentsFromConfigs: function() {
          loadComponentsFromConfigs = true;
        },
        loadRecommendations: function() {
          loadRecommendations = true;
        }
      };

      beforeEach(function () {
        installerController.loadMap['5'][0].callback.call(checker);
      });

      it('confirmed hosts are loaded', function() {
        expect(loadConfirmedHosts).to.be.true;
      });

      it('`skipSlavesStep` is loaded', function() {
        expect(setSkipSlavesStep).to.be.true;
      });

      it('master components hosts are loaded', function() {
        expect(loadMasterComponentHosts).to.be.true;
      });

      it('components added via configs are loaded', function () {
        expect(loadComponentsFromConfigs).to.be.true;
      });

      it('recommendations are loaded', function() {
        expect(loadRecommendations).to.be.true;
      });

    });

    describe ('Should load serviceConfigGroups', function() {
      var loadServiceConfigGroups = false;
      var loadServiceConfigProperties = false;
      var loadCurrentHostGroups = false;
      var loadRecommendationsConfigs = false;
      var loadComponentsFromConfigs = false;
      var loadConfigThemes = false;

      var checker = {
        loadServiceConfigGroups: function() {
          loadServiceConfigGroups = true;
        },
        loadServiceConfigProperties: function() {
          loadServiceConfigProperties = true;
          return $.Deferred().resolve().promise();
        },
        loadCurrentHostGroups: function() {
          loadCurrentHostGroups = true;
        },
        loadRecommendationsConfigs: function() {
          loadRecommendationsConfigs = true;
        },
        loadComponentsFromConfigs: function() {
          loadComponentsFromConfigs = true;
        },
        loadConfigThemes: function() {
          loadConfigThemes = true;
          return $.Deferred().resolve().promise();
        }
      };

      beforeEach(function () {
        installerController.loadMap['7'][0].callback.call(checker);
      });

      it('config groups are loaded', function () {
        expect(loadServiceConfigGroups).to.be.true;
      });

      it('config properties are loaded', function () {
        expect(loadServiceConfigProperties).to.be.true;
      });

      it('current host groups are loaded', function () {
        expect(loadCurrentHostGroups).to.be.true;
      });

      it('recommendations are loaded', function () {
        expect(loadRecommendationsConfigs).to.be.true;
      });

      it('components added via configs are loaded', function () {
        expect(loadComponentsFromConfigs).to.be.true;
      });

      it('config themes are loaded', function () {
        expect(loadConfigThemes).to.be.true;
      });

    });

    describe('Should load clients', function() {
      var loadSlaveComponentHosts = false;
      var loadClients = false;
      var loadRecommendations = false;
      var loadComponentsFromConfigs = false;

      var checker = {
        loadSlaveComponentHosts: function() {
          loadSlaveComponentHosts = true;
        },
        loadClients: function() {
          loadClients = true;
        },
        loadComponentsFromConfigs: function() {
          loadComponentsFromConfigs = true;
        },
        loadRecommendations: function() {
          loadRecommendations = true;
        }
      };

      beforeEach(function () {
        installerController.loadMap['6'][0].callback.call(checker);
      });

      it('slave components hosts are loaded', function () {
        expect(loadSlaveComponentHosts).to.be.true;
      });

      it('clients are loaded', function () {
        expect(loadClients).to.be.true;
      });

      it('components added via configs are loaded', function () {
        expect(loadComponentsFromConfigs).to.be.true;
      });

      it('recommendations are loaded', function () {
        expect(loadRecommendations).to.be.true;
      });

    });

  });

  describe('#removeHosts', function() {
    var hostsDb = {
      'h1': {},
      'h2': {},
      'h3': {},
      'h4': {}
    };
    beforeEach(function () {
      sinon.stub(installerController, 'getDBProperty').returns(hostsDb);
    });
    afterEach(function () {
      installerController.getDBProperty.restore();
    });
    it ('Should remove hosts from the list', function() {
      var hosts = Em.A([
        {
          name: 'h1'
        },
        {
          name: 'h2'
        },
        {
          name: 'h3'
        }
      ]);
      installerController.removeHosts(hosts);
      expect(hostsDb).to.eql({
        'h4': {}
      });
    });
  });

  describe('#allHosts', function() {
    it ('Should return hosts', function() {
      var hosts = {
        'h1': {
          hostComponents: Em.A([])
        }
      };
      var masterComponentHosts = Em.A([
        {
          hostName: 'h1',
          component: 'component',
          display_name: 'n1'
        }
      ]);
      var slaveComponentHosts = Em.A([
        {
          hosts: Em.A([
          {
            hostName: 'h1'
          }
          ])
        }
      ]);
      installerController.set('content.hosts', hosts);
      installerController.set('content.masterComponentHosts', masterComponentHosts);
      installerController.set('content.slaveComponentHosts', slaveComponentHosts);
      var res = JSON.parse(JSON.stringify(installerController.get('allHosts')));
      expect(res).to.eql([
        {
          "hostComponents": [
            {
              "componentName": "component",
              "displayName": "n1"
            },
            {}
          ]
        }
      ]);
    });
  });

  describe('#saveServices', function() {
    it ('Should return correct names', function() {
      var stepController = Em.A([
        Em.Object.create({
          isInstalled: true,
          isSelected: true,
          serviceName: 'i1'
        }),
        Em.Object.create({
          isInstalled: false,
          isSelected: true,
          serviceName: 'i2'
        }),
        Em.Object.create({
          isInstalled: true,
          isSelected: false,
          serviceName: 'i3'
        })
      ]);
      installerController.saveServices(stepController);
      expect(installerController.get('content.selectedServiceNames')).to.eql(['i1','i2']);
      expect(installerController.get('content.installedServiceNames')).to.eql(['i1','i3']);
    });
  });

  describe('#saveClients', function() {
    var stepController;

    beforeEach(function () {
      stepController = Em.Object.create({
        content: Em.A([
          Em.Object.create({
            isInstalled: true,
            isSelected: true,
            serviceName: 'i1',
            serviceComponents: Em.A([
              Em.Object.create({
                isClient: true,
                componentName: 'name',
                displayName: 'dname'
              })
            ])
          }),
          Em.Object.create({
            isInstalled: false,
            isSelected: true,
            serviceName: 'i2',
            serviceComponents: Em.A([
              Em.Object.create({
                isClient: false
              })
            ])
          }),
          Em.Object.create({
            isInstalled: true,
            isSelected: false,
            serviceName: 'i3',
            serviceComponents: Em.A([
              Em.Object.create({
                isClient: false
              })
            ])
          })
        ])
      });
    });
    it ('Should return correct clients names', function() {
      installerController.saveClients(stepController);
      var res = JSON.parse(JSON.stringify(installerController.get('content.clients')));
      expect(res).to.eql([
        {
          "component_name": "name",
          "display_name": "dname",
          "isInstalled": false
        }
      ]);
    });
  });

  describe('#saveMasterComponentHosts', function() {
    beforeEach(function () {
      sinon.stub(installerController, 'getDBProperty').returns({
        'h1': {
          id: 11
        },
        'h3': {
          id: 13
        },
        'h2': {
          id: 12
        }
      });
    });
    afterEach(function () {
      installerController.getDBProperty.restore();
    });
    it ('Should return hosts', function() {
      var stepController = Em.Object.create({
        selectedServicesMasters: Em.A([
          Em.Object.create({
            display_name: 'n1',
            component_name: 'c1',
            serviceId: 1,
            selectedHost: 'h1'
          })
        ])
      });
      installerController.saveMasterComponentHosts(stepController);
      expect(installerController.get('content.masterComponentHosts')).to.eql([
        {
          "display_name": "n1",
          "component": "c1",
          "serviceId": 1,
          "isInstalled": false,
          "host_id": 11
        }
      ]);
    });
  });

  describe('#loadConfirmedHosts', function() {
    beforeEach(function () {
      sinon.stub(installerController, 'getDBProperty').returns({
        'h1': {
          id: 11
        },
        'h3': {
          id: 13
        },
        'h2': {
          id: 12
        }
      });
    });
    afterEach(function () {
      installerController.getDBProperty.restore();
    });
    it ('Should load hosts from db', function() {
      installerController.loadConfirmedHosts();
      expect(installerController.get('content.hosts')).to.eql({
        'h1': {
          id: 11
        },
        'h3': {
          id: 13
        },
        'h2': {
          id: 12
        }
      });
    });
  });

  describe('#loadMasterComponentHosts', function() {
    beforeEach(function () {
      sinon.stub(installerController, 'getDBProperties', function() {
        return {
          masterComponentHosts: Em.A([
            {
              hostName: '',
              host_id: 11
            }
          ]),
          hosts: {
            'h1': {
              id: 11
            },
            'h3': {
              id: 13
            },
            'h2': {
              id: 12
            }
          }
        }
      });
    });
    afterEach(function () {
      installerController.getDBProperties.restore();
    });
    it ('Should load hosts', function() {
      installerController.loadMasterComponentHosts();
      expect(installerController.get('content.masterComponentHosts')).to.eql([
        {
          "hostName": "h1",
          "host_id": 11
        }
      ]);
    });
  });

  describe('#loadSlaveComponentHosts', function() {
    beforeEach(function () {
      sinon.stub(installerController, 'getDBProperties', function() {
        return {
          hosts: {
            'h1': {
              id: 11
            },
            'h3': {
              id: 13
            },
            'h2': {
              id: 12
            }
          },
          slaveComponentHosts: Em.A([
            {
              hosts: Em.A([
                {
                  hostName: '',
                  host_id: 11
                }
              ])
            }
          ])
        };
      });
    });
    afterEach(function () {
      installerController.getDBProperties.restore();
    });
    it ('Should load slave hosts', function() {
      installerController.loadSlaveComponentHosts();
      expect(installerController.get('content.slaveComponentHosts')).to.eql([
        {
          "hosts": [
            {
              "hostName": "h1",
              "host_id": 11
            }
          ]
        }
      ]);
    });
  });

  describe('#getServerVersionSuccessCallback', function () {

    var cases = [
        {
          osFamily: 'redhat5',
          expected: false
        },
        {
          osFamily: 'redhat6',
          expected: true
        },
        {
          osFamily: 'suse11',
          expected: false
        }
      ],
      title = 'App.isManagedMySQLForHiveEnabled should be {0} for {1}';

    cases.forEach(function (item) {
      it(title.format(item.expected, item.osFamily), function () {
        installerController.getServerVersionSuccessCallback({
          'RootServiceComponents': {
            'component_version': '',
            'properties': {
              'server.os_family': item.osFamily
            }
          }
        });
        expect(App.get('isManagedMySQLForHiveEnabled')).to.equal(item.expected);
      });
    });

  });

  describe('#validateJDKVersion', function() {
    var tests = [
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.8'
        },
        successCallbackCalled: false,
        popupCalled: true,
        stacks: [Em.Object.create({
          minJdkVersion: '1.6',
          maxJdkVersion: '1.7',
          isSelected: true
        })],
        m: 'JDK 1.8, stack supports 1.6-1.7 popup should be displayed'
      },
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.8'
        },
        successCallbackCalled: true,
        popupCalled: false,
        stacks: [Em.Object.create({
          minJdkVersion: '1.6',
          maxJdkVersion: '1.8',
          isSelected: true
        })],
        m: 'JDK 1.8, stack supports 1.7-1.8 procceed installation without warning'
      },
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.5'
        },
        successCallbackCalled: false,
        popupCalled: true,
        stacks: [Em.Object.create({
          minJdkVersion: '1.6',
          maxJdkVersion: '1.8',
          isSelected: true
        })],
        m: 'JDK 1.5, stack supports 1.6-1.8, popup should be displayed'
      },
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.5'
        },
        successCallbackCalled: true,
        popupCalled: false,
        stacks: [Em.Object.create({
          minJdkVersion: null,
          maxJdkVersion: null,
          isSelected: true
        })],
        m: 'JDK 1.5, stack supports max and min are null, procceed installation without warning'
      },
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.5'
        },
        successCallbackCalled: true,
        popupCalled: false,
        stacks: [Em.Object.create({
          minJdkVersion: '1.5',
          maxJdkVersion: null,
          isSelected: true
        })],
        m: 'JDK 1.5, stack supports max is missed and min is 1.5, procceed installation without warning'
      },
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.6'
        },
        successCallbackCalled: false,
        popupCalled: true,
        stacks: [Em.Object.create({
          minJdkVersion: '1.5',
          maxJdkVersion: null,
          isSelected: true
        })],
        m: 'JDK 1.6, stack supports max is missed and min is 1.5, popup should be displayed'
      },
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.5'
        },
        successCallbackCalled: true,
        popupCalled: false,
        stacks: [Em.Object.create({
          minJdkVersion: null,
          maxJdkVersion: '1.5',
          isSelected: true
        })],
        m: 'JDK 1.5, stack supports max 1.5 and min is missed, procceed installation without warning'
      },
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.5'
        },
        successCallbackCalled: false,
        popupCalled: true,
        stacks: [Em.Object.create({
          minJdkVersion: null,
          maxJdkVersion: '1.8',
          isSelected: true
        })],
        m: 'JDK 1.5, stack supports max 1.8 and min is missed, popup should be displayed'
      },
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.8'
        },
        successCallbackCalled: true,
        popupCalled: false,
        stacks: [Em.Object.create({
          isSelected: true
        })],
        m: 'JDK 1.8, min, max jdk missed in stack definition, procceed installation without warning'
      },
      {
        isCustomJDK: true,
        ambariProperties: {
          'java.version': '1.8'
        },
        successCallbackCalled: true,
        popupCalled: false,
        stacks: [Em.Object.create({
          minJdkVersion: '1.6',
          maxJdkVersion: '1.8',
          isSelected: true
        })],
        m: 'JDK 1.8, custom jdk location used, procceed installation without warning'
      }
    ];

    tests.forEach(function(test) {

      describe(test.m, function() {

        var successCallback;

        beforeEach(function () {
          sinon.stub(App.Stack, 'find').returns(test.stacks);
          sinon.stub(App.router, 'get').withArgs('clusterController.isCustomJDK').returns(test.isCustomJDK)
            .withArgs('clusterController.ambariProperties').returns(test.ambariProperties);
          sinon.stub(App, 'showConfirmationPopup', Em.K);
          successCallback = sinon.spy();
          installerController.validateJDKVersion(successCallback);
        });

        afterEach(function () {
          App.router.get.restore();
          App.Stack.find.restore();
          App.showConfirmationPopup.restore();
        });

        it('successCallback is ' + (test.successCallbackCalled ? '' : 'not') + ' called', function () {
          expect(successCallback.called).to.be.equal(test.successCallbackCalled);
        });

        it('App.showConfirmationPopup. is ' + (test.popupCalled ? '' : 'not') + ' called', function () {
          expect(App.showConfirmationPopup.called).to.be.equal(test.popupCalled);
        });

      });
    });
  });

  describe('#postVersionDefinitionFileErrorCallback', function () {

    beforeEach(function () {
      sinon.stub(App, 'showAlertPopup', Em.K);
    });

    afterEach(function () {
      App.showAlertPopup.restore();
    });

    it('should delete VDF-data', function () {
      App.db.setLocalRepoVDFData({});
      expect(App.db.getLocalRepoVDFData()).to.not.be.an.object;
      installerController.postVersionDefinitionFileErrorCallback({}, {}, {}, {}, {dfd: $.Deferred()});
      expect(App.db.getLocalRepoVDFData()).to.be.undefined;
    });

  });

  describe('#finish', function() {
    beforeEach(function() {
      sinon.stub(installerController, 'setCurrentStep');
      sinon.stub(installerController, 'clearStorageData');
      sinon.stub(installerController, 'clearServiceConfigProperties');
      sinon.stub(App.themesMapper, 'resetModels');
      installerController.finish();
    });
    afterEach(function() {
      installerController.setCurrentStep.restore();
      installerController.clearStorageData.restore();
      installerController.clearServiceConfigProperties.restore();
      App.themesMapper.resetModels.restore();
    });

    it('setCurrentStep should be called', function() {
      expect(installerController.setCurrentStep.calledWith('0')).to.be.true;
    });

    it('clearStorageData should be called', function() {
      expect(installerController.clearStorageData.calledOnce).to.be.true;
    });

    it('clearServiceConfigProperties should be called', function() {
      expect(installerController.clearServiceConfigProperties.calledOnce).to.be.true;
    });

    it('App.themesMapper.resetModels should be called', function() {
      expect(App.themesMapper.resetModels.calledOnce).to.be.true;
    });
  });

  describe('#showStackErrorAndSkipStepIfNeeded', function () {
    afterEach(function () {
      App.Stack.find.restore();
      App.showAlertPopup.restore();
    });
    it('Should show alert popup and decrement loadStacksRequestsCounter property', function() {
      sinon.stub(App.Stack, 'find').returns([]);
      sinon.stub(App, 'showAlertPopup');
      installerController.set('loadStacksRequestsCounter', 2);
      installerController.showStackErrorAndSkipStepIfNeeded({
        Versions: {
          stack_name: 'HDP',
          stack_version: '3.0',
          'stack-errors': ['Error text1', 'Error text2']
        }
      });
      var header = Em.I18n.t('installer.step1.useLocalRepo.getSurpottedOs.stackError.title').format('HDP', '3.0');
      var body = 'Error text1. Error text2';
      expect(App.showAlertPopup.calledOnce);
      expect(App.showAlertPopup.calledWith(header, body));
      expect(installerController.get('loadStacksRequestsCounter')).to.equal(1);
    });

    it('Should not return back if we have stacks available and show one popup', function() {
      var controller = Em.Object.create({hasNotStacksAvailable: false});
      sinon.stub(App.Stack, 'find').returns([{}]);
      sinon.stub(App, 'showAlertPopup');
      sinon.stub(App.router, 'get').returns(controller);
      installerController.set('loadStacksRequestsCounter', 1);
      installerController.showStackErrorAndSkipStepIfNeeded({
        Versions: {
          stack_name: 'HDP',
          stack_version: '3.0',
          'stack-errors': ['Error text1', 'Error text2']
        }
      });
      expect(App.showAlertPopup.calledOnce);
      expect( controller.get('hasNotStacksAvailable') ).to.equal(false);
      App.router.get.restore();
    });

    it('Should return back if we have stacks available and show two popups', function() {
      var controller = Em.Object.create({hasNotStacksAvailable: false});
      sinon.stub(App.Stack, 'find').returns([]);
      sinon.stub(App, 'showAlertPopup');
      sinon.stub(App.router, 'get').returns(controller);
      sinon.stub(App.router, 'send');
      installerController.set('loadStacksRequestsCounter', 1);
      installerController.showStackErrorAndSkipStepIfNeeded({
        Versions: {
          stack_name: 'HDP',
          stack_version: '3.0',
          'stack-errors': ['Error text1', 'Error text2']
        }
      });
      expect(App.showAlertPopup.calledTwice);
      expect(App.router.send.calledWith('gotoStep0')).to.be.true;
      expect( controller.get('hasNotStacksAvailable') ).to.equal(true);
      App.router.send.restore();
      App.router.get.restore();
    });
  });
});
