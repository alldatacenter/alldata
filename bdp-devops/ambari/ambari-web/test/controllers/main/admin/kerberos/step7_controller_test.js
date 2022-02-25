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

function getController() {
  return App.KerberosWizardStep7Controller.create({});
}

describe('App.KerberosWizardStep7Controller', function () {
  var controller;

  beforeEach(function () {
    controller = getController();
  });

  describe("#setRequest()", function () {

    beforeEach(function () {
      sinon.stub(controller, 'clearStage');
      sinon.stub(controller, 'loadStep');
    });

    afterEach(function () {
      controller.clearStage.restore();
      controller.loadStep.restore();
    });

    it("request should be set", function () {
      controller.setRequest(true);
      expect(controller.get('request')).to.be.eql({
        name: 'KERBERIZE_CLUSTER',
        ajaxName: 'admin.kerberize.cluster.force'
      });
    });

    it("clearStage should be called", function () {
      controller.setRequest(true);
      expect(controller.clearStage.calledOnce).to.be.true;
    });

    it("loadStep should be called", function () {
      controller.setRequest(true);
      expect(controller.loadStep.calledOnce).to.be.true;
    });

    it("kerberize request should be set", function () {
      controller.setRequest(false);
      expect(controller.get('request')).to.be.eql({
        name: 'KERBERIZE_CLUSTER',
        ajaxName: 'admin.kerberize.cluster',
        ajaxData: {
          data: {
            Clusters: {
              security_type: "KERBEROS"
            }
          }
        }
      });
    });
  });

  describe("#unkerberizeCluster()", function () {

    it("App.ajax.send should be called", function () {
      controller.unkerberizeCluster();
      var args = testHelpers.findAjaxRequest('name', 'admin.unkerberize.cluster');
      expect(args[0]).to.be.eql({
        name: 'admin.unkerberize.cluster',
        sender: controller,
        success: 'goToNextStep',
        error: 'goToNextStep'
      });
    });
  });

  describe("#goToNextStep()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'clearStage');
      sinon.stub(App.router, 'transitionTo');
      controller.goToNextStep();
    });

    afterEach(function() {
      controller.clearStage.restore();
      App.router.transitionTo.restore();
    });

    it("clearStage should be called", function() {
      expect(controller.clearStage.calledOnce).to.be.true;
    });

    it("App.router.transitionTo should be called", function() {
      expect(App.router.transitionTo.calledWith('step7')).to.be.true;
    });
  });

  describe("#retry()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'removeObserver');
      sinon.stub(controller, 'setRequest');
      controller.set('tasks', [Em.Object.create()]);
      controller.retry();
    });

    afterEach(function() {
      controller.removeObserver.restore();
      controller.setRequest.restore();
    });

    it("showRetry should be false", function() {
      expect(controller.get('showRetry')).to.be.false;
    });

    it("removeObserver should be called", function() {
      expect(controller.removeObserver.calledWith('tasks.@each.status', controller, 'onTaskStatusChange')).to.be.true;
    });

    it("status should be IN_PROGRESS", function() {
      expect(controller.get('status')).to.be.equal('IN_PROGRESS');
    });

    it("tasks status should be PENDING", function() {
      expect(controller.get('tasks').mapProperty('status')).to.be.eql(['PENDING']);
    });

    it("setRequest should be called", function() {
      expect(controller.setRequest.calledWith(true)).to.be.true;
    });
  });

  describe("#enableDisablePreviousSteps()", function () {
    var mock = {
      enableStep: Em.K,
      setLowerStepsDisable: Em.K
    };

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(mock, 'setLowerStepsDisable');
      sinon.stub(mock, 'enableStep');
    });

    afterEach(function() {
      App.router.get.restore();
      mock.enableStep.restore();
      mock.setLowerStepsDisable.restore();
    });

    it("FAILED tasks", function() {
      controller.set('tasks', [{status: 'FAILED'}]);
      controller.enableDisablePreviousSteps();
      expect(controller.get('isBackButtonDisabled')).to.be.false;
      expect(mock.enableStep.calledWith(4)).to.be.true;
    });

    it("COMPLETED tasks", function() {
      controller.set('tasks', [{status: 'COMPLETED'}]);
      controller.enableDisablePreviousSteps();
      expect(controller.get('isBackButtonDisabled')).to.be.true;
      expect(mock.setLowerStepsDisable.calledWith(6)).to.be.true;
    });
  });
});
