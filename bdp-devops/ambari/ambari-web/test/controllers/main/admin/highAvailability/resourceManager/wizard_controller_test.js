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
require('controllers/main/admin/highAvailability/resourceManager/wizard_controller');

describe('App.RMHighAvailabilityWizardController', function () {
  var controller;

  beforeEach(function() {
    controller = App.RMHighAvailabilityWizardController.create({
      content: Em.Object.create({})
    });
  });

  describe("#loadMap 1st step callback", function () {

    beforeEach(function() {
      sinon.stub(controller, 'load');
    });

    afterEach(function() {
      controller.load.restore();
    });

    it("load should be called", function() {
      controller.get('loadMap.1')[0].callback.apply(controller);
      expect(controller.load.calledWith('cluster')).to.be.true;
    });
  });

  describe("#loadMap 2nd step callback", function () {

    beforeEach(function() {
      sinon.stub(controller, 'loadRmHosts');
      sinon.stub(controller, 'loadServicesFromServer');
      sinon.stub(controller, 'loadMasterComponentHosts').returns({done: Em.clb});
      sinon.stub(controller, 'loadConfirmedHosts');
      controller.get('loadMap.2')[0].callback.apply(controller);
    });

    afterEach(function() {
      controller.loadRmHosts.restore();
      controller.loadServicesFromServer.restore();
      controller.loadMasterComponentHosts.restore();
      controller.loadConfirmedHosts.restore();
    });

    it("loadRmHosts should be called", function() {
      expect(controller.loadRmHosts.calledOnce).to.be.true;
    });

    it("loadServicesFromServer should be called", function() {
      expect(controller.loadServicesFromServer.calledOnce).to.be.true;
    });

    it("loadMasterComponentHosts should be called", function() {
      expect(controller.loadMasterComponentHosts.calledOnce).to.be.true;
    });

    it("loadConfirmedHosts should be called", function() {
      expect(controller.loadConfirmedHosts.calledOnce).to.be.true;
    });
  });

  describe("#loadMap 4th step callback", function () {

    beforeEach(function() {
      sinon.stub(controller, 'loadTasksStatuses');
      sinon.stub(controller, 'loadTasksRequestIds');
      sinon.stub(controller, 'loadRequestIds');
      sinon.stub(controller, 'loadConfigs');
      controller.get('loadMap.4')[0].callback.apply(controller);
    });

    afterEach(function() {
      controller.loadTasksStatuses.restore();
      controller.loadTasksRequestIds.restore();
      controller.loadRequestIds.restore();
      controller.loadConfigs.restore();
    });

    it("loadTasksStatuses should be called", function() {
      expect(controller.loadTasksStatuses.calledOnce).to.be.true;
    });

    it("loadTasksRequestIds should be called", function() {
      expect(controller.loadTasksRequestIds.calledOnce).to.be.true;
    });

    it("loadRequestIds should be called", function() {
      expect(controller.loadRequestIds.calledOnce).to.be.true;
    });

    it("loadConfigs should be called", function() {
      expect(controller.loadConfigs.calledOnce).to.be.true;
    });
  });

  describe("#init()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'clearStep');
    });

    afterEach(function() {
      controller.clearStep.restore();
    });

    it("clearStep should be called", function() {
      controller.init();
      expect(controller.clearStep.calledOnce).to.be.true;
    });
  });

  describe("#clearStep()", function () {

    it("isFinished should be false", function() {
      controller.clearStep();
      expect(controller.get('isFinished')).to.be.false;
    });
  });

  describe("#setCurrentStep()", function () {

    beforeEach(function() {
      sinon.stub(App.clusterStatus, 'setClusterStatus');
    });

    afterEach(function() {
      App.clusterStatus.setClusterStatus.restore();
    });

    it("App.clusterStatus.setClusterStatus should be called", function() {
      controller.setCurrentStep();
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe("#saveRmHosts()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'setDBProperty');
    });

    afterEach(function() {
      controller.setDBProperty.restore();
    });

    it("rmHosts should be set", function() {
      controller.saveRmHosts(['host1']);
      expect(controller.get('content.rmHosts')).to.be.eql(['host1']);
    });

    it("setDBProperty should be called", function() {
      controller.saveRmHosts(['host1']);
      expect(controller.setDBProperty.calledWith('rmHosts', ['host1'])).to.be.true;
    });
  });

  describe("#loadRmHosts()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'getDBProperty').returns(['host1']);
    });

    afterEach(function() {
      controller.getDBProperty.restore();
    });

    it("rmHosts should be set", function() {
      controller.loadRmHosts();
      expect(controller.get('content.rmHosts')).to.be.eql(['host1']);
    });
  });

  describe("#saveConfigs()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'setDBProperty');
    });

    afterEach(function() {
      controller.setDBProperty.restore();
    });

    it("configs should be set", function() {
      controller.saveConfigs([{}]);
      expect(controller.get('content.configs')).to.be.eql([{}]);
    });

    it("setDBProperty should be called", function() {
      controller.saveConfigs([{}]);
      expect(controller.setDBProperty.calledWith('configs', [{}])).to.be.true;
    });
  });

  describe("#loadConfigs()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'getDBProperty').returns([{}]);
    });

    afterEach(function() {
      controller.getDBProperty.restore();
    });

    it("configs should be set", function() {
      controller.loadConfigs();
      expect(controller.get('content.configs')).to.be.eql([{}]);
    });
  });

  describe("#clearAllSteps()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'clearInstallOptions');
      sinon.stub(controller, 'getCluster').returns({});
      controller.clearAllSteps();
    });

    afterEach(function() {
      controller.clearInstallOptions.restore();
      controller.getCluster.restore();
    });

    it("clearInstallOptions should be called", function() {
      expect(controller.clearInstallOptions.calledOnce).to.be.true;
    });

    it("cluster should be set", function() {
      expect(controller.get('content.cluster')).to.be.eql({});
    });
  });

  describe("#finish()", function () {
    var container = {
      updateAll: Em.K
    };

    beforeEach(function() {
      sinon.stub(controller, 'resetDbNamespace');
      sinon.stub(App.router, 'get').returns(container);
      sinon.stub(container, 'updateAll');
      controller.finish();
    });

    afterEach(function() {
      controller.resetDbNamespace.restore();
      App.router.get.restore();
      container.updateAll.restore();
    });

    it("resetDbNamespace should be called", function() {
      expect(controller.resetDbNamespace.calledOnce).to.be.true;
    });

    it("updateAll should be called", function() {
      expect(container.updateAll.calledOnce).to.be.true;
    });

    it("isFinished should be true", function() {
      expect(controller.get('isFinished')).to.be.true;
    });
  });
});
