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
require('controllers/main/service/info/metric');
var testHelpers = require('test/helpers');
function getController() {
  return App.MainServiceInfoMetricsController.create();
}

describe('App.MainServiceInfoMetricsController', function () {

  var controller;

  beforeEach(function () {
    controller = App.MainServiceInfoMetricsController.create();
  });

  App.TestAliases.testAsComputedOr(getController(), 'showTimeRangeControl', ['!isServiceWithEnhancedWidgets', 'someWidgetGraphExists']);


  describe("#getActiveWidgetLayout() for Enhanced Dashboard", function () {

    it("make GET call", function () {
      controller.reopen({
        isServiceWithEnhancedWidgets: true,
        content: Em.Object.create({serviceName: 'HDFS'})
      });
      controller.getActiveWidgetLayout();
      expect(testHelpers.findAjaxRequest('name', 'widgets.layouts.active.get')).to.exists;
    });
  });

  describe("#getActiveWidgetLayoutSuccessCallback()", function () {
    beforeEach(function () {
      sinon.stub( App.widgetLayoutMapper, 'map');
      sinon.stub( App.widgetMapper, 'map');
    });
    afterEach(function () {
      App.widgetLayoutMapper.map.restore();
      App.widgetMapper.map.restore();
    });
    it("isWidgetLayoutsLoaded should be set to true", function () {
      controller.reopen({
        isServiceWithEnhancedWidgets: true,
        content: Em.Object.create({serviceName: 'HDFS'})
      });
      controller.getActiveWidgetLayoutSuccessCallback({items:[{
        WidgetLayoutInfo: {}
      }]});
      expect(controller.get('isWidgetsLoaded')).to.be.true;
    });

  });

  describe("#hideWidgetSuccessCallback()", function () {
    beforeEach(function () {
      sinon.stub(App.widgetLayoutMapper, 'map');
      sinon.stub(controller, 'propertyDidChange');
      var params = {
        data: {
          WidgetLayoutInfo: {
            widgets: [
              {id: 1}
            ]
          }
        }
      };
      controller.hideWidgetSuccessCallback({}, {}, params);
    });
    afterEach(function () {
      App.widgetLayoutMapper.map.restore();
      controller.propertyDidChange.restore();
    });
    it("mapper is called with valid data", function () {
      expect(App.widgetLayoutMapper.map.calledWith({
        items: [{
          WidgetLayoutInfo: {
            widgets: [
              {
                WidgetInfo: {
                  id: 1
                }
              }
            ]
          }
        }]
      })).to.be.true;
    });
    it('`widgets` is forced to be recalculated', function () {
      expect(controller.propertyDidChange.calledWith('widgets')).to.be.true;
    });
  });

});