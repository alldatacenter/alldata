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
require('views/common/chart/pie');
require('views/common/configs/services_config');

describe('App.ServiceConfigView', function () {

  var controller = App.WizardStep7Controller.create({
    selectedServiceObserver: Em.K,
    switchConfigGroupConfigs: Em.K,
    saveStepConfigs: Em.K,
    content: Em.Object.create(),
    selectedConfigGroup: Em.Object.create()
  });

  var view = App.ServiceConfigView.create({
    controller: controller
  });

  var testCases = [
    {
      title: 'selectedConfigGroup is null',
      result: {
        'category1': false,
        'category2': true,
        'category3': false
      },
      selectedConfigGroup: null,
      selectedService: {
        serviceName: 'TEST',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'category1', canAddProperty: false}),
          App.ServiceConfigCategory.create({ name: 'category2', siteFileName: 'category2.xml', canAddProperty: true}),
          App.ServiceConfigCategory.create({ name: 'category3', siteFileName: 'category3.xml', canAddProperty: false})
        ],
        configs: []
      }
    },
    {
      title: 'selectedConfigGroup is default group',
      result: {
        'category1': true,
        'category2': true,
        'category3': false
      },
      selectedConfigGroup: {isDefault: true},
      selectedService: {
        serviceName: 'TEST',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'category1', canAddProperty: true}),
          App.ServiceConfigCategory.create({ name: 'category2', siteFileName: 'category2.xml', canAddProperty: true}),
          App.ServiceConfigCategory.create({ name: 'category3', siteFileName: 'category3.xml', canAddProperty: false})
        ],
        configs: []
      }
    }
  ];

  describe('#checkCanEdit', function () {
    before(function () {
      sinon.stub(App.config, 'shouldSupportAddingForbidden').withArgs('TEST', 'category2.xml').returns(true).withArgs('TEST', 'category3.xml').returns(false);
    });

    after(function () {
      App.config.shouldSupportAddingForbidden.restore();
    });

    testCases.forEach(function (test) {
      describe(test.title, function () {

        beforeEach(function () {
          controller.set('selectedService', test.selectedService);
          controller.set('selectedConfigGroup', test.selectedConfigGroup);
          view.checkCanEdit();
        });
        Object.keys(test.result).forEach(function (categoryName) {
          it(categoryName, function () {
            var canAddProperty = controller.get('selectedService.configCategories').findProperty('name', categoryName).get('canAddProperty');
            expect(canAddProperty).to.be.equal(test.result[categoryName]);
          });
        });
      });
    });
  });

  describe('#pickActiveTab', function () {

    Em.A([
        {
          tabs: [
            Em.Object.create({isAdvanced: false, isActive: false, name: 'settings', isHiddenByFilter: false}),
            Em.Object.create({isAdvanced: true, isActive: false, name: 'advanced', isHiddenByFilter: false})
          ],
          m: 'Should make `settings` active (1)',
          e: 'settings'
        },
        {
          tabs: [
            Em.Object.create({isAdvanced: false, isActive: false, name: 'settings', isHiddenByFilter: true}),
            Em.Object.create({isAdvanced: true, isActive: false, name: 'advanced', isHiddenByFilter: false})
          ],
          m: 'Should make `advanced` active (1)',
          e: 'advanced'
        },
        {
          tabs: [
            Em.Object.create({isAdvanced: false, isActive: true, name: 'settings', isHiddenByFilter: false}),
            Em.Object.create({isAdvanced: true, isActive: false, name: 'advanced', isHiddenByFilter: false})
          ],
          m: 'Should make `settings` active (2)',
          e: 'settings'
        },
        {
          tabs: [
            Em.Object.create({isAdvanced: false, isActive: true, name: 'settings', isHiddenByFilter: true}),
            Em.Object.create({isAdvanced: true, isActive: false, name: 'advanced', isHiddenByFilter: false})
          ],
          m: 'Should make `advanced` active (2)',
          e: 'advanced'
        },
        {
          tabs: [
            Em.Object.create({isAdvanced: false, isActive: false, name: 'settings', isHiddenByFilter: false}),
            Em.Object.create({isAdvanced: true, isActive: false, name: 'advanced', isHiddenByFilter: false})
          ],
          m: 'Should make `settings` active (3)',
          e: 'settings'
        },
        {
          tabs: [
            Em.Object.create({isAdvanced: false, isActive: false, name: 'settings', isHiddenByFilter: false}),
            Em.Object.create({isAdvanced: true, isActive: true, name: 'advanced', isHiddenByFilter: false})
          ],
          m: 'Should make `advanced` active (3)',
          e: 'advanced'
        }
    ]).forEach(function (test) {
        it(test.m, function () {
          view.pickActiveTab(test.tabs);
          expect(test.tabs.findProperty('name', test.e).get('isActive')).to.be.true;
        });
      });

  });

  describe('#save()', function () {

    beforeEach(function () {
      sinon.stub(App.ServiceConfigVersion, 'find').returns([
        { serviceName: 'service', version: 1}
      ]);
      sinon.stub(view.get('controller'), 'saveStepConfigs');
    });

    afterEach(function () {
      App.ServiceConfigVersion.find.restore();
      view.get('controller').saveStepConfigs.restore();
    });

    it('modal popup should be displayed', function () {
      view.save();
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });

    it('controller properties should be modified on save', function () {
      view.setProperties({
        'serviceName': 'service',
        'controller.saveConfigsFlag': false,
        'controller.serviceConfigVersionNote': '',
        'controller.preSelectedConfigVersion': null,
        'controller.content.serviceName': 'service',
        'controller.selectedConfigGroup.name': 'group'
      });
      var popup = view.save();
      popup.set('serviceConfigNote', 'note');
      popup.onSave();
      expect(view.get('controller.saveConfigsFlag')).to.be.true;
      expect(view.get('controller').getProperties(['saveConfigsFlag', 'serviceConfigVersionNote', 'preSelectedConfigVersion'])).to.eql({
        saveConfigsFlag: true,
        serviceConfigVersionNote: 'note',
        preSelectedConfigVersion: Em.Object.create({
          version: 2,
          serviceName: 'service',
          groupName: 'group'
        })
      });
      expect(view.get('controller').saveStepConfigs.calledOnce).to.be.true;
    });
  });

});
