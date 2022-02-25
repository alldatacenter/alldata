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
require('controllers/main/admin/highAvailability/hawq/activateStandby/step3_controller');
var testHelpers = require('test/helpers');

describe('App.ActivateHawqStandbyWizardStep3Controller', function () {
  var controller;
  beforeEach(function () {
    controller = App.ActivateHawqStandbyWizardStep3Controller.create({});
  });
  
  describe('#activateStandby', function () {
    it('should send ajax request with proper params', function () {
      controller.set('content', {hawqHosts: {hawqStandby: ['test1', 'test2']}});
      controller.activateStandby();
      var args = testHelpers.findAjaxRequest('name', 'service.item.executeCustomCommand');
      expect(args[0]).to.be.eql({
        name : 'service.item.executeCustomCommand',
        sender: controller,
        data : {
          command : controller.hawqActivateStandbyCustomCommand,
          context : Em.I18n.t('admin.activateHawqStandby.wizard.step3.activateHawqStandbyCommand.context'),
          hosts : ['test1', 'test2'],
          serviceName : controller.hawqServiceName,
          componentName : controller.hawqStandbyComponentName
        },
        success: 'startPolling',
        error: 'onTaskError'
      });
    });
  });

  describe('#stopRequiredServices', function () {
    it('should call stop services method with hawqServiceName', function () {
      sinon.stub(controller, 'stopServices');
      controller.stopRequiredServices();
      expect(controller.stopServices.calledWith([controller.hawqServiceName], true)).to.be.true;
      controller.stopServices.restore();
    });
  });

  describe('#reconfigureHAWQ', function(){
    it('should send ajax request with proper params', function() {
      controller.reconfigureHAWQ();
      var args = testHelpers.findAjaxRequest('name', 'config.tags');
      expect(args[0]).to.be.eql({
        name: 'config.tags',
        sender: controller,
        success: 'onLoadHawqConfigsTags',
        error: 'onTaskError'
      });
    });
  });

  describe('#onLoadHawqConfigsTags', function () {
    it('should load configs with proper params depends on data param', function () {
      controller.onLoadHawqConfigsTags({
        Clusters: {
          desired_configs : {
            'hawq-site': {tag: 'test', param: 'test1'},
            'test-site': {tag: 'test2', param: 'test2'}
          }
        }
      });
      var args = testHelpers.findAjaxRequest('name', 'reassign.load_configs');
      expect(args[0]).to.be.eql({
        name: 'reassign.load_configs',
        sender: controller,
        data: {
          urlParams: '(type=hawq-site&tag=test)',
          type: 'hawq-site'
        },
        success: 'onLoadConfigs',
        error: 'onTaskError'
      });
    });
  });

  describe('#onLoadConfigs', function () {
    it('should send ajax request with proper params', function () {
      var data = {
        items: [{
          properties: {test1: 't1', test2: 't2', 'hawq_standby_address_host': 't5'}
        }, {
          properties: {test3: 't3', test4: 't4'}
        }]
      };
      var params = {type: 'some-file.xml'};
      controller.set('content', {
        configs: [{
          filename: 'some-file.xml',
          value: 'tt1',
          name: 'test1'
        }, {
          filename: 'some-file.xml',
          value: 'tt2',
          name: 'test2'
        }, {
          filename: 'some-file2.xml',
          value: 'tt3',
          name: 'test3'
        }]
      })
      controller.onLoadConfigs(data, null, params);
      var args = testHelpers.findAjaxRequest('name', 'common.service.configurations');
      var paramStr = Em.I18n.t('admin.activateHawqStandby.step4.save.configuration.note').format(App.format.role('HAWQSTANDBY', false));
      expect(args[0]).to.be.eql({
        name: 'common.service.configurations',
        sender: controller,
        data: {
          desired_config: controller.reconfigureSites(['some-file.xml'], {
            items: [{
              properties: {test1: 'tt1', test2: 'tt2'}
            }, {
              properties: {test3: 't3', test4: 't4'}
            }]
          }, paramStr)
        },
        success: 'onSaveConfigs',
        error: 'onTaskError'
      });
    });
  });

  describe('#installHawqMaster', function () {
    it('should call createInstallComponentTask with hostNames', function () {
      controller.set('content', {hawqHosts: {hawqStandby: ['test1', 'test2']}});
      sinon.stub(controller, 'createInstallComponentTask');
      controller.installHawqMaster();
      expect(controller.createInstallComponentTask.calledWith(controller.hawqMasterComponentName, ['test1', 'test2'], controller.hawqServiceName)).to.be.true;
      controller.createInstallComponentTask.restore();
    });
  });

  describe('#deleteOldHawqMaster', function () {
    it('should call deleteComponent with hostNames', function () {
      controller.set('content', {hawqHosts: {hawqMaster: ['test1', 'test2']}});
      sinon.stub(controller, 'deleteComponent');
      controller.deleteOldHawqMaster();
      expect(controller.deleteComponent.calledWith(controller.hawqMasterComponentName, ['test1', 'test2'])).to.be.true;
      controller.deleteComponent.restore();
    });
  });

  describe('#deleteHawqStandby', function () {
    it('should call deleteComponent with hostNames', function () {
      controller.set('content', {hawqHosts: {hawqStandby: ['test1', 'test2']}});
      sinon.stub(controller, 'deleteComponent');
      controller.deleteHawqStandby();
      expect(controller.deleteComponent.calledWith(controller.hawqStandbyComponentName, ['test1', 'test2'])).to.be.true;
      controller.deleteComponent.restore();
    });
  });


});