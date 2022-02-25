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
require('/views/main/service/widgets/create/wizard_view');


describe('App.WidgetWizardView', function () {
  var view;

  beforeEach(function() {
    view = App.WidgetWizardView.create({
      controller: Em.Object.create({
        content: Em.Object.create()
      })
    });
  });

  describe("#previewWidgetClass", function() {

    it("widgetType is null", function() {
      view.set('controller.content.widgetType', null);
      view.propertyDidChange('previewWidgetClass');
      expect(view.get('previewWidgetClass').create()).to.be.instanceof(Em.View);
    });

    it("widgetType is GRAPH", function() {
      view.set('controller.content.widgetType', 'GRAPH');
      view.propertyDidChange('previewWidgetClass');
      expect(view.get('previewWidgetClass').create()).to.be.instanceof(App.GraphWidgetView);
    });

    it("widgetType is TEMPLATE", function() {
      view.set('controller.content.widgetType', 'TEMPLATE');
      view.propertyDidChange('previewWidgetClass');
      expect(view.get('previewWidgetClass').create()).to.be.instanceof(App.TemplateWidgetView);
    });

    it("widgetType is NUMBER", function() {
      view.set('controller.content.widgetType', 'NUMBER');
      view.propertyDidChange('previewWidgetClass');
      expect(view.get('previewWidgetClass').create()).to.be.instanceof(App.NumberWidgetView);
    });

    it("widgetType is GAUGE", function() {
      view.set('controller.content.widgetType', 'GAUGE');
      view.propertyDidChange('previewWidgetClass');
      expect(view.get('previewWidgetClass').create()).to.be.instanceof(App.GaugeWidgetView);
    });
  });

  describe("#isStep2", function() {

    it("currentStep is '2'", function() {
      view.set('controller.currentStep', '2');
      view.propertyDidChange('isStep2');
      expect(view.get('isStep2')).to.be.true;
    });

    it("currentStep is 2", function() {
      view.set('controller.currentStep', 2);
      view.propertyDidChange('isStep2');
      expect(view.get('isStep2')).to.be.true;
    });

    it("currentStep is null", function() {
      view.set('controller.currentStep', null);
      view.propertyDidChange('isStep2');
      expect(view.get('isStep2')).to.be.false;
    });
  });

  describe("#isStep3", function() {

    it("currentStep is '3'", function() {
      view.set('controller.currentStep', '3');
      view.propertyDidChange('isStep3');
      expect(view.get('isStep3')).to.be.true;
    });

    it("currentStep is 3", function() {
      view.set('controller.currentStep', 3);
      view.propertyDidChange('isStep3');
      expect(view.get('isStep3')).to.be.true;
    });

    it("currentStep is null", function() {
      view.set('controller.currentStep', null);
      view.propertyDidChange('isStep3');
      expect(view.get('isStep3')).to.be.false;
    });
  });

});
