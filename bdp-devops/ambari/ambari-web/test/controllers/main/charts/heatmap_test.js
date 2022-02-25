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
var controller;
require('controllers/main/charts/heatmap');
var testHelpers = require('test/helpers');

function getController() {
  return App.MainChartsHeatmapController.create();
}

describe('MainChartsHeatmapController', function () {

  before(function () {
    controller = getController();
  });

  App.TestAliases.testAsComputedAlias(getController(), 'activeWidget', 'widgets.firstObject', 'object');

  App.TestAliases.testAsComputedAlias(getController(), 'hostToSlotMap', 'selectedMetric.hostToSlotMap', 'object');

  describe('#validation()', function () {

    beforeEach(function() {
      controller.setProperties({
        allMetrics: [],
        selectedMetric: Ember.Object.create({maximumValue: 100})
      });
    });

    it('should set maximumValue if inputMaximum consists only of digits', function () {
      controller.set("inputMaximum", 5);
      expect(controller.get('selectedMetric.maximumValue')).to.equal(5);
    });
    it('should not set maximumValue if inputMaximum consists not only of digits', function () {
      controller.set("inputMaximum", 'qwerty');
      expect(controller.get('selectedMetric.maximumValue')).to.equal(100);
    });
    it('should not set maximumValue if inputMaximum consists not only of digits (2)', function () {
      controller.set("inputMaximum", '100%');
      expect(controller.get('selectedMetric.maximumValue')).to.equal(100);
    });
    it('should set maximumValue if inputMaximum consists only of digits (2)', function () {
      controller.set("inputMaximum", 1000);
      expect(controller.get('selectedMetric.maximumValue')).to.equal(1000);
    })
  });

  describe('#showHeatMapMetric()', function () {
    beforeEach(function () {
      controller.setProperties({
        activeWidgetLayout: Em.Object.create({
          displayName: 'widget',
          id: '1',
          scope: 'CLUSTER',
          layoutName: 'defualt_layout',
          sectionName: 'default_section'
        })
      });
    });

    it('should call App.ajax', function () {
      controller.showHeatMapMetric({context:{id: 2}});
      var args = testHelpers.findAjaxRequest('name', 'widget.layout.edit');
      expect(args).to.exists;
    });
  });

  describe('#rackClass', function () {

    beforeEach(function () {
      controller.setProperties({
        allMetrics: [],
        racks: [1]
      });
    });

    it('should return "col-md-12" for 1 cluster rack', function () {
      expect(controller.get('rackClass')).to.equal('col-md-12');
    });
    it('should return "col-md-6" for 2 cluster racks', function () {
      controller.set('racks', [1, 2]);
      expect(controller.get('rackClass')).to.equal('col-md-6');
    });
    it('should return "col-md-4" for 3 cluster racks', function () {
      controller.set('racks', [1, 2, 3]);
      expect(controller.get('rackClass')).to.equal('col-md-4');
    });
  });

  describe("#loadHeatmapsUrlParams", function() {

    it("content.serviceName is null", function() {
      controller.set('content', Em.Object.create({serviceName: null}));
      expect(controller.get('loadHeatmapsUrlParams')).to.equal('WidgetInfo/widget_type=HEATMAP&WidgetInfo/scope=CLUSTER&fields=WidgetInfo/metrics');
    });

    it("content.serviceName is correct", function() {
      controller.set('content', Em.Object.create({serviceName: 'S1'}));
      expect(controller.get('loadHeatmapsUrlParams')).to.equal('WidgetInfo/widget_type=HEATMAP&WidgetInfo/scope=CLUSTER&WidgetInfo/metrics.matches(.*\"service_name\":\"S1\".*)&fields=WidgetInfo/metrics');
    });
  });

  describe("#loadPageData()", function() {
    var allHeatmapData = {
      items: [
        {
          WidgetInfo: 'info'
        }
      ]
    };

    beforeEach(function(){
      sinon.stub(controller, 'loadRacks').returns({
        always: function(callback) {
          callback();
        }
      });
      sinon.stub(controller, 'getAllHeatMaps').returns({
        done: function(callback) {
          callback(allHeatmapData);
        }
      });
      sinon.stub(controller, 'resetPageData');
      sinon.stub(controller, 'categorizeByServiceName').returns('categories');
      sinon.stub(controller, 'getActiveWidgetLayout');
      controller.get('allHeatmaps').clear();
      controller.loadPageData();
    });

    afterEach(function() {
      controller.loadRacks.restore();
      controller.resetPageData.restore();
      controller.getAllHeatMaps.restore();
      controller.categorizeByServiceName.restore();
      controller.getActiveWidgetLayout.restore();
    });

    it("loadRacks() should be called", function() {
      expect(controller.loadRacks.calledOnce).to.be.true;
      expect(controller.resetPageData.calledOnce).to.be.true;
    });

    it("getAllHeatMaps() should be called", function() {
      expect(controller.getAllHeatMaps.calledOnce).to.be.true;
      expect(controller.get('isLoaded')).to.be.true;
      expect(controller.get('allHeatmaps')[0]).to.equal('info')
    });

    it("categorizeByServiceName() should be called", function() {
      expect(controller.categorizeByServiceName.calledOnce).to.be.true;
      expect(controller.get('heatmapCategories')).to.equal('categories');
    });

    it("getActiveWidgetLayout() should be called", function() {
      expect(controller.getActiveWidgetLayout.calledOnce).to.be.true;
    });
  });

  describe("#categorizeByServiceName()", function() {

    beforeEach(function() {
      sinon.stub(App.format, 'role').returns('S1');
    });

    afterEach(function() {
      App.format.role.restore();
    });

    it("single category", function() {
      var allHeatmaps = [
        {
          metrics: JSON.stringify([{service_name: 'S1'}])
        }
      ];
      var categories = controller.categorizeByServiceName(allHeatmaps);
      expect(categories[0].get('serviceName')).to.equal('S1');
      expect(categories[0].get('displayName')).to.equal('S1');
      expect(categories[0].get('heatmaps')).to.eql(allHeatmaps);
    });

    describe("two categories", function() {
      var allHeatmaps;
      beforeEach(function () {
        allHeatmaps = [
          {
            metrics: JSON.stringify([{service_name: 'S1'}])
          },
          {
            metrics: JSON.stringify([{service_name: 'S1'}])
          }
        ];
        this.categories = controller.categorizeByServiceName(allHeatmaps);
      });

      it('serviceName is S1', function () {
        expect(this.categories[0].get('serviceName')).to.equal('S1');
      });
      it('displayName is S1', function () {
        expect(this.categories[0].get('displayName')).to.equal('S1');
      });
      it('heatmaps.0 is valid', function () {
        expect(this.categories[0].get('heatmaps')[0]).to.eql(allHeatmaps[0]);
      });
      it('heatmaps.1 is valid', function () {
        expect(this.categories[0].get('heatmaps')[1]).to.eql(allHeatmaps[1]);
      });
    });
  });

  describe("#resetPageData()", function() {

    it("should clean heatmapCategories and allHeatmaps", function() {
      controller.set('heatmapCategories', [{}]);
      controller.set('allHeatmaps', [{}]);
      controller.resetPageData();
      expect(controller.get('heatmapCategories')).to.be.empty;
      expect(controller.get('allHeatmaps')).to.be.empty;
    });
  });

  describe("#getAllHeatMaps()", function() {

    it("should call App.ajax.send", function() {
      controller.reopen({
        loadHeatmapsUrlParams: 'url',
        sectionName: 's1'
      });
      controller.getAllHeatMaps();
      var args = testHelpers.findAjaxRequest('name', 'widgets.get');
      expect(args[0]).to.exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        urlParams: 'url',
        sectionName: 's1'
      });
    });
  });

  describe("#loadRacks()", function() {

    it("should call App.ajax.send", function() {
      controller.reopen({
        loadRacksUrlParams: 'url'
      });
      controller.loadRacks();
      var args = testHelpers.findAjaxRequest('name', 'hosts.heatmaps');
      expect(args[0]).to.exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        urlParams: 'url'
      });
    });
  });

  describe("#loadRacksSuccessCallback()", function() {

    var data = {
      items: [
        {
          Hosts: {
            host_name: 'host1',
            public_host_name: 'host1',
            os_type: 'os1',
            ip: 'ip1',
            rack_info: 'info'
          },
          host_components: [
            {
              HostRoles: {
                component_name: 'c1'
              }
            }
          ]
        }
      ]
    };

    beforeEach(function() {
      sinon.stub(controller, 'indexByRackId').returns({rack: {}});
      sinon.stub(controller, 'toList').returns(['rack']);
      controller.loadRacksSuccessCallback(data);
    });

    afterEach(function(){
      controller.indexByRackId.restore();
      controller.toList.restore();
    });

    it("indexByRackId should be called", function() {
      expect(controller.indexByRackId.calledWith([{
        hostName: 'host1',
        publicHostName: 'host1',
        osType: 'os1',
        ip: 'ip1',
        rack: 'info',
        diskTotal: 0,
        diskFree: 0,
        cpuSystem: 0,
        cpuUser: 0,
        memTotal: 0,
        memFree: 0,
        hostComponents: ['c1']
      }])).to.be.true;
    });

    it("toList should be called", function() {
      expect(controller.toList.calledWith({rack: {}})).to.be.true;
      expect(controller.get('rackMap')).to.eql({rack: {}});
      expect(controller.get('racks')).to.eql(['rack']);
    });
  });

  describe("#indexByRackId()", function() {

    it("should return rack map", function() {
      var hosts = [
        {rack: 'r1'},
        {rack: 'r1'}
      ];
      var rackMap = controller.indexByRackId(hosts);
      expect(rackMap.r1.name).to.equal('r1');
      expect(rackMap.r1.rackId).to.equal('r1');
      expect(rackMap.r1.hosts).to.eql([{rack: 'r1'}, {rack: 'r1'}]);
    });
  });

  describe("#toList()", function() {
    var rackMap = {'r1': {
      name: 'r1',
      rackId: 'r1',
      hosts: [{rack: 'r1'}, {rack: 'r1'}]
    }};

    it('toList result is valid', function() {
      expect(controller.toList(rackMap)).to.eql([Em.Object.create(rackMap.r1, {
        isLoaded: false,
        index: 0
      })]);
    });
  });

  describe("#addRackView()", function() {

    beforeEach(function() {
      sinon.stub(controller, 'displayAllRacks');
    });

    afterEach(function() {
      controller.displayAllRacks.restore();
    });

    it("displayAllRacks should be called", function() {
      controller.set('racks', [{}]);
      controller.set('rackViews', []);
      controller.addRackView({});
      expect(controller.displayAllRacks.calledOnce).to.be.true;
    });
  });

  describe("#displayAllRacks", function() {
    var rackView = {
      displayHosts: Em.K
    };

    beforeEach(function() {
      sinon.spy(controller, 'displayAllRacks');
      sinon.spy(rackView, 'displayHosts');
    });

    afterEach(function() {
      controller.displayAllRacks.restore();
      rackView.displayHosts.restore();
    });

    it("displayAllRacks should be called again", function() {
      controller.set('rackViews', [rackView]);
      controller.displayAllRacks();
      expect(controller.displayAllRacks.calledTwice).to.be.true;
      expect(rackView.displayHosts.calledOnce).to.be.true;
    });

    it("displayAllRacks should not be called again", function() {
      controller.set('rackViews', []);
      controller.displayAllRacks();
      expect(controller.displayAllRacks.calledOnce).to.be.true;
    });
  });


});

