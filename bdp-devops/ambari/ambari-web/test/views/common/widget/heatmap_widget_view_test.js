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
require('views/common/widget/heatmap_widget_view');

describe('App.HeatmapWidgetView', function () {

  var view;

  beforeEach(function () {
    view = App.HeatmapWidgetView.create({
      controller: Em.Object.create(),
      content: Em.Object.create({
        properties: {},
        values: []
      })
    });
  });

  describe("#onMetricsLoaded()", function () {

    it("isLoaded false", function() {
      view.set('isLoaded', false);
      view.set('controller.inputMaximum', null);
      view.set('content.properties.max_limit', 1);
      view.onMetricsLoaded();
      expect(view.get('controller.inputMaximum')).to.be.equal(1);
    });

    it("isLoaded true", function() {
      view.set('isLoaded', true);
      view.set('controller.inputMaximum', null);
      view.set('content.properties.max_limit', 1);
      view.onMetricsLoaded();
      expect(view.get('controller.inputMaximum')).to.be.null
    });
  });

  describe("#willDestroyElement()", function () {
    var container = {
      abort: Em.K
    };

    beforeEach(function() {
      sinon.stub(container, 'abort');
      view.set('activeRequest', container);
      view.willDestroyElement();
    });

    afterEach(function() {
      container.abort.restore();
    });

    it("abort should be called", function() {
      expect(container.abort.calledOnce).to.be.true;
    });

    it("activeRequest should be null", function() {
      expect(view.get('activeRequest')).to.be.null;
    });
  });

  describe("#loadMetrics()", function () {

    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns(Em.Object.create({
        isStarted: false
      }));
      sinon.stub(view, 'onMetricsLoaded');
    });

    afterEach(function() {
      App.Service.find.restore();
      view.onMetricsLoaded.restore();
    });

    it("onMetricsLoaded should be called", function() {
      view.loadMetrics();
      expect(view.onMetricsLoaded.calledOnce).to.be.true;
    });
  });

  describe("#drawWidget()", function () {

    beforeEach(function() {
      sinon.stub(view, 'calculateValues').returns({});
      sinon.stub(App.MainChartHeatmapMetric, 'create', function(obj) {
        return obj;
      });
    });

    afterEach(function() {
      view.calculateValues.restore();
      App.MainChartHeatmapMetric.create.restore();
    });

    it("isLoaded = false", function() {
      view.set('controller.selectedMetric', null);
      view.set('isLoaded', false);
      view.drawWidget();
      expect(view.get('controller.selectedMetric')).to.be.null;
    });

    it("no loaded racks", function() {
      view.set('controller.selectedMetric', null);
      view.set('controller.inputMaximum', 1);
      view.setProperties({
        isLoaded: true,
        racks: [],
        content: Em.Object.create({
          widgetName: 'm1',
          properties: {
            display_unit: 'u1'
          }
        })
      });
      view.drawWidget();
      expect(view.get('controller.selectedMetric')).to.be.eql({
        name: 'm1',
        units: 'u1',
        maximumValue: 1,
        hostNames: [],
        hostToValueMap: {}
      });
    });

    it("racks loaded", function() {
      view.set('controller.selectedMetric', null);
      view.set('controller.inputMaximum', 1);
      view.setProperties({
        isLoaded: true,
        racks: [{
          hosts: [{
            hostName: 'host1'
          }],
          isLoaded: true
        }],
        content: Em.Object.create({
          widgetName: 'm1',
          properties: {
            display_unit: 'u1'
          }
        })
      });
      view.drawWidget();
      expect(view.get('controller.selectedMetric')).to.be.eql({
        name: 'm1',
        units: 'u1',
        maximumValue: 1,
        hostNames: ['host1'],
        hostToValueMap: {}
      });
    });
  });

  describe("#calculateValues()", function () {

    beforeEach(function() {
      sinon.stub(view, 'computeExpression').returns({});
      sinon.stub(view, 'extractExpressions');
    });

    afterEach(function() {
      view.computeExpression.restore();
      view.extractExpressions.restore();
    });

    it("calculateValues should return object", function() {
      view.set('content.values', [{}]);
      view.set('metrics', [{}]);
      expect(view.calculateValues()).to.be.eql({});
    });
  });

  describe("#computeExpression()", function () {

    beforeEach(function() {
      sinon.stub(view, 'convertDataWhenMB');
    });

    afterEach(function() {
      view.convertDataWhenMB.restore();
    });

    var testCases = [
      {
        expressions: [],
        metrics: [],
        expected: {}
      },
      {
        expressions: ['m2'],
        metrics: [{
          name: 'm1',
          hostName: 'host1'
        }],
        expected: {'host1': undefined}
      },
      {
        expressions: ['1'],
        metrics: [{
          name: 'm1',
          hostName: 'host1'
        }],
        expected: {
          'host1': '1'
        }
      },
      {
        expressions: ['m1'],
        metrics: [{
          name: 'm1',
          hostName: 'host1',
          data: '2'
        }],
        expected: {
          'host1': '2'
        }
      },
      {
        expressions: ['m1+1'],
        metrics: [{
          name: 'm1',
          hostName: 'host1',
          data: '2'
        }],
        expected: {
          'host1': '3'
        }
      },
      {
        expressions: ['m1'],
        metrics: [{
          name: 'm1',
          hostName: 'host1',
          data: '0/0'
        }],
        expected: {
          'host1': 'NaN'
        }
      },
      {
        expressions: ['m1-m2'],
        metrics: [{
          name: 'm1',
          hostName: 'host1',
          data: '2'
        }],
        expected: {'host1': undefined}
      }
    ];

    testCases.forEach(function(test) {
      it("expressions=" + JSON.stringify(test.expressions) +
         " metrics=" + JSON.stringify(test.metrics), function() {
        expect(view.computeExpression(test.expressions, test.metrics)).to.be.eql(test.expected);
      });
    });
  });

  describe('#convertDataWhenMB', function() {

    it('should convert MB to bytes', function() {
      var metric = {
        metric_path: 'readM',
        data: 1
      };
      view.convertDataWhenMB(metric);
      expect(metric.data).to.be.equal(1 * 1024 * 1024);
      expect(metric.originalData).to.be.equal(1);
    });
  });
});