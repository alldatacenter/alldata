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

describe('App.RAHighAvailabilityWizardStep4Controller', function () {
  var controller;

  beforeEach(function () {
    controller = App.RAHighAvailabilityWizardStep4Controller.create();
  });

  describe('#stopAllServices', function () {
    it('should call stopServices method with 2 empty arrays and true props', function () {
      sinon.stub(controller, 'stopServices');
      controller.stopAllServices();
      expect(controller.stopServices.calledWith([], true, true)).to.be.true;
      controller.stopServices.restore();
    });
  });

  describe('#installRangerAdmin', function () {
    it('should call createInstallComponentTask with proper params', function () {
      sinon.stub(controller, 'createInstallComponentTask');
      var hostNames = ['test1', 'test2'];
      controller.set('content', {raHosts: hostNames});
      controller.installRangerAdmin();
      expect(controller.createInstallComponentTask.calledWith('RANGER_ADMIN', hostNames, 'RANGER'));
      controller.createInstallComponentTask.restore();
    });
  });

  describe('#reconfigureRanger', function () {
    it('should call loadConfigsTags method', function () {
      sinon.stub(controller, 'loadConfigsTags');
      controller.reconfigureRanger();
      expect(controller.loadConfigsTags.calledOnce).to.be.true;
      controller.loadConfigsTags.restore();
    });
  });

  describe('#loadConfigsTags', function () {
    it('should send ajax request with proper body', function () {
      controller.loadConfigsTags();
      var args = testHelpers.findAjaxRequest('name', 'config.tags');
      expect(args[0]).to.be.eql({
        name: 'config.tags',
        sender: controller,
        success: 'onLoadConfigsTags',
        error: 'onTaskError'
      });
    });
  });

  describe('#onLoadConfigs', function() {
    it('should send ajax request with proper params', function () {
      controller.set('wizardController', Em.Object.create({
        configs: [{siteName: 'test1', propertyName: 'prop1'}, {siteName: 'test2', propertyName: 'prop2'}, {siteName: 'test3', propertyName: 'prop3'}]
      }));
      controller.set('content.loadBalancerURL', 'loadBalancerURL');
      var data = {
        items: [{type: 'test1', properties: {}}, {type: 'test2', properties: {}}]
      };
      controller.onLoadConfigs(data);

      var args = testHelpers.findAjaxRequest('name', 'common.service.multiConfigurations');
      expect(args[0]).to.be.eql({
        name: 'common.service.multiConfigurations',
        sender: controller,
        data: {
          configs: [{
            Clusters: {
              desired_config: controller.reconfigureSites(['test1'], data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('RANGER_ADMIN', false)))
            }
          }, {
            Clusters: {
              desired_config: controller.reconfigureSites(['test2'], data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('RANGER_ADMIN', false)))
            }
          }]
        },
        success: 'onSaveConfigs',
        error: 'onTaskError'
      });
    });
  });

  describe('#onLoadConfigsTags', function () {
    it('should send ajax request with proper params', function () {
      sinon.stub(controller, 'get').returns([{siteName: 'test1'}, {siteName: 'test2'}, {siteName: 'test3'}]);
      var data = {Clusters: {
          desired_configs: {
            'test1': {tag: 'test1'},
            'test3': {tag: 'test3'},
            'test5': {tag: 'test5'}
          }
        }};
      controller.onLoadConfigsTags(data);
      var args = testHelpers.findAjaxRequest('name', 'reassign.load_configs');
      expect(args[0]).to.be.eql({
        name: 'reassign.load_configs',
        sender: controller,
        data: {
          urlParams: '(type=test1&tag=test1)|(type=test3&tag=test3)'
        },
        success: 'onLoadConfigs',
        error: 'onTaskError'
      });
      controller.get.restore();
    });
  });
});