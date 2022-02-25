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
var c;
require('utils/ajax/ajax');
require('utils/http_client');
require('models/host');
require('controllers/wizard/step3_controller');
var testHelpers = require('test/helpers');

function getController() {
  return App.WizardStep3Controller.create({
    content: Em.Object.create({installedHosts: Em.A([]), installOptions: {}, controllerName: ''}),
    wizardController: App.InstallerController.create(),
    setRegistrationInProgressOnce: Em.K,
    disablePreviousSteps: Em.K
  });
}

describe('App.WizardStep3Controller', function () {

  beforeEach(function () {

    c = getController();

    sinon.stub(App.db, 'getDisplayLength', Em.K);
    sinon.stub(App.db, 'getFilterConditions').returns([]);
    sinon.stub(App.router, 'send', Em.K);
    App.set('router.nextBtnClickInProgress', false);
  });

  afterEach(function () {
    App.db.getDisplayLength.restore();
    App.router.send.restore();
    App.db.getFilterConditions.restore();
    App.set('router.nextBtnClickInProgress', false);
  });

  App.TestAliases.testAsComputedGt(getController(), 'isHostHaveWarnings', 'warnings.length', 0);

  App.TestAliases.testAsComputedEqual(getController(), 'isAddHostWizard', 'content.controllerName', 'addHostController');

  App.TestAliases.testAsComputedIfThenElse(getController(), 'registrationTimeoutSecs', 'content.installOptions.manualInstall', 15, 120);

  App.TestAliases.testAsComputedAnd(getController(), 'isWarningsLoaded', ['isJDKWarningsLoaded', 'isHostsWarningsLoaded']);

  describe('#getAllRegisteredHostsCallback', function () {

    it('One host is already in the cluster, one host is registered', function () {
      c.get('content.installedHosts').pushObject({
        name: 'wst3_host1'
      });
      c.reopen({
        bootHosts: [
          Em.Object.create({name: 'wst3_host1'}),
          Em.Object.create({name: 'wst3_host2'})
        ]
      });
      var testData = {
        items: [
          {
            Hosts: {
              host_name: 'wst3_host1'
            }
          },
          {
            Hosts: {
              host_name: 'wst3_host2'
            }
          },
          {
            Hosts: {
              host_name: 'wst3_host3'
            }
          }
        ]
      };
      c.getAllRegisteredHostsCallback(testData);
      expect(c.get('hasMoreRegisteredHosts')).to.equal(true);
      expect(c.get('registeredHosts').length).to.equal(1);
    });

    it('All hosts are new', function () {
      c.get('content.installedHosts').pushObject({
        name: 'wst3_host1'
      });
      c.reopen({
        bootHosts: [
          {name: 'wst3_host3'},
          {name: 'wst3_host4'}
        ]
      });
      var testData = {
        items: [
          {
            Hosts: {
              host_name: 'wst3_host3'
            }
          },
          {
            Hosts: {
              host_name: 'wst3_host4'
            }
          }
        ]
      };
      c.getAllRegisteredHostsCallback(testData);
      expect(c.get('hasMoreRegisteredHosts')).to.equal(false);
      expect(c.get('registeredHosts')).to.equal('');
    });

    it('No new hosts', function () {
      c.get('content.installedHosts').pushObject({
        name: 'wst3_host1'
      });
      c.reopen({
        bootHosts: [
          {name: 'wst3_host1'}
        ]
      });
      var testData = {
        items: [
          {
            Hosts: {
              host_name: 'wst3_host1'
            }
          }
        ]
      };
      c.getAllRegisteredHostsCallback(testData);
      expect(c.get('hasMoreRegisteredHosts')).to.equal(false);
      expect(c.get('registeredHosts')).to.equal('');
    });

  });

  describe('#isWarningsBoxVisible', function () {

    it('for "real" mode should be based on isRegistrationInProgress', function () {
      c.set('isRegistrationInProgress', false);
      expect(c.get('isWarningsBoxVisible')).to.equal(true);
      c.set('isRegistrationInProgress', true);
      expect(c.get('isWarningsBoxVisible')).to.equal(false);
    });

  });

  describe('#clearStep', function () {

    it('should clear hosts', function () {
      c.set('hosts', [
        {},
        {}
      ]);
      c.clearStep();
      expect(c.get('hosts')).to.eql([]);
    });

    it('should clear bootHosts', function () {
      c.set('bootHosts', [
        {},
        {}
      ]);
      c.clearStep();
      expect(c.get('bootHosts').length).to.equal(0);
    });

    it('should set stopBootstrap to false', function () {
      c.set('stopBootstrap', true);
      c.clearStep();
      expect(c.get('stopBootstrap')).to.equal(false);
    });

    it('should set wizardController DBProperty bootStatus to false', function () {
      c.get('wizardController').setDBProperty('bootStatus', true);
      c.clearStep();
      expect(c.get('wizardController').getDBProperty('bootStatus')).to.equal(false);
    });

    it('should set isSubmitDisabled to true', function () {
      c.set('isSubmitDisabled', false);
      c.clearStep();
      expect(c.get('isSubmitDisabled')).to.equal(true);
    });

  });

  describe('#loadStep', function () {

    beforeEach(function () {
      sinon.stub(App.router, 'get').withArgs('clusterController').returns({
        loadAmbariProperties: Em.K
      });
      sinon.spy(c, 'clearStep');
      sinon.stub(c, 'loadHosts', Em.K);
      sinon.stub(c, 'disablePreviousSteps', Em.K);
    });

    afterEach(function () {
      App.router.get.restore();
      c.clearStep.restore();
      c.disablePreviousSteps.restore();
      c.loadHosts.restore();
    });

    it('should set registrationStartedAt to null', function () {
      c.set('registrationStartedAt', {});
      c.loadStep();
      expect(c.get('registrationStartedAt')).to.be.null;
    });

    it('should call clearStep', function () {
      c.loadStep();
      expect(c.get('clearStep').calledOnce).to.equal(true);
    });

    it('should call loadHosts', function () {
      c.loadStep();
      expect(c.get('loadHosts').calledOnce).to.equal(true);
    });

    it('should call disablePreviousSteps', function () {
      c.loadStep();
      expect(c.get('disablePreviousSteps').calledOnce).to.equal(true);
    });

  });

  describe('#loadHosts', function () {

    beforeEach(function () {
      sinon.stub(c, 'navigateStep');
    });

    afterEach(function () {
      c.navigateStep.restore();
    });

    it('should set bootStatus DONE on "real" mode and when installOptions.manualInstall is selected', function () {
      c.set('content.installOptions', {manualInstall: true});
      c.set('content.hosts', {c: {name: 'name'}});
      c.loadHosts();
      expect(c.get('hosts').everyProperty('bootStatus', 'DONE')).to.equal(true);
    });

    it('should set bootStatus PENDING on "real" mode and when installOptions.manualInstall is not selected', function () {
      c.set('content', {installOptions: {manualInstall: false}, hosts: {c: {name: 'name'}}});
      c.loadHosts();
      expect(c.get('hosts').everyProperty('bootStatus', 'PENDING')).to.equal(true);
    });

    it('should set bootStatus PENDING on "real" mode and when installOptions.manualInstall is not selected (2)', function () {
      c.set('content', {hosts: {c: {name: 'name'}, d: {name: 'name1'}}});
      c.loadHosts();
      expect(c.get('hosts').everyProperty('isChecked', false)).to.equal(true);
    });

  });

  describe('#parseHostInfo', function () {

    var tests = Em.A([
      {
        bootHosts: Em.A([
          Em.Object.create({name: 'c1', bootStatus: 'REGISTERED', bootLog: ''}),
          Em.Object.create({name: 'c2', bootStatus: 'REGISTERING', bootLog: ''}),
          Em.Object.create({name: 'c3', bootStatus: 'RUNNING', bootLog: ''})
        ]),
        hostsStatusFromServer: Em.A([
          {hostName: 'c1', status: 'REGISTERED', log: 'c1'},
          {hostName: 'c2', status: 'REGISTERED', log: 'c2'},
          {hostName: 'c3', status: 'RUNNING', log: 'c3'}
        ]),
        m: 'bootHosts not empty, hostsStatusFromServer not empty, one is RUNNING',
        e: {
          c: true,
          r: true
        }
      },
      {
        bootHosts: Em.A([]),
        hostsStatusFromServer: Em.A([
          {hostName: 'c1', status: 'REGISTERED', log: 'c1'},
          {hostName: 'c2', status: 'REGISTERED', log: 'c2'},
          {hostName: 'c3', status: 'RUNNING', log: 'c3'}
        ]),
        m: 'bootHosts is empty',
        e: {
          c: false,
          r: false
        }
      },
      {
        bootHosts: Em.A([
          Em.Object.create({name: 'c1', bootStatus: 'REGISTERED', bootLog: ''}),
          Em.Object.create({name: 'c2', bootStatus: 'REGISTERING', bootLog: ''}),
          Em.Object.create({name: 'c3', bootStatus: 'REGISTERED', bootLog: ''})
        ]),
        hostsStatusFromServer: Em.A([
          {hostName: 'c1', status: 'REGISTERED', log: 'c1'},
          {hostName: 'c2', status: 'REGISTERED', log: 'c2'},
          {hostName: 'c3', status: 'REGISTERED', log: 'c3'}
        ]),
        m: 'bootHosts not empty, hostsStatusFromServer not empty, no one is RUNNING',
        e: {
          c: true,
          r: false
        }
      }
    ]);

    tests.forEach(function (test) {
      describe(test.m, function () {
        var r;
        beforeEach(function () {
          c.set('bootHosts', test.bootHosts);
          r = c.parseHostInfo(test.hostsStatusFromServer);
        });

        it('parsed hosts info is valid', function () {
          expect(r).to.equal(test.e.r);
        });

        if (test.e.c) {
          test.hostsStatusFromServer.forEach(function (h) {

            it('bootStatus and bootLog are valid', function () {
              var bootHosts = c.get('bootHosts').findProperty('name', h.hostName);
              if (!['REGISTERED', 'REGISTERING'].contains(bootHosts.get('bootStatus'))) {
                expect(bootHosts.get('bootStatus')).to.equal(h.status);
                expect(bootHosts.get('bootLog')).to.equal(h.log);
              }
            });

          });
        }
      });
    });
  });

  describe('#removeHosts', function () {

    beforeEach(function () {
      sinon.spy(App, 'showConfirmationPopup');
    });

    afterEach(function () {
      App.showConfirmationPopup.restore();
    });

    it('should call App.showConfirmationPopup', function () {
      c.removeHosts(Em.A([]));
      expect(App.showConfirmationPopup.calledOnce).to.equal(true);
    });

    it('primary should disable Submit if no more hosts', function () {
      var hosts = [
        {}
      ];
      c.set('hosts', hosts);
      c.removeHosts(hosts).onPrimary();
      expect(c.get('isSubmitDisabled')).to.equal(true);
    });

  });

  describe('#removeSelectedHosts', function () {

    beforeEach(function () {
      sinon.stub(c, 'removeHosts', Em.K);
    });

    afterEach(function () {
      c.removeHosts.restore();
    });

    it('should remove selected hosts', function () {
      c.set('hosts', [
        {isChecked: true, name: 'c1'},
        {isChecked: false, name: 'c2'}
      ]);
      c.removeSelectedHosts();
      expect(c.removeHosts.calledWith([
        {isChecked: true, name: 'c1'}
      ])).to.be.true;
    });

  });

  describe('#selectedHostsPopup', function () {

    it('should show App.ModalPopup', function () {
      c.selectedHostsPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });

  });

  describe('#retryHosts', function () {
    var s;
    var agentUserCases = [
      {
        customizeAgentUserAccount: true,
        userRunAs: 'user',
        title: 'Ambari Agent user account customize enabled'
      },
      {
        customizeAgentUserAccount: false,
        userRunAs: 'root',
        title: 'Ambari Agent user account customize disabled'
      }
    ];
    var installer = {launchBootstrap: Em.K};

    beforeEach(function () {
      sinon.spy(installer, "launchBootstrap");
      s = sinon.stub(App.router, 'get', function () {
        return installer;
      });
      sinon.stub(c, 'doBootstrap', Em.K);
      sinon.spy(c, 'startRegistration');
    });

    afterEach(function () {
      c.doBootstrap.restore();
      s.restore();
      installer.launchBootstrap.restore();
      c.startRegistration.restore();
    });

    it('should set numPolls to 0', function () {
      c.set('content', {installOptions: {}});
      c.set('numPolls', 123);
      c.retryHosts(Em.A([]));
      expect(c.get('numPolls')).to.equal(0);
    });

    it('should set registrationStartedAt to null', function () {
      c.set('content', {installOptions: {}});
      c.retryHosts(Em.A([]));
      expect(c.get('registrationStartedAt')).to.be.null;
    });

    it('should startRegistration if installOptions.manualInstall is true', function () {
      c.set('content', {installOptions: {manualInstall: true}});
      c.retryHosts(Em.A([]));
      expect(c.startRegistration.calledOnce).to.equal(true);
    });

    it('should launchBootstrap if installOptions.manualInstall is false', function () {
      c.set('content', {installOptions: {manualInstall: false}});
      c.retryHosts(Em.A([]));
      expect(installer.launchBootstrap.calledOnce).to.be.true;
    });

    agentUserCases.forEach(function (item) {
      describe(item.title, function () {
        var controller;
        beforeEach(function () {
          controller = App.WizardStep3Controller.create({
            content: {
              installOptions: {
                sshKey: 'key',
                sshUser: 'root',
                sshPort: '123',
                agentUser: 'user'
              },
              hosts: { "host0": { "name": "host0" }, "host1": { "name": "host1" } }
            }
          });
          sinon.stub(App, 'get').withArgs('supports.customizeAgentUserAccount').returns(item.customizeAgentUserAccount);
          controller.setupBootStrap();
        });

        afterEach(function () {
          App.get.restore();
        });

        it('launchBootstrap is called with correct arguments', function () {
          expect(installer.launchBootstrap.firstCall.args[0]).to.equal(JSON.stringify({
            verbose: true,
            sshKey: 'key',
            hosts: ['host0', 'host1'],
            user: 'root',
            sshPort: '123',
            userRunAs: item.userRunAs
          }));
        });

      });
    });
  });

  describe('#retryHost', function () {

    beforeEach(function () {
      sinon.spy(c, 'retryHosts');
      sinon.stub(App.router, 'get', function () {
        return {launchBootstrap: Em.K}
      });
      sinon.stub(c, 'doBootstrap', Em.K);
    });

    afterEach(function () {
      c.retryHosts.restore();
      App.router.get.restore();
      c.doBootstrap.restore();
    });

    it('should callretryHosts with array as arg', function () {
      var host = {n: 'c'};
      c.set('content', {installOptions: {}});
      c.retryHost(host);
      expect(c.retryHosts.calledWith([host])).to.equal(true);
    });

  });

  describe('#retrySelectedHosts', function () {

    beforeEach(function () {
      sinon.spy(c, 'retryHosts');
      sinon.stub(App.router, 'get', function () {
        return {launchBootstrap: Em.K}
      });
      sinon.stub(c, 'doBootstrap', Em.K);
    });

    afterEach(function () {
      c.retryHosts.restore();
      App.router.get.restore();
      c.doBootstrap.restore();
    });

    it('shouldn\'t do nothing if isRetryDisabled is true', function () {
      c.set('isRetryDisabled', true);
      c.retrySelectedHosts();
      expect(c.retryHosts.called).to.be.false;
    });

    it('should retry hosts with FAILED bootStatus', function () {
      c.set('bootHosts', Em.A([
        Em.Object.create({
          name: 'c1',
          bootStatus: 'FAILED'
        }),
        Em.Object.create({
          name: 'c2',
          bootStatus: 'REGISTERED'
        })
      ]));
      c.reopen({isRetryDisabled: false});
      c.retrySelectedHosts();
      expect(c.retryHosts.calledWith([
        Em.Object.create({name: 'c1', bootStatus: 'DONE', bootLog: 'Retrying ...'})
      ])).to.be.true;
    });

  });

  describe('#startBootstrap', function () {

    beforeEach(function () {
      sinon.stub(c, 'doBootstrap', Em.K);
    });

    afterEach(function () {
      c.doBootstrap.restore();
    });

    it('should drop numPolls and registrationStartedAt', function () {
      c.set('numPolls', 123);
      c.set('registrationStartedAt', 1234);
      c.startBootstrap();
      expect(c.get('numPolls')).to.equal(0);
      expect(c.get('registrationStartedAt')).to.be.null;
    });

    it('should drop numPolls and registrationStartedAt (2)', function () {
      c.set('hosts', Em.A([
        {name: 'c1'},
        {name: 'c2'}
      ]));
      c.startBootstrap();
      expect(c.get('bootHosts').mapProperty('name')).to.eql(['c1', 'c2']);
    });

  });

  describe('#setRegistrationInProgress', function () {

    var tests = Em.A([
      {
        bootHosts: [],
        isLoaded: false,
        e: true,
        m: 'no bootHosts and isLoaded is false'
      },
      {
        bootHosts: [],
        isLoaded: true,
        e: false,
        m: 'no bootHosts and isLoaded is true'
      },
      {
        bootHosts: [
          Em.Object.create({bootStatus: 'RUNNING'}),
          Em.Object.create({bootStatus: 'RUNNING'})
        ],
        isLoaded: true,
        e: true,
        m: 'bootHosts without REGISTERED/FAILED and isLoaded is true'
      },
      {
        bootHosts: [
          Em.Object.create({bootStatus: 'RUNNING'}),
          Em.Object.create({bootStatus: 'RUNNING'})
        ],
        isLoaded: false,
        e: true,
        m: 'bootHosts without REGISTERED/FAILED and isLoaded is false'
      },
      {
        bootHosts: [
          Em.Object.create({bootStatus: 'REGISTERED'}),
          Em.Object.create({bootStatus: 'RUNNING'})
        ],
        isLoaded: false,
        e: true,
        m: 'bootHosts with one REGISTERED and isLoaded is false'
      },
      {
        bootHosts: [
          Em.Object.create({bootStatus: 'FAILED'}),
          Em.Object.create({bootStatus: 'RUNNING'})
        ],
        isLoaded: false,
        e: true,
        m: 'bootHosts with one FAILED and isLoaded is false'
      },
      {
        bootHosts: [
          Em.Object.create({bootStatus: 'REGISTERED'}),
          Em.Object.create({bootStatus: 'RUNNING'})
        ],
        isLoaded: true,
        e: true,
        m: 'bootHosts with one REGISTERED and isLoaded is true'
      },
      {
        bootHosts: [
          Em.Object.create({bootStatus: 'FAILED'}),
          Em.Object.create({bootStatus: 'RUNNING'})
        ],
        isLoaded: true,
        e: true,
        m: 'bootHosts with one FAILED and isLoaded is true'
      }
    ]);

    beforeEach(function () {
      sinon.stub(c, 'getAllRegisteredHosts', Em.K);
      sinon.stub(c, 'disablePreviousSteps', Em.K);
      sinon.stub(c, 'navigateStep', Em.K);
    });

    afterEach(function () {
      c.disablePreviousSteps.restore();
      c.getAllRegisteredHosts.restore();
      c.navigateStep.restore();
    });

    tests.forEach(function (test) {
      it(test.m, function () {
        c.set('bootHosts', test.bootHosts);
        c.set('isLoaded', test.isLoaded);
        c.setRegistrationInProgress();
        expect(c.get('isRegistrationInProgress')).to.equal(test.e);
      });
    });
  });

  describe('#doBootstrap()', function () {

    it('shouldn\'t do nothing if stopBootstrap is true', function () {
      c.set('stopBootstrap', true);
      c.doBootstrap();
      var args = testHelpers.findAjaxRequest('name', 'wizard.step3.bootstrap');
      expect(args).not.exists;
    });

    it('should increment numPolls if stopBootstrap is false', function () {
      c.set('numPolls', 0);
      c.set('stopBootstrap', false);
      c.doBootstrap();
      var args = testHelpers.findAjaxRequest('name', 'wizard.step3.bootstrap');
      expect(args).exists;
      expect(c.get('numPolls')).to.equal(1);
    });

  });

  describe('#startRegistration', function () {

    beforeEach(function () {
      sinon.spy(c, 'isHostsRegistered');
    });

    afterEach(function () {
      c.isHostsRegistered.restore();
    });

    it('shouldn\'t do nothing if registrationStartedAt isn\'t null', function () {
      c.set('registrationStartedAt', 1234);
      c.startRegistration();
      expect(c.isHostsRegistered.called).to.equal(false);
      expect(c.get('registrationStartedAt')).to.equal(1234);
    });

    it('shouldn\'t do nothing if registrationStartedAt isn\'t null (2)', function () {
      c.set('registrationStartedAt', null);
      c.startRegistration();
      expect(c.isHostsRegistered.calledOnce).to.equal(true);
    });
  });

  describe('#isHostsRegistered', function () {

    it('shouldn\'t do nothing if stopBootstrap is true', function () {
      c.set('stopBootstrap', true);
      c.isHostsRegistered();
      var args = testHelpers.findAjaxRequest('name', 'wizard.step3.is_hosts_registered');
      expect(args).not.exists;
    });

    it('should do ajax call if stopBootstrap is false', function () {
      c.set('stopBootstrap', false);
      c.isHostsRegistered();
      var args = testHelpers.findAjaxRequest('name', 'wizard.step3.is_hosts_registered');
      expect(args).exists;

    });
  });

  describe('#isHostsRegisteredSuccessCallback', function () {

    var tests = Em.A([
      {
        bootHosts: Em.A([
          Em.Object.create({bootStatus: 'DONE'})
        ]),
        data: {items: []},
        registrationStartedAt: 1000000,
        m: 'one host DONE',
        e: {
          bs: 'REGISTERING',
          getHostInfoCalled: false
        }
      },
      {
        bootHosts: Em.A([
          Em.Object.create({bootStatus: 'REGISTERING', name: 'c1'})
        ]),
        data: {items: [
          {Hosts: {host_name: 'c1'}}
        ]},
        m: ' one host REGISTERING',
        e: {
          bs: 'REGISTERED',
          getHostInfoCalled: false
        }
      },
      {
        bootHosts: Em.A([
          Em.Object.create({bootStatus: 'REGISTERING', name: 'c1'})
        ]),
        data: {items: [
          {Hosts: {host_name: 'c2'}}
        ]},
        registrationStartedAt: 0,
        m: 'one host REGISTERING but data missing info about it, timeout',
        e: {
          bs: 'FAILED',
          getHostInfoCalled: false
        }
      },
      {
        bootHosts: Em.A([
          Em.Object.create({bootStatus: 'REGISTERING', name: 'c1'})
        ]),
        data: {items: [
          {Hosts: {host_name: 'c2'}}
        ]},
        registrationStartedAt: 1000000,
        m: 'one host REGISTERING but data missing info about it',
        e: {
          bs: 'REGISTERING',
          getHostInfoCalled: false
        }
      },
      {
        bootHosts: Em.A([
          Em.Object.create({bootStatus: 'RUNNING', name: 'c1'})
        ]),
        data: {items: [
          {Hosts: {host_name: 'c1'}}
        ]},
        m: ' one host RUNNING',
        e: {
          bs: 'RUNNING',
          getHostInfoCalled: false
        }
      }
    ]);

    beforeEach(function () {
      sinon.spy(c, 'getHostInfo');
      sinon.stub(App, 'dateTime').returns(1000000);
    });

    afterEach(function () {
      c.getHostInfo.restore();
      App.dateTime.restore();
    });

    tests.forEach(function (test) {
      it(test.m, function () {
        c.set('content.installedHosts', []);
        c.set('bootHosts', test.bootHosts);
        c.set('registrationStartedAt', test.registrationStartedAt);
        c.isHostsRegisteredSuccessCallback(test.data);
        expect(c.get('bootHosts')[0].get('bootStatus')).to.equal(test.e.bs);
        expect(c.getHostInfo.called).to.equal(test.e.getHostInfoCalled);
      });
    });

  });

  describe('#getAllRegisteredHosts', function () {

    it('should call App.ajax.send', function () {
      c.getAllRegisteredHosts();
      var args = testHelpers.findAjaxRequest('name', 'wizard.step3.is_hosts_registered');
      expect(args).exists;
    });
  });

  describe('#getAllRegisteredHostsCallback', function () {

    var tests = Em.A([
      {
        hostsInCluster: ['c3'],
        bootHosts: [
          {name: 'c1'},
          {name: 'c2'}
        ],
        hosts: Em.A([
          {Hosts: {host_name: 'c1'}},
          {Hosts: {host_name: 'c2'}}
        ]),
        m: 'No registered hosts',
        e: {
          hasMoreRegisteredHosts: false,
          registeredHosts: ''
        }
      },
      {
        hostsInCluster: ['c4'],
        bootHosts: [
          {name: 'c3'},
          {name: 'c5'}
        ],
        hosts: Em.A([
          {Hosts: {host_name: 'c1'}},
          {Hosts: {host_name: 'c2'}}
        ]),
        m: '2 registered hosts',
        e: {
          hasMoreRegisteredHosts: true,
          registeredHosts: ['c1', 'c2']
        }
      },
      {
        hostsInCluster: ['c4'],
        bootHosts: [
          {name: 'c1'},
          {name: 'c5'}
        ],
        hosts: Em.A([
          {Hosts: {host_name: 'c1'}},
          {Hosts: {host_name: 'c2'}}
        ]),
        m: '1 registered host',
        e: {
          hasMoreRegisteredHosts: true,
          registeredHosts: ['c2']
        }
      },
      {
        hostsInCluster: ['c1'],
        bootHosts: [
          {name: 'c3'},
          {name: 'c5'}
        ],
        hosts: Em.A([
          {Hosts: {host_name: 'c1'}},
          {Hosts: {host_name: 'c2'}}
        ]),
        m: '1 registered host (2)',
        e: {
          hasMoreRegisteredHosts: true,
          registeredHosts: ['c2']
        }
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        c.reopen({setRegistrationInProgress: Em.K, hostsInCluster: test.hostsInCluster});
        c.set('bootHosts', test.bootHosts);
        c.getAllRegisteredHostsCallback({items: test.hosts});
        expect(c.get('hasMoreRegisteredHosts')).to.equal(test.e.hasMoreRegisteredHosts);
        expect(c.get('registeredHosts')).to.eql(test.e.registeredHosts);
      });
    });

  });

  describe('#registerErrPopup', function () {
    it('should call App.ModalPopup.show', function () {
      c.registerErrPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });
  });

  describe('#getHostInfo', function () {

    it('should do ajax request', function () {
      c.getHostInfo();
      var args = testHelpers.findAjaxRequest('name', 'wizard.step3.host_info');
      expect(args).exists;
    });

  });

  describe('#getHostInfoErrorCallback', function () {

    beforeEach(function () {
      sinon.spy(c, 'registerErrPopup');
    });

    afterEach(function () {
      c.registerErrPopup.restore();
    });

    it('should call registerErrPopup', function () {
      c.getHostInfoErrorCallback();
      expect(c.registerErrPopup.calledOnce).to.equal(true);
    });

  });

  describe('#stopRegistration', function () {

    var tests = Em.A([
      {
        bootHosts: [
          Em.Object.create({bootStatus: 'REGISTERED'}),
          Em.Object.create({bootStatus: 'RUNNING'})
        ],
        e: {isSubmitDisabled: false}
      },
      {
        bootHosts: [
          Em.Object.create({bootStatus: 'FAILED'}),
          Em.Object.create({bootStatus: 'RUNNING'})
        ],
        e: {isSubmitDisabled: true}
      },
      {
        bootHosts: [
          Em.Object.create({bootStatus: 'FAILED'}),
          Em.Object.create({bootStatus: 'REGISTERED'})
        ],
        e: {isSubmitDisabled: false}
      },
      {
        bootHosts: [
          Em.Object.create({bootStatus: 'RUNNING'}),
          Em.Object.create({bootStatus: 'RUNNING'})
        ],
        e: {isSubmitDisabled: true}
      }
    ]);
    tests.forEach(function (test) {
      it(test.bootHosts.mapProperty('bootStatus').join(', '), function () {
        c.reopen({bootHosts: test.bootHosts});
        c.stopRegistration();
        expect(c.get('isSubmitDisabled')).to.equal(test.e.isSubmitDisabled);
      });
    });

  });

  describe('#submit', function () {

    it('if isHostHaveWarnings should show confirmation popup', function () {
      c.reopen({isHostHaveWarnings: true});
      c.submit();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });

    it('if isHostHaveWarnings should show confirmation popup. on Primary should set bootHosts to content.hosts', function () {
      var bootHosts = [
        Em.Object.create({name: 'c1'})
      ];
      c.reopen({isHostHaveWarnings: true, bootHosts: bootHosts, hosts: []});
      c.submit().onPrimary();
      expect(c.get('confirmedHosts')).to.eql(bootHosts);
    });

    it('if isHostHaveWarnings is false should set bootHosts to content.hosts', function () {
      var bootHosts = [
        Em.Object.create({name: 'c1'})
      ];
      c.reopen({isHostHaveWarnings: false, bootHosts: bootHosts, hosts: []});
      c.submit();
      expect(c.get('confirmedHosts')).to.eql(bootHosts);
    });

  });

  describe('#hostLogPopup', function () {

    it('should show App.ModalPopup', function () {
      c.hostLogPopup({context: Em.Object.create({})});
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });

  });

  describe('#rerunChecksSuccessCallback', function () {

    beforeEach(function () {
      sinon.stub(c, 'parseWarnings', Em.K);
    });

    afterEach(function () {
      c.parseWarnings.restore();
    });

    it('should set checksUpdateProgress to 100', function () {
      c.set('checksUpdateProgress', 0);
      c.rerunChecksSuccessCallback({items: []});
      expect(c.get('checksUpdateProgress')).to.equal(100);
    });

    it('should set checksUpdateStatus to SUCCESS', function () {
      c.set('checksUpdateStatus', '');
      c.rerunChecksSuccessCallback({items: []});
      expect(c.get('checksUpdateStatus')).to.equal('SUCCESS');
    });

    it('should call parseWarnings', function () {
      c.rerunChecksSuccessCallback({items: []});
      expect(c.parseWarnings.calledOnce).to.equal(true);
    });

  });

  describe('#rerunChecksErrorCallback', function () {

    it('should set checksUpdateProgress to 100', function () {
      c.set('checksUpdateProgress', 0);
      c.rerunChecksErrorCallback({});
      expect(c.get('checksUpdateProgress')).to.equal(100);
    });

    it('should set checksUpdateStatus to FAILED', function () {
      c.set('checksUpdateStatus', '');
      c.rerunChecksErrorCallback({});
      expect(c.get('checksUpdateStatus')).to.equal('FAILED');
    });

  });

  describe('#filterBootHosts', function () {
    var tests = Em.A([
      {
        bootHosts: [
          Em.Object.create({name: 'c1'}),
          Em.Object.create({name: 'c2'})
        ],
        data: {
          items: [
            {Hosts: {host_name: 'c1'}}
          ]
        },
        m: 'one host',
        e: ['c1']
      },
      {
        bootHosts: [
          Em.Object.create({name: 'c1'}),
          Em.Object.create({name: 'c2'})
        ],
        data: {
          items: [
            {Hosts: {host_name: 'c3'}}
          ]
        },
        m: 'no hosts',
        e: []
      },
      {
        bootHosts: [
          Em.Object.create({name: 'c1'}),
          Em.Object.create({name: 'c2'})
        ],
        data: {
          items: [
            {Hosts: {host_name: 'c1'}},
            {Hosts: {host_name: 'c2'}}
          ]
        },
        m: 'many hosts',
        e: ['c1', 'c2']
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        c.reopen({bootHosts: test.bootHosts});
        var filteredData = c.filterBootHosts(test.data);
        expect(filteredData.items.mapProperty('Hosts.host_name')).to.eql(test.e);
      });
    });
  });

  describe('#hostWarningsPopup', function () {

    beforeEach(function () {
      sinon.stub(c, 'rerunChecks', Em.K);
    });

    afterEach(function () {
      c.rerunChecks.restore();
    });

    it('should show App.ModalPopup', function () {
      c.hostWarningsPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });

    it('should clear checksUpdateStatus on primary', function () {
      c.set('checksUpdateStatus', 'not null value');
      c.hostWarningsPopup().onPrimary();
      expect(c.get('checksUpdateStatus')).to.be.null;
    });

    it('should clear checksUpdateStatus on close', function () {
      c.set('checksUpdateStatus', 'not null value');
      c.hostWarningsPopup().onClose();
      expect(c.get('checksUpdateStatus')).to.be.null;
    });

    it('should rerunChecks onSecondary', function () {
      c.hostWarningsPopup().onSecondary();
      expect(c.rerunChecks.calledOnce).to.equal(true);
    });
  });

  describe('#registeredHostsPopup', function () {
    it('should show App.ModalPopup', function () {
      c.registeredHostsPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });
  });

  describe('#parseHostCheckWarnings', function () {

    beforeEach(function () {
      sinon.stub(c, 'filterHostsData', function (k) {
        return k;
      });
    });

    afterEach(function () {
      c.filterHostsData.restore();
    });

    it('no warnings if last_agent_env isn\'t specified', function () {
      c.set('warnings', [
        {}
      ]);
      c.set('warningsByHost', [
        {},
        {}
      ]);
      c.parseHostCheckWarnings({tasks: [
        {Tasks: {host_name: 'c1'}}
      ]});
      expect(c.get('warnings')).to.eql([]);
      expect(c.get('warningsByHost.length')).to.equal(1); // default group
    });

    Em.A([
        {
          m: 'parse stackFoldersAndFiles',
          tests: Em.A([
            {
              tasks: [
                {Tasks: {host_name: 'c1',
                         structured_out:{last_agent_env_check: {stackFoldersAndFiles: []}}
                        }
                }
              ],
              m: 'empty stackFoldersAndFiles',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              tasks: [
                {Tasks: {host_name: 'c1',
                         structured_out: {last_agent_env_check: {stackFoldersAndFiles: [{name: 'n1'}]}}
                        }
                }
              ],
              m: 'not empty stackFoldersAndFiles',
              e: {
                warnings: [
                  {
                    name: 'n1',
                    hosts: ['c1'],
                    category: 'fileFolders'
                  }
                ],
                warningsByHost: [1]
              }
            },
            {
              tasks: [
                {Tasks: {host_name: 'c1', structured_out:{last_agent_env_check: {stackFoldersAndFiles: [
                  {name: 'n1'}
                ]}}}},
                {Tasks: {host_name: 'c2', structured_out: {last_agent_env_check: {stackFoldersAndFiles: [
                  {name: 'n1'}
                ]}}}}
              ],
              m: 'not empty stackFoldersAndFiles on two hosts',
              e: {
                warnings: [
                  {
                    name: 'n1',
                    hosts: ['c1', 'c2'],
                    category: 'fileFolders'
                  }
                ],
                warningsByHost: [1]
              }
            }
          ])
        },
        {
          m: 'parse hostHealth.liveServices',
          tests: Em.A([
            {
              tasks: [
                {Tasks: {host_name: 'c1', structured_out: {last_agent_env_check: {hostHealth: []}}}}
              ],
              m: 'empty hostHealth',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              tasks: [
                {Tasks: {host_name: 'c1', structured_out: {last_agent_env_check: {hostHealth: {liveServices: []}}}}}
              ],
              m: 'empty liveServices',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              tasks: [
                {Tasks: {host_name: 'c1',
                         structured_out: {last_agent_env_check: {hostHealth: {liveServices: [{status: 'Unhealthy', name: 'n1'}]}}}
                        }
                }
              ],
              m: 'not empty hostHealth.liveServices',
              e: {
                warnings: [
                  {
                    name: 'n1',
                    hosts: ['c1'],
                    category: 'services'
                  }
                ],
                warningsByHost: [1]
              }
            },
            {
              tasks: [
                {Tasks: {host_name: 'c1',
                         structured_out:{last_agent_env_check: {hostHealth: {liveServices: [{status: 'Unhealthy', name: 'n1'}]}}}
                        }
                },

                {Tasks: {host_name: 'c2',
                         structured_out:{last_agent_env_check: {hostHealth: {liveServices: [{status: 'Unhealthy', name: 'n1'}]}}}
                        }
                }
              ],
              m: 'not empty hostHealth.liveServices on two hosts',
              e: {
                warnings: [
                  {
                    name: 'n1',
                    hosts: ['c1', 'c2'],
                    category: 'services'
                  }
                ],
                warningsByHost: [1, 1]
              }
            }
          ])
        },
        {
          m: 'parse existingUsers',
          tests: Em.A([
            {
              tasks: [
                {Tasks: {host_name: 'c1',
                         structured_out: {last_agent_env_check: {existingUsers: []}}
                        }
                }
              ],
              m: 'empty existingUsers',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              tasks: [
                {Tasks: {host_name: 'c1',
                         structured_out: {last_agent_env_check: {existingUsers: [{name: 'n1'}]}}
                        }
                }
              ],
              m: 'not empty existingUsers',
              e: {
                warnings: [
                  {
                    name: 'n1',
                    hosts: ['c1'],
                    category: 'users'
                  }
                ],
                warningsByHost: [1]
              }
            },
            {
              tasks: [
                {Tasks: {host_name: 'c1',
                         structured_out:{last_agent_env_check: {existingUsers: [{name: 'n1'}]}}
                        }
                },
                {Tasks: {host_name: 'c2',
                         structured_out:{last_agent_env_check: {existingUsers: [{name: 'n1'}]}}
                        }
                }
              ],
              m: 'not empty existingUsers on two hosts',
              e: {
                warnings: [
                  {
                    name: 'n1',
                    hosts: ['c1', 'c2'],
                    category: 'users'
                  }
                ],
                warningsByHost: [1, 1]
              }
            }
          ])
        },
        {
          m: 'parse alternatives',
          tests: Em.A([
            {
              tasks: [
                {Tasks: {host_name: 'c1',
                         structured_out:{last_agent_env_check: {alternatives: []}}
                        }
                }
              ],
              m: 'empty alternatives',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              tasks: [
                {Tasks: {host_name: 'c1',
                         structured_out: {last_agent_env_check: {alternatives: [{name: 'n1'}]}}
                        }
                }
              ],
              m: 'not empty alternatives',
              e: {
                warnings: [
                  {
                    name: 'n1',
                    hosts: ['c1'],
                    category: 'alternatives'
                  }
                ],
                warningsByHost: [1]
              }
            },
            {
              tasks: [
                {Tasks: {host_name: 'c1',
                         structured_out:{last_agent_env_check: {alternatives: [{name: 'n1'}]}}
                        }
                },
                {Tasks: {host_name: 'c2',
                         structured_out:{last_agent_env_check: {alternatives: [{name: 'n1'}]}}
                        }
                }
              ],
              m: 'not empty alternatives on two hosts',
              e: {
                warnings: [
                  {
                    name: 'n1',
                    hosts: ['c1', 'c2'],
                    category: 'alternatives'
                  }
                ],
                warningsByHost: [1, 1]
              }
            }
          ])
        },
        {
          m: 'parse hostHealth.activeJavaProcs',
          tests: Em.A([
            {
              tasks: [
                {Tasks: {host_name: 'c1',
                         structured_out:{last_agent_env_check: {hostHealth: [], javaProcs: []}}
                        }
                }
              ],
              m: 'empty hostHealth',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              tasks: [
                {Tasks: {host_name: 'c1', structured_out:{last_agent_env_check: {hostHealth: {activeJavaProcs: []}}}}}
              ],
              m: 'empty activeJavaProcs',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              tasks: [
                {Tasks: {host_name: 'c1',
                         structured_out:{last_agent_env_check: {hostHealth: {activeJavaProcs: [{pid: 'n1', command: ''}]}}}
                        }
                }
              ],
              m: 'not empty hostHealth.activeJavaProcs',
              e: {
                warnings: [
                  {
                    pid: 'n1',
                    hosts: ['c1'],
                    category: 'processes'
                  }
                ],
                warningsByHost: [1]
              }
            },
            {
              tasks: [
                {Tasks: {host_name: 'c1',
                         structured_out:{last_agent_env_check: {hostHealth: {activeJavaProcs: [{pid: 'n1', command: ''}]}}}
                        }
                },
                {Tasks: {host_name: 'c2',
                         structured_out:{last_agent_env_check: {hostHealth: {activeJavaProcs: [{pid: 'n1', command: ''}]}}}
                        }
                }
              ],
              m: 'not empty hostHealth.activeJavaProcs on two hosts',
              e: {
                warnings: [
                  {
                    pid: 'n1',
                    hosts: ['c1', 'c2'],
                    category: 'processes'
                  }
                ],
                warningsByHost: [1, 1]
              }
            }
          ])
        }
      ]).forEach(function (category) {
      describe(category.m, function () {
        category.tests.forEach(function (test) {

          describe(test.m, function () {

            beforeEach(function () {
              c.parseHostCheckWarnings({tasks: test.tasks});
            });

            it('warnings count is valid', function () {
              expect(c.get('warnings.length')).to.be.equal(test.e.warnings.length);
            });

            test.e.warnings.forEach(function (warning, index) {
              Object.keys(warning).forEach(function (warningKey) {
                it('warning #' + (index + 1) + ' key: ' + warningKey, function () {
                  expect(c.get('warnings')[index][warningKey]).to.be.eql(warning[warningKey]);
                });
              });
            });

            Object.keys(test.e.warningsByHost).forEach(function (warningByHostKey, index) {
              it ('warningsByHost #' + (index + 1), function () {
                expect(c.get('warningsByHost')[index].warnings.length).to.equal(test.e.warningsByHost[warningByHostKey]);
              });
            });

          });
        });
      });
      });

    it('should parse umask warnings', function () {

      var tasks = [
        {Tasks: {host_name: 'c1', structured_out:{last_agent_env_check: {umask: 24}}}},
        {Tasks: {host_name: 'c2', structured_out:{last_agent_env_check: {umask: 1}}}}
      ];

      c.parseHostCheckWarnings({tasks: tasks});
      var warnings = c.get('warnings');
      expect(warnings.length).to.equal(1);
      expect(warnings[0].hosts).to.eql(['c1']);
      expect(warnings[0].hostsLong).to.eql(['c1']);

    });

    it('should parse umask warnings (2)', function () {

      var tasks = [
        {Tasks: {host_name: 'c1', structured_out:{last_agent_env_check: {umask: 24}}}},
        {Tasks: {host_name: 'c2', structured_out:{last_agent_env_check: {umask: 25}}}}
      ];

      c.parseHostCheckWarnings({tasks: tasks});
      var warnings = c.get('warnings');
      expect(warnings.length).to.equal(2);
      expect(warnings.mapProperty('hosts')).to.eql([
        ['c1'],
        ['c2']
      ]);

    });

    it('should parse firewall warnings', function () {

      var tasks = [
        {Tasks: {host_name: 'c1', structured_out:{last_agent_env_check: {firewallRunning: true, firewallName: "iptables"}}}},
        {Tasks: {host_name: 'c2', structured_out:{last_agent_env_check: {firewallRunning: false, firewallName: "iptables"}}}}
      ];

      c.parseHostCheckWarnings({tasks: tasks});
      var warnings = c.get('warnings');
      expect(warnings.length).to.equal(1);
      expect(warnings[0].hosts).to.eql(['c1']);
      expect(warnings[0].hostsLong).to.eql(['c1']);

    });

    it('should parse firewall warnings (2)', function () {

      var tasks = [
        {Tasks: {host_name: 'c1', structured_out:{last_agent_env_check: {firewallRunning: true, firewallName: "iptables"}}}},
        {Tasks: {host_name: 'c2', structured_out:{last_agent_env_check: {firewallRunning: true, firewallName: "iptables"}}}}
      ];

      c.parseHostCheckWarnings({tasks: tasks});
      var warnings = c.get('warnings');
      expect(warnings.length).to.equal(1);
      expect(warnings[0].hosts).to.eql(['c1', 'c2']);
      expect(warnings[0].hostsLong).to.eql(['c1', 'c2']);

    });

    it('should parse reverseLookup warnings', function () {

      var tasks = [
        {Tasks: {host_name: 'c1', structured_out:{last_agent_env_check: {reverseLookup: true}}}}
      ];

      c.parseHostCheckWarnings({tasks: tasks});
      var warnings = c.get('warnings');
      expect(warnings.length).to.equal(0);

    });

    it('should parse reverseLookup warnings (2)', function () {

      var tasks = [
        {Tasks: {host_name: 'c1', structured_out:{last_agent_env_check: {reverseLookup: false}}}}
      ];

      c.parseHostCheckWarnings({tasks: tasks});
      var warnings = c.get('warnings');
      expect(warnings.length).to.equal(1);
      expect(warnings[0].hosts).to.eql(['c1']);
      expect(warnings[0].hostsLong).to.eql(['c1']);

    });

    it('should parse reverseLookup warnings (3)', function () {

      var tasks = [
        {Tasks: {host_name: 'c1', structured_out:{last_agent_env_check: {reverseLookup: false}}}},
        {Tasks: {host_name: 'c2', structured_out:{last_agent_env_check: {reverseLookup: false}}}}
      ];

      c.parseHostCheckWarnings({tasks: tasks});
      var warnings = c.get('warnings');
      expect(warnings.length).to.equal(1);
      expect(warnings[0].hosts).to.eql(['c1', 'c2']);
      expect(warnings[0].hostsLong).to.eql(['c1', 'c2']);

    });
  });

  describe('#parseWarnings', function () {

    beforeEach(function () {
      sinon.stub(c, 'filterBootHosts', function (k) {
        return k;
      });
    });

    afterEach(function () {
      c.filterBootHosts.restore();
    });

    it('no warnings if last_agent_env isn\'t specified', function () {
      c.set('warnings', [
        {}
      ]);
      c.set('warningsByHost', [
        {},
        {}
      ]);
      c.parseWarnings({items: [
        {Hosts: {host_name: 'c1'}}
      ]});
      expect(c.get('warnings')).to.eql([]);
      expect(c.get('warningsByHost.length')).to.equal(1); // default group
    });

    Em.A([
        {
          m: 'parse stackFoldersAndFiles',
          tests: Em.A([
            {
              items: [
                {Hosts: {host_name: 'c1', last_agent_env: {stackFoldersAndFiles: []}}}
              ],
              m: 'empty stackFoldersAndFiles',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              items: [
                {Hosts: {host_name: 'c1', last_agent_env: {stackFoldersAndFiles: [
                  {name: 'n1'}
                ]}}}
              ],
              m: 'not empty stackFoldersAndFiles',
              e: {
                warnings: [
                  {
                    name: 'n1',
                    hosts: ['c1'],
                    category: 'fileFolders'
                  }
                ],
                warningsByHost: [1]
              }
            },
            {
              items: [
                {Hosts: {host_name: 'c1', last_agent_env: {stackFoldersAndFiles: [
                  {name: 'n1'}
                ]}}},
                {Hosts: {host_name: 'c2', last_agent_env: {stackFoldersAndFiles: [
                  {name: 'n1'}
                ]}}}
              ],
              m: 'not empty stackFoldersAndFiles on two hosts',
              e: {
                warnings: [
                  {
                    name: 'n1',
                    hosts: ['c1', 'c2'],
                    category: 'fileFolders'
                  }
                ],
                warningsByHost: [1]
              }
            }
          ])
        },
        {
          m: 'parse hostHealth.liveServices',
          tests: Em.A([
            {
              items: [
                {Hosts: {host_name: 'c1', last_agent_env: {hostHealth: []}}}
              ],
              m: 'empty hostHealth',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              items: [
                {Hosts: {host_name: 'c1', last_agent_env: {hostHealth: {liveServices: []}}}}
              ],
              m: 'empty liveServices',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              items: [
                {Hosts: {host_name: 'c1', last_agent_env: {hostHealth: {liveServices: [
                  {status: 'Unhealthy', name: 'n1'}
                ]}}}}
              ],
              m: 'not empty hostHealth.liveServices',
              e: {
                warnings: [
                  {
                    name: 'n1',
                    hosts: ['c1'],
                    category: 'services'
                  }
                ],
                warningsByHost: [1]
              }
            },
            {
              items: [
                {Hosts: {host_name: 'c1', last_agent_env: {hostHealth: {liveServices: [
                  {status: 'Unhealthy', name: 'n1'}
                ]}}}},
                {Hosts: {host_name: 'c2', last_agent_env: {hostHealth: {liveServices: [
                  {status: 'Unhealthy', name: 'n1'}
                ]}}}}
              ],
              m: 'not empty hostHealth.liveServices on two hosts',
              e: {
                warnings: [
                  {
                    name: 'n1',
                    hosts: ['c1', 'c2'],
                    category: 'services'
                  }
                ],
                warningsByHost: [1, 1]
              }
            }
          ])
        },
        {
          m: 'parse existingUsers',
          tests: Em.A([
            {
              items: [
                {Hosts: {host_name: 'c1', last_agent_env: {existingUsers: []}}}
              ],
              m: 'empty existingUsers',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              items: [
                {Hosts: {host_name: 'c1', last_agent_env: {existingUsers: [
                  {name: 'n1'}
                ]}}}
              ],
              m: 'not empty existingUsers',
              e: {
                warnings: [
                  {
                    name: 'n1',
                    hosts: ['c1'],
                    category: 'users'
                  }
                ],
                warningsByHost: [1]
              }
            },
            {
              items: [
                {Hosts: {host_name: 'c1', last_agent_env: {existingUsers: [
                  {name: 'n1'}
                ]}}},
                {Hosts: {host_name: 'c2', last_agent_env: {existingUsers: [
                  {name: 'n1'}
                ]}}}
              ],
              m: 'not empty existingUsers on two hosts',
              e: {
                warnings: [
                  {
                    name: 'n1',
                    hosts: ['c1', 'c2'],
                    category: 'users'
                  }
                ],
                warningsByHost: [1, 1]
              }
            }
          ])
        },
        {
          m: 'parse alternatives',
          tests: Em.A([
            {
              items: [
                {Hosts: {host_name: 'c1', last_agent_env: {alternatives: []}}}
              ],
              m: 'empty alternatives',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              items: [
                {Hosts: {host_name: 'c1', last_agent_env: {alternatives: [
                  {name: 'n1'}
                ]}}}
              ],
              m: 'not empty alternatives',
              e: {
                warnings: [
                  {
                    name: 'n1',
                    hosts: ['c1'],
                    category: 'alternatives'
                  }
                ],
                warningsByHost: [1]
              }
            },
            {
              items: [
                {Hosts: {host_name: 'c1', last_agent_env: {alternatives: [
                  {name: 'n1'}
                ]}}},
                {Hosts: {host_name: 'c2', last_agent_env: {alternatives: [
                  {name: 'n1'}
                ]}}}
              ],
              m: 'not empty alternatives on two hosts',
              e: {
                warnings: [
                  {
                    name: 'n1',
                    hosts: ['c1', 'c2'],
                    category: 'alternatives'
                  }
                ],
                warningsByHost: [1, 1]
              }
            }
          ])
        },
        {
          m: 'parse hostHealth.activeJavaProcs',
          tests: Em.A([
            {
              items: [
                {Hosts: {host_name: 'c1', last_agent_env: {hostHealth: [], javaProcs: []}}}
              ],
              m: 'empty hostHealth',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              items: [
                {Hosts: {host_name: 'c1', last_agent_env: {hostHealth: {activeJavaProcs: []}}}}
              ],
              m: 'empty activeJavaProcs',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              items: [
                {Hosts: {host_name: 'c1', last_agent_env: {hostHealth: {activeJavaProcs: [
                  {pid: 'n1', command: ''}
                ]}}}}
              ],
              m: 'not empty hostHealth.activeJavaProcs',
              e: {
                warnings: [
                  {
                    pid: 'n1',
                    hosts: ['c1'],
                    category: 'processes'
                  }
                ],
                warningsByHost: [1]
              }
            },
            {
              items: [
                {Hosts: {host_name: 'c1', last_agent_env: {hostHealth: {activeJavaProcs: [
                  {pid: 'n1', command: ''}
                ]}}}},
                {Hosts: {host_name: 'c2', last_agent_env: {hostHealth: {activeJavaProcs: [
                  {pid: 'n1', command: ''}
                ]}}}}
              ],
              m: 'not empty hostHealth.activeJavaProcs on two hosts',
              e: {
                warnings: [
                  {
                    pid: 'n1',
                    hosts: ['c1', 'c2'],
                    category: 'processes'
                  }
                ],
                warningsByHost: [1, 1]
              }
            }
          ])
        }
      ]).forEach(function (category) {
        describe(category.m, function () {
          category.tests.forEach(function (test) {
            describe(test.m, function () {

              beforeEach(function () {
                c.parseWarnings({items: test.items});
              });

              test.e.warnings.forEach(function (warning, index) {
                Object.keys(warning).forEach(function (warningKey) {
                  it('warning #' + (index + 1) + ' key: ' + warningKey, function () {
                    expect(c.get('warnings')[index][warningKey]).to.be.eql(warning[warningKey]);
                  });
                });
              });

              Object.keys(test.e.warningsByHost).forEach(function (warningByHostKey, index) {
                it ('warningsByHost #' + (index + 1), function () {
                  expect(c.get('warningsByHost')[index].warnings.length).to.equal(test.e.warningsByHost[warningByHostKey]);
                });
              });

            });
          });
        });
      });

    it('should parse umask warnings', function () {

      var items = [
        {Hosts: {host_name: 'c1', last_agent_env: {umask: 24}}},
        {Hosts: {host_name: 'c2', last_agent_env: {umask: 1}}}
      ];

      c.parseWarnings({items: items});
      var warnings = c.get('warnings');
      expect(warnings.length).to.equal(1);
      expect(warnings[0].hosts).to.eql(['c1']);
      expect(warnings[0].hostsLong).to.eql(['c1']);

    });

    it('should parse umask warnings (2)', function () {

      var items = [
        {Hosts: {host_name: 'c1', last_agent_env: {umask: 24}}},
        {Hosts: {host_name: 'c2', last_agent_env: {umask: 25}}}
      ];

      c.parseWarnings({items: items});
      var warnings = c.get('warnings');
      expect(warnings.length).to.equal(2);
      expect(warnings.mapProperty('hosts')).to.eql([
        ['c1'],
        ['c2']
      ]);

    });

    it('should parse firewall warnings', function () {

      var items = [
        {Hosts: {host_name: 'c1', last_agent_env: {firewallRunning: true, firewallName: "iptables"}}},
        {Hosts: {host_name: 'c2', last_agent_env: {firewallRunning: false, firewallName: "iptables"}}}
      ];

      c.parseWarnings({items: items});
      var warnings = c.get('warnings');
      expect(warnings.length).to.equal(1);
      expect(warnings[0].hosts).to.eql(['c1']);
      expect(warnings[0].hostsLong).to.eql(['c1']);

    });

    it('should parse firewall warnings (2)', function () {

      var items = [
        {Hosts: {host_name: 'c1', last_agent_env: {firewallRunning: true, firewallName: "iptables"}}},
        {Hosts: {host_name: 'c2', last_agent_env: {firewallRunning: true, firewallName: "iptables"}}}
      ];

      c.parseWarnings({items: items});
      var warnings = c.get('warnings');
      expect(warnings.length).to.equal(1);
      expect(warnings[0].hosts).to.eql(['c1', 'c2']);
      expect(warnings[0].hostsLong).to.eql(['c1', 'c2']);

    });

    it('should parse reverseLookup warnings', function () {

      var items = [
        {Hosts: {host_name: 'c1', last_agent_env: {reverseLookup: true}}}
      ];

      c.parseWarnings({items: items});
      var warnings = c.get('warnings');
      expect(warnings.length).to.equal(0);

    });

    it('should parse reverseLookup warnings (2)', function () {

      var items = [
        {Hosts: {host_name: 'c1', last_agent_env: {reverseLookup: false}}}
      ];

      c.parseWarnings({items: items});
      var warnings = c.get('warnings');
      expect(warnings.length).to.equal(1);
      expect(warnings[0].hosts).to.eql(['c1']);
      expect(warnings[0].hostsLong).to.eql(['c1']);

    });

    it('should parse reverseLookup warnings (3)', function () {

      var items = [
        {Hosts: {host_name: 'c1', last_agent_env: {reverseLookup: false}}},
        {Hosts: {host_name: 'c2', last_agent_env: {reverseLookup: false}}}
      ];

      c.parseWarnings({items: items});
      var warnings = c.get('warnings');
      expect(warnings.length).to.equal(1);
      expect(warnings[0].hosts).to.eql(['c1', 'c2']);
      expect(warnings[0].hostsLong).to.eql(['c1', 'c2']);

    });

  });

  describe('#navigateStep', function () {

    beforeEach(function () {
      sinon.stub(c, 'setupBootStrap', Em.K);
    });

    afterEach(function () {
      c.setupBootStrap.restore();
    });

    Em.A([
        {
          isLoaded: true,
          manualInstall: false,
          bootStatus: false,
          m: 'should call setupBootStrap',
          e: true
        },
        {
          isLoaded: true,
          manualInstall: false,
          bootStatus: true,
          m: 'shouldn\'t call setupBootStrap (1)',
          e: false
        },
        {
          isLoaded: false,
          manualInstall: false,
          bootStatus: false,
          m: 'shouldn\'t call setupBootStrap (2)',
          e: false
        },
        {
          isLoaded: false,
          manualInstall: true,
          bootStatus: false,
          m: 'shouldn\'t call setupBootStrap (3)',
          e: false
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          c.reopen({
            isLoaded: test.isLoaded,
            content: {
              installedHosts: [],
              installOptions: {
                manualInstall: test.manualInstall
              }
            },
            wizardController: Em.Object.create({
              getDBProperty: function () {
                return test.bootStatus
              }
            })
          });
          c.navigateStep();
          if (test.e) {
            expect(c.setupBootStrap.calledOnce).to.equal(true);
          }
          else {
            expect(c.setupBootStrap.called).to.equal(false);
          }
        });
      });

    describe('should start registration', function () {

      beforeEach(function () {
        c.reopen({
          isLoaded: true,
          hosts: [
            {},
            {},
            {}
          ],
          content: {
            installedHosts: [],
            installOptions: {
              manualInstall: true
            }
          },
          setRegistrationInProgress: Em.K,
          startRegistration: Em.K
        });
        sinon.spy(c, 'startRegistration');
        c.navigateStep();
      });


      afterEach(function () {
        c.startRegistration.restore();
      });

      it('startRegistration is called once', function () {
        expect(c.startRegistration.calledOnce).to.equal(true);
      });

      it('all hosts are bootHosts', function () {
        expect(c.get('bootHosts.length')).to.equal(c.get('hosts.length'));
      });

      it('registrationStartedAt is null', function () {
        expect(c.get('registrationStartedAt')).to.be.null;
      });
    });

  });

  describe('#setupBootStrap', function () {

    var cases = [
        {
          customizeAgentUserAccount: true,
          userRunAs: 'user',
          title: 'Ambari Agent user account customize enabled'
        },
        {
          customizeAgentUserAccount: false,
          userRunAs: 'root',
          title: 'Ambari Agent user account customize disabled'
        }
      ],

      controller = App.WizardStep3Controller.create({
        content: {
          installOptions: {
            sshKey: 'key',
            sshUser: 'root',
            sshPort: '123',
            agentUser: 'user'
          },
          hosts: { "host0": { "name": "host0" }, "host1": { "name": "host1" } },
          controllerName: 'installerController'
        }
      });

    beforeEach(function () {
      sinon.stub(App.router.get('installerController'), 'launchBootstrap', Em.K);
      this.mock = sinon.stub(App, 'get');
    });

    afterEach(function () {
      App.router.get('installerController').launchBootstrap.restore();
      this.mock.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        this.mock.withArgs('supports.customizeAgentUserAccount').returns(item.customizeAgentUserAccount);
        controller.setupBootStrap();
        expect(App.router.get('installerController.launchBootstrap').firstCall.args[0]).to.equal(JSON.stringify({
          verbose: true,
          sshKey: 'key',
          hosts: ['host0', 'host1'],
          user: 'root',
          sshPort: '123',
          userRunAs: item.userRunAs
        }));
      });
    });

  });

  describe('#checkHostDiskSpace', function () {
    Em.A([
        {
          diskInfo: [
            {
              available: App.minDiskSpace * 1024 * 1024 - 1024,
              mountpoint: '/'
            }
          ],
          m: 'available less than App.minDiskSpace',
          e: false
        },
        {
          diskInfo: [
            {
              available: App.minDiskSpaceUsrLib * 1024 * 1024 - 1024,
              mountpoint: '/usr'
            }
          ],
          m: 'available less than App.minDiskSpaceUsrLib (1)',
          e: false
        },
        {
          diskInfo: [
            {
              available: App.minDiskSpaceUsrLib * 1024 * 1024 - 1024,
              mountpoint: '/usr/lib'
            }
          ],
          m: 'available less than App.minDiskSpaceUsrLib (2)',
          e: false
        },
        {
          diskInfo: [
            {
              available: App.minDiskSpace * 1024 * 1024 + 1024,
              mountpoint: '/'
            }
          ],
          m: 'available greater than App.minDiskSpace',
          e: true
        },
        {
          diskInfo: [
            {
              available: App.minDiskSpaceUsrLib * 1024 * 1024 + 1024,
              mountpoint: '/usr'
            }
          ],
          m: 'available greater than App.minDiskSpaceUsrLib (1)',
          e: true
        },
        {
          diskInfo: [
            {
              available: App.minDiskSpaceUsrLib * 1024 * 1024 + 1024,
              mountpoint: '/usr/lib'
            }
          ],
          m: 'available greater than App.minDiskSpaceUsrLib (2)',
          e: true
        },
        {
          diskInfo: [
            {
              available: App.minDiskSpaceUsrLib * 1024 * 1024 + 1024,
              mountpoint: '/home/tdk'
            }
          ],
          m: 'mount point without free space checks',
          e: true
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          var r = c.checkHostDiskSpace('', test.diskInfo);
          expect(Em.isEmpty(r)).to.equal(test.e);
        });
      });
  });

  describe('#checkHostOSType', function () {

    it('should return empty string if no stacks provided', function () {
      c.reopen({content: {stacks: null}});
      expect(c.checkHostOSType()).to.equal('');
    });

    it('os type is valid', function () {
      var osType = 'redhat6';
      c.reopen({
        content: {
          stacks: [
            Em.Object.create({isSelected: true, operatingSystems: [Em.Object.create({isSelected: true, osType: osType})]})
          ]
        }
      });
      expect(c.checkHostOSType(osType, '')).to.equal('');
    });

    it('os type is invalid', function () {
      var osType = 'os2';
      c.reopen({
        content: {
          stacks: [
            Em.Object.create({isSelected: true, operatingSystems: [Em.Object.create({isSelected: true, osType: 'os1'})]})
          ]
        }
      });
      expect(Em.isEmpty(c.checkHostOSType(osType, ''))).to.equal(false);
    });

  });

  describe('#getHostInfoSuccessCallback', function () {

    var jsonData = {items: [
      {Hosts: {host_name: 'h1'}}
    ]};

    var skipBootstrap = false;

    beforeEach(function () {
      sinon.stub(c, 'parseWarnings', Em.K);
      sinon.stub(c, 'stopRegistration', Em.K);
      sinon.stub(c, 'checkHostDiskSpace', Em.K);
      sinon.stub(c, '_setHostDataFromLoadedHostInfo', Em.K);
      sinon.spy(c, '_setHostDataWithSkipBootstrap');
      sinon.stub(App, 'get', function (k) {
        if ('skipBootstrap' === k) return skipBootstrap;
        return Em.get(App, k);
      });
      c.reopen({
        bootHosts: [Em.Object.create({name: 'h1'})]
      });
      sinon.stub(c, 'checkHostOSType', function () {
        return 'not_null_value';
      });
    });

    afterEach(function () {
      c.parseWarnings.restore();
      c.stopRegistration.restore();
      c.checkHostDiskSpace.restore();
      c._setHostDataFromLoadedHostInfo.restore();
      c._setHostDataWithSkipBootstrap.restore();
      App.get.restore();
      c.checkHostOSType.restore();
    });

    it('should call _setHostDataWithSkipBootstrap if skipBootstrap is true', function () {
        skipBootstrap = true;
        c.getHostInfoSuccessCallback(jsonData);
        expect(c._setHostDataWithSkipBootstrap.calledOnce).to.equal(true);
    });

    it('should add repo warnings', function () {
      skipBootstrap = false;
      c.getHostInfoSuccessCallback(jsonData);
      expect(c.get('repoCategoryWarnings.length')).to.equal(1);
      expect(c.get('repoCategoryWarnings.firstObject.hostsNames').contains('h1')).to.equal(true);
    });

    it('should add disk warnings', function () {
      skipBootstrap = false;
      c.getHostInfoSuccessCallback(jsonData);
      expect(c.get('diskCategoryWarnings.length')).to.equal(1);
      expect(c.get('diskCategoryWarnings.firstObject.hostsNames').contains('h1')).to.equal(true);
    });

  });

  describe('#_setHostDataWithSkipBootstrap', function () {

    it('should set mock-data', function () {
      var host = Em.Object.create({});
      c._setHostDataWithSkipBootstrap(host);
      expect(host.get('cpu')).to.equal(2);
      expect(host.get('memory')).to.equal('2000000.00');
      expect(host.get('disk_info.length')).to.equal(4);
    });

  });

  describe('#_setHostDataFromLoadedHostInfo', function () {
    var host;
    beforeEach(function () {
      host = Em.Object.create();
      var hostInfo = {
        Hosts: {
          cpu_count: 2,
          total_mem: 12345,
          os_type: 't1',
          os_arch: 'os1',
          os_family: 'osf1',
          ip: '0.0.0.0',
          disk_info: [
            {mountpoint: '/boot'},
            {mountpoint: '/usr'},
            {mountpoint: '/no-boot'},
            {mountpoint: '/boot'}
          ]
        }
      };
      c._setHostDataFromLoadedHostInfo(host, hostInfo);
    });

    it('cpu', function () {
      expect(host.get('cpu')).to.equal(2);
    });
    it('os_type', function () {
      expect(host.get('os_type')).to.equal('t1');
    });
    it('os_arch', function () {
      expect(host.get('os_arch')).to.equal('os1');
    });
    it('os_family', function () {
      expect(host.get('os_family')).to.equal('osf1');
    });
    it('ip', function () {
      expect(host.get('ip')).to.equal('0.0.0.0');
    });
    it('memory', function () {
      expect(host.get('memory')).to.equal('12345.00');
    });
    it('disk_info.length', function () {
      expect(host.get('disk_info.length')).to.equal(2);
    });

  });

  describe('#getJDKNameSuccessCallback', function () {

    it('should set proper data to controller properties', function () {

      var expected = {
          name: 'name',
          home: 'home',
          location: 'location'
        },
        data = {
          RootServiceComponents: {
            properties: {
              'jdk.name': expected.name,
              'java.home': expected.home,
              'jdk_location': expected.location
            }
          }
        };

      c.getJDKNameSuccessCallback(data);
      expect(c.get('needJDKCheckOnHosts')).to.equal(false);
      expect(c.get('jdkLocation')).to.equal(expected.location);
      expect(c.get('javaHome')).to.equal(expected.home);
    });

  });

  describe('#doCheckJDK', function () {

    it('should do request to the ambari-server', function () {

      var bootHosts = [
          Em.Object.create({name: 'n1', bootStatus: 'REGISTERED'}),
          Em.Object.create({name: 'n2', bootStatus: 'REGISTERED'})
        ],
        javaHome = '/java',
        jdkLocation = '/jdk';
      c.reopen({
        bootHosts: bootHosts,
        javaHome: javaHome,
        jdkLocation: jdkLocation
      });
      c.doCheckJDK();
      var args = testHelpers.findAjaxRequest('name', 'wizard.step3.jdk_check');
      expect(args).exists;
    });

  });

  describe('#doCheckJDKsuccessCallback', function () {

    it('should set jdkRequestIndex if data provided', function () {

      var data = {
          href: '/a/b/c'
        },
        expected = 'c';
      c.set('jdkRequestIndex', null);
      c.doCheckJDKsuccessCallback(data);
      expect(c.get('jdkRequestIndex')).to.equal(expected);
    });

    it('should set isJDKWarningsLoaded to true if jdkCategoryWarnings is not null', function () {

      var data = null,
        expected = true;
      c.set('isJDKWarningsLoaded', false);
      c.set('jdkCategoryWarnings', {});
      c.doCheckJDKsuccessCallback(data);
      expect(c.get('isJDKWarningsLoaded')).to.equal(expected);
    });

    it('should do request to ambari-server', function () {

      var data = null,
        jdkRequestIndex = 'jdkRequestIndex';
      c.set('jdkRequestIndex', jdkRequestIndex);
      c.set('jdkCategoryWarnings', null);
      c.doCheckJDKsuccessCallback(data);
      var args = testHelpers.findAjaxRequest('name', 'wizard.step3.jdk_check.get_results');
      expect(args).exists;
    });

  });

  describe('#doCheckJDKerrorCallback', function () {

    it('should set isJDKWarningsLoaded to true', function () {
      c.set('isJDKWarningsLoaded', false);
      c.doCheckJDKerrorCallback();
      expect(c.get('isJDKWarningsLoaded')).to.be.true;
    });

  });

  describe('#parseJDKCheckResults', function () {

    beforeEach(function () {
      sinon.stub(c, 'doCheckJDKsuccessCallback', Em.K);
    });

    afterEach(function () {
      c.doCheckJDKsuccessCallback.restore();
    });

    it('should set jdkCategoryWarnings to null if no data', function () {

      var data = {Requests: {}};
      c.set('jdkCategoryWarnings', {});
      c.parseJDKCheckResults(data);
      expect(c.get('jdkCategoryWarnings')).to.be.null;

    });

    it('should parse warnings (1)', function () {

      var data = {
        Requests: {
          end_time: 1
        },
        tasks: []
      };

      c.set('jdkCategoryWarnings', {});
      c.parseJDKCheckResults(data);
      expect(c.get('jdkCategoryWarnings')).to.eql([]);

    });

    it('should parse warnings (2)', function () {

      var data = {
        Requests: {
          end_time: 1
        },
        tasks: [
          {
            Tasks: {
              host_name: 'h1',
              structured_out: {
                java_home_check: {
                  exit_code: 1
                }
              }
            }
          },
          {
            Tasks: {
              host_name: 'h2',
              structured_out: {
                java_home_check: {
                  exit_code: 0
                }
              }
            }
          }
        ]
      };

      c.set('jdkCategoryWarnings', {});
      c.parseJDKCheckResults(data);
      var result = c.get('jdkCategoryWarnings');
      expect(result.length).to.equal(1);
      expect(result[0].hostsNames).to.eql(['h1']);

    });

  });

  describe('#getHostCheckTasksSuccess', function() {

    beforeEach(function() {
      sinon.stub(c, 'getHostInfo', Em.K);
      sinon.stub(c, 'parseHostNameResolution', Em.K);
      sinon.stub(c, 'getGeneralHostCheck', Em.K);
      sinon.stub(c, 'getHostCheckTasks', Em.K);

    });

    afterEach(function() {
      c.getHostInfo.restore();
      c.parseHostNameResolution.restore();
      c.getGeneralHostCheck.restore();
      c.getHostCheckTasks.restore();
    });

    var dataInProgress = {
      Requests: {
        request_status: "IN_PROGRESS"
      }
    };
    it('run getHostCheckTasks', function() {
      c.getHostCheckTasksSuccess(dataInProgress);
      expect(c.getHostCheckTasks.calledOnce).to.be.true;
    });

    var hostResolutionCheckComplete = {
      Requests: {
        request_status: "COMPLETED",
        inputs: "host_resolution_check"
      }
    };
    it('run parseHostNameResolution and getGeneralHostCheck', function() {
      c.getHostCheckTasksSuccess(hostResolutionCheckComplete);
      expect(c.parseHostNameResolution.calledWith(hostResolutionCheckComplete)).to.be.true;
      expect(c.getGeneralHostCheck.calledOnce).to.be.true;
    });

    var lastAgentEnvCheckComplete = {
      Requests: {
        request_status: "COMPLETED",
        inputs: "last_agent_env_check"
      },
      tasks: [
        {
          Tasks: {
            host_name: 'h1',
            structured_out: {
              "installed_packages": [
                {
                  "version": "b1",
                  "name": "n1",
                  "repoName": "r1"
                },
                {
                  "version": "b2",
                  "name": "n2",
                  "repoName": "r2"
                }
              ]
            }
          }
        }
      ]
    };
    it('run getHostInfo', function() {
      c.getHostCheckTasksSuccess(lastAgentEnvCheckComplete);
      expect(c.get('stopChecking')).to.be.true;
      expect(c.getHostInfo.calledOnce).to.be.true;
      expect(JSON.parse(JSON.stringify(c.get('hostsPackagesData')))).eql([
        {
          hostName: 'h1',
          installedPackages: [
            {
              "version": "b1",
              "name": "n1",
              "repoName": "r1"
            },
            {
              "version": "b2",
              "name": "n2",
              "repoName": "r2"
            }
          ]
        }
      ]);
    });

  });

  describe('#getDataForCheckRequest', function() {
    var tests = [
      {
        bootHosts: [
          Em.Object.create({'bootStatus': 'REGISTERED', 'name': 'h1'}),
          Em.Object.create({'bootStatus': 'FAILED', 'name': 'h2'})
        ],
        addHosts: true,
        rez: {
          RequestInfo: {
            "action": "check_host",
            "context": "Check host",
            "parameters": {
              "check_execute_list": 'checkExecuteList',
              "jdk_location" : "jdk_location",
              "threshold": "20",
              "hosts": "h1"
            }
          },
          resource_filters: {
              "hosts": "h1"
          }
        },
        m: 'with add host param'
      },
      {
        bootHosts: [
          Em.Object.create({'bootStatus': 'REGISTERED', 'name': 'h1'}),
          Em.Object.create({'bootStatus': 'FAILED', 'name': 'h2'})
        ],
        addHosts: false,
        rez: {
          RequestInfo: {
            "action": "check_host",
            "context": "Check host",
            "parameters": {
              "check_execute_list": 'checkExecuteList',
              "jdk_location" : "jdk_location",
              "threshold": "20"
            }
          },
          resource_filters: {
            "hosts": "h1"
          }
        },
        m: 'without add host param'
      },
      {
        bootHosts: [
          Em.Object.create({'bootStatus': 'FAILED', 'name': 'h1'}),
          Em.Object.create({'bootStatus': 'FAILED', 'name': 'h2'})
        ],
        rez: null,
        m: 'with all hosts failed'
      }
    ];

    beforeEach(function() {
      sinon.stub(App.get('router'), 'get' , function(p) {
        return p === 'clusterController.ambariProperties.jdk_location' ? 'jdk_location' : Em.get(App.get('router'), p);
      })
    });
    afterEach(function() {
      App.get('router').get.restore();
    });
    tests.forEach(function(t) {
      it(t.m, function() {
        c.set('bootHosts', t.bootHosts);
        expect(c.getDataForCheckRequest('checkExecuteList', t.addHosts)).to.be.eql(t.rez);
      });
    })
  });

  describe('#isBackDisabled', function () {

    var cases = [
      {
        inputData: {
          isRegistrationInProgress: true,
          isWarningsLoaded: true,
          isBootstrapFailed: true
        },
        isBackDisabled: false
      },
      {
        inputData: {
          isRegistrationInProgress: true,
          isWarningsLoaded: false,
          isBootstrapFailed: false
        },
        isBackDisabled: true
      },
      {
        inputData: {
          isRegistrationInProgress: true,
          isWarningsLoaded: true,
          isBootstrapFailed: false
        },
        isBackDisabled: true
      },
      {
        inputData: {
          isRegistrationInProgress: true,
          isWarningsLoaded: false,
          isBootstrapFailed: true
        },
        isBackDisabled: false
      },
      {
        inputData: {
          isRegistrationInProgress: false,
          isWarningsLoaded: true,
          isBootstrapFailed: true
        },
        isBackDisabled: false
      },
      {
        inputData: {
          isRegistrationInProgress: false,
          isWarningsLoaded: false,
          isBootstrapFailed: false
        },
        isBackDisabled: true
      },
      {
        inputData: {
          isRegistrationInProgress: false,
          isWarningsLoaded: true,
          isBootstrapFailed: false
        },
        isBackDisabled: false
      },
      {
        inputData: {
          isRegistrationInProgress: false,
          isWarningsLoaded: false,
          isBootstrapFailed: true
        },
        isBackDisabled: false
      }
    ];

    cases.forEach(function (item) {
      var title = Em.keys(item.inputData).map(function (key) {
        return key + ':' + item.inputData[key];
      }).join(', ');
      it(title, function () {
        c.setProperties({
          isRegistrationInProgress: item.inputData.isRegistrationInProgress,
          isBootstrapFailed: item.inputData.isBootstrapFailed
        });
        c.reopen({
          isWarningsLoaded: item.inputData.isWarningsLoaded
        });
        expect(c.get('isBackDisabled')).to.equal(item.isBackDisabled);
      });
    });

  });

  describe('#hostsInCluster', function () {

    var hosts = {
      host1: {isInstalled: true},
      host2: {isInstalled: false},
      host3: {isInstalled: true},
      host4: {isInstalled: false}
    };

    beforeEach(function () {
      c.set('content', {hosts: hosts});
      c.propertyDidChange('hostsInCluster');
    });

    it('should take only installed hosts', function () {
      expect(c.get('hostsInCluster')).to.be.eql(['host1', 'host3']);
    });

  });

  describe('#filterHostsData', function () {

    var bootHosts = [
      Em.Object.create({name: 'c1'}),
      Em.Object.create({name: 'c2'}),
      Em.Object.create({name: 'c3'})
    ];

    var data = {
      href: 'abc',
      tasks: [
        {Tasks: {host_name: 'c1'}},
        {Tasks: {host_name: 'c2'}},
        {Tasks: {host_name: 'c3'}},
        {Tasks: {host_name: 'c2'}},
        {Tasks: {host_name: 'c3'}},
        {Tasks: {host_name: 'c4'}}
      ]
    };

    beforeEach(function() {
      c.set('bootHosts', bootHosts);
      this.result = c.filterHostsData(data);
    });

    it('href is valid', function () {
      expect(this.result.href).to.be.equal('abc');
    });

    it('tasks are valid', function () {
      expect(this.result.tasks).to.be.eql([
        {Tasks: {host_name: 'c1'}},
        {Tasks: {host_name: 'c2'}},
        {Tasks: {host_name: 'c3'}},
        {Tasks: {host_name: 'c2'}},
        {Tasks: {host_name: 'c3'}}
      ]);
    });


  });

  describe('#parseHostNameResolution', function () {

    var data = {
      tasks: [
        {Tasks: {status: 'COMPLETED', host_name: 'h1', structured_out: {host_resolution_check: {failed_count: 2, hosts_with_failures: ['h2', 'h3']}}}},
        {Tasks: {status: 'COMPLETED', host_name: 'h4', structured_out: {host_resolution_check: {failed_count: 2, hosts_with_failures: ['h5', 'h6']}}}},
        {Tasks: {status: 'COMPLETED', host_name: 'h7', structured_out: {host_resolution_check: {failed_count: 1, hosts_with_failures: ['h8']}}}}
      ]
    };
    var hostCheckWarnings = [];

    beforeEach(function () {
      c.set('hostCheckWarnings', hostCheckWarnings);
      c.parseHostNameResolution(data);
      this.warnings = c.get('hostCheckWarnings').findProperty('name', Em.I18n.t('installer.step3.hostWarningsPopup.resolution.validation.error'));
    });

    it('Host check warnings for hostname resolutions exist', function () {
      expect(this.warnings).to.exist;
    });

    it('hostsNames are ["h1", "h4", "h7"]', function () {
      expect(this.warnings.hostsNames.toArray()).to.be.eql(['h1', 'h4', 'h7']);
    });

    it('validation context for hosts is valid', function () {
      var hosts = this.warnings.hosts;
      var expected = [
        Em.I18n.t('installer.step3.hostWarningsPopup.resolution.validation.context').format('h1', 2 + ' ' + Em.I18n.t('installer.step3.hostWarningsPopup.hosts')),
        Em.I18n.t('installer.step3.hostWarningsPopup.resolution.validation.context').format('h4', 2 + ' ' + Em.I18n.t('installer.step3.hostWarningsPopup.hosts')),
        Em.I18n.t('installer.step3.hostWarningsPopup.resolution.validation.context').format('h7', 1 + ' ' + Em.I18n.t('installer.step3.hostWarningsPopup.host'))
      ];
      expect(hosts).to.be.eql(expected);
    });

    it('validation context (long) for hosts is valid', function () {
      var hostsLong = this.warnings.hostsLong;
      var expected = [
        Em.I18n.t('installer.step3.hostWarningsPopup.resolution.validation.context').format('h1', 'h2, h3'),
        Em.I18n.t('installer.step3.hostWarningsPopup.resolution.validation.context').format('h4', 'h5, h6'),
        Em.I18n.t('installer.step3.hostWarningsPopup.resolution.validation.context').format('h7', 'h8')
      ];
      expect(hostsLong).to.be.eql(expected);
    });

  });

  describe('#getHostCheckTasksError', function () {

    beforeEach(function () {
      c.set('stopChecking', false);
    });

    it('should set `stopChecking` to true', function () {
      c.getHostCheckTasksError();
      expect(c.get('stopChecking')).to.be.true;
    });

  });

  describe('#closeReloadPopupOnExit', function () {

    var cases = [
      {
        stopBootstrap: true,
        closeReloadPopupCallCount: 1,
        title: 'bootstrap should be stopped'
      },
      {
        stopBootstrap: false,
        closeReloadPopupCallCount: 0,
        title: 'bootstrap should not be stopped'
      }
    ];

    beforeEach(function () {
      sinon.stub(c, 'closeReloadPopup', Em.K);
    });

    afterEach(function () {
      c.closeReloadPopup.restore();
    });

    cases.forEach(function (item) {

      it(item.title, function () {
        if (c.get('stopBootstrap') === item.stopBootstrap) {
          c.propertyDidChange('stopBootstrap');
        } else {
          c.set('stopBootstrap', item.stopBootstrap);
        }
        expect(c.closeReloadPopup.callCount).to.equal(item.closeReloadPopupCallCount);
      });

    });

  });

  describe('#isNextButtonDisabled', function () {

    var cases = [
      {
        btnClickInProgress: true,
        isSubmitDisabled: true,
        isNextButtonDisabled: true,
        description: 'button clicked, submit disabled',
        title: 'next button disabled'
      },
      {
        btnClickInProgress: true,
        isSubmitDisabled: false,
        isNextButtonDisabled: true,
        description: 'button clicked, submit not disabled',
        title: 'next button disabled'
      },
      {
        btnClickInProgress: false,
        isSubmitDisabled: true,
        isNextButtonDisabled: true,
        description: 'no button clicked, submit disabled',
        title: 'next button disabled'
      },
      {
        btnClickInProgress: false,
        isSubmitDisabled: false,
        isNextButtonDisabled: false,
        description: 'no button clicked, submit not disabled',
        title: 'next button enabled'
      }
    ];

    cases.forEach(function (item) {

      describe(item.description, function () {

        beforeEach(function () {
          c.set('isSubmitDisabled', item.isSubmitDisabled);
          sinon.stub(App, 'get').withArgs('router.btnClickInProgress').returns(item.btnClickInProgress);
          c.propertyDidChange('isSubmitDisabled');
          c.propertyDidChange('App.router.btnClickInProgress');
        });

        afterEach(function () {
          App.get.restore();
        });

        it(item.title, function () {
          expect(c.get('isNextButtonDisabled')).to.equal(item.isNextButtonDisabled);
        });

      });

    });

  });

  describe('#isBackButtonDisabled', function () {

    var cases = [
      {
        btnClickInProgress: true,
        isBackDisabled: true,
        isBackButtonDisabled: true,
        description: 'button clicked, stepping back disabled',
        title: 'back button disabled'
      },
      {
        btnClickInProgress: true,
        isBackDisabled: false,
        isBackButtonDisabled: true,
        description: 'button clicked, stepping back not disabled',
        title: 'back button disabled'
      },
      {
        btnClickInProgress: false,
        isBackDisabled: true,
        isBackButtonDisabled: true,
        description: 'no button clicked, stepping back disabled',
        title: 'back button disabled'
      },
      {
        btnClickInProgress: false,
        isBackDisabled: false,
        isBackButtonDisabled: false,
        description: 'no button clicked, stepping back not disabled',
        title: 'back button enabled'
      }
    ];

    cases.forEach(function (item) {

      describe(item.description, function () {

        beforeEach(function () {
          c.reopen({
            isBackDisabled: item.isBackDisabled
          });
          sinon.stub(App, 'get').withArgs('router.btnClickInProgress').returns(item.btnClickInProgress);
          c.propertyDidChange('isBackDisabled');
          c.propertyDidChange('App.router.btnClickInProgress');
        });

        afterEach(function () {
          App.get.restore();
        });

        it(item.title, function () {
          expect(c.get('isBackButtonDisabled')).to.equal(item.isBackButtonDisabled);
        });

      });

    });

  });

});
