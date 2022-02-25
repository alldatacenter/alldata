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
require('controllers/main/admin/highAvailability/hawq/addStandby/step3_controller');
var testHelpers = require('test/helpers');

function getController() {
  return App.AddHawqStandbyWizardStep3Controller.create({
    content: Em.Object.create({})
  });
}
var controller;

describe('App.AddHawqStandbyWizardStep3Controller', function () {

  beforeEach(function () {
    controller = getController();
  });

  describe('#isSubmitDisabled', function () {

    var cases = [
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

  describe('#loadConfigTagsSuccessCallback', function () {

    it('should send proper ajax request', function () {
      controller.loadConfigTagsSuccessCallback({
        'Clusters': {
          'desired_configs': {
            'hawq-site': {
              'tag': 1
            }
          }
        }
      }, {}, {
        'serviceConfig': {}
      });
      var data = testHelpers.findAjaxRequest('name', 'reassign.load_configs')[0].data;
      expect(data.urlParams).to.equal('(type=hawq-site&tag=1)');
      expect(data.serviceConfig).to.eql({});
    });

  });

  describe('#loadConfigsSuccessCallback', function () {

    var cases = [
        {
          'title': 'should set properties from load config success callback',
          'items': [
            {
              'type': 'hawq-site',
              'properties': {
                'hawq_master_address_host' : 'h0'
              }
            }
          ],
          'params': {
            'serviceConfig': {}
          }
        }
      ];

    beforeEach(function () {
      sinon.stub(controller, 'setDynamicConfigValues', Em.K);
    });

    afterEach(function () {
      controller.setDynamicConfigValues.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.loadConfigsSuccessCallback({
          items: item.items
        }, {}, item.params);
        expect(controller.get('selectedService')).to.eql({});
        expect(controller.get('isLoaded')).to.be.true;
      });
    });

  });

  describe('#loadConfigsSuccessCallback=loadConfigsErrorCallback(we have one callback for both cases)', function () {

    beforeEach(function () {
      sinon.stub(controller, 'setDynamicConfigValues', Em.K);
    });

    afterEach(function () {
      controller.setDynamicConfigValues.restore();
    });

    it('should proceed with default value', function () {
      controller.loadConfigsSuccessCallback({}, {}, {});
      expect(controller.get('selectedService')).to.eql({});
      expect(controller.get('isLoaded')).to.be.true;
    });

  });

  describe('#setDynamicConfigValues', function () {

    var data = {
      items: [
        {
          type: 'hawq-site',
          properties: {
           hawq_master_address_host : 'h0'
          }
        }
      ]
    };

    var configs = {
      configs: [
        Em.Object.create({
          name: 'hawq_standby_address_host'
        })
      ]
    };


    beforeEach(function () {
      controller = App.AddHawqStandbyWizardStep3Controller.create({
        content: Em.Object.create({
          masterComponentHosts: [
            {component: 'HAWQMASTER', hostName: 'h0', isInstalled: true},
            {component: 'HAWQSTANDBY', hostName: 'h1', isInstalled: false}
          ],
          slaveComponentHosts: [],
          hosts: {},
          hawqHost: {
            hawqMaster: 'h0',
            newHawqStandby: 'h1'
          }
        })
      });
      controller.setDynamicConfigValues(configs, data);
    });

    it('hawq_standby_address_host value', function () {
      expect(configs.configs.findProperty('name', 'hawq_standby_address_host').get('value')).to.equal('h1');
    });
    it('hawq_standby_address_host recommendedValue', function () {
      expect(configs.configs.findProperty('name', 'hawq_standby_address_host').get('recommendedValue')).to.equal('h1');
    });
  });
  
  describe('#loadStep', function() {
    beforeEach(function() {
      sinon.stub(controller, 'renderConfigs');
    });
    afterEach(function() {
      controller.renderConfigs.restore();
    });
    
    it('renderConfigs should be called', function() {
      controller.loadStep();
      expect(controller.renderConfigs.calledOnce).to.be.true;
    });
  });
  
  describe('#renderConfigs', function() {
    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns([{
        serviceName: 'HAWQ'
      }]);
      sinon.stub(controller, 'renderConfigProperties');
      controller.renderConfigs();
    });
    afterEach(function() {
      App.Service.find.restore();
      controller.renderConfigProperties.restore();
    });
    
    it('Request should be sent', function() {
      var request = testHelpers.findAjaxRequest('name', 'config.tags');
      expect(request[0]).to.exist;
    });
  
    it('renderConfigProperties should be called', function() {
      expect(controller.renderConfigProperties.calledOnce).to.be.true;
    });
  });
  
  describe('#renderConfigProperties', function() {
    
    it('should move component configs', function() {
      var _componentConfig = {configs: [{isReconfigurable: true}]};
      var componentConfig = {configs: []};
      controller.renderConfigProperties(_componentConfig, componentConfig);
      expect(componentConfig.configs).to.not.be.empty;
      expect(componentConfig.configs[0].get('isEditable')).to.be.true;
    });
  });
  
  describe('#submit', function() {
    beforeEach(function() {
      sinon.stub(App, 'showConfirmationPopup', Em.clb);
      sinon.stub(App, 'get').returns({getKDCSessionState: Em.clb});
      sinon.stub(App.router, 'send');
      controller.set('isLoaded', true);
      controller.set('hawqProps', {items: [{properties: {'hawq_master_directory': 'dir'}}]});
      controller.submit();
    });
    afterEach(function() {
      App.showConfirmationPopup.restore();
      App.get.restore();
      App.router.send.restore();
    });
    
    it('App.showConfirmationPopup should be called', function() {
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
    });
  
    it('App.router.send should be called', function() {
      expect(App.router.send.calledWith('next')).to.be.true;
    });
  });
  
  
});
