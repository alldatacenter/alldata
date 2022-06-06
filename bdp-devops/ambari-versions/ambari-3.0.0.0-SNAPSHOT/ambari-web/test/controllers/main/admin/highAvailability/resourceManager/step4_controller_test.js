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
require('controllers/main/admin/highAvailability/resourceManager/step4_controller');
var testHelpers = require('test/helpers');

describe('App.RMHighAvailabilityWizardStep4Controller', function () {
  var controller;

  beforeEach(function() {
    controller = App.RMHighAvailabilityWizardStep4Controller.create({
      content: Em.Object.create({})
    });
  });

  describe("#initializeTasks()", function () {

    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns([]);
    });

    afterEach(function() {
      App.Service.find.restore();
    });

    it("reconfigureHAWQ should be removed", function() {
      controller.initializeTasks();
      expect(controller.get('tasks').mapProperty('command')).to.not.contain('reconfigureHAWQ');
    });
  });

  describe("#stopRequiredServices()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'stopServices');
    });

    afterEach(function() {
      controller.stopServices.restore();
    });

    it("stopServices should be called", function() {
      controller.stopRequiredServices();
      expect(controller.stopServices.calledWith(['HDFS'])).to.be.true;
    });
  });

  describe("#installResourceManager()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'createInstallComponentTask');
    });

    afterEach(function() {
      controller.createInstallComponentTask.restore();
    });

    it("createInstallComponentTask should be called", function() {
      controller.set('content.rmHosts', {additionalRM: 'host1'});
      controller.installResourceManager();
      expect(controller.createInstallComponentTask.calledWith('RESOURCEMANAGER', 'host1', 'YARN')).to.be.true;
    });
  });

  describe("#reconfigureYARN()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'loadConfigsTags');
    });

    afterEach(function() {
      controller.loadConfigsTags.restore();
    });

    it("loadConfigsTags should be called", function() {
      controller.reconfigureYARN();
      expect(controller.loadConfigsTags.calledWith('Yarn')).to.be.true;
    });
  });

  describe("#reconfigureHAWQ()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'loadConfigsTags');
    });

    afterEach(function() {
      controller.loadConfigsTags.restore();
    });

    it("loadConfigsTags should be called", function() {
      controller.reconfigureHAWQ();
      expect(controller.loadConfigsTags.calledWith('Hawq')).to.be.true;
    });
  });

  describe("#reconfigureHDFS()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'loadConfigsTags');
    });

    afterEach(function() {
      controller.loadConfigsTags.restore();
    });

    it("loadConfigsTags should be called", function() {
      controller.reconfigureHDFS();
      expect(controller.loadConfigsTags.calledWith('Hdfs')).to.be.true;
    });
  });

  describe("#loadConfigsTags()", function () {

    it("App.ajax.send should be called", function() {
      controller.loadConfigsTags('S1');
      var args = testHelpers.findAjaxRequest('name', 'config.tags');
      expect(args[0]).to.be.eql({
        name: 'config.tags',
        sender: controller,
        success: 'onLoadS1ConfigsTags',
        error: 'onTaskError'
      });
    });
  });

  describe("#onLoadYarnConfigsTags()", function () {

    it("App.ajax.send should be called", function() {
      var data = {
        Clusters: {
          desired_configs: {
            'yarn-site': {
              tag: 1
            }
          }
        }
      };
      controller.onLoadYarnConfigsTags(data);
      var args = testHelpers.findAjaxRequest('name', 'reassign.load_configs');
      expect(args[0]).to.be.eql({
        name: 'reassign.load_configs',
        sender: controller,
        data: {
          urlParams: '(type=yarn-site&tag=1)',
          type: 'yarn-site'
        },
        success: 'onLoadConfigs',
        error: 'onTaskError'
      });
    });
  });

  describe("#onLoadHawqConfigsTags()", function () {

    it("App.ajax.send should be called", function() {
      var data = {
        Clusters: {
          desired_configs: {
            'yarn-client': {
              tag: 1
            }
          }
        }
      };
      controller.onLoadHawqConfigsTags(data);
      var args = testHelpers.findAjaxRequest('name', 'reassign.load_configs');
      expect(args[0]).to.be.eql({
        name: 'reassign.load_configs',
        sender: controller,
        data: {
          urlParams: '(type=yarn-client&tag=1)',
          type: 'yarn-client'
        },
        success: 'onLoadConfigs',
        error: 'onTaskError'
      });
    });
  });

  describe("#onLoadHdfsConfigsTags()", function () {

    it("App.ajax.send should be called", function() {
      var data = {
        Clusters: {
          desired_configs: {
            'core-site': {
              tag: 1
            }
          }
        }
      };
      controller.onLoadHdfsConfigsTags(data);
      var args = testHelpers.findAjaxRequest('name', 'reassign.load_configs');
      expect(args[0]).to.be.eql({
        name: 'reassign.load_configs',
        sender: controller,
        data: {
          urlParams: '(type=core-site&tag=1)',
          type: 'core-site'
        },
        success: 'onLoadConfigs',
        error: 'onTaskError'
      });
    });
  });

  describe("#onLoadConfigs()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'reconfigureSites').returns({});
      controller.set('content.configs', [
        {
          filename: 't1',
          name: 'p1',
          value: 'val1'
        }
      ]);
      sinon.stub(App.format, 'role').returns('comp1');
    });

    afterEach(function() {
      controller.reconfigureSites.restore();
      App.format.role.restore();
    });

    it("reconfigureSites should be called", function() {
      var data = {
        items: [{
          properties: {}
        }]
      };
      var str = Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format('comp1');
      controller.onLoadConfigs(data, {}, {type: 't1'});
      expect(controller.reconfigureSites.getCall(0).args).to.be.eql([['t1'], data, str]);
    });

    it("App.ajax.send should be called", function() {
      var data = {
        items: [{
          properties: {}
        }]
      };
      controller.onLoadConfigs(data, {}, {type: 't1'});
      var args = testHelpers.findAjaxRequest('name', 'common.service.configurations');
      expect(args[0]).to.be.eql({
        name: 'common.service.configurations',
        sender: controller,
        data: {
          desired_config: {}
        },
        success: 'onSaveConfigs',
        error: 'onTaskError'
      });
    });
  });

  describe("#onSaveConfigs()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'onTaskCompleted');
    });

    afterEach(function() {
      controller.onTaskCompleted.restore();
    });

    it("onTaskCompleted should be called", function() {
      controller.onSaveConfigs();
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
    });
  });

  describe("#startAllServices()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'startServices');
    });

    afterEach(function() {
      controller.startServices.restore();
    });

    it("startServices should be called", function() {
      controller.startAllServices();
      expect(controller.startServices.calledWith(true)).to.be.true;
    });
  });
});
