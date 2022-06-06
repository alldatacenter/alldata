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

describe('App.MainAdminServiceAutoStartController', function() {
  var controller;

  beforeEach(function() {
    controller = App.MainAdminServiceAutoStartController.create({
      clusterConfigs: {}
    });
  });

  describe('#parseComponentConfigs', function() {
    var components = [
      {
        service_name: 'S1',
        component_name: 'C1',
        recovery_enabled: 'true',
        category: 'SLAVE',
        total_count: 1
      },
      {
        service_name: 'S1',
        component_name: 'C2',
        recovery_enabled: 'false',
        total_count: 2,
        category: 'SLAVE',
      }
    ];
    it('should return parsed components, filter out not installed components', function() {
      expect(controller.parseComponentConfigs(components)).to.be.eql([
        Em.Object.create({
          "componentName": "C1",
          "displayName": "C1",
          "isFirst": true,
          "recoveryEnabled": true,
          "serviceDisplayName": "S1"
        }),
        Em.Object.create({
          "componentName": "C2",
          "displayName": "C2",
          "isFirst": false,
          "recoveryEnabled": false,
          "serviceDisplayName": "S1"
        })
      ]);
    });
  });

  describe('#load', function() {

    beforeEach(function() {
      sinon.stub(App.router.get('configurationController'), 'getCurrentConfigsBySites').returns({
        done: function(callback) {
          callback([
            {
              properties: {
                recovery_enabled: 'true'
              }
            }
          ]);
        }
      });
      sinon.stub(App.router.get('configurationController'), 'updateConfigTags').returns({
        always: Em.clb
      });
      sinon.stub(controller, 'loadComponentsConfigs').returns({
        then: Em.clb
      });
      controller.load();
    });
    afterEach(function() {
      App.router.get('configurationController').getCurrentConfigsBySites.restore();
      App.router.get('configurationController').updateConfigTags.restore();
      controller.loadComponentsConfigs.restore();
    });

    it('clusterConfigs should be set', function() {
      expect(controller.get('clusterConfigs')).to.be.eql({
        recovery_enabled: 'true'
      });
    });

    it('isGeneralRecoveryEnabled should be true', function() {
      expect(controller.get('isGeneralRecoveryEnabled')).to.be.true;
    });

    it('isGeneralRecoveryEnabledCached should be true', function() {
      expect(controller.get('isGeneralRecoveryEnabledCached')).to.be.true;
    });

    it('isLoaded should be true', function() {
      expect(controller.get('isLoaded')).to.be.true;
    });
  });

  describe('#loadComponentsConfigs()', function() {

    it('App.ajax.send should be called', function() {
      controller.loadComponentsConfigs();
      var args = testHelpers.findAjaxRequest('name', 'components.get_category');
      expect(args[0]).to.be.eql({
        name: 'components.get_category',
        sender: controller,
        success: 'loadComponentsConfigsSuccess'
      });
    });
  });

  describe('#loadComponentsConfigsSuccess()', function() {

    beforeEach(function() {
      sinon.stub(controller, 'parseComponentConfigs').returns({});
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.Object.create({
        isRestartable: true
      }));
      controller.loadComponentsConfigsSuccess({items: [
          {ServiceComponentInfo:{total_count: 0}},
          {ServiceComponentInfo:{total_count: 1}}
        ]});
    });
    afterEach(function() {
      controller.parseComponentConfigs.restore();
      App.StackServiceComponent.find.restore();
    });

    it('componentsConfigsCached should be set', function() {
      expect(controller.get('componentsConfigsCached')).to.be.eql([{total_count: 1}]);
    });

    it('componentsConfigsGrouped should be set', function() {
      expect(controller.get('componentsConfigsGrouped')).to.be.eql({});
    });
  });

  describe('#saveClusterConfigs()', function() {
    it('App.ajax.send should be called', function() {
      controller.saveClusterConfigs({recovery_enabled: 'false'}, true);
      var args = testHelpers.findAjaxRequest('name', 'admin.save_configs');
      expect(args[0]).to.be.eql({
        name: 'admin.save_configs',
        sender: controller,
        data: {
          siteName: 'cluster-env',
          properties: {
            recovery_enabled: "true"
          }
        }
      });
    });
  });

  describe('#saveComponentSettingsCall()', function() {
    it('App.ajax.send should be called', function() {
      controller.saveComponentSettingsCall(true, ['c1', 'c2']);
      var args = testHelpers.findAjaxRequest('name', 'components.update');
      expect(args[0]).to.be.eql({
        name: 'components.update',
        sender: controller,
        data: {
          ServiceComponentInfo: {
            recovery_enabled: true
          },
          query: 'ServiceComponentInfo/component_name.in(c1,c2)'
        }
      });
    });
  });

  describe('#syncStatus', function() {
    beforeEach(function() {
      sinon.stub(controller, 'propertyDidChange');
    });
    afterEach(function() {
      controller.propertyDidChange.restore();
    });

    it('should apply new values', function() {
      controller.set('isGeneralRecoveryEnabled', true);
      controller.set('componentsConfigsCached', [
        {
          component_name: 'C1',
          recovery_enabled: 'false'
        }
      ]);
      controller.set('componentsConfigsGrouped', [
        Em.Object.create({
          componentName: 'C1',
          recoveryEnabled: true
        })
      ]);
      controller.syncStatus();
      expect(controller.get('componentsConfigsCached')[0].recovery_enabled).to.be.equal('true');
      expect(controller.get('isGeneralRecoveryEnabledCached')).to.be.true;
    });
  });

  describe('#restoreCachedValues', function() {
    beforeEach(function() {
      sinon.stub(controller, 'parseComponentConfigs').returns([]);
      controller.set('isGeneralRecoveryEnabledCached', true);
      controller.restoreCachedValues();
    });
    afterEach(function() {
      controller.parseComponentConfigs.restore();
    });

    it('isGeneralRecoveryEnabled should be true', function() {
      expect(controller.get('isGeneralRecoveryEnabled')).to.be.true;
    });

    it('componentsConfigsGrouped should be set', function() {
      expect(controller.get('componentsConfigsGrouped')).to.be.eql([]);
    });
  });

  describe('#filterComponentsByChange', function() {

    it('should return checked components', function() {
      var components = [
        Em.Object.create({
          recoveryEnabled: true,
          componentName: 'C1'
        })
      ];
      controller.set('componentsConfigsCachedMap', {'C1': false});
      expect(controller.filterComponentsByChange(components, true)).to.be.eql(['C1']);
    });

    it('should return unchecked components', function() {
      var components = [
        Em.Object.create({
          recoveryEnabled: false,
          componentName: 'C1'
        })
      ];
      controller.set('componentsConfigsCachedMap', {'C1': true});
      expect(controller.filterComponentsByChange(components, false)).to.be.eql(['C1']);
    });
  });


});
