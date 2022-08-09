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
var testHelpers = require('test/helpers');

describe('App.KerberosWizardStep3Controller', function() {
  var controller;

  beforeEach(function() {
    controller = App.KerberosWizardStep3Controller.create({});
  });

  describe('#onTestKerberosError', function() {

    beforeEach(function(){
      sinon.stub(App.ajax, 'defaultErrorHandler', Em.K);
      sinon.stub(controller, 'onTaskError', Em.K);
    });

    afterEach(function(){
      App.ajax.defaultErrorHandler.restore();
      controller.onTaskError.restore();
    });

    it('should call App.ajax.defaultErrorHandler and onTaskError', function () {
      controller.onTestKerberosError({status: 3}, null, null, {url: 1, type: 2});
      expect(App.ajax.defaultErrorHandler.calledWith({status: 3}, 1, 2, 3)).to.be.true;
      expect(controller.onTaskError.calledOnce).to.be.true;
    });
  });

  describe("#loadStep()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'enableDisablePreviousSteps');
    });

    afterEach(function() {
      controller.enableDisablePreviousSteps.restore();
    });

    it("enableDisablePreviousSteps should be called", function() {
      controller.loadStep();
      expect(controller.enableDisablePreviousSteps.calledOnce).to.be.true;
    });
  });

  describe("#installKerberos()", function () {

    beforeEach(function() {
      this.mock = sinon.stub(controller, 'getKerberosClientState');
      sinon.stub(controller, 'updateComponent');
    });

    afterEach(function() {
      this.mock.restore();
      controller.updateComponent.restore();
    });

    it("not INIT state", function() {
      this.mock.returns({
        done: function(callback) {
          callback({ServiceComponentInfo: {
            state: ""
          }});
        }
      });
      App.set('allHostNames', ['host1']);
      controller.installKerberos();
      expect(controller.updateComponent.calledWith('KERBEROS_CLIENT', ['host1'], "KERBEROS", "Install")).to.be.true;
    });

    it("INIT state", function() {
      this.mock.returns({
        done: function(callback) {
          callback({ServiceComponentInfo: {
            state: "INIT"
          }});
        }
      });
      controller.installKerberos();
      var args = testHelpers.findAjaxRequest('name', 'common.services.update');
      expect(args[0]).to.be.eql({
        name: 'common.services.update',
        sender: controller,
        data: {
          context: Em.I18n.t('requestInfo.kerberosService'),
          ServiceInfo: {"state": "INSTALLED"},
          urlParams: "ServiceInfo/state=INSTALLED&ServiceInfo/service_name=KERBEROS"
        },
        success: 'startPolling',
        error: 'onTaskError'
      });
    });
  });

  describe("#getKerberosClientState()", function () {

    it("App.ajax.send should be called", function() {
      controller.setProperties({
        serviceName: 'S1',
        componentName: 'C1'
      });
      controller.getKerberosClientState();
      var args = testHelpers.findAjaxRequest('name', 'common.service_component.info');
      expect(args[0]).to.be.eql({
        name: 'common.service_component.info',
        sender: controller,
        data: {
          serviceName: 'S1',
          componentName: 'C1',
          urlParams: "fields=ServiceComponentInfo/state"
        }
      });
    });
  });

  describe("#testKerberos()", function () {

    it("App.ajax.send should be called", function() {
      controller.testKerberos();
      var args = testHelpers.findAjaxRequest('name', 'service.item.smoke');
      expect(args[0]).to.exists;
    });
  });

  describe("#onTestKerberosError()", function () {

    beforeEach(function() {
      sinon.stub(App.ajax, 'defaultErrorHandler');
      sinon.stub(controller, 'onTaskError');
      controller.onTestKerberosError({status: 's1'}, {}, "error", {type: 't1', url: 'u1'});
    });

    afterEach(function() {
      App.ajax.defaultErrorHandler.restore();
      controller.onTaskError.restore();
    });

    it("App.ajax.defaultErrorHandler should be called", function() {
      expect(App.ajax.defaultErrorHandler.calledWith({status: 's1'}, 'u1', 't1', 's1')).to.be.true;
    });

    it("onTaskError should be called", function() {
      expect(controller.onTaskError.calledWith({status: 's1'}, {}, "error", {type: 't1', url: 'u1'})).to.be.true;
    });
  });

  describe("#enableDisablePreviousSteps()", function () {
    var mock = {
      setStepsEnable: Em.K,
      setLowerStepsDisable: Em.K
    };

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(mock, 'setStepsEnable');
      sinon.stub(mock, 'setLowerStepsDisable');
    });

    afterEach(function() {
      App.router.get.restore();
      mock.setStepsEnable.restore();
      mock.setLowerStepsDisable.restore();
    });

    it("setLowerStepsDisable should be called", function() {
      controller.set('tasks', [{
        status: 'COMPLETED'
      }]);
      controller.enableDisablePreviousSteps();
      expect(mock.setLowerStepsDisable.calledWith(3)).to.be.true;
    });

    it("setStepsEnable should be called", function() {
      controller.set('tasks', [{
        status: 'FAILED'
      }]);
      controller.enableDisablePreviousSteps();
      expect(mock.setStepsEnable.called).to.be.true;
    });
  });

  describe("#ignoreAndProceed()", function () {

    it("isSubmitDisabled should be true", function() {
      controller.setProperties({
        showIgnore: true,
        ignore: false
      });
      controller.ignoreAndProceed();
      expect(controller.get('isSubmitDisabled')).to.be.true;
    });

    it("isSubmitDisabled should be false", function() {
      controller.setProperties({
        showIgnore: true,
        ignore: true
      });
      controller.ignoreAndProceed();
      expect(controller.get('isSubmitDisabled')).to.be.true;
    });

    it("isSubmitDisabled should not be changed", function() {
      controller.setProperties({
        showIgnore: false,
        ignore: true,
        isSubmitDisabled: false
      });
      controller.ignoreAndProceed();
      expect(controller.get('isSubmitDisabled')).to.be.false;
    });

  });

  describe('#statusDidChange', function() {
    var cases;
    beforeEach(function() {
      controller.set('status', 'PENDING');
      controller.set('tasks', [
        Em.Object.create({
          id: 0,
          status: 'COMPLETED'
        }),
        Em.Object.create({
          id: 1,
          status: 'COMPLETED'
        })
      ]);
      this.getHeartbeatLostHostsStub = sinon.stub(controller, 'getHeartbeatLostHosts');
    });
    afterEach(function() {
      controller.getHeartbeatLostHosts.restore();
    });

    cases = [
      {
        m: 'Heartbeat lost host during kerberization',
        heartBeatHosts: ['host1'],
        getHeartbeatLostHostsResponse: {
          items: [
            {
              Hosts: {
                host_name: 'host1'
              }
            }
          ]
        },
        expected: {
          heartbeatHosts: ['host1'],
          installClientsTaskStatus: ['FAILED']
        }
      },
      {
        m: 'All hosts in HEALTHY state',
        heartBeatHosts: [],
        getHeartbeatLostHostsResponse: {
          items: []
        },
        expected: {
          heartbeatHosts: [],
          installClientsTaskStatus: ['COMPLETED']
        }
      }
    ];

    cases.forEach(function(test) {
      it(test.m, function() {
        this.getHeartbeatLostHostsStub.returns($.Deferred().resolve(test.getHeartbeatLostHostsResponse).promise());
        controller.set('status', 'COMPLETED');
        controller.propertyDidChange('status');
        assert.sameMembers(controller.get('heartBeatLostHosts'), test.expected.heartbeatHosts, 'heartbeat lost host stored in controller');
        assert.equal(controller.get('tasks').objectAt(0).get('status'), test.expected.installClientsTaskStatus, 'Install Clients task status')
      });
    });
  });
});
