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
require('controllers/wizard/step2_controller');
require('models/host');
require('models/host_component');
require('messages');
var testHelpers = require('test/helpers');
var c;

function getController() {
  return App.WizardStep2Controller.create({content: {controllerName: ''}});
}

describe('App.WizardStep2Controller', function () {

  var userErrorTests = Em.A([
    {
      manualInstall: false,
      user: '',
      sshPort:'',
      e: ''
    },
    {
      manualInstall: true,
      user: '',
      sshPort:'',
      e: null
    },
    {
      manualInstall: true,
      user: 'nobody',
      sshPort:'123',
      e: null
    },
    {
      manualInstall: false,
      user: 'nobody',
      sshPort:'123',
      e: null
    }
  ]);


  beforeEach(function() {
    c = getController();
  });

  App.TestAliases.testAsComputedEqual(getController(), 'isInstaller', 'content.controllerName', 'installerController');

  App.TestAliases.testAsComputedAlias(getController(), 'manualInstall', 'content.installOptions.manualInstall', 'boolean');

  App.TestAliases.testAsComputedAlias(getController(), 'sshKey', 'content.installOptions.sshKey', 'string');

  App.TestAliases.testAsComputedAlias(getController(), 'sshUser', 'content.installOptions.sshUser', 'string');

  App.TestAliases.testAsComputedAlias(getController(), 'sshPort', 'content.installOptions.sshPort', 'string');

  App.TestAliases.testAsComputedAlias(getController(), 'agentUser', 'content.installOptions.agentUser', 'string');

  App.TestAliases.testAsComputedOr(getController(), 'isSubmitDisabled', ['hostsError', 'sshKeyError', 'sshUserError', 'sshPortError', 'agentUserError', 'App.router.btnClickInProgress']);

  describe('#hostNames', function() {
    it('should be equal to content.installOptions.hostNames', function() {
      var controller = App.WizardStep2Controller.create({content: {installOptions: {hostNames: 'A,b,C'}}});
      expect(controller.get('hostNames')).to.equal('a,b,c');
      controller.set('content.installOptions.hostNames', 'a,B');
      expect(controller.get('hostNames')).to.equal('a,b');
    });
  });

  App.TestAliases.testAsComputedIfThenElse(getController(), 'installType', 'manualInstall', 'manualDriven', 'ambariDriven');

  describe('#updateHostNameArr()', function () {

    var controller = App.WizardStep2Controller.create({
      hostNames: 'apache.ambari'
    });
    controller.updateHostNameArr();

    it('should push to hostNameArr only new host names', function(){
      expect(controller.get('hostNameArr').length).to.equal(1);
    });

    it('should push to inputtedAgainHostNames already installed host names', function(){
      expect(controller.get('inputtedAgainHostNames').length).to.equal(0);
    })
  });

  describe('#isAllHostNamesValid()', function () {

    var controller = App.WizardStep2Controller.create({
      hostNames: ''
    });

    it('should return true if all host names are valid', function(){
      controller.set('hostNames', 'amache.org ambari.com');
      expect(controller.isAllHostNamesValid()).to.equal(true);
    });

    var tests = Em.A([
      'hostname',
      '-hostname.com',
      'hostname-.com',
      'host_name.com',
      '123.123.123.123',
      'hostnamehostnamehostnamehostnamehostnamehostnamehostnamehostname.hostnamehostnamehostnamehostnamehostnamehostnamehostnamehostname.hostnamehostnamehostnamehostnamehostnamehostnamehostnamehostname.hostnamehostnamehostnamehostnamehostnamehostnamehostnamehostname',
      'hostnamehostnamehostnamehostnamehostnamehostnamehostnamehostnamehostname.hostname'
    ]);
    tests.forEach(function (test) {
      it('should return false for invalid host names ' + test + ' ', function () {
        controller.set('hostNames', test);
        expect(controller.isAllHostNamesValid()).to.equal(false);
      });
    });
  });

  describe('#checkHostError()', function () {

    var controller = App.WizardStep2Controller.create();

    it('should set hostsError if hostNames is ""', function () {
      controller.set('content', {'installOptions': {'hostNames': ''}});
      controller.checkHostError();
      expect(controller.get('hostsError').length).to.be.above(2);
    });

    it('should set hostsError to null if hostNames is valid', function () {
      controller.set('content', {'installOptions': {'hostNames': 'ambari'}});
      controller.checkHostError();
      expect(controller.get('hostsError')).to.equal(null);
    })
  });

  describe('#checkHostAfterSubmitHandler()', function () {

    it('should be called after changing hasSubmitted', function (done) {
      var controller = App.WizardStep2Controller.create({
        checkHostError: function () {
          done();
        }
      });
      controller.set('hasSubmitted', true);
    });

    it('should be called after changing hostNames', function (done) {
      var controller = App.WizardStep2Controller.create({
        hasSubmitted: true,
        checkHostError: function () {
          done();
        }
      });
      controller.set('content', {'installOptions': {'hostNames': 'ambari'}});
    })
  });

  describe('#sshKeyError', function () {

    var tests = Em.A([
      {
        manualInstall: false,
        sshKey: '',
        hasSubmitted: false,
        e: null
      },
      {
        manualInstall: true,
        sshKey: '',
        hasSubmitted: false,
        e: null
      },
      {
        manualInstall: true,
        sshKey: 'nobody',
        hasSubmitted: false,
        e: null
      },
      {
        manualInstall: false,
        sshKey: 'nobody',
        hasSubmitted: false,
        e: null
      },
      {
        manualInstall: false,
        sshKey: '',
        hasSubmitted: true,
        e: null
      },
      {
        manualInstall: true,
        sshKey: '',
        hasSubmitted: true,
        e: null
      },
      {
        manualInstall: true,
        sshKey: 'nobody',
        hasSubmitted: true,
        e: null
      },
      {
        manualInstall: false,
        sshKey: 'nobody',
        hasSubmitted: true,
        e: null
      }
    ]);

    tests.forEach(function(test) {
      it(test.sshKey + ' ' + test.manualInstall.toString() + ' ' + test.hasSubmitted.toString(), function() {
        var controller = App.WizardStep2Controller.create({content: {installOptions: {manualInstall: test.manualInstall, sshKey: test.sshKey}}});
        if(Em.isNone(test.e)) {
          expect(controller.get('sshKeyError')).to.equal(null);
        }
        else {
          expect(controller.get('sshKeyError').length).to.be.above(2);
        }
      });
    });
  });

  describe('#sshUserError', function () {

    userErrorTests.forEach(function(test) {
      it(JSON.stringify(test), function() {
        var controller = App.WizardStep2Controller.create({content: {installOptions: {manualInstall: test.manualInstall, sshUser: test.user}}});
        if(Em.isNone(test.e)) {
          expect(controller.get('sshUserError')).to.equal(null);
        }
        else {
          expect(controller.get('sshUserError').length).to.be.above(2);
        }
      });
    });

  });

  describe('#sshPortError', function () {

      userErrorTests.forEach(function(test) {
          it(JSON.stringify(test), function() {
              var controller = App.WizardStep2Controller.create({content: {installOptions: {manualInstall: test.manualInstall, sshPort: test.sshPort}}});
              if(Em.isNone(test.e)) {
                  expect(controller.get('sshPortError')).to.equal(null);
              }
              else {
                  expect(controller.get('sshPortError').length).to.be.above(2);
              }
          });
      });

  });

  describe('#agentUserError', function () {

    beforeEach(function () {
      this.stub = sinon.stub(App, 'get');
    });

    afterEach(function () {
      App.get.restore();
    });

    userErrorTests.forEach(function(test) {
      it('Ambari Agent user account customize enabled', function() {
        this.stub.withArgs('supports.customizeAgentUserAccount').returns(true);
        var controller = App.WizardStep2Controller.create({content: {installOptions: {manualInstall: test.manualInstall, agentUser: test.user}}});
        if(Em.isNone(test.e)) {
          expect(controller.get('agentUserError')).to.be.null;
        }
        else {
          expect(controller.get('agentUserError').length).to.be.above(2);
        }
      });
    });

    userErrorTests.forEach(function(test) {
      it('Ambari Agent user account customize disabled', function() {
        this.stub.withArgs('supports.customizeAgentUserAccount').returns(false);
        var controller = App.WizardStep2Controller.create({content: {installOptions: {manualInstall: test.manualInstall, agentUser: test.user}}});
        expect(controller.get('agentUserError')).to.be.null;
      });
    });

  });

  describe('#getHostInfo()', function () {

    it('should return object with bootStatus, installType and name for every element in hostNameArr', function () {
      var controller = App.WizardStep2Controller.create({
        hostNameArr: ['apache', 'ambari'],
        installType: 'manualDriven'
      });

      var test = controller.getHostInfo();
      expect(test).to.eql({
        'apache':{'name':'apache', 'installType': 'manualDriven', 'bootStatus': 'PENDING', isInstalled: false},
        'ambari':{'name':'ambari', 'installType': 'manualDriven', 'bootStatus': 'PENDING', isInstalled: false}
      });
    })
  });

  describe('#setSshKey()', function () {

    it('should set content.installOptions.sshKey', function () {
      var controller = App.WizardStep2Controller.create({
        content: {'installOptions': {'sshKey': '111'}}
      });
      controller.setSshKey('222');
      expect(controller.get('content.installOptions.sshKey')).to.equal('222');
    })
  });

  describe('#evaluateStep()', function () {

    it('should return false if isSubmitDisabled is true', function () {
      var controller = App.WizardStep2Controller.create({
        hostNames: 'apache.ambari',
        parseHostNamesAsPatternExpression: Em.K
      });
      controller.reopen({isSubmitDisabled: true});
      expect(controller.evaluateStep()).to.equal(false);
    });

    it('should return false if hostsError is not empty', function () {
      var controller = App.WizardStep2Controller.create({
        hostNames: 'apache.ambari',
        parseHostNamesAsPatternExpression: Em.K
      });
      controller.set('hostsError', 'error');
      expect(controller.evaluateStep()).to.equal(false);
    });

    it('should return false if sshKeyError is not empty', function () {
      var controller = App.WizardStep2Controller.create({
        hostNames: 'apache.ambari',
        parseHostNamesAsPatternExpression: Em.K
      });
      controller.reopen({sshKeyError: 'error'});
      expect(controller.evaluateStep()).to.equal(false);
    });

    it('should return false if sshUserError is not empty', function () {
      var controller = App.WizardStep2Controller.create({
        hostNames: 'apache.ambari',
        parseHostNamesAsPatternExpression: Em.K
      });
      controller.reopen({sshUserError: 'error'});
      expect(controller.evaluateStep()).to.equal(false);
    });

    it('should return false if sshPortError is not empty', function () {
        var controller = App.WizardStep2Controller.create({
            hostNames: 'apache.ambari',
            parseHostNamesAsPatternExpression: Em.K
        });
        controller.reopen({sshPortError: 'error'});
        expect(controller.evaluateStep()).to.equal(false);
    });

    it('should return false if agentUserError is not empty', function () {
      var controller = App.WizardStep2Controller.create({
        hostNames: 'apache.ambari',
        parseHostNamesAsPatternExpression: Em.K
      });
      controller.reopen({agentUserError: 'error'});
      expect(controller.evaluateStep()).to.equal(false);
    });

    it('should return false if hostNameArr is empty', function () {
      var controller = App.WizardStep2Controller.create({
        hostNames: '',
        parseHostNamesAsPatternExpression: Em.K
      });
      expect(controller.evaluateStep()).to.equal(false);
    });

    it('should return false if isPattern is true', function () {
      var controller = App.WizardStep2Controller.create({
        hostNames: 'apache.ambari',
        isPattern: true,
        parseHostNamesAsPatternExpression: Em.K
      });
      expect(controller.evaluateStep()).to.equal(false);
    })
  });

  describe('#parseHostNamesAsPatternExpression()', function () {

    it('should parse hosts from pattern expression to hostNameArr', function () {
      var controller = App.WizardStep2Controller.create({
        hostNameArr: ['host[001-011]']
      });
      controller.parseHostNamesAsPatternExpression();
      var result = true;
      var hosts = controller.get('hostNameArr');
      for (var i = 1; i<12; i++) {
        var extra = i.toString().length === 1 ? 0 : '';
        if (hosts[i-1] !== 'host0' + extra + i) {
          result = false;
        }
      }
      expect(result).to.equal(true);
    });

    it('should parse hosts from multiple pattern expression to hostNameArr', function () {
      var controller = App.WizardStep2Controller.create({
        hostNameArr: ['test[1-2]host[01-05]']
      });
      controller.parseHostNamesAsPatternExpression();
      var hosts = controller.get('hostNameArr');
      expect(hosts).eql([
          'test1host01',
          'test1host02',
          'test1host03',
          'test1host04',
          'test1host05',
          'test2host01',
          'test2host02',
          'test2host03',
          'test2host04',
          'test2host05'
      ]);
    });

    it('should skip duplicates', function () {
      var controller = App.WizardStep2Controller.create({
        hostNameArr: ['host[1-3]', 'host2']
      });
      controller.parseHostNamesAsPatternExpression();
      expect(controller.get('hostNameArr')).to.eql(['host1', 'host2', 'host3']);
    });

  });

  describe('#proceedNext()', function () {

    beforeEach(function () {
      sinon.stub(c, 'warningPopup', Em.K);
      sinon.stub(c, 'manualInstallPopup', Em.K);
      sinon.stub(App.router, 'send', Em.K);
    });

    afterEach(function () {
      c.warningPopup.restore();
      c.manualInstallPopup.restore();
      App.router.send.restore();
    });

    it('should call warningPopup if not isAllHostNamesValid and no warningConfirmed', function() {
      c.reopen({
        isAllHostNamesValid: function() {
          return false;
        }
      });
      var r = c.proceedNext(false);
      expect(r).to.equal(false);
      expect(c.warningPopup.calledOnce).to.equal(true);
    });

    it('should call manualInstallPopup if manualInstall is true', function () {
      c.reopen({
        hostNames: '',
        manualInstall: true
      });
      var r = c.proceedNext(true);
      expect(r).to.equal(false);
      expect(c.manualInstallPopup.calledOnce).to.equal(true);
    });

    it ('should save hosts and proceed next if manualInstall is false', function() {
      c.reopen({
        hostNameArr: ['h1'],
        manualInstall: false,
        isAllHostNamesValid: function() {return true;},
        content: {
          installOptions: {},
          hosts: null
        }
      });
      var r = c.proceedNext();
      expect(r).to.equal(true);
      expect(Em.keys(c.get('content.hosts'))).to.eql(['h1']);
      expect(App.router.send.calledWith('next')).to.equal(true);
    });

  });

  describe('#installedHostsPopup', function() {
    beforeEach(function() {
      sinon.stub(c, 'proceedNext', Em.K);
    });
    afterEach(function() {
      c.proceedNext.restore();
    });
    it('should call App.ModalPopup.show', function() {
      c.installedHostsPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });
    it('should proceed next on primary', function() {
      c.installedHostsPopup().onPrimary();
      expect(c.proceedNext.calledOnce).to.equal(true);
    });
  });

  describe('#warningPopup', function() {
    beforeEach(function() {
      sinon.stub(c, 'proceedNext', Em.K);
    });
    afterEach(function() {
      c.proceedNext.restore();

    });
    it('should call App.ModalPopup.show', function() {
      c.warningPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });
    it('should proceed next on primary', function() {
      c.warningPopup().onPrimary();
      expect(c.proceedNext.calledWith(true)).to.equal(true);
    });
  });

  describe('#hostNamePatternPopup', function() {
    beforeEach(function() {
      sinon.stub(c, 'proceedNext', Em.K);
    });
    afterEach(function() {
      c.proceedNext.restore();
    });
    it('should call App.ModalPopup.show', function() {
      c.hostNamePatternPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });
    it('should proceed next on primary', function() {
      c.hostNamePatternPopup().onPrimary();
      expect(c.proceedNext.calledOnce).to.equal(true);
    });
  });

  describe('#manualInstallPopup', function() {
    beforeEach(function() {
      sinon.stub(App.router, 'send', Em.K);
      sinon.stub(c, 'saveHosts', Em.K);
    });
    afterEach(function() {
      App.router.send.restore();
      c.saveHosts.restore();
    });
    it('should call App.ModalPopup.show', function() {
      c.manualInstallPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });
    it('should save hosts and go next on primary', function() {
      c.manualInstallPopup().onPrimary();
      expect(c.saveHosts.calledOnce).to.equal(true);
      expect(App.router.send.calledWith('next')).to.equal(true);
    });
  });

  describe('#manualInstallWarningPopup', function() {
    it('should call App.ModalPopup.show if content.installOptions.useSsh is false', function() {
      var controller = App.WizardStep2Controller.create({content: {installOptions: {useSsh: false}}});
      controller.manualInstallWarningPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });
    it('shouldn\'t call App.ModalPopup.show if content.installOptions.useSsh is true', function() {
      var controller = App.WizardStep2Controller.create({content: {installOptions: {useSsh: true}}});
      controller.manualInstallWarningPopup();
      expect(App.ModalPopup.show.called).to.equal(false);
    });
  });

  describe('#setAmbariJavaHome', function() {

    it('should do ajax-request', function() {
      var controller = App.WizardStep2Controller.create({onGetAmbariJavaHomeSuccess: Em.K, onGetAmbariJavaHomeError: Em.K});
      controller.setAmbariJavaHome();
      var args = testHelpers.findAjaxRequest('name', 'ambari.service');
      expect(args).exists;
    });
  });

  describe('#onGetAmbariJavaHomeSuccess', function() {
    it('should set java.home value receiced from server', function() {
      var controller = App.WizardStep2Controller.create({content: {installOptions: {}}});
      var test = {RootServiceComponents: {properties: {'java.home': '/root'}}};
      controller.onGetAmbariJavaHomeSuccess(test);
      expect(controller.content.installOptions.javaHome).to.equal('/root');
    });
  });

  describe('#onGetAmbariJavaHomeError', function() {
    it('should set default java.home value', function() {
      var controller = App.WizardStep2Controller.create({content: {installOptions: {}}});
      controller.onGetAmbariJavaHomeError();
      expect(controller.content.installOptions.javaHome).to.equal(App.get('defaultJavaHome'));
    });
  });

  describe('#saveHosts', function() {
    beforeEach(function() {
      sinon.stub(c, 'setAmbariJavaHome', Em.K);
      c.reopen({
        hostNameArr: ['h1'],
        content: {
          hosts: null
        }
      });
    });
    afterEach(function() {
      c.setAmbariJavaHome.restore();
    });
    it('should call setAmbariJavaHome', function() {
      c.saveHosts();
      expect(c.setAmbariJavaHome.calledOnce).to.equal(true);
    });
    it('should set content.hosts', function() {
      c.saveHosts();
      expect(Em.keys(c.get('content.hosts'))).to.eql(['h1']);
    });
  });

});
