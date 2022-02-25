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
require('views/common/quick_view_link_view');
require('models/host_component');
require('models/stack_service_component');
App.auth = ["AMBARI.ADD_DELETE_CLUSTERS", "AMBARI.ASSIGN_ROLES", "AMBARI.EDIT_STACK_REPOS", "AMBARI.MANAGE_GROUPS", "AMBARI.MANAGE_STACK_VERSIONS", "AMBARI.MANAGE_USERS", "AMBARI.MANAGE_VIEWS", "AMBARI.RENAME_CLUSTER", "SERVICE.SET_SERVICE_USERS_GROUPS", "CLUSTER.TOGGLE_ALERTS", "CLUSTER.TOGGLE_KERBEROS", "CLUSTER.UPGRADE_DOWNGRADE_STACK", "CLUSTER.VIEW_ALERTS", "CLUSTER.VIEW_CONFIGS", "CLUSTER.VIEW_METRICS", "CLUSTER.VIEW_STACK_DETAILS", "CLUSTER.VIEW_STATUS_INFO", "HOST.ADD_DELETE_COMPONENTS", "HOST.ADD_DELETE_HOSTS", "HOST.TOGGLE_MAINTENANCE", "HOST.VIEW_CONFIGS", "HOST.VIEW_METRICS", "HOST.VIEW_STATUS_INFO", "SERVICE.ADD_DELETE_SERVICES", "SERVICE.COMPARE_CONFIGS", "SERVICE.DECOMMISSION_RECOMMISSION", "SERVICE.ENABLE_HA", "SERVICE.MANAGE_CONFIG_GROUPS", "SERVICE.MODIFY_CONFIGS", "SERVICE.MOVE", "SERVICE.RUN_CUSTOM_COMMAND", "SERVICE.RUN_SERVICE_CHECK", "SERVICE.START_STOP", "SERVICE.TOGGLE_ALERTS", "SERVICE.TOGGLE_MAINTENANCE", "SERVICE.VIEW_ALERTS", "SERVICE.VIEW_CONFIGS", "SERVICE.VIEW_METRICS", "SERVICE.VIEW_STATUS_INFO", "VIEW.USE"];

