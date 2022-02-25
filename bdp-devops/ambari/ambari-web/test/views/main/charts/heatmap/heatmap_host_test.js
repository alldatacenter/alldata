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
var date = require('utils/date/date');
require('models/host');
require('views/main/charts/heatmap/heatmap_host');

describe('App.MainChartsHeatmapHostView', function () {

  var view = App.MainChartsHeatmapHostView.create({
    templateName: '',
    controller: Em.Object.create(),
    content: {}
  });

  describe('#hostTemperatureStyle', function () {
    var testCases = [
      {
        title: 'if hostToSlotMap is null then hostTemperatureStyle should be empty',
        hostName: 'host',
        controller: Em.Object.create({
          hostToSlotMap: null,
          selectedMetric: {
            slotDefinitions: []
          }
        }),
        result: ''
      },
      {
        title: 'if hostName is null then hostTemperatureStyle should be empty',
        hostName: '',
        controller: Em.Object.create({
          hostToSlotMap: {},
          selectedMetric: {
            slotDefinitions: []
          }
        }),
        result: ''
      },
      {
        title: 'if slot less than 0 then hostTemperatureStyle should be empty',
        hostName: 'host1',
        controller: Em.Object.create({
          hostToSlotMap: {
            "host1": -1
          },
          selectedMetric: {
            slotDefinitions: []
          }
        }),
        result: ''
      },
      {
        title: 'if slotDefinitions is null then hostTemperatureStyle should be empty',
        hostName: 'host1',
        controller: Em.Object.create({
          hostToSlotMap: {
            "host1": 1
          },
          selectedMetric: {
            slotDefinitions: null
          }
        }),
        result: ''
      },
      {
        title: 'if slotDefinitions length not more than slot number then hostTemperatureStyle should be empty',
        hostName: 'host1',
        controller: Em.Object.create({
          hostToSlotMap: {
            "host1": 1
          },
          selectedMetric: {
            slotDefinitions: [{}]
          }
        }),
        result: ''
      },
      {
        title: 'if slotDefinitions correct then hostTemperatureStyle should be "style1"',
        hostName: 'host1',
        controller: Em.Object.create({
          hostToSlotMap: {
            "host1": 1
          },
          selectedMetric: {
            slotDefinitions: [
              Em.Object.create({cssStyle: 'style0'}),
              Em.Object.create({cssStyle: 'style1'})
            ]
          }
        }),
        result: 'style1'
      }
    ];
    testCases.forEach(function (test) {
      it(test.title, function () {
        view.set('content.hostName', test.hostName);
        view.set('controller', test.controller);
        expect(view.get('hostTemperatureStyle')).to.equal(test.result);
      });
    });
  });

  describe("#hostModelLink", function () {

    before(function () {
      sinon.stub(App.Host, 'find').returns(Em.Object.create({id: 'host1'}));
    });

    after(function () {
      App.Host.find.restore();
    });

    it("should return hostname", function () {
      view.set('content.hostName', 'host1');
      view.propertyDidChange('hostModelLink');
      expect(view.get('hostModelLink.id')).to.equal('host1');
    });
  });

  describe("#mouseEnter()", function () {

    beforeEach(function () {
      sinon.stub(view, 'getUsage').returns('usage');
      sinon.stub(view, 'getCpuUsage').returns('cpu_usage');
      sinon.stub(view, 'getHostComponents').returns(['c1']);
      sinon.stub(view, 'setMetric');
      sinon.stub(view, 'openDetailsBlock');
      this.mock = sinon.stub(App.MainChartsHeatmapHostDetailView, 'create');
      view.set('details', {});
    });

    afterEach(function () {
      view.getUsage.restore();
      view.getCpuUsage.restore();
      view.getHostComponents.restore();
      view.setMetric.restore();
      view.openDetailsBlock.restore();
      this.mock.restore();
    });

    describe('set diskUsage', function () {
      var childView;
      beforeEach(function () {
        childView = Em.Object.create({
          details: {
            diskUsage: ''
          }
        });
        this.mock.returns(childView);
        view.set('content', {
          diskTotal: 100,
          diskFree: 50
        });
        view.mouseEnter();
      });

      it("details.diskUsage = usage", function () {
        expect(childView.get('details.diskUsage')).to.equal('usage');
      });

      it("getUsage is called with valid arguments", function () {
        expect(view.getUsage.calledWith(100, 50)).to.be.true;
      });

      it("setMetric is called once", function () {
        expect(view.setMetric.calledOnce).to.be.true;
      });

      it("openDetailsBlock is called once", function () {
        expect(view.openDetailsBlock.calledOnce).to.be.true;
      });

    });

    describe('set cpuUsage', function () {
      var childView;
      beforeEach(function () {
        childView = Em.Object.create({
          details: {
            cpuUsage: ''
          }
        });
        this.mock.returns(childView);
        view.set('content', {
          cpuSystem: 100,
          cpuUser: 50
        });
        view.mouseEnter();
      });

      it("details.cpuUsage = cpu_usage", function () {
        expect(childView.get('details.cpuUsage')).to.equal('cpu_usage');
      });

      it("getCpuUsage is called with valid arguments", function () {
        expect(view.getCpuUsage.calledWith(100, 50)).to.be.true;
      });

      it("setMetric is called once", function () {
        expect(view.setMetric.calledOnce).to.be.true;
      });

      it("openDetailsBlock is called once", function () {
        expect(view.openDetailsBlock.calledOnce).to.be.true;
      });

    });

    describe('set memoryUsage', function () {
      var childView;
      beforeEach(function () {
        childView = Em.Object.create({
          details: {
            memoryUsage: ''
          }
        });
        this.mock.returns(childView);
        view.set('content', {
          memTotal: 100,
          memFree: 50
        });
        view.mouseEnter();
      });

      it("details.memoryUsage = usage", function () {
        expect(childView.get('details.memoryUsage')).to.equal('usage');
      });

      it("getUsage is called with valid arguments", function () {
        expect(view.getUsage.calledWith(100, 50)).to.be.true;
      });

      it("setMetric is called once", function () {
        expect(view.setMetric.calledOnce).to.be.true;
      });

      it("openDetailsBlock is called once", function () {
        expect(view.openDetailsBlock.calledOnce).to.be.true;
      });

    });

    describe('set hostComponents', function () {
      var childView;
      beforeEach(function () {
        childView = Em.Object.create({
          details: {
            hostComponents: ''
          }
        });
        this.mock.returns(childView);
        view.set('content', {
          hostComponents: ['host1']
        });
        view.mouseEnter();
      });

      it("hostComponents = ['c1']", function () {
        expect(childView.get('details.hostComponents')).to.eql(['c1']);
      });

      it("getHostComponents is called with valid arguments", function () {
        expect(view.getHostComponents.calledWith(['host1'])).to.be.true;
      });

      it("setMetric is called once", function () {
        expect(view.setMetric.calledOnce).to.be.true;
      });

      it("openDetailsBlock is called once", function () {
        expect(view.openDetailsBlock.calledOnce).to.be.true;
      });

    });

    describe('set hostName', function () {
      var childView;
      beforeEach(function () {
        childView = Em.Object.create({
          details: {
            hostName: ''
          }
        });
        this.mock.returns(childView);
        view.set('content', {
          hostName: 'host1'
        });
        view.mouseEnter();
      });

      it("hostName = host1", function () {
        expect(childView.get('details.hostName')).to.equal('host1');
      });

      it("setMetric is called once", function () {
        expect(view.setMetric.calledOnce).to.be.true;
      });

      it("openDetailsBlock is called once", function () {
        expect(view.openDetailsBlock.calledOnce).to.be.true;
      });

    });
  });

  describe("#getUsage()", function () {
    var testCases = [
      {
        input: {
          total: null,
          free: null
        },
        expected: '0.0'
      },
      {
        input: {
          total: 100,
          free: null
        },
        expected: '0.0'
      },
      {
        input: {
          total: null,
          free: 50
        },
        expected: '0.0'
      },
      {
        input: {
          total: 0,
          free: 0
        },
        expected: '0.0'
      },
      {
        input: {
          total: 100,
          free: 50
        },
        expected: '50.0'
      }
    ];

    testCases.forEach(function (test) {
      it("total = " + test.input.total + "; free = " + test.input.free, function () {
        expect(view.getUsage(test.input.total, test.input.free)).to.equal(test.expected);
      });
    });
  });

  describe("#getCpuUsage()", function () {
    var testCases = [
      {
        input: {
          cpuSystem: null,
          cpuUser: null
        },
        expected: '0.0'
      },
      {
        input: {
          cpuSystem: 1.0,
          cpuUser: null
        },
        expected: '0.0'
      },
      {
        input: {
          cpuSystem: null,
          cpuUser: 1.0
        },
        expected: '0.0'
      },
      {
        input: {
          cpuSystem: 2.22,
          cpuUser: 1.0
        },
        expected: '3.2'
      }
    ];

    testCases.forEach(function (test) {
      it("cpuSystem = " + test.input.cpuSystem + "; cpuUser = " + test.input.cpuUser, function () {
        expect(view.getCpuUsage(test.input.cpuSystem, test.input.cpuUser)).to.equal(test.expected);
      });
    });
  });

  describe("#getHostComponents()", function () {

    beforeEach(function () {
      sinon.stub(App.format, 'role', function (name) {
        return name;
      });
      sinon.stub(App, 'get').returns('non-client');
    });

    afterEach(function () {
      App.format.role.restore();
      App.get.restore();
    });

    it("should return host-components", function () {
      expect(view.getHostComponents(['is-client', 'non-client', 'non-client'])).to.equal('non-client, non-client');
    });
  });

  describe("#setMetric()", function () {
    var viewObject;

    beforeEach(function () {
      sinon.stub(view, 'convertValue').returns('converted');
      viewObject = Em.Object.create({
        details: {}
      });
    });

    afterEach(function () {
      view.convertValue.restore();
    });

    it("selected metric is null", function () {
      view.set('controller.selectedMetric', null);
      view.setMetric(viewObject, {});
      expect(viewObject.get('details')).to.be.empty;
    });

    it("metric name is null", function () {
      view.set('controller.selectedMetric', Em.Object.create({
        name: null,
        hostToValueMap: {}
      }));
      view.setMetric(viewObject, {});
      expect(viewObject.get('details')).to.be.empty;
    });

    it("host value is undefined", function () {
      view.set('controller.selectedMetric', Em.Object.create({
        name: 'm1',
        hostToValueMap: {},
        slotDefinitions: {
          7: Em.Object.create({
            label: 'na'
          })
        }
      }));
      view.set('controller.hostToSlotMap', {
        'host1': 7
      });
      view.setMetric(viewObject, {hostName: 'host1'});
      expect(viewObject.get('details')).to.eql({
        metricName: 'm1',
        metricValue: 'na'
      });
    });

    it("metric name is 'Garbage Collection Time'", function () {
      view.set('controller.selectedMetric', Em.Object.create({
        name: 'Garbage Collection Time',
        units: 'ms',
        hostToValueMap: {
          host1: '111'
        }
      }));
      view.setMetric(viewObject, {hostName: 'host1'});
      expect(viewObject.get('details')).to.eql({
        metricName: 'Garbage Collection Time',
        metricValue: 'converted'
      });
    });

    it("metric value is NaN", function () {
      view.set('controller.selectedMetric', Em.Object.create({
        name: 'm1',
        hostToValueMap: {
          host1: 'val'
        },
        slotDefinitions: {
          7: Em.Object.create({
            label: 'na'
          })
        }
      }));
      view.set('controller.hostToSlotMap', {
        'host1': 7
      });
      view.setMetric(viewObject, {hostName: 'host1'});
      expect(viewObject.get('details')).to.eql({
        metricName: 'm1',
        metricValue: 'na'
      });
    });

    it("metric value is number", function () {
      view.set('controller.selectedMetric', Em.Object.create({
        name: 'm1',
        hostToValueMap: {
          host1: 10
        },
        units: 'mb'
      }));
      view.setMetric(viewObject, {hostName: 'host1'});
      expect(viewObject.get('details')).to.eql({
        metricName: 'm1',
        metricValue: 'converted'
      });
    });
  });

  describe('#convertValue', function() {
    beforeEach(function() {
      sinon.stub(date, 'timingFormat').returns('time');
    });
    afterEach(function() {
      date.timingFormat.restore();
    });

    it('should return null', function() {
      expect(view.convertValue(null, null)).to.be.null;
    });

    it('should return 40', function() {
      expect(view.convertValue('40', '')).to.be.equal('40');
    });

    it('should return 40%', function() {
      expect(view.convertValue('40', '%')).to.be.equal('40%');
    });

    it('should return 1MB', function() {
      expect(view.convertValue('1048576', 'MB')).to.be.equal('1.00MB');
    });

    it('should return time', function() {
      expect(view.convertValue('1', 'ms')).to.be.equal('time');
    });
  });
});
