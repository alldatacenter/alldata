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
require('controllers/main/admin/highAvailability/hawq/activateStandby/step2_controller');

describe('App.ActivateHawqStandbyWizardStep2Controller', function () {

  describe('#isSubmitDisabled', function () {

    var controller = App.ActivateHawqStandbyWizardStep2Controller.create({
        content: Em.Object.create({})
      }),
      cases = [
        {
          isLoaded: false,
          isSubmitDisabled: true,
          title: 'wizard step content not loaded'
        },
        {
          isLoaded: true,
          isSubmitDisabled: false,
          title: 'wizard step content loaded'
        }
      ];

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.set('isLoaded', item.isLoaded);
        expect(controller.get('isSubmitDisabled')).to.equal(item.isSubmitDisabled);
      });
    });

  });


  describe('#setDynamicConfigValues', function () {

    var controller = App.ActivateHawqStandbyWizardStep2Controller.create({
        content: Em.Object.create({
          masterComponentHosts: [
            {component: 'HAWQMASTER', hostName: 'h0', isInstalled: true},
            {component: 'HAWQSTANDBY', hostName: 'h1', isInstalled: true}
          ],
          hawqHost: {
            hawqMaster: 'h0',
            hawqStandby: 'h1'
          }
        })
      }),
      configs = {
        configs: [
          Em.Object.create({
            name: 'hawq_master_address_host'
          })
        ]
      };


    beforeEach(function () {
      controller.setDynamicConfigValues(configs);
    });

    it('hawq_master_address_host value', function () {
      expect(configs.configs.findProperty('name', 'hawq_master_address_host').get('value')).to.equal('h1');
    });
    it('hawq_master_address_host recommendedValue', function () {
      expect(configs.configs.findProperty('name', 'hawq_master_address_host').get('recommendedValue')).to.equal('h1');
    });
  });

});