describe('App', function () {

  describe('#stackVersionURL', function () {

    App.set('defaultStackVersion', "HDP-1.2.2");
    App.set('currentStackVersion', "HDP-1.2.2");

    var testCases = [
      {
        title: 'if currentStackVersion and defaultStackVersion are empty then stackVersionURL should contain prefix',
        currentStackVersion: '',
        defaultStackVersion: '',
        result: '/stacks/HDP/versions/'
      },
      {
        title: 'if currentStackVersion is "HDP-1.3.1" then stackVersionURL should be "/stacks/HDP/versions/1.3.1"',
        currentStackVersion: 'HDP-1.3.1',
        defaultStackVersion: '',
        result: '/stacks/HDP/versions/1.3.1'
      },
      {
        title: 'if defaultStackVersion is "HDP-1.3.1" then stackVersionURL should be "/stacks/HDP/versions/1.3.1"',
        currentStackVersion: '',
        defaultStackVersion: 'HDP-1.3.1',
        result: '/stacks/HDP/versions/1.3.1'
      },
      {
        title: 'if defaultStackVersion and currentStackVersion are different then stackVersionURL should have currentStackVersion value',
        currentStackVersion: 'HDP-1.3.2',
        defaultStackVersion: 'HDP-1.3.1',
        result: '/stacks/HDP/versions/1.3.2'
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        App.set('defaultStackVersion', test.defaultStackVersion);
        App.set('currentStackVersion', test.currentStackVersion);
        expect(App.get('stackVersionURL')).to.equal(test.result);
        App.set('defaultStackVersion', "HDP-1.2.2");
        App.set('currentStackVersion', "HDP-1.2.2");
      });
    });
  });

  describe('#falconServerURL', function () {

    var testCases = [
      {
        title: 'No services installed, url should be empty',
        service: Em.A([]),
        result: ''
      },
      {
        title: 'FALCON is not installed, url should be empty',
        service: Em.A([
          {
            serviceName: 'HDFS'
          }
        ]),
        result: ''
      },
      {
        title: 'FALCON is installed, url should be "host1"',
        service: Em.A([
          Em.Object.create({
            serviceName: 'FALCON',
            hostComponents: [
              Em.Object.create({
                componentName: 'FALCON_SERVER',
                hostName: 'host1'
              })
            ]
          })
        ]),
        result: 'host1'
      }
    ];

    testCases.forEach(function (test) {
      describe(test.title, function () {

        beforeEach(function () {
          sinon.stub(App.Service, 'find', function () {
            return test.service;
          });
        });

        afterEach(function () {
          App.Service.find.restore();
        });

        it('App.falconServerURL is ' + test.result, function () {
          expect(App.get('falconServerURL')).to.equal(test.result);
        });

      });
    });
  });

  describe('#currentStackVersionNumber', function () {

    var testCases = [
      {
        title: 'if currentStackVersion is empty then currentStackVersionNumber should be empty',
        currentStackVersion: '',
        result: ''
      },
      {
        title: 'if currentStackVersion is "HDP-1.3.1" then currentStackVersionNumber should be "1.3.1',
        currentStackVersion: 'HDP-1.3.1',
        result: '1.3.1'
      },
      {
        title: 'if currentStackVersion is "HDPLocal-1.3.1" then currentStackVersionNumber should be "1.3.1',
        currentStackVersion: 'HDPLocal-1.3.1',
        result: '1.3.1'
      }
    ];
    before(function () {
      App.set('defaultStackVersion', '');
    });
    after(function () {
      App.set('defaultStackVersion', 'HDP-2.0.5');
    });
    testCases.forEach(function (test) {
      it(test.title, function () {
        App.set('currentStackVersion', test.currentStackVersion);
        expect(App.get('currentStackVersionNumber')).to.equal(test.result);
        App.set('currentStackVersion', "HDP-1.2.2");
      });
    });
  });

  describe('#isHaEnabled when HDFS is installed:', function () {

    beforeEach(function () {
      sinon.stub(App.Service, 'find').returns(Em.Object.create({'isLoaded': true}));
      this.mock = sinon.stub(App.MasterComponent, 'find');
    });
    afterEach(function () {
      App.Service.find.restore();
      this.mock.restore();
    });

    it('if one SECONDARY_NAMENODE and one NAMENODE then isHaEnabled should be false', function () {
      this.mock.withArgs('SECONDARY_NAMENODE').returns(Em.Object.create({totalCount: 1}));
      this.mock.withArgs('NAMENODE').returns(Em.Object.create({totalCount: 1}));
      App.propertyDidChange('isHaEnabled');
      expect(App.get('isHaEnabled')).to.be.false;
    });
    it('if no SECONDARY_NAMENODE and two NAMENODE then isHaEnabled should be true', function () {
      this.mock.withArgs('SECONDARY_NAMENODE').returns(Em.Object.create({totalCount: 0}));
      this.mock.withArgs('NAMENODE').returns(Em.Object.create({totalCount: 2}));
      App.propertyDidChange('isHaEnabled');
      expect(App.get('isHaEnabled')).to.be.true;
    });
    it('if no SECONDARY_NAMENODE and no NAMENODE then isHaEnabled should be false', function () {
      this.mock.withArgs('SECONDARY_NAMENODE').returns(Em.Object.create({totalCount: 0}));
      this.mock.withArgs('NAMENODE').returns(Em.Object.create({totalCount: 0}));
      App.propertyDidChange('isHaEnabled');
      expect(App.get('isHaEnabled')).to.be.false;
    });
  });

  describe('#isHaEnabled when HDFS is not installed:', function () {

    beforeEach(function () {
      sinon.stub(App.Service, 'find').returns(Em.Object.create({'isLoaded': false}));
    });
    afterEach(function () {
      App.Service.find.restore();
    });

    it('if hadoop stack version higher than 2 but HDFS not installed then isHaEnabled should be false', function () {
      App.set('currentStackVersion', 'HDP-2.1');
      expect(App.get('isHaEnabled')).to.equal(false);
      App.set('currentStackVersion', "HDP-1.2.2");
    });

  });


  describe('#services', function () {
    var stackServices = [
      Em.Object.create({
        serviceName: 'S1',
        isClientOnlyService: true
      }),
      Em.Object.create({
        serviceName: 'S2',
        hasClient: true
      }),
      Em.Object.create({
        serviceName: 'S3',
        hasMaster: true
      }),
      Em.Object.create({
        serviceName: 'S4',
        hasSlave: true
      }),
      Em.Object.create({
        serviceName: 'S5',
        isNoConfigTypes: true
      }),
      Em.Object.create({
        serviceName: 'S6',
        isMonitoringService: true
      }),
      Em.Object.create({
        serviceName: 'S7'
      })
    ];

    beforeEach(function () {
      sinon.stub(App.StackService, 'find', function () {
        return stackServices;
      });
    });

    afterEach(function () {
      App.StackService.find.restore();
    });

    it('App.services.all', function () {
      expect(App.get('services.all')).to.eql(['S1', 'S2', 'S3', 'S4', 'S5', 'S6', 'S7']);
    });

    it('App.services.clientOnly', function () {
      expect(App.get('services.clientOnly')).to.eql(['S1']);
    });

    it('App.services.hasClient', function () {
      expect(App.get('services.hasClient')).to.eql(['S2']);
    });

    it('App.services.hasMaster', function () {
      expect(App.get('services.hasMaster')).to.eql(['S3']);
    });

    it('App.services.hasSlave', function () {
      expect(App.get('services.hasSlave')).to.eql(['S4']);
    });

    it('App.services.noConfigTypes', function () {
      expect(App.get('services.noConfigTypes')).to.eql(['S5']);
    });

    it('App.services.monitoring', function () {
      expect(App.get('services.monitoring')).to.eql(['S6']);
    });
  });


  describe('#components', function () {
    var i = 0,
      testCases = [
        {
          key: 'allComponents',
          data: [
            Em.Object.create({
              componentName: 'C1'
            })
          ],
          result: ['C1']
        },
        {
          key: 'reassignable',
          data: [
            Em.Object.create({
              componentName: 'C2',
              isReassignable: true
            })
          ],
          result: ['C2']
        },
        {
          key: 'restartable',
          data: [
            Em.Object.create({
              componentName: 'C3',
              isRestartable: true
            })
          ],
          result: ['C3']
        },
        {
          key: 'deletable',
          data: [
            Em.Object.create({
              componentName: 'C4',
              isDeletable: true
            })
          ],
          result: ['C4']
        },
        {
          key: 'rollinRestartAllowed',
          data: [
            Em.Object.create({
              componentName: 'C5',
              isRollinRestartAllowed: true
            })
          ],
          result: ['C5']
        },
        {
          key: 'decommissionAllowed',
          data: [
            Em.Object.create({
              componentName: 'C6',
              isDecommissionAllowed: true
            })
          ],
          result: ['C6']
        },
        {
          key: 'refreshConfigsAllowed',
          data: [
            Em.Object.create({
              componentName: 'C7',
              isRefreshConfigsAllowed: true
            })
          ],
          result: ['C7']
        },
        {
          key: 'addableToHost',
          data: [
            Em.Object.create({
              componentName: 'C8',
              isAddableToHost: true
            })
          ],
          result: ['C8']
        },
        {
          key: 'addableMasterInstallerWizard',
          data: [
            Em.Object.create({
              componentName: 'C9',
              isMasterAddableInstallerWizard: true,
              showAddBtnInInstall: true
            })
          ],
          result: ['C9']
        },
        {
          key: 'multipleMasters',
          data: [
            Em.Object.create({
              componentName: 'C10',
              isMasterWithMultipleInstances: true
            })
          ],
          result: ['C10']
        },
        {
          key: 'slaves',
          data: [
            Em.Object.create({
              componentName: 'C11',
              isSlave: true
            })
          ],
          result: ['C11']
        },
        {
          key: 'clients',
          data: [
            Em.Object.create({
              componentName: 'C12',
              isClient: true
            })
          ],
          result: ['C12']
        }
      ];

    beforeEach(function () {
      sinon.stub(App.StackServiceComponent, 'find', function () {
        return testCases[i].data;
      });
    });

    afterEach(function () {
      i++;
      App.StackServiceComponent.find.restore();
    });

    testCases.forEach(function (test) {
      it(test.key + ' should contain ' + test.result, function () {
        var key = 'components.' + test.key;
        App.components.propertyDidChange(test.key);
        expect(App.get(key)).to.eql(test.result);
      });
    });
  });

  describe('#upgradeIsRunning', function () {

    Em.A([
        {
          upgradeState: 'IN_PROGRESS',
          m: 'should be true (1)',
          e: true
        },
        {
          upgradeState: 'HOLDING',
          m: 'should be true (2)',
          e: true
        },
        {
          upgradeState: 'FAKE',
          m: 'should be false',
          e: false
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          App.set('upgradeState', test.upgradeState);
          expect(App.get('upgradeIsRunning')).to.equal(test.e);
        });
      });

  });

  describe('#upgradeSuspended', function () {
    var cases = [
      {
        upgradeState: 'NOT_REQUIRED',
        isSuspended: false,
        upgradeSuspended: false
      },
      {
        upgradeState: 'ABORTED',
        isSuspended: false,
        upgradeSuspended: false
      },
      {
        upgradeState: 'ABORTED',
        isSuspended: true,
        upgradeSuspended: true
      }
    ];

    beforeEach(function() {
      this.mock = sinon.stub(App.router, 'get');
    });
    afterEach(function() {
      this.mock.restore();
    });

    cases.forEach(function (test) {
      it(test.upgradeState + ", isSuspended=" + test.isSuspended, function () {
        App.set('upgradeState', test.upgradeState);
        this.mock.returns(test.isSuspended);
        App.propertyDidChange('upgradeSuspended');
        expect(App.get('upgradeSuspended')).to.equal(test.upgradeSuspended);
      });
    });
  });

  describe('#upgradeAborted', function () {

    var cases = [
      {
        upgradeState: 'NOT_REQUIRED',
        isSuspended: false,
        upgradeAborted: false
      },
      {
        upgradeState: 'ABORTED',
        isSuspended: true,
        upgradeAborted: false
      },
      {
        upgradeState: 'ABORTED',
        isSuspended: false,
        upgradeAborted: true
      }
    ];

    beforeEach(function() {
      this.mock = sinon.stub(App.router, 'get');
    });
    afterEach(function() {
      this.mock.restore();
    });

    cases.forEach(function (test) {
      it(test.upgradeState + ", isSuspended=" + test.isSuspended, function () {
        App.set('upgradeState', test.upgradeState);
        this.mock.returns(test.isSuspended);
        App.propertyDidChange('upgradeAborted');
        expect(App.get('upgradeAborted')).to.equal(test.upgradeAborted);
      });
    });
  });

  describe('#wizardIsNotFinished', function () {
    var cases = [
      {
        upgradeState: 'NOT_REQUIRED',
        wizardIsNotFinished: false
      },
      {
        upgradeState: 'IN_PROGRESS',
        wizardIsNotFinished: true
      },
      {
        upgradeState: 'HOLDING',
        wizardIsNotFinished: true
      },
      {
        upgradeState: 'HOLDING_TIMEDOUT',
        wizardIsNotFinished: true
      },
      {
        upgradeState: 'ABORTED',
        wizardIsNotFinished: true
      }
    ];

    cases.forEach(function (item) {
      it(item.upgradeState, function () {
        App.set('upgradeState', item.upgradeState);
        App.propertyDidChange('wizardIsNotFinished');
        expect(App.get('wizardIsNotFinished')).to.equal(item.wizardIsNotFinished);
      });
    });
  });

  describe("#upgradeHolding", function () {
    var cases = [
      {
        upgradeState: 'NOT_REQUIRED',
        upgradeAborted: false,
        upgradeHolding: false
      },
      {
        upgradeState: 'HOLDING',
        upgradeAborted: false,
        upgradeHolding: true
      },
      {
        upgradeState: 'HOLDING_FAILED',
        upgradeAborted: false,
        upgradeHolding: true
      },
      {
        upgradeState: 'NOT_REQUIRED',
        upgradeAborted: true,
        upgradeHolding: true
      }
    ];

    beforeEach(function() {
      this.mock = sinon.stub(App.router, 'get');
    });
    afterEach(function() {
      this.mock.restore();
    });

    cases.forEach(function (test) {
      it(test.upgradeState + ", upgradeAborted=" + test.upgradeAborted, function () {
        App.reopen({
          upgradeAborted: test.upgradeAborted,
          upgradeState: test.upgradeState
        });
        App.propertyDidChange('upgradeHolding');
        expect(App.get('upgradeHolding')).to.equal(test.upgradeHolding);
      });
    });
  });
});
