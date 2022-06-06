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

App = require('app');
var testHelpers = require('test/helpers');

require('controllers/main/service/widgets/edit_controller');

describe('App.WidgetEditController', function () {
  var controller;

  beforeEach(function() {
    controller = App.WidgetEditController.create({
      content: Em.Object.create()
    });
  });
  
  describe('#putWidgetDefinition', function() {
    
    it('App.ajax.send should be called', function() {
      controller.set('content.widgetId', 1);
      controller.putWidgetDefinition({});
      var args = testHelpers.filterAjaxRequests('name', 'widgets.wizard.edit');
      expect(args[0][0].data).to.eql({
        data: {},
        widgetId: 1
      });
    });
  });
  
  describe('#finish', function() {
    beforeEach(function() {
      sinon.stub(controller, 'setCurrentStep');
      sinon.stub(controller, 'resetDbNamespace');
      sinon.stub(controller, 'save');
    });
    afterEach(function() {
      controller.setCurrentStep.restore();
      controller.resetDbNamespace.restore();
      controller.save.restore();
    });
    
    it('setCurrentStep should be called', function() {
      controller.finish();
      expect(controller.setCurrentStep.calledWith('1', false, true)).to.be.true;
    });
  
    it('resetDbNamespace should be called', function() {
      controller.finish();
      expect(controller.resetDbNamespace.calledOnce).to.be.true;
    });
  
    it('save should be called', function() {
      controller.finish();
      expect(controller.save.callCount).to.be.equal(13);
    });
  });
  
  describe('#loadMap', function() {
    describe('#step1', function() {
      beforeEach(function() {
        sinon.stub(controller, 'load');
        sinon.stub(controller, 'loadAllMetrics');
      });
      afterEach(function() {
        controller.load.restore();
        controller.loadAllMetrics.restore();
      });
      
      it('should load widgetType', function() {
        controller.loadMap['1'][0].callback.apply(controller);
        expect(controller.load.calledWith('widgetType')).to.be.true;
      });
  
      it('should load widgetService', function() {
        controller.loadMap['1'][0].callback.apply(controller);
        expect(controller.load.calledWith('widgetService')).to.be.true;
      });
  
      it('should load widgetProperties', function() {
        controller.loadMap['1'][0].callback.apply(controller);
        expect(controller.load.calledWith('widgetProperties', true)).to.be.true;
      });
  
      it('should load widgetValues', function() {
        controller.loadMap['1'][0].callback.apply(controller);
        expect(controller.load.calledWith('widgetValues', true)).to.be.true;
      });
  
      it('should load widgetMetrics', function() {
        controller.loadMap['1'][0].callback.apply(controller);
        expect(controller.load.calledWith('widgetMetrics', true)).to.be.true;
      });
  
      it('loadAllMetrics should be called', function() {
        controller.loadMap['1'][1].callback.apply(controller);
        expect(controller.loadAllMetrics.calledOnce).to.be.true;
      });
    });
    
    describe('#step2', function() {
      beforeEach(function() {
        sinon.stub(controller, 'load');
        controller.loadMap['2'][0].callback.apply(controller);
      });
      afterEach(function() {
        controller.load.restore();
      });
  
      it('should load widgetName', function() {
        expect(controller.load.calledWith('widgetName')).to.be.true;
      });
  
      it('should load widgetDescription', function() {
        expect(controller.load.calledWith('widgetDescription')).to.be.true;
      });
  
      it('should load widgetAuthor', function() {
        expect(controller.load.calledWith('widgetAuthor')).to.be.true;
      });
    });
  });
  
  
});
