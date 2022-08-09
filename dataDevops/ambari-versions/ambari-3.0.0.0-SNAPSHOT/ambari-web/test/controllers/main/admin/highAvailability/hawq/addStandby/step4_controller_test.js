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
  return App.AddHawqStandbyWizardStep4Controller.create({
    content: Em.Object.create({})
  });
}

describe('App.AddHawqStandbyWizardStep3Controller', function () {
  var controller;
  beforeEach(function () {
    controller = getController();
  });

  describe('#installHawqStandbyMaster', function () {
    it('should call createInstallComponentTask with host name of new HAWQ', function () {
      controller.set('content.hawqHosts', {newHawqStandby: 'test1'});
      sinon.stub(controller, 'createInstallComponentTask');
      controller.installHawqStandbyMaster();
      expect(controller.createInstallComponentTask.calledWith('HAWQSTANDBY', 'test1', "HAWQ")).to.be.true;
      controller.createInstallComponentTask.restore();
    });
  });

  describe('#reconfigureHAWQ', function () {
    it('should call App.ajax with proper params', function () {
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
    it('should call App.ajax with proper params', function () {
      var data = {Clusters: {test: 'test2', desired_configs: {test: 'test2', 'hawq-site': {tag: 'test1', test: 'test2'}}}}
      controller.onLoadHawqConfigsTags(data);
      var args = testHelpers.findAjaxRequest('name', 'reassign.load_configs');
      expect(args[0]).to.be.eql({
        name: 'reassign.load_configs',
        sender: controller,
        data: {
          urlParams: '(type=hawq-site&tag=test1)',
          type: 'hawq-site'
        },
        success: 'onLoadConfigs',
        error: 'onTaskError'
      });
    });
  });

  describe('#onLoadConfigs', function() {
    it('should call App.ajax with configured params', function () {
      controller.set('content.configs', [
        {filename: 'test1', name: 'test1', value: 'test1'},
        {filename: 'test2', name: 'test2', value: 'test2'},
        {filename: 'test1', name: 'test3', value: 'test3'},
        {filename: 'test13', name: 'test4', value: 'test4'}]);
      var data = {
        items: [{
          properties: {
            'test3': 'testvalue3'
          }
        }, {
          properties: {
          }
        }]
      };
      var params = {type: 'test1'};
      var configData = controller.reconfigureSites([params.type], data, Em.I18n.t('admin.addHawqStandby.step4.save.configuration.note').format(App.format.role('HAWQSTANDBY', false)));
      controller.onLoadConfigs(data, null, params);
      var args = testHelpers.findAjaxRequest('name', 'common.service.configurations');
      expect(args[0]).to.be.eql({
        name: 'common.service.configurations',
        sender: controller,
        data: {
          desired_config: configData
        },
        success: 'onSaveConfigs',
        error: 'onTaskError'
      });
    });
  });
});

