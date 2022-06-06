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
require('views/main/service/info/metrics_view');

describe('App.MainServiceInfoMetricsView', function() {

  var view = App.MainServiceInfoMetricsView.create({
    controller: Em.Object.create({
      content: Em.Object.create({
        id: 'HDFS',
        serviceName: 'HDFS',
        hostComponents: []
      }),
      getActiveWidgetLayout: Em.K,
      loadWidgetLayouts: Em.K
    }),
    service: Em.Object.create()
  });

  describe("#getServiceModel()", function() {

    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns({serviceName: 'S1'});
      sinon.stub(App.HDFSService, 'find').returns([{serviceName: 'HDFS'}]);
    });
    afterEach(function() {
      App.Service.find.restore();
      App.HDFSService.find.restore();
    });

    it("HDFS service", function() {
      expect(view.getServiceModel('HDFS')).to.eql({serviceName: 'HDFS'});
    });

    it("Simple model service", function() {
      expect(view.getServiceModel('S1')).to.eql({serviceName: 'S1'});
    });
  });

  describe("#constructGraphObjects()", function() {
    var mock = Em.Object.create({
      isServiceWithWidgets: false
    });

    beforeEach(function() {
      sinon.stub(App.StackService, 'find').returns(mock);
      sinon.stub(view, 'getUserPref').returns({
        complete: function(callback){callback();}
      })
    });
    afterEach(function() {
      App.StackService.find.restore();
      view.getUserPref.restore();
    });

    it("metrics not loaded", function() {
      mock.set('isServiceWithWidgets', false);
      view.constructGraphObjects(null);
      expect(view.get('serviceHasMetrics')).to.be.false;
      expect(view.getUserPref.called).to.be.false;
    });

    it("metrics loaded", function() {
      App.ChartServiceMetricsG1 = Em.Object.extend();
      mock.set('isServiceWithWidgets', true);
      view.constructGraphObjects(['G1']);
      expect(view.get('serviceHasMetrics')).to.be.true;
      expect(view.getUserPref.calledOnce).to.be.true;
      expect(view.get('serviceMetricGraphs')).to.not.be.empty;
    });
  });

  describe("#getUserPrefSuccessCallback()", function() {

    it("currentTimeRangeIndex should be set", function() {
      view.getUserPrefSuccessCallback(1);
      expect(view.get('currentTimeRangeIndex')).to.equal(1);
    });
  });

  describe("#getUserPrefErrorCallback()", function() {

    beforeEach(function() {
      sinon.stub(view, 'postUserPref');
    });
    afterEach(function() {
      view.postUserPref.restore();
    });

    it("request.status = 404", function() {
      view.getUserPrefErrorCallback({status: 404});
      expect(view.get('currentTimeRangeIndex')).to.equal(0);
      expect(view.postUserPref.calledOnce).to.be.true;
    });

    it("request.status = 403", function() {
      view.getUserPrefErrorCallback({status: 403});
      expect(view.postUserPref.called).to.be.false;
    });
  });

  describe("#widgetActions", function() {

    beforeEach(function() {
      this.mock = sinon.stub(App, 'isAuthorized');
      view.setProperties({
        staticWidgetLayoutActions: [{id: 1}],
        staticAdminPrivelegeWidgetActions: [{id: 2}],
        staticGeneralWidgetActions: [{id: 3}]
      });
    });
    afterEach(function() {
      this.mock.restore();
    });

    it("not authorized", function() {
      this.mock.returns(false);
      view.propertyDidChange('widgetActions');
      expect(view.get('widgetActions').mapProperty('id')).to.eql([3]);
    });

    it("is authorized", function() {
      this.mock.returns(true);
      App.supports.customizedWidgetLayout = true;
      view.propertyDidChange('widgetActions');
      expect(view.get('widgetActions').mapProperty('id')).to.eql([1, 2, 3]);
    });
  });

  describe("#doWidgetAction()", function() {

    beforeEach(function() {
      view.set('controller.action1', Em.K);
      sinon.stub(view.get('controller'), 'action1');
    });
    afterEach(function() {
      view.get('controller').action1.restore();
    });

    it("action exist", function() {
      view.doWidgetAction({context: 'action1'});
      expect(view.get('controller').action1.calledOnce).to.be.true;
    });
  });

  describe("#setTimeRange", function() {

    it("range = 0", function() {
      var widget = Em.Object.create({
        widgetType: 'GRAPH',
        properties: {
          time_range: '0'
        }
      });
      view.set('controller.widgets', [widget]);
      view.setTimeRange({context: {value: '0'}});
      expect(widget.get('properties').time_range).to.be.equal('0')
    });

    it("range = 1", function() {
      var widget = Em.Object.create({
        widgetType: 'GRAPH',
        properties: {
          time_range: 0
        }
      });
      view.set('controller.widgets', [widget]);
      view.setTimeRange({context: {value: '1'}});
      expect(widget.get('properties').time_range).to.be.equal('1')
    });
  });

  describe("#makeSortable()", function() {
    var mock = {
      on: function(arg1, arg2, callback) {
        callback();
      },
      off: Em.K,
      sortable: function() {
        return {
          disableSelection: Em.K
        }
      }
    };

    beforeEach(function() {
      sinon.stub(window, '$').returns(mock);
      sinon.spy(mock, 'on');
      sinon.spy(mock, 'off');
      sinon.spy(mock, 'sortable');
    });
    afterEach(function() {
      window.$.restore();
      mock.on.restore();
      mock.off.restore();
      mock.sortable.restore();
    });

    it("on() should be called", function() {
      view.makeSortable('#widget_layout');
      expect(mock.on.calledWith('DOMNodeInserted', '#widget_layout')).to.be.true;
    });

    it("sortable() should be called", function() {
      view.makeSortable('#widget_layout');
      expect(mock.sortable.called).to.be.true;
    });

    it("off() should be called", function() {
      view.makeSortable('#widget_layout');
      expect(mock.off.calledWith('DOMNodeInserted', '#widget_layout')).to.be.true;
    });
  });

  describe('#didInsertElement', function () {

    beforeEach(function () {
      sinon.stub(view, 'constructGraphObjects', Em.K);
      this.mock = sinon.stub(App, 'get');
      sinon.stub(view, 'getServiceModel');
      sinon.stub(view, 'loadActiveWidgetLayout');
      sinon.stub(view.get('controller'), 'loadWidgetLayouts');
      sinon.stub(view, 'makeSortable');
      sinon.stub(view, 'addWidgetTooltip');

    });

    afterEach(function () {
      view.constructGraphObjects.restore();
      this.mock.restore();
      view.getServiceModel.restore();
      view.loadActiveWidgetLayout.restore();
      view.get('controller').loadWidgetLayouts.restore();
      view.makeSortable.restore();
      view.addWidgetTooltip.restore();
    });

    it("getServiceModel should be called", function() {
      view.didInsertElement();
      expect(view.getServiceModel.calledOnce).to.be.true;
    });
    it("addWidgetTooltip should be called", function() {
      view.didInsertElement();
      expect(view.addWidgetTooltip.calledOnce).to.be.true;
    });
    it("makeSortable should be called", function() {
      view.didInsertElement();
      expect(view.makeSortable.called).to.be.true;
    });
    it("loadActiveWidgetLayout should be called", function() {
      view.didInsertElement();
      expect(view.loadActiveWidgetLayout.called).to.be.true;
    });

    describe("serviceName is null, metrics not supported, widgets not supported", function() {
      beforeEach(function () {
        view.set('controller.content.serviceName', null);
        this.mock.returns(false);
        view.didInsertElement();
      });

      it("loadWidgetLayouts should not be called", function() {
        expect(view.get('controller').loadWidgetLayouts.called).to.be.false;
      });
      it("constructGraphObjects should not be called", function() {
        expect(view.constructGraphObjects.called).to.be.false;
      });
    });

    describe("serviceName is set, metrics is supported, widgets is supported", function() {
      beforeEach(function () {
        view.set('controller.content.serviceName', 'S1');
        this.mock.returns(true);
        view.didInsertElement();
      });

      it("loadWidgetLayouts should be called", function() {
        expect(view.get('controller').loadWidgetLayouts.calledOnce).to.be.true;
      });
      it("constructGraphObjects should be called", function() {
        expect(view.constructGraphObjects.calledOnce).to.be.true;
      });
    });
  });

  describe("#addWidgetTooltip()", function() {
    var mock = {
      hoverIntent: Em.K
    };

    beforeEach(function() {
      sinon.stub(Em.run, 'later', function(arg1, callback) {
        callback();
      });
      sinon.stub(App, 'tooltip');
      sinon.stub(window, '$').returns(mock);
      sinon.spy(mock, 'hoverIntent');
      view.addWidgetTooltip();
    });
    afterEach(function() {
      Em.run.later.restore();
      App.tooltip.restore();
      window.$.restore();
      mock.hoverIntent.restore();
    });

    it("Em.run.later should be called", function() {
      expect(Em.run.later.calledOnce).to.be.true;
    });
    it("App.tooltip should be called", function() {
      expect(App.tooltip.calledOnce).to.be.true;
    });
    it("hoverIntent should be called", function() {
      expect(mock.hoverIntent.calledOnce).to.be.true;
    });
  });

});