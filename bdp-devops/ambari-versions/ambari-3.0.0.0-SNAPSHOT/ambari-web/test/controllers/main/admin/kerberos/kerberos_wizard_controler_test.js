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

describe('App.KerberosWizardController', function() {
  var controller;

  beforeEach(function() {
    controller = App.KerberosWizardController.create({});
  });

  describe('#warnBeforeExitPopup()', function () {
    beforeEach(function () {
      sinon.stub(App, "showConfirmationPopup", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
    });
    it('should open warning confirmation popup', function () {
      var f = Em.K;
      controller.warnBeforeExitPopup(f, false);
      expect(App.showConfirmationPopup.calledWith(f, Em.I18n.t('admin.kerberos.wizard.exit.warning.msg'), null, null, Em.I18n.t('common.exitAnyway'), 'success')).to.be.true;
    });

    it('should open critical confirmation popup', function () {
      var f = Em.K;
      controller.warnBeforeExitPopup(f, true);
      expect(App.showConfirmationPopup.calledWith(f, Em.I18n.t('admin.kerberos.wizard.exit.critical.msg'), null, null, Em.I18n.t('common.exitAnyway'), 'danger')).to.be.true;
    });
  });

  describe("#skipClientInstall", function () {

    it("kerberosOption is null", function() {
      controller.set('content.kerberosOption', null);
      controller.propertyDidChange('skipClientInstall');
      expect(controller.get('skipClientInstall')).to.be.false;
    });

    it("kerberosOption is 'opt1'", function() {
      controller.set('content.kerberosOption', 'opt1');
      controller.propertyDidChange('skipClientInstall');
      expect(controller.get('skipClientInstall')).to.be.false;
    });

    it("kerberosOption is manual", function() {
      controller.set('content.kerberosOption', Em.I18n.t('admin.kerberos.wizard.step1.option.manual'));
      controller.propertyDidChange('skipClientInstall');
      expect(controller.get('skipClientInstall')).to.be.true;
    });
  });

  describe("#setCurrentStep()", function () {

    beforeEach(function() {
      sinon.stub(App.clusterStatus, 'setClusterStatus');
    });

    afterEach(function() {
      App.clusterStatus.setClusterStatus.restore();
    });

    it("skipStateSave is true", function() {
      controller.setCurrentStep("", true, true);
      expect(App.clusterStatus.setClusterStatus.called).to.be.false;
    });

    it("skipStateSave is false", function() {
      controller.setCurrentStep("", true, false);
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe("#getCluster()", function () {

    it("jQuery.extend should be called", function() {
      App.set('clusterName', 'c1');
      controller.set('clusterStatusTemplate', {});
      expect(controller.getCluster()).to.be.eql({name: 'c1'});
    });
  });

  describe("#updateClusterEnvData()", function () {

    it("configs should be set", function() {
      var configs = {};
      controller.set('kerberosDescriptorConfigs', {
        properties: {
          realm: 'realm'
        }
      });
      controller.updateClusterEnvData(configs);
      expect(configs).to.be.eql({
        security_enabled: true,
        kerberos_domain: 'realm'
      });
    });
  });

  describe("#saveClusterStatus()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'save');
    });

    afterEach(function() {
      controller.save.restore();
    });

    it("save should be called", function() {
      controller.set('content.cluster', {});
      controller.saveClusterStatus({});
      expect(controller.save.calledWith('cluster')).to.be.true;
    });

    it("new requestId should be pushed", function() {
      controller.set('content.cluster', {});
      controller.saveClusterStatus({requestId: [1], oldRequestsId: []});
      expect(controller.get('content.cluster')).to.be.eql({
        oldRequestsId: [1],
        requestId: [1]
      });
    });

    it("new requestId should not be pushed", function() {
      controller.set('content.cluster', {});
      controller.saveClusterStatus({requestId: [1], oldRequestsId: [1]});
      expect(controller.get('content.cluster')).to.be.eql({
        oldRequestsId: [1],
        requestId: [1]
      });
    });
  });

  describe("#saveConfigTag()", function () {
    var tag = {
      name: 'n1',
      value: 'v1'
    };

    beforeEach(function() {
      sinon.stub(App.db, 'setKerberosWizardConfigTag');
      controller.saveConfigTag(tag);
    });

    afterEach(function() {
      App.db.setKerberosWizardConfigTag.restore();
    });

    it("App.db.setKerberosWizardConfigTag should be called", function() {
      expect(App.db.setKerberosWizardConfigTag.calledWith(tag)).to.be.true;
    });

    it("content property should be set", function() {
      expect(controller.get('content.n1')).to.be.equal('v1');
    });
  });

  describe("#saveKerberosOption()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'setDBProperty');
      controller.saveKerberosOption(Em.Object.create({selectedItem: "selectedItem"}));
    });

    afterEach(function() {
      controller.setDBProperty.restore();
    });

    it("setDBProperty should be called", function() {
      expect(controller.setDBProperty.calledWith('kerberosOption', "selectedItem")).to.be.true;
    });

    it("kerberosOption should be set", function() {
      expect(controller.get('content.kerberosOption')).to.be.equal('selectedItem');
    });
  });

  describe("#loadKerberosDescriptorConfigs()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'getDBProperty').returns({});
    });

    afterEach(function() {
      controller.getDBProperty.restore();
    });

    it("kerberosDescriptorConfigs should be set", function() {
      controller.loadKerberosDescriptorConfigs();
      expect(controller.get('kerberosDescriptorConfigs')).to.be.eql({});
    });
  });

  describe("#overrideVisibility()", function () {

    it("empty object", function() {
      var itemsArray = [
        {}
      ];
      controller.overrideVisibility(itemsArray, true, []);
      expect(itemsArray[0]).to.be.empty;
    });

    it("override in exception", function() {
      var itemsArray = [
        {
          category: 'c1',
          name: 'n1',
          isVisible: false
        }
      ];
      controller.overrideVisibility(itemsArray, true, [{c1: 'n1'}]);
      expect(itemsArray[0].isVisible).to.be.false;
    });

    it("override should be set to visible", function() {
      var itemsArray = [
        {
          category: 'c1',
          name: 'n1',
          isVisible: false
        }
      ];
      controller.overrideVisibility(itemsArray, true, []);
      expect(itemsArray[0].isVisible).to.be.true;
    });
  });

  describe("#loadKerberosOption()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'getDBProperty').returns('kerberosOption');
    });

    afterEach(function() {
      controller.getDBProperty.restore();
    });

    it("kerberosOption should be set", function() {
      controller.loadKerberosOption();
      expect(controller.get('content.kerberosOption')).to.be.equal('kerberosOption');
    });
  });

  describe("#saveKerberosDescriptorConfigs()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'setDBProperty');
      controller.saveKerberosDescriptorConfigs([]);
    });

    afterEach(function() {
      controller.setDBProperty.restore();
    });

    it("setDBProperty should be called", function() {
      expect(controller.setDBProperty.calledWith('kerberosDescriptorConfigs', [])).to.be.true;
    });

    it("kerberosDescriptorConfigs should be set", function() {
      expect(controller.get('kerberosDescriptorConfigs')).to.be.eql([]);
    });
  });

  describe("#createKerberosResources()", function () {
    var mock = {
      callback: Em.K
    };

    beforeEach(function() {
      sinon.stub(mock, 'callback');
      sinon.stub(controller, 'createKerberosService').returns({
        done: Em.clb
      });
      sinon.stub(controller, 'updateAndCreateServiceComponent').returns({
        done: Em.clb
      });
      sinon.stub(controller, 'createKerberosHostComponents').returns({
        done: Em.clb
      });
      controller.createKerberosResources(mock.callback);
    });

    afterEach(function() {
      controller.createKerberosHostComponents.restore();
      controller.updateAndCreateServiceComponent.restore();
      controller.createKerberosService.restore();
      mock.callback.restore();
    });

    it("createKerberosService should be called", function() {
      expect(controller.createKerberosService.calledOnce).to.be.true;
    });

    it("updateAndCreateServiceComponent should be called", function() {
      expect(controller.updateAndCreateServiceComponent.calledWith('KERBEROS_CLIENT')).to.be.true;
    });

    it("createKerberosHostComponents should be called", function() {
      expect(controller.createKerberosHostComponents.calledOnce).to.be.true;
    });

    it("callback should be called", function() {
      expect(mock.callback.calledOnce).to.be.true;
    });
  });

  describe("#createKerberosService()", function () {

    it("App.ajax.send should be called", function() {
      App.set('clusterName', 'c1');
      controller.createKerberosService();
      var args = testHelpers.findAjaxRequest('name', 'wizard.step8.create_selected_services');
      expect(args[0]).to.be.eql({
        name: 'wizard.step8.create_selected_services',
        sender: controller,
        data: {
          data: '{"ServiceInfo": { "service_name": "KERBEROS"}}',
          cluster: 'c1'
        }
      });
    });
  });

  describe("#deleteKerberosService()", function () {

    beforeEach(function() {
      App.cache.services = [{
        ServiceInfo: {
          service_name: 'KERBEROS'
        }
      }];
      sinon.stub(App.serviceMapper, 'deleteRecord');
      sinon.stub(App.Service, 'find').returns(Em.Object.create({
        isLoaded: true
      }));
      controller.deleteKerberosService();
    });

    afterEach(function() {
      App.serviceMapper.deleteRecord.restore();
      App.Service.find.restore();
    });

    it("App.cache.services should be empty", function() {
      expect(App.cache.services).to.be.empty;
    });

    it("App.serviceMapper.deleteRecord should be called", function () {
      expect(App.serviceMapper.deleteRecord.calledWith(Em.Object.create({
        isLoaded: true
      }))).to.be.true;
    });

    it("App.ajax.send should be called", function() {
      var args = testHelpers.findAjaxRequest('name', 'common.delete.service');
      expect(args[0]).to.be.eql({
        name: 'common.delete.service',
        sender: controller,
        data: {
          serviceName: 'KERBEROS'
        }
      });
    });
  });

  describe("#unkerberize()", function () {

    it("App.ajax.send should be called", function() {
      controller.unkerberize();
      var args = testHelpers.findAjaxRequest('name', 'admin.unkerberize.cluster');
      expect(args[0]).to.be.eql({
        name: 'admin.unkerberize.cluster',
        sender: controller
      });
    });
  });

  describe("#createKerberosHostComponents()", function () {

    it("App.ajax.send should be called", function() {
      App.set('clusterName', 'c1');
      App.set('allHostNames', ['host1']);
      controller.createKerberosHostComponents();
      var args = testHelpers.findAjaxRequest('name', 'wizard.step8.register_host_to_component');
      expect(args[0]).to.be.exists;
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

    it("content.cluster should be set", function() {
      expect(controller.get('content.cluster')).to.be.eql({});
    });
  });

  describe("#clearTasksData()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'saveTasksStatuses');
      sinon.stub(controller, 'saveRequestIds');
      sinon.stub(controller, 'saveTasksRequestIds');
      controller.clearTasksData();
    });

    afterEach(function() {
      controller.saveTasksRequestIds.restore();
      controller.saveTasksStatuses.restore();
      controller.saveRequestIds.restore();
    });

    it("saveTasksRequestIds should be called", function() {
      expect(controller.saveTasksRequestIds.calledWith(undefined)).to.be.true;
    });

    it("saveRequestIds should be called", function() {
      expect(controller.saveRequestIds.calledWith(undefined)).to.be.true;
    });

    it("saveTasksStatuses should be called", function() {
      expect(controller.saveTasksStatuses.calledWith(undefined)).to.be.true;
    });
  });

  describe("#warnBeforeExitPopup()", function () {

    beforeEach(function() {
      sinon.stub(App, 'showConfirmationPopup');
    });

    afterEach(function() {
      App.showConfirmationPopup.restore();
    });

    it("isCritical is true", function() {
      controller.warnBeforeExitPopup(Em.K, true);
      expect(App.showConfirmationPopup.calledWith(Em.K, Em.I18n.t('admin.kerberos.wizard.exit.critical.msg'), null, null, Em.I18n.t('common.exitAnyway'), 'danger')).to.be.true;
    });

    it("isCritical is false", function() {
      controller.warnBeforeExitPopup(Em.K, false);
      expect(App.showConfirmationPopup.calledWith(Em.K, Em.I18n.t('admin.kerberos.wizard.exit.warning.msg'), null, null, Em.I18n.t('common.exitAnyway'), 'success')).to.be.true;
    });
  });

  describe("#finish()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'setCurrentStep');
      sinon.stub(controller, 'resetDbNamespace');
      controller.finish();
    });

    afterEach(function() {
      controller.setCurrentStep.restore();
      controller.resetDbNamespace.restore();
    });

    it("setCurrentStep should be called", function() {
      expect(controller.setCurrentStep.calledWith('1', false, true)).to.be.true;
    });

    it("resetDbNamespace should be called", function() {
      expect(controller.resetDbNamespace.calledOnce).to.be.true;
    });
  });

  describe("#discardChanges()", function () {
    var mock = {
      onResolve: Em.K
    };

    beforeEach(function() {
      sinon.stub(controller, 'unkerberize').returns({always: Em.clb});
      sinon.stub(controller, 'deleteKerberosService').returns({always: Em.clb});
      sinon.stub(mock, 'onResolve');
      controller.discardChanges().done(mock.onResolve);
    });

    afterEach(function() {
      controller.unkerberize.restore();
      controller.deleteKerberosService.restore();
      mock.onResolve.restore();
    });

    it("unkerberize should be called", function() {
      expect(controller.unkerberize.calledOnce).to.be.true;
    });

    it("deleteKerberosService should be called", function() {
      expect(controller.deleteKerberosService.calledOnce).to.be.true;
    });

    it("onResolve should be called", function() {
      expect(mock.onResolve.calledOnce).to.be.true;
    });
  });
});
