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
require('controllers/main/admin/highAvailability/rangerAdmin/step3_controller');
require('controllers/main/admin/highAvailability/rangerAdmin/wizard_controller');
require('controllers/main');

describe('App.RAHighAvailabilityWizardStep3Controller', function () {

  var controller;

  beforeEach(function () {
    controller = App.RAHighAvailabilityWizardStep3Controller.create();
  });

  describe('#loadStep', function () {

    var dfd,
      testCases = [
        {
          path: 'isLoaded',
          result: true
        },
        {
          path: 'selectedService.configs.length',
          result: 1,
          massage: 'configs length'
        },
        {
          path: 'selectedService.configs.firstObject.name',
          result: 'policymgr_external_url',
          message: 'property name'
        },
        {
          path: 'selectedService.configs.firstObject.category',
          result: 'RANGER',
          message: 'config category'
        },
        {
          path: 'selectedService.configs.firstObject.value',
          result: 'http://localhost:1111',
          message: 'property value'
        }
      ],
      service = Em.Object.create({
        serviceName: 'RANGER',
        displayName: 'Ranger'
      });

    beforeEach(function () {
      dfd = $.Deferred();
      sinon.stub(App.get('router.mainController'), 'isLoading').returns(dfd);
      sinon.stub(App.Service, 'find').returns([service]);
      sinon.stub(App.config, 'get').withArgs('serviceByConfigTypeMap').returns({
        'admin-properties': service
      });
      sinon.stub(App.configsCollection, 'getConfigByName').returns({
        name: 'policymgr_external_url'
      });
      controller.setProperties({
        wizardController: App.get('router.rAHighAvailabilityWizardController'),
        content: {
          loadBalancerURL: 'http://localhost:1111'
        }
      });
      controller.loadStep();
      dfd.resolve();
    });

    afterEach(function () {
      App.get('router.mainController.isLoading').restore();
      App.Service.find.restore();
      App.config.get.restore();
      App.configsCollection.getConfigByName.restore();
    });

    testCases.forEach(function (testCase) {

      it(testCase.message || testCase.path, function () {
        expect(controller.get(testCase.path)).to.equal(testCase.result);
      });

    });

  });

});