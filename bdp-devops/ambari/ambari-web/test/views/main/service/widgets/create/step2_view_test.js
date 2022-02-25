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
require('/views/main/service/widgets/create/step2_view');


describe('App.WidgetWizardStep2View', function () {
  var view;

  beforeEach(function() {
    view = App.WidgetWizardStep2View.create({
      controller: Em.Object.create({
        content: Em.Object.create(),
        convertData: Em.K,
        initWidgetData: Em.K,
        renderProperties: Em.K,
        updateExpressions: Em.K
      })
    });
  });

  describe("#templateType", function () {

    it("widgetType is null", function () {
      view.set('controller.content.widgetType', null);
      view.propertyDidChange('templateType');
      expect(view.get('templateType')).to.be.empty;
    });

    it("widgetType is GAUGE", function () {
      view.set('controller.content.widgetType', 'GAUGE');
      view.propertyDidChange('templateType');
      expect(view.get('templateType')).to.eql({isNumber: true});
    });

    it("widgetType is NUMBER", function () {
      view.set('controller.content.widgetType', 'NUMBER');
      view.propertyDidChange('templateType');
      expect(view.get('templateType')).to.eql({isNumber: true});
    });

    it("widgetType is TEMPLATE", function () {
      view.set('controller.content.widgetType', 'TEMPLATE');
      view.propertyDidChange('templateType');
      expect(view.get('templateType')).to.eql({isTemplate: true});
    });

    it("widgetType is GRAPH", function () {
      view.set('controller.content.widgetType', 'GRAPH');
      view.propertyDidChange('templateType');
      expect(view.get('templateType')).to.eql({isGraph: true});
    });
  });

  describe("#ensureTooltip()", function() {

    beforeEach(function () {
      sinon.stub(Em.run, 'later', function (ctx, callback) {
        callback();
      });
      sinon.stub(App, 'tooltip');
      view.ensureTooltip();
    });
    afterEach(function () {
      Em.run.later.restore();
      App.tooltip.restore();
    });

    it("Em.run.later should be called", function() {
      expect(Em.run.later.calledOnce).to.be.true;
    });

    it("App.tooltip should be called", function() {
      expect(App.tooltip.calledOnce).to.be.true;
    });
  });

  describe("#didInsertElement()", function () {

    beforeEach(function () {
      sinon.stub(view, 'ensureTooltip');
      sinon.stub(view.get('controller'), 'convertData');
      sinon.stub(view.get('controller'), 'initWidgetData');
      sinon.stub(view.get('controller'), 'renderProperties');
      sinon.stub(view.get('controller'), 'updateExpressions');
      view.didInsertElement();
    });
    afterEach(function () {
      view.ensureTooltip.restore();
      view.get('controller').convertData.restore();
      view.get('controller').initWidgetData.restore();
      view.get('controller').renderProperties.restore();
      view.get('controller').updateExpressions.restore();
    });

    it("ensureTooltip should be called", function () {
      expect(view.ensureTooltip.calledOnce).to.be.true;
    });

    it("convertData should be called", function () {
      expect(view.get('controller').convertData.calledOnce).to.be.true;
    });

    it("initWidgetData should be called", function () {
      expect(view.get('controller').initWidgetData.calledOnce).to.be.true;
    });

    it("renderProperties should be called", function () {
      expect(view.get('controller').renderProperties.calledOnce).to.be.true;
    });

    it("updateExpressions should be called", function () {
      expect(view.get('controller').updateExpressions.calledOnce).to.be.true;
    });
  });
});

describe('#App.WidgetPropertySelectView', function() {
  var view;
  
  beforeEach(function() {
    view = App.WidgetPropertySelectView.create({
      property: Em.Object.create({
        options: [
          {
            label: 'l1',
            value: 'v1'
          }
        ]
      })
    });
  });
  
  describe('#didInsertElement', function() {
    beforeEach(function() {
      sinon.stub(view, 'addObserver');
      sinon.stub(view, 'setValue');
      view.set('property.value', 'v1');
      view.didInsertElement();
    });
    afterEach(function() {
      view.addObserver.restore();
      view.setValue.restore();
    });
    
    it('should set selection', function() {
      expect(view.get('selection')).to.be.eql({
        label: 'l1',
        value: 'v1'
      });
    });
  
    it('addObserver should be called', function() {
      expect(view.addObserver.calledWith('selection.value', view, 'setValue')).to.be.true;
    });
  
    it('setValue should be called', function() {
      expect(view.setValue.called).to.be.true;
    });
  });
 
  describe('#setValue', function() {
    
    it('should set value to 1', function() {
      view.set('selection', {
        value: 1
      });
      view.setValue();
      expect(view.get('property.value')).to.be.equal(1);
    });
  });
});
