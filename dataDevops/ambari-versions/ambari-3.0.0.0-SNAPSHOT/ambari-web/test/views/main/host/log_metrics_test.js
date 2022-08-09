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

describe('App.MainHostLogMetrics', function() {
  var view;

  beforeEach(function() {
    view = App.MainHostLogMetrics.create({
      content: Em.Object.create()
    });
  });

  describe("#logsData", function () {

    it("should return logsData", function() {
      view.set('content.hostComponents', [{service: 'S1'}]);
      view.propertyDidChange('logsData');
      expect(view.get('logsData').mapProperty('service')).to.be.eql(['S1']);
      expect(view.get('logsData')[0].get('logs').mapProperty('level')).to.be.eql([
        'fatal', 'critical', 'error', 'warning', 'info', 'debug'
      ]);
    });
  });

  describe("#chartView", function () {
    var chartView;

    beforeEach(function () {
      chartView = view.get('chartView').create({
        donut: Em.K
      });
    });

    describe("#prepareChartData()", function () {

      it("data should be set", function () {
        chartView.prepareChartData(Em.Object.create({logs: [Em.Object.create()]}));
        expect(chartView.get('data')).to.be.eql([Em.Object.create()]);
      });
    });

    describe("#didInsertElement()", function () {

      beforeEach(function() {
        sinon.stub(chartView, 'prepareChartData');
        sinon.stub(chartView, 'appendLabels');
        sinon.stub(chartView, 'formatCenterText');
        sinon.stub(chartView, 'attachArcEvents');
        sinon.stub(chartView, 'colorizeArcs');
        chartView.didInsertElement();
      });

      afterEach(function() {
        chartView.prepareChartData.restore();
        chartView.appendLabels.restore();
        chartView.formatCenterText.restore();
        chartView.attachArcEvents.restore();
        chartView.colorizeArcs.restore();
      });

      it("prepareChartData should be called", function() {
        expect(chartView.prepareChartData.calledOnce).to.be.true;
      });

      it("appendLabels should be called", function() {
        expect(chartView.appendLabels.calledOnce).to.be.true;
      });

      it("formatCenterText should be called", function() {
        expect(chartView.formatCenterText.calledOnce).to.be.true;
      });

      it("attachArcEvents should be called", function() {
        expect(chartView.attachArcEvents.calledOnce).to.be.true;
      });

      it("colorizeArcs should be called", function() {
        expect(chartView.colorizeArcs.calledOnce).to.be.true;
      });
    });
  });

  describe("#transitionByService()", function () {

    beforeEach(function() {
      sinon.stub(App.router, 'transitionTo');
    });

    afterEach(function() {
      App.router.transitionTo.restore()
    });

    it("App.router.transitionTo should be called", function() {
      view.transitionByService({context: Em.Object.create({service: {serviceName: 'S1'}})});
      expect(App.router.transitionTo.calledWith('logs', {query: '?service_name=S1'})).to.be.true;
    });
  });
});
