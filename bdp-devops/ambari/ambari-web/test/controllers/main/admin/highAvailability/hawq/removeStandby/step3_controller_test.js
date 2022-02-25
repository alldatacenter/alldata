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
require('controllers/main/admin/highAvailability/hawq/removeStandby/step3_controller');

describe('App.RemoveHawqStandbyWizardStep3Controller', function () {
  
  var controller;
  
  beforeEach(function() {
    controller = App.RemoveHawqStandbyWizardStep3Controller.create({
      content: Em.Object.create({}),
      deleteComponent: Em.K,
      startServices: Em.K,
      onTaskCompleted: Em.K
    });
  });
  
  describe('#removeStandby', function() {
    
    it('App.ajax.send should be called', function() {
      controller.set('content.hawqHosts', {hawqMaster: ['host1']});
      controller.removeStandby();
      
      expect(testHelpers.findAjaxRequest('name', 'service.item.executeCustomCommand')[0].data).to.be.eql({
        command: 'REMOVE_HAWQ_STANDBY',
        context: Em.I18n.t('admin.removeHawqStandby.wizard.step3.removeHawqStandbyCommand.context'),
        hosts: ['host1'],
        serviceName: 'HAWQ',
        componentName: 'HAWQMASTER'
      });
    });
  });
  
  describe('#stopRequiredServices', function() {
    beforeEach(function() {
      sinon.stub(controller, 'stopServices');
    });
    afterEach(function() {
      controller.stopServices.restore();
    });
    
    it('stopServices should be called', function() {
      controller.stopRequiredServices();
      expect(controller.stopServices.calledWith(['HAWQ'], true)).to.be.true;
    });
  });
  
  describe('#reconfigureHAWQ', function() {
    
    it('App.ajax.send should be called', function() {
      controller.reconfigureHAWQ();
      
      expect(testHelpers.findAjaxRequest('name', 'config.tags')[0]).to.be.eql({
        name: 'config.tags',
        sender: controller,
        success: 'onLoadHawqConfigsTags',
        error: 'onTaskError'
      });
    });
  });
  
  describe('#onLoadHawqConfigsTags', function() {
    
    it('App.ajax.send should be called', function() {
      controller.onLoadHawqConfigsTags({Clusters: {desired_configs: {'hawq-site': {tag: 'tag1'}}}});
      
      expect(testHelpers.findAjaxRequest('name', 'reassign.load_configs')[0]).to.be.eql({
        name: 'reassign.load_configs',
        sender: controller,
        data: {
          urlParams: '(type=hawq-site&tag=tag1)',
          type: 'hawq-site'
        },
        success: 'onLoadConfigs',
        error: 'onTaskError'
      });
    });
  });
  
  describe('#onLoadConfigs', function() {
  
    beforeEach(function() {
      sinon.stub(controller, 'reconfigureSites').returns({});
    });
    afterEach(function() {
      controller.reconfigureSites.restore();
    });
    
    it('App.ajax.send should be called', function() {
      controller.onLoadConfigs({items: [{properties: {}}]}, {}, {type: 'type1'});
      
      expect(testHelpers.findAjaxRequest('name', 'common.service.configurations')[0]).to.be.eql({
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
  
  describe('#onSaveConfigs', function() {
    beforeEach(function() {
      sinon.stub(controller, 'onTaskCompleted');
    });
    afterEach(function() {
      controller.onTaskCompleted.restore();
    });
    
    it('onTaskCompleted should be called', function() {
      controller.onSaveConfigs();
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
    });
  });
  
  describe('#deleteHawqStandbyComponent', function() {
    beforeEach(function() {
      sinon.stub(controller, 'deleteComponent');
    });
    afterEach(function() {
      controller.deleteComponent.restore();
    });
    
    it('deleteComponent should be called', function() {
      controller.set('content.hawqHosts', {hawqStandby: 'host1'});
      controller.deleteHawqStandbyComponent();
      expect(controller.deleteComponent.calledWith('HAWQSTANDBY', 'host1')).to.be.true;
    });
  });
  
  describe('#startRequiredServices', function() {
    beforeEach(function() {
      sinon.stub(controller, 'startServices');
    });
    afterEach(function() {
      controller.startServices.restore();
    });
    
    it('startServices should be called', function() {
      controller.startRequiredServices();
      expect(controller.startServices.calledWith(false, ['HAWQ'], true)).to.be.true;
    });
  });
  


});
