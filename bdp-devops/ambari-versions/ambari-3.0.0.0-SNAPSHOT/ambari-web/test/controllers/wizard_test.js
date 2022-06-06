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

var c;

function getSteps(start, count) {
  var steps = [];
  for (var j = start; j <= count; j++) {
    steps.push(Em.Object.create({step: j, value: false}));
  }
  return steps;
}

describe('App.WizardController', function () {

  var wizardController = App.WizardController.create({});

  var totalSteps = 11;
  var ruller = d3.range(0, totalSteps);
  var i;
  beforeEach(function () {
    c = App.WizardController.create({});
  });

  describe('#setLowerStepsDisable', function () {
    var steps = getSteps(1, 10);
    wizardController.set('isStepDisabled', steps);
    steps.forEach(function (step) {
      var index = step.get('step');
      it('Steps: 10 | Disabled: ' + (index - 1), function () {
        wizardController.setLowerStepsDisable(index);
        expect(wizardController.get('isStepDisabled').filterProperty('value', true).length).to.be.equal(index - 1);
      });
    });
  });

  // isStep0 ... isStep10 tests
  App.WizardController1 = App.WizardController.extend({currentStep:''});
  var tests = [];
  for (i = 0; i < totalSteps; i++) {
    var n = ruller.slice(0);
    n.splice(i, 1);
    tests.push({i: i, n: n});
  }
  tests.forEach(function (test) {
    describe('isStep' + test.i, function () {
      var w = App.WizardController1.create();
      w.set('currentStep', test.i);

      it('Current Step is {0}, so isStep{1} is TRUE'.format(test.i, test.i), function () {
        expect(w.get('isStep' + test.i)).to.equal(true);
      });

      test.n.forEach(function (indx) {
        it('Current Step is {0}, so isStep{1} is FALSE'.format(test.i, indx), function () {
          expect(w.get('isStep' + indx)).to.equal(false);
        });
      });
    });
  });
  // isStep0 ... isStep10 tests end

  describe('#gotoStep', function() {
    var w = App.WizardController1.create();
    var steps = getSteps(0, totalSteps - 1);
    steps.forEach(function(step, index) {
      step.set('value', true);
      w.set('isStepDisabled', steps);
      it('step {0} is disabled, so gotoStep({1}) is not possible'.format(index, index), function() {
        expect(w.gotoStep(index)).to.equal(false);
      });
    });
  });

  describe('#launchBootstrapSuccessCallback', function() {
    var params = {popup: {finishLoading: function(){}}};
    beforeEach(function () {
      sinon.spy(params.popup, "finishLoading");
    });

    afterEach(function () {
      params.popup.finishLoading.restore();
    });

    it('Save bootstrapRequestId', function() {
      var data = {requestId: 123, status: 'SUCCESS', log: 'ok'};
      wizardController.launchBootstrapSuccessCallback(data, {}, params);
      expect(params.popup.finishLoading.calledWith(123, null, 'SUCCESS', 'ok')).to.be.true;
    });
  });

  describe('#getInstallOptions', function () {

    var cases = [
        {
          isHadoopWindowsStack: true,
          expected: {
            useSsh: false
          }
        },
        {
          isHadoopWindowsStack: false,
          expected: {
            useSsh: true
          }
        }
      ],
      title = 'should return {0}';

    beforeEach(function () {
      sinon.stub(wizardController, 'get')
        .withArgs('installOptionsTemplate').returns({useSsh: true})
        .withArgs('installWindowsOptionsTemplate').returns({useSsh: false});
      this.stub = sinon.stub(App, 'get');
    });

    afterEach(function () {
      App.get.restore();
      wizardController.get.restore();
    });

    cases.forEach(function (item) {
      it(title.format(item.expected), function () {
        this.stub.withArgs('isHadoopWindowsStack').returns(item.isHadoopWindowsStack);
        expect(wizardController.getInstallOptions()).to.eql(item.expected);
      });
    });

  });

  describe('#clearInstallOptions', function () {

    wizardController.setProperties({
      content: {},
      name: 'wizardController'
    });

    beforeEach(function () {
      sinon.stub(App, 'get').withArgs('isHadoopWindowsStack').returns(false);
    });

    afterEach(function () {
      App.get.restore();
    });

    describe('should clear install options', function () {

      beforeEach(function () {
        wizardController.clearInstallOptions();
      });
      it('content.installOptions', function () {
        expect(wizardController.get('content.installOptions')).to.eql(wizardController.get('installOptionsTemplate'));
      });
      it('content.hosts', function () {
        expect(wizardController.get('content.hosts')).to.eql({});
      });
      it('installOptions', function () {
        expect(wizardController.getDBProperty('installOptions')).to.eql(wizardController.get('installOptionsTemplate'));
      });
      it('hosts', function () {
        expect(wizardController.getDBProperty('hosts')).to.eql({});
      });
    });
  });

  describe('#loadServiceConfigGroups', function () {
     beforeEach(function () {
      sinon.stub(wizardController, 'getDBProperties', function() {
        return {
          serviceConfigGroups: [
            {
              hosts: ['h1']
            }
          ],
          hosts: Em.Object.create({
            h1: Em.Object.create({
              id: 'h1'
            })
          })
        };
      });
    });
    afterEach(function () {
      wizardController.getDBProperties.restore();
    });
    it('should load service confgig group', function () {
      wizardController.loadServiceConfigGroups();
      expect(wizardController.get('content.configGroups')).to.eql([
        {
          "hosts": [
            "h1"
          ]
        }
      ]);
    });
  });

  describe('#saveTasksStatuses', function () {
    it('should set status', function () {
      wizardController.saveTasksStatuses('st');
      expect(wizardController.get('content.tasksStatuses')).to.equal('st');
    });
  });

  describe('#saveSlaveComponentHosts', function () {
    beforeEach(function(){
      sinon.stub(wizardController,'getDBProperty').returns(Em.A({
        'h1': {
          id: 1
        }
      }));
    });
    afterEach(function(){
      wizardController.getDBProperty.restore();
    });
    it('should save slave components', function () {
      var stepController = Em.Object.create({
        hosts: Em.A([
          Em.Object.create({
            hostName: 'h1',
            checkboxes: Em.A([
              Em.Object.create({title: 'hl1', checked: true})
            ])
          })
        ]),
        headers: Em.A([
          Em.Object.create({name: 'header1', label: 'hl1'})
        ])
      });
      wizardController.saveSlaveComponentHosts(stepController);
      var res = JSON.parse(JSON.stringify(wizardController.get('content.slaveComponentHosts')));
      expect(res).to.eql([
        {
          "componentName": "header1",
          "displayName": "hl1",
          "hosts": [
            {
              "group": "Default",
              "host_id": 1
            }
          ]
        }
      ]);
    });
  });

  describe('#showLaunchBootstrapPopup', function () {
    beforeEach(function(){
      App.ModalPopup.show.restore();
    });

    describe('errors', function () {

      beforeEach(function () {
        sinon.stub(App.ModalPopup,'show', function (data) {
          data.finishLoading.call(c);
        });
      });

      it('should set error', function () {
        c.showLaunchBootstrapPopup(Em.K);
        expect(c.get('isError')).to.be.true;
      });
    });

    describe('#finishLoading', function () {
      var stepController = App.get('router.wizardStep3Controller'),
        cases = [
          {
            requestId: null,
            serverError: 'error',
            wizardControllerProperties: {
              isError: true,
              showFooter: true,
              showCloseButton: true,
              serverError: 'error'
            },
            stepControllerProperties: {
              isRegistrationInProgress: false,
              isBootstrapFailed: true
            },
            bootStatus: 'FAILED',
            callbackCallCount: 0,
            hideCallCount: 0,
            title: 'no request id'
          },
          {
            requestId: 0,
            status: 'ERROR',
            log: 'log',
            wizardControllerProperties: {
              isError: true,
              showFooter: true,
              showCloseButton: true,
              serverError: 'log'
            },
            stepControllerProperties: {
              isRegistrationInProgress: false,
              isBootstrapFailed: true
            },
            bootStatus: 'FAILED',
            callbackCallCount: 0,
            hideCallCount: 0,
            title: 'ERROR status'
          },
          {
            requestId: 1,
            log: 'log',
            wizardControllerProperties: {
              isError: false,
              showFooter: false,
              showCloseButton: false,
              serverError: null
            },
            stepControllerProperties: {
              isRegistrationInProgress: true,
              isBootstrapFailed: false
            },
            bootStatus: 'PENDING',
            callbackCallCount: 1,
            hideCallCount: 1,
            title: 'request accepted'
          }
        ];
      beforeEach(function () {
        c.setProperties({
          isError: false,
          showFooter: false,
          showCloseButton: false,
          serverError: null,
          hide: Em.K,
          callback: Em.K
        });
        stepController.setProperties({
          isRegistrationInProgress: true,
          isBootstrapFailed: false,
          hosts: [
            {
              bootStatus: 'PENDING'
            },
            {
              bootStatus: 'PENDING'
            }
          ]
        });
        sinon.spy(c, 'hide');
        sinon.spy(c, 'callback');
      });
      afterEach(function () {
        c.hide.restore();
        c.callback.restore();
      });
      cases.forEach(function (item) {
        describe(item.title, function () {
          var wizardControllerProperties = Em.keys(item.wizardControllerProperties),
            stepControllerProperties = Em.keys(item.stepControllerProperties);

          beforeEach(function () {
            sinon.stub(App.ModalPopup,'show', function (data) {
              data.finishLoading.call(c, item.requestId, item.serverError, item.status, item.log);
            });
            c.showLaunchBootstrapPopup(c.callback);
          });

          it('wizardControllerProperties are valid', function () {
            expect(c.getProperties.apply(c, wizardControllerProperties)).to.eql(item.wizardControllerProperties);
          });

          it('stepControllerProperties are valid', function () {
            expect(stepController.getProperties.apply(stepController, stepControllerProperties)).to.eql(item.stepControllerProperties);
          });

          it('bootStatus is valid', function () {
            expect(stepController.get('hosts').mapProperty('bootStatus').uniq()).to.eql([item.bootStatus]);
          });

          it('callback is called needed number of times', function () {
            expect(c.callback.callCount).to.equal(item.callbackCallCount);
          });

          it('hide is called needed number of times', function () {
            expect(c.hide.callCount).to.equal(item.hideCallCount);
          });
        });
      });
    });
  });

  describe('#gotoStep0', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 0 step', function () {
      wizardController.gotoStep0(Em.K);
      expect(res).to.be.equal(0);
    });
  });

  describe('#gotoStep1', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 1 step', function () {
      wizardController.gotoStep1(Em.K);
      expect(res).to.be.equal(1);
    });
  });

  describe('#gotoStep2', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 2 step', function () {
      wizardController.gotoStep2(Em.K);
      expect(res).to.be.equal(2);
    });
  });

  describe('#gotoSte3', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 3 step', function () {
      wizardController.gotoStep3(Em.K);
      expect(res).to.be.equal(3);
    });
  });

  describe('#gotoStep4', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 4 step', function () {
      wizardController.gotoStep4(Em.K);
      expect(res).to.be.equal(4);
    });
  });

  describe('#gotoStep5', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 5 step', function () {
      wizardController.gotoStep5(Em.K);
      expect(res).to.be.equal(5);
    });
  });

  describe('#gotoStep6', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 6 step', function () {
      wizardController.gotoStep6(Em.K);
      expect(res).to.be.equal(6);
    });
  });

  describe('#gotoStep7', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 7 step', function () {
      wizardController.gotoStep7(Em.K);
      expect(res).to.be.equal(7);
    });
  });

  describe('#gotoStep8', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 8 step', function () {
      wizardController.gotoStep8(Em.K);
      expect(res).to.be.equal(8);
    });
  });

  describe('#gotoStep9', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 9 step', function () {
      wizardController.gotoStep9(Em.K);
      expect(res).to.be.equal(9);
    });
  });

  describe('#gotoStep10', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 10 step', function () {
      wizardController.gotoStep10(Em.K);
      expect(res).to.be.equal(10);
    });
  });

  describe('#gotoStep', function () {
    beforeEach(function(){
      sinon.stub(App.clusterStatus,'setClusterStatus', Em.K);
      sinon.stub(App.router,'send', Em.K);  
    });
    afterEach(function(){
      App.clusterStatus.setClusterStatus.restore();
      App.router.send.restore();
    });
    it('should go to step', function () {
      wizardController.set('isStepDisabled', Em.A([
        Em.Object.create({
          step: '8',
          value: false
        })
      ]));
      wizardController.hide = Em.K;
      wizardController.set('content.controllerName','installerController');
      wizardController.set('currentStep','9');

      expect(wizardController.gotoStep('8')).to.be.true;
    });
  });

  describe('#launchBootstrap', function () {
    beforeEach(function(){
      sinon.stub(wizardController,'showLaunchBootstrapPopup').returns({
        name: 'popup'
      });
    });
    afterEach(function(){
      wizardController.showLaunchBootstrapPopup.restore();
    });
    it('should return popup', function () {
      expect(wizardController.launchBootstrap()).to.be.eql({
        name: 'popup'
      });
    });
  });

  describe('#save', function () {
    var res;
    beforeEach(function () {
      sinon.stub(wizardController,'setDBProperty', function(data){
        res = data;
      });
      sinon.stub(wizardController,'toJSInstance').returns('val');
    });

    afterEach(function () {
      wizardController.setDBProperty.restore();
      wizardController.toJSInstance.restore();
    });

    it('should save data', function () {
      wizardController.save('name');
      expect(res).to.be.equal('name');
    });
  });

  describe('#installServicesSuccessCallback', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'saveClusterStatus', function(data){
        res = JSON.parse(JSON.stringify(data));
      });
      sinon.stub(App,'dateTime').returns('22');
    });
    afterEach(function(){
      wizardController.saveClusterStatus.restore();
      App.dateTime.restore();
    });
    it('should call callbeck with data', function () {
      var jsonData = {
        Requests: {
          id: 1
        }
      };
      wizardController.installServicesSuccessCallback(jsonData);
      expect(res).to.be.eql({
        "status": "PENDING",
        "requestId": 1,
        "isInstallError": false,
        "isCompleted": false,
        "installStartTime": "22"
      });
    });
  });

  describe('#installServices', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'saveClusterStatus', function(data){
        res = JSON.parse(JSON.stringify(data));
      });
    });
    afterEach(function(){
      wizardController.saveClusterStatus.restore();
    });
    it('should call callbeck with data', function () {
      wizardController.set('content', Em.Object.create({
        cluster: {
          oldRequestsId: '1'
        }
      }));
      wizardController.installServices(true);
      expect(res).to.be.eql({
        "status": "PENDING"
      });
    });
  });

  describe('#saveInstalledHosts', function () {
    beforeEach(function(){
      sinon.stub(wizardController,'getDBProperty').returns({
        'h1': {
          id: 1,
          status: '',
          name: 'h1'
        }
      });
    });
    afterEach(function(){
      wizardController.getDBProperty.restore();
    });
    it('should save installed hosts', function () {
      var stepController = Em.Object.create({
        hosts: Em.A([
          Em.Object.create({
            hostName: 'h1',
            name: 'h1',
            status: 'st',
            message: 'ms',
            checkboxes: Em.A([
              Em.Object.create({title: 'hl1', checked: true})
            ])
          })
        ])
      });
      wizardController.saveInstalledHosts(stepController);
      var res = JSON.parse(JSON.stringify(wizardController.get('content.hosts')));
      expect(res).to.eql({
        "h1": {
          "id": 1,
          "status": "st",
          "name": "h1",
          "message": "ms"
        }
      });
    });
  });

  describe('#saveConfirmedHosts', function () {
    beforeEach(function(){
      sinon.stub(wizardController,'getDBProperty').returns({
        'h1': {
          id: 1,
          status: '',
          name: 'h1'
        }
      });
    });
    afterEach(function(){
      wizardController.getDBProperty.restore();
    });
    it('should save confirmed hosts', function () {
      var stepController = Em.Object.create({
        confirmedHosts: Em.A([
          {
            name: 'h2',
            cpu: '1',
            isInstalled: true
          }
        ])
      });
      wizardController.set('content.hosts', {
        'h1': {
          isInstalled: false,
          bootStatus: 'REGISTERED'
        },
        'h2': {
          isInstalled: true,
          bootStatus: 'REGISTERED'
        }
      });
      wizardController.saveConfirmedHosts(stepController);
      var res = JSON.parse(JSON.stringify(wizardController.get('content.hosts')));
      expect(res).to.eql({
        "h2": {
          "isInstalled": true,
          "bootStatus": "REGISTERED"
        }
      });
    });
  });

  describe('#loadTasksStatuses', function () {
    beforeEach(function () {
      sinon.stub(wizardController, 'getDBProperty').returns('st');
    });
    afterEach(function () {
      wizardController.getDBProperty.restore();
    });
    it('should load status', function () {
      wizardController.loadTasksStatuses();
      expect(wizardController.get('content.tasksStatuses')).to.equal('st');
    });
  });

  describe('#saveTasksRequestIds', function () {
    it('should save id', function () {
      wizardController.saveTasksRequestIds('st');
      expect(wizardController.get('content.tasksRequestIds')).to.equal('st');
    });
  });

  describe('#loadTasksRequestIds', function () {
    beforeEach(function () {
      sinon.stub(wizardController, 'getDBProperty').returns('st');
    });
    afterEach(function () {
      wizardController.getDBProperty.restore();
    });
    it('should load status', function () {
      wizardController.loadTasksRequestIds();
      expect(wizardController.get('content.tasksRequestIds')).to.equal('st');
    });
  });

  describe('#saveRequestIds', function () {
    it('should save id', function () {
      wizardController.saveRequestIds('st');
      expect(wizardController.get('content.requestIds')).to.equal('st');
    });
  });

  describe('#load', function () {
    it('should clear install options', function () {
      var name = 'Name';
      wizardController.set('get'+name.capitalize(), function() {return 'res';});
      wizardController.load(name, true);
      expect(wizardController.get('content.' + name)).to.equal('res');
    });
  });

  describe('#usersLoading', function () {
    beforeEach(function(){
      sinon.stub(App.MainAdminServiceAccountsController,'create').returns({
        loadUsers: function() {},
        get: function(type) {
          if (type === 'dataIsLoaded') {
            return true;
          }
          return Em.Object.create({
            hdfsUser: {
              name: 'user'
            }
          });
        }
      });
    });
    afterEach(function(){
      App.MainAdminServiceAccountsController.create.restore();
    });
    it('should load users', function () {
      wizardController.set('content.hdfsUser', true);
      wizardController.usersLoading().then(function(data){
        expect(data).to.be.undefined;
      });
    });
  });

  describe('#loadConfirmedHosts', function () {
    beforeEach(function(){
      sinon.stub(wizardController, 'getDBProperty').returns(Em.A([
        Em.Object.create({
          name: 'h1'
        })
      ]));
    });
    afterEach(function(){
      wizardController.getDBProperty.restore();
    });
    it('should load hosts from db', function () {
      wizardController.loadConfirmedHosts();
      var res = JSON.parse(JSON.stringify(wizardController.get('content.hosts')));
      expect(res).to.eql([
        {
          "name": "h1"
        }
      ]);
    });
  });

  describe('#loadServicesFromServer', function () {//TODO
    var res;
    beforeEach(function(){
      sinon.stub(App.StackService, 'find').returns(Em.A([
        Em.Object.create({
          isSelected: false,
          isInstalled: false,
          serviceName: 's1'
        })
      ]));
      sinon.stub(App.Service, 'find').returns(Em.A([
        Em.Object.create({
          isSelected: false,
          isInstalled: false,
          serviceName: 's1'
        })
      ]));
      sinon.stub(wizardController, 'setDBProperty', function(data) {
        res = data;
      });
    });
    
    afterEach(function () {
      App.StackService.find.restore();
      App.Service.find.restore();
      wizardController.setDBProperty.restore();
    });
    it('should load services from server', function () {
      wizardController.loadServicesFromServer();
      expect(res).to.be.equal('services');
    });
  });

  describe('#loadRequestIds', function () {
    beforeEach(function () {
      sinon.stub(wizardController, 'getDBProperty').returns('st');
    });
    afterEach(function () {
      wizardController.getDBProperty.restore();
    });
    it('should load status', function () {
      wizardController.loadRequestIds();
      expect(wizardController.get('content.requestIds')).to.equal('st');
    });
  });

  describe('#loadServiceComponentsSuccessCallback', function () {
    beforeEach(function () {
      sinon.stub(wizardController, 'getDBProperties', function() {
        return {
          selectedServiceNames: ['a','b'],
          installedServiceNames: ['c','d']
        };
      });
      sinon.stub(App.stackServiceMapper, 'mapStackServices', Em.K); 
    });
    afterEach(function () {
      wizardController.getDBProperties.restore();
      App.stackServiceMapper.mapStackServices.restore();
    });
    it('should load json data', function () {
      var jsonData = {
        items: [
          {
            StackServices: {
              isSelected: false,
              service_name: 'a'
            }
          },
          {
            StackServices: {
              isSelected: false,
              service_name: 'none'
            }
          }
        ]
      };
      wizardController.loadServiceComponentsSuccessCallback(jsonData);
      var exp = {
        "items": [
          {
            "StackServices": {
              "isSelected": false,
              "service_name": "a",
              "is_selected": true,
              "is_installed": false
            }
          },
          {
            "StackServices": {
              "isSelected": false,
              "service_name": "none",
              "is_selected": false,
              "is_installed": false
            }
          }
        ]
      };

      expect(jsonData).to.eql(exp);
    });
  });

  describe('#setInfoForStep9', function () {

    var res;

    beforeEach(function () {
      sinon.stub(wizardController, 'getDBProperty').returns(Em.Object.create({
        status: {},
        message: {},
        logTasks: {},
        tasks: {},
        progress: {}
      }));
      sinon.stub(wizardController, 'setDBProperty', function(title,data) {
        res = data;
      });
    });

    afterEach(function () {
      wizardController.getDBProperty.restore();
      wizardController.setDBProperty.restore();
    });

    it('should return info for step 9', function () {
      wizardController.setInfoForStep9();
      var exp = {
        "status": {
          "status": "pending",
          "message": "Waiting",
          "logTasks": [],
          "tasks": [],
          "progress": "0"
        },
        "message": {
          "status": "pending",
          "message": "Waiting",
          "logTasks": [],
          "tasks": [],
          "progress": "0"
        },
        "logTasks": {
          "status": "pending",
          "message": "Waiting",
          "logTasks": [],
          "tasks": [],
          "progress": "0"
        },
        "tasks": {
          "status": "pending",
          "message": "Waiting",
          "logTasks": [],
          "tasks": [],
          "progress": "0"
        },
        "progress": {
          "status": "pending",
          "message": "Waiting",
          "logTasks": [],
          "tasks": [],
          "progress": "0"
        }
      };

      res = JSON.parse(JSON.stringify(res));

      expect(res).to.eql(exp);
    });
  });

  describe('#saveServiceConfigProperties', function () {

    beforeEach(function () {
      c.set('content', {});
      sinon.stub(c, 'setDBProperty', Em.K);
      sinon.stub(c, 'setDBProperties', Em.K);
      sinon.stub(c, 'getDBProperty').withArgs('fileNamesToUpdate').returns([]);
      sinon.stub(App.config, 'shouldSupportFinal').returns(true);
    });

    afterEach(function () {
      c.setDBProperty.restore();
      c.setDBProperties.restore();
      c.getDBProperty.restore();
      App.config.shouldSupportFinal.restore();
    });

    var kerberosStepController = Em.Object.create({
      installedServiceNames: ['KERBEROS'],
      stepConfigs: [
        Em.Object.create({
          serviceName: 'KERBEROS',
          configs: [
            Em.Object.create({
              id: 'id',
              name: 'admin_password',
              value: 'value',
              defaultValue: 'defaultValue',
              description: 'description',
              serviceName: 'serviceName',
              domain: 'domain',
              isVisible: true,
              isNotDefaultValue: true,
              isFinal: true,
              defaultIsFinal: true,
              supportsFinal: true,
              filename: 'krb5-conf.xml',
              displayType: 'string',
              isRequiredByAgent: true,
              hasInitialValue: true,
              isRequired: true,
              group: {name: 'group'},
              showLabel: true,
              category: 'some_category'
            }),

            Em.Object.create({
              id: 'id',
              name: 'admin_principal',
              value: 'value',
              defaultValue: 'defaultValue',
              description: 'description',
              serviceName: 'serviceName',
              domain: 'domain',
              isVisible: true,
              isNotDefaultValue: true,
              isFinal: true,
              defaultIsFinal: true,
              supportsFinal: true,
              filename: 'krb5-conf.xml',
              displayType: 'string',
              isRequiredByAgent: true,
              hasInitialValue: true,
              isRequired: true,
              group: {name: 'group'},
              showLabel: true,
              category: 'some_category'
            })
          ]
        })
      ]
    });

    var stepController = Em.Object.create({
      installedServiceNames: ['HDFS'],
      stepConfigs: [
      Em.Object.create({
        serviceName: 'HDFS',
        configs: [
          Em.Object.create({
            id: 'id',
            name: 'name',
            value: 'value',
            defaultValue: 'defaultValue',
            description: 'description',
            serviceName: 'serviceName',
            domain: 'domain',
            isVisible: true,
            isNotDefaultValue: true,
            isFinal: true,
            defaultIsFinal: true,
            supportsFinal: true,
            filename: 'hdfs-site',
            displayType: 'string',
            isRequiredByAgent: true,
            hasInitialValue: true,
            isRequired: true,
            isUserProperty: true,
            showLabel: true,
            category: 'some_category'
          }),
          Em.Object.create({
            id: 'id',
            name: 'name2',
            value: 'value',
            defaultValue: 'defaultValue',
            description: 'description',
            serviceName: 'serviceName',
            domain: 'domain',
            isVisible: true,
            isNotDefaultValue: true,
            isFinal: true,
            defaultIsFinal: true,
            supportsFinal: true,
            filename: 'hdfs-site',
            displayType: 'string',
            isRequiredByAgent: true,
            hasInitialValue: true,
            isRequired: false,
            isUserProperty: false,
            showLabel: true,
            category: 'some_category'
          })
        ]
      }),
      Em.Object.create({
        serviceName: 'YARN',
        configs: [
          Em.Object.create({
            id: 'id',
            name: 'name',
            value: 'value',
            defaultValue: 'defaultValue',
            description: 'description',
            serviceName: 'serviceName',
            domain: 'domain',
            isVisible: true,
            isFinal: true,
            defaultIsFinal: true,
            supportsFinal: true,
            filename: 'filename',
            displayType: 'string',
            isRequiredByAgent: true,
            hasInitialValue: true,
            isRequired: true,
            isUserProperty: false,
            group: {name: 'group'},
            showLabel: true,
            category: 'some_category'
          })
        ]
      })
    ]});

    it('should save configs from default config group to content.serviceConfigProperties', function () {
      c.saveServiceConfigProperties(stepController);
      var saved = c.get('content.serviceConfigProperties');
      expect(saved.length).to.equal(2);
      expect(saved[0].category).to.equal('some_category');
    });

    it('should not save admin_principal or admin_password to the localStorage', function () {
      c.saveServiceConfigProperties(kerberosStepController);
      var saved = c.get('content.serviceConfigProperties');
      expect(saved.everyProperty('value', '')).to.be.true;
    });

    it('should save `isUserProperty` and `isRequired` attributes correctly', function() {
      c.saveServiceConfigProperties(stepController);
      var saved = c.get('content.serviceConfigProperties'),
          nameProp = saved.filterProperty('filename', 'hdfs-site.xml').findProperty('name', 'name'),
          name2Prop = saved.filterProperty('filename', 'hdfs-site.xml').findProperty('name', 'name2');
      assert.isTrue(Em.get(nameProp, 'isRequired'), 'hdfs-site.xml:name isRequired validation');
      assert.isTrue(Em.get(nameProp, 'isUserProperty'), 'hdfs-site.xml:name isUserProperty validation');
      assert.isFalse(Em.get(name2Prop, 'isRequired'), 'hdfs-site.xml:name2 isRequired validation');
      assert.isFalse(Em.get(name2Prop, 'isUserProperty'), 'hdfs-site.xml:name2 isUserProperty validation');
    });
  });

  describe('#enableStep', function () {

    beforeEach(function () {
      c.set('isStepDisabled', [
        Em.Object.create({step: 1, value: true}),
        Em.Object.create({step: 2, value: true}),
        Em.Object.create({step: 3, value: true}),
        Em.Object.create({step: 4, value: true}),
        Em.Object.create({step: 5, value: true}),
        Em.Object.create({step: 6, value: true}),
        Em.Object.create({step: 7, value: true})
      ]);
    });

    it('should update 1st value in isStepDisabled', function () {
      c.enableStep(1);
      expect(c.get('isStepDisabled')[0].get('value')).to.be.false;
    });

    it('should update 6th value in isStepDisabled', function () {
      c.enableStep(7);
      expect(c.get('isStepDisabled')[6].get('value')).to.be.false;
    });

  });

  describe('#allHosts', function () {

    it('should return all hosts', function () {
      var hosts = {
        'h1': {hostComponents: ['c1', 'c2']},
        'h2': {hostComponents: ['c3', 'c4']}
      };

      var content = Em.Object.create({
        hosts: hosts
      });

      c.set('content', content);

      var exp = [
        {
          "id": "h1",
          "hostName": "h1",
          "hostComponents": [
            {
              "componentName": "c1",
              "displayName": "C1"
            },
            {
              "componentName": "c2",
              "displayName": "C2"
            }
          ]
        },
        {
          "id": "h2",
          "hostName": "h2",
          "hostComponents": [
            {
              "componentName": "c3",
              "displayName": "C3"
            },
            {
              "componentName": "c4",
              "displayName": "C4"
            }
          ]
        }
      ];

      var res = JSON.parse(JSON.stringify(c.get('allHosts')));

      expect(res).to.be.eql(exp);
    });
  });

  describe('#getSlaveComponentHosts', function () {
    beforeEach(function () {
      sinon.stub(App.Service, 'find').returns(Em.A([
        Em.Object.create({
          serviceName: 's1'
        })
      ]));
      sinon.stub(App.StackService, 'find').returns(Em.A([
        Em.Object.create({
          serviceName: 's2',
          isSelected: true
        })
      ]));
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.A([
        Em.Object.create({componentName: 'DATANODE', serviceName: 's1', isSlave: true}),
        Em.Object.create({componentName: 'c2', serviceName: 's2', isSlave: true})
      ]));
      sinon.stub(App.HostComponent, 'find').returns(Em.A([
        Em.Object.create({
          componentName: 'DATANODE',
          hostName: 'h1'
        })
      ]));
    });

    afterEach(function () {
      App.Service.find.restore();
      App.HostComponent.find.restore();
      App.StackService.find.restore();
      App.StackServiceComponent.find.restore();
    });

    it('should return slave components', function () {
      var res = JSON.parse(JSON.stringify(c.getSlaveComponentHosts()));
      var exp = [
        {
          "componentName": "DATANODE",
          "displayName": "DataNode",
          "hosts": [
            {
              "group": "Default",
              "hostName": "h1",
              "isInstalled": true
            }
          ],
          "isInstalled": true
        },
        {
          "componentName": "CLIENT",
          "displayName": "Client",
          "hosts": [],
          "isInstalled": true
        },
        {
          "componentName": "c2",
          "displayName": "C2",
          "hosts": [
            {
              "group": "Default",
              "hostName": "h1",
              "isInstalled": false
            }
          ],
          "isInstalled": false
        }
      ];

      expect(res).to.be.eql(exp);
    });

  });

  describe('#setSkipSlavesStep', function () {

    var step = 6,
      cases = [
        {
          services: [
            {
              hasSlave: true,
              hasNonMastersWithCustomAssignment: true
            }
          ],
          skipSlavesStep: false,
          title: 'service with customizable slave selected'
        },
        {
          services: [
            {
              hasClient: true,
              hasNonMastersWithCustomAssignment: true
            }
          ],
          skipSlavesStep: false,
          title: 'service with customizable client selected'
        },
        {
          services: [
            {
              hasSlave: true,
              hasNonMastersWithCustomAssignment: false
            },
            {
              hasClient: true,
              hasNonMastersWithCustomAssignment: false
            }
          ],
          skipSlavesStep: true,
          title: 'no service with customizable slaves or clients selected'
        },
        {
          services: [
            {
              hasSlave: false,
              hasClient: false
            }
          ],
          skipSlavesStep: true,
          title: 'no service with slaves or clients selected'
        }
      ];

    beforeEach(function () {
      c.reopen({
        isStepDisabled: [
          Em.Object.create({
            step: 6
          })
        ],
        content: {}
      });
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        c.setSkipSlavesStep(item.services, step);
        expect(Boolean(c.get('isStepDisabled').findProperty('step', step).get('value'))).to.equal(item.skipSlavesStep);
      });
    });

  });

  describe('#toJSInstance', function () {

    var testCases = [
      {
        o: {'test': 'test'},
        e: {'test': 'test'}
      },
      {
        o: {'test': Em.Object.create()},
        e: {'test': {}}
      },
      {
        o: {'test': Em.Object.create({'test': {}})},
        e: {'test': {'test': {}}}
      },
      {
        o: [],
        e: []
      },
      {
        o: Em.A([[]]),
        e: [[]]
      },
      {
        o: 11,
        e: 11
      },
      {
        o: '11',
        e: '11'
      },
      {
        o: null,
        e: null
      }
    ];

    testCases.forEach(function (testCase, index) {
      it('should convert objects and arrays to pure JS objects and arrays (' + (index + 1) + ')', function () {
        expect(c.toJSInstance(testCase.o)).to.eql(testCase.e);
      });
    });
  });

  describe('#loadConfigThemes', function() {
    beforeEach(function () {
      sinon.stub(wizardController, 'loadConfigThemeForServices').returns({
        always: Em.clb
      });
      sinon.stub(App.themesMapper, 'generateAdvancedTabs').returns(true);
      sinon.stub(App.config, 'loadConfigsFromStack').returns({
        done: Em.clb
      });
      sinon.stub(App.StackService, 'find').returns(Em.A([
        Em.Object.create({
          isSelected: true,
          serviceName: 's1'
        })
      ]));
      this.stub = sinon.stub(App, 'get');
    });
    afterEach(function () {
      App.get.restore();
      App.StackService.find.restore();
      App.config.loadConfigsFromStack.restore();
      App.themesMapper.generateAdvancedTabs.restore();
      wizardController.loadConfigThemeForServices.restore();
    });
    it('Should load config themes', function(done) {
      this.stub.returns(true);
      wizardController.loadConfigThemes().then(function() {
        done();
      });
    });
    it('Should load config themes (2)', function(done) {
      this.stub.returns(false);
      wizardController.loadConfigThemes().then(function() {
        done();
      });
    });
  });

  describe('#dataLoading', function () {
    var clusterController = Em.Object.create({
      isLoaded: false
    });
    beforeEach(function(){
      sinon.stub(App.router,'get').returns(clusterController);
      sinon.stub(wizardController, 'connectOutlet', Em.K);
      clusterController.set('isLoaded', false);
    });
    afterEach(function(){
      App.router.get.restore();
      wizardController.connectOutlet.restore();
    });
    it('should load data', function () {
      clusterController.set('isLoaded', true);
      wizardController.dataLoading().then(function(data){
        expect(data).to.be.undefined;
      });
    });
    it('should load data after 25ms', function () {
      clusterController.set('isLoaded', false);
      setTimeout(function(){
        clusterController.set('isLoaded', true);
      },25);
      wizardController.dataLoading().then(function(data){
        expect(data).to.be.undefined;
      });
    });
  });

  describe('#saveMasterComponentHosts', function () {

    var stepController = Em.Object.create({
        selectedServicesMasters: [
          Em.Object.create({
            display_name: 'd0',
            component_name: 'c0',
            selectedHost: 'h0',
            serviceId: 's0',
            isInstalled: true
          }),
          Em.Object.create({
            display_name: 'd1',
            component_name: 'c1',
            selectedHost: 'h1',
            serviceId: 's1',
            isInstalled: false
          })
        ]
      }),
      masterComponentHosts = [
        {
          display_name: 'd0',
          component: 'c0',
          hostName: 'h0',
          serviceId: 's0',
          isInstalled: true
        },
        {
          display_name: 'd1',
          component: 'c1',
          hostName: 'h1',
          serviceId: 's1',
          isInstalled: false
        }
      ];

    beforeEach(function () {
      sinon.stub(wizardController, 'setDBProperty', Em.K);
    });

    afterEach(function () {
      wizardController.setDBProperty.restore();
    });

    it('should save master component hosts', function () {
      wizardController.saveMasterComponentHosts(stepController);
      expect(wizardController.setDBProperty.calledOnce).to.be.true;
      expect(wizardController.setDBProperty.calledWith('masterComponentHosts', masterComponentHosts)).to.be.true;
      expect(wizardController.get('content.masterComponentHosts')).to.eql(masterComponentHosts);
    });

  });

  describe('#clearMasterComponentHosts', function () {

    beforeEach(function () {
      sinon.stub(wizardController, 'setDBProperty', Em.K);
    });

    afterEach(function () {
      wizardController.setDBProperty.restore();
    });

    it('should clear master component hosts', function () {
      wizardController.set('content.masterComponentHosts', {});
      wizardController.clearMasterComponentHosts();
      expect(wizardController.setDBProperty.calledOnce).to.be.true;
      expect(wizardController.setDBProperty.calledWith('masterComponentHosts', null)).to.be.true;
      expect(wizardController.get('content.masterComponentHosts')).to.be.null;
    });

  });

  describe('#loadRecommendations', function () {

    beforeEach(function () {
      sinon.stub(c, 'getDBProperty').returns({});
    });

    afterEach(function () {
      c.getDBProperty.restore();
    });

    it('should set recommendations', function () {
      c.set('content', {});
      c.loadRecommendations();
      expect(c.get('content.recommendations')).to.eql({});
    });

  });

  describe("#resetOnClose()", function () {
    var ctrl = Em.Object.create({
      finish: Em.K,
      popup: {
        hide: Em.K
      }
    });

    var mock = Em.Object.create({
      resetUser: Em.K
    });

    beforeEach(function () {
      sinon.stub(ctrl, 'finish');
      sinon.stub(ctrl.popup, 'hide');
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(App.clusterStatus, 'setClusterStatus', function (arg1, arg2) {
        arg2.alwaysCallback();
      });
      sinon.stub(Em.run, 'next');
      sinon.stub(mock, 'resetUser');
      sinon.stub(App.router, 'transitionTo');

      c.resetOnClose(ctrl, 'path');
    });

    afterEach(function () {
      ctrl.finish.restore();
      ctrl.popup.hide.restore();
      App.router.get.restore();
      App.clusterStatus.setClusterStatus.restore();
      Em.run.next.restore();
      mock.resetUser.restore();
      App.router.transitionTo.restore();
    });

    it("resetUser should be called", function () {
      expect(mock.resetUser.calledOnce).to.be.true;
    });

    it("finish should be called", function () {
      expect(ctrl.finish.calledOnce).to.be.true;
    });

    it("App.clusterStatus.setClusterStatus should be called", function () {
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });

    it("popup should be hidden", function () {
      expect(ctrl.get('popup').hide.calledOnce).to.be.true;
    });

    it("App.router.transitionTo should be called", function () {
      expect(App.router.transitionTo.calledOnce).to.be.true;
    });

    it("Em.run.next should be called", function () {
      expect(Em.run.next.calledOnce).to.be.true;
    });
  });

  describe('#applyStoredConfigs', function() {

    it('should return null when storedConfigs null', function() {
      expect(c.applyStoredConfigs([], null)).to.be.null;
    });

    it('should merged configs when storedConfigs has items', function() {
      var storedConfigs = [
        {
          id: 1,
          value: 'foo',
          isFinal: false
        },
        {
          id: 2,
          value: 'foo2',
          isFinal: true,
          isUserProperty: true
        }
      ];
      var configs = [
        {
          id: 1,
          value: '',
          isFinal: true
        }
      ];
      expect(c.applyStoredConfigs(configs, storedConfigs)).to.be.eql([
        {
          id: 1,
          value: 'foo',
          isFinal: false,
          savedValue: null
        },
        {
          id: 2,
          value: 'foo2',
          isFinal: true,
          isUserProperty: true
        }
      ]);
    });
  });
  
  describe('#setStackServiceSelectedByDefault', function() {
   
    it('regular service should be selected', function() {
      var service = {
        StackServices: {
          selection: null,
          service_name: 'S1'
        }
      };
      c.setStackServiceSelectedByDefault(service);
      expect(service.StackServices.is_selected).to.be.true;
    });
  
    it('TECH_PREVIEW service should not be selected', function() {
      var service = {
        StackServices: {
          selection: "TECH_PREVIEW",
          service_name: 'S1'
        }
      };
      c.setStackServiceSelectedByDefault(service);
      expect(service.StackServices.is_selected).to.be.false;
    });
  
    it('service_type service should not be selected', function() {
      var service = {
        StackServices: {
          selection: null,
          service_name: 'S1',
          service_type: 'HCFS'
        }
      };
      c.setStackServiceSelectedByDefault(service);
      expect(service.StackServices.is_selected).to.be.false;
    });
  });

});
