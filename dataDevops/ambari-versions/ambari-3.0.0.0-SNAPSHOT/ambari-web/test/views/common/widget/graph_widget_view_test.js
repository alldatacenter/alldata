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
require('views/common/widget/graph_widget_view');
var fileUtils = require('utils/file_utils');

describe('App.GraphWidgetView', function () {

  var view;

  beforeEach(function () {
    view = App.GraphWidgetView.create({
      content: Em.Object.create({
        properties: {}
      }),
      parentView: Em.Object.create()
    });
  });

  afterEach(function () {
    clearTimeout(view.get('timeoutId'));
    view.destroy();
  });

  describe("#adjustData()", function() {
    var testCases = [
      {
        title: 'empty data',
        data: {
          dataLinks: {},
          dataLength: 0
        },
        result: {}
      },
      {
        title: 'correct data',
        data: {
          dataLinks: {
            s1: [[0, 0]]
          },
          dataLength: 1
        },
        result:  {
          s1: [[0, 0]]
        }
      },
      {
        title: 'second series empty',
        data: {
          dataLinks: {
            s1: [[1, 0]],
            s2: []
          },
          dataLength: 1
        },
        result:  {
          s1: [[1, 0]],
          s2: [[null, 0]]
        }
      },
      {
        title: 'second series missing data at the end',
        data: {
          dataLinks: {
            s1: [[1, 0], [2, 1], [3, 2]],
            s2: [[1, 0]]
          },
          dataLength: 3
        },
        result:  {
          s1: [[1, 0], [2, 1], [3, 2]],
          s2: [[1, 0], [null, 1], [null, 2]]
        }
      },
      {
        title: 'second series missing data at the beginning',
        data: {
          dataLinks: {
            s1: [[1, 0], [2, 1], [3, 2]],
            s2: [[3, 2]]
          },
          dataLength: 3
        },
        result:  {
          s1: [[1, 0], [2, 1], [3, 2]],
          s2: [[null, 0], [null, 1], [3, 2]]
        }
      },
      {
        title: 'second series missing data in the middle',
        data: {
          dataLinks: {
            s1: [[1, 0], [2, 1], [3, 2]],
            s2: [[1, 1]]
          },
          dataLength: 3
        },
        result:  {
          s1: [[1, 0], [2, 1], [3, 2]],
          s2: [[null, 0], [1, 1], [null, 2]]
        }
      },
      {
        title: 'second and third series missing data',
        data: {
          dataLinks: {
            s1: [[1, 0], [2, 1], [3, 2]],
            s2: [[1, 1]],
            s3: [[1, 2]]
          },
          dataLength: 3
        },
        result:  {
          s1: [[1, 0], [2, 1], [3, 2]],
          s2: [[null, 0], [1, 1], [null, 2]],
          s3: [[null, 0], [null, 1], [1, 2]]
        }
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        view.adjustData(test.data.dataLinks, test.data.dataLength);
        expect(test.data.dataLinks).to.eql(test.result);
      });
    });
  });

  describe('#exportGraphData', function () {

    var cases = [
      {
        data: null,
        downloadTextFileCallCount: 0,
        showAlertPopupCallCount: 1,
        title: 'no data'
      },
      {
        data: {},
        downloadTextFileCallCount: 0,
        showAlertPopupCallCount: 1,
        title: 'invalid data'
      },
      {
        data: [
          {
            data: null
          }
        ],
        downloadTextFileCallCount: 0,
        showAlertPopupCallCount: 1,
        title: 'empty data'
      },
      {
        data: [
          {
            data: {}
          }
        ],
        downloadTextFileCallCount: 0,
        showAlertPopupCallCount: 1,
        title: 'malformed data'
      },
      {
        data: [
          {
            name: 'name',
            data: [0,1]
          }
        ],
        downloadTextFileCallCount: 1,
        showAlertPopupCallCount: 0,
        fileData: '[{"name":"name","data":[0,1]}]',
        title: 'JSON export'
      },
      {
        data: [
          {
            data: [
              {
                key: 'value'
              }
            ]
          }
        ],
        event: {
          context: true
        },
        downloadTextFileCallCount: 1,
        showAlertPopupCallCount: 0,
        fileData: 'key,value',
        title: 'CSV export'
      }
    ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(view, 'prepareCSV').returns('key,value');
          sinon.stub(fileUtils, 'downloadTextFile', Em.K);
          sinon.stub(App, 'showAlertPopup', Em.K);
          view.set('data', item.data);
          view.exportGraphData(item.event || {});
        });

        afterEach(function () {
          view.prepareCSV.restore();
          fileUtils.downloadTextFile.restore();
          App.showAlertPopup.restore();
        });

        it('isExportMenuHidden is true', function () {
          expect(view.get('isExportMenuHidden')).to.be.true;
        });

        it('downloadTextFile calls count is calid', function () {
          expect(fileUtils.downloadTextFile.callCount).to.equal(item.downloadTextFileCallCount);
        });

        it('showAlertPopup calls count is valid', function () {
          expect(App.showAlertPopup.callCount).to.equal(item.showAlertPopupCallCount);
        });

        if (item.downloadTextFileCallCount) {
          it('download args are valid', function () {
            var fileType = item.event && item.event.context ? 'csv' : 'json',
              downloadArgs = fileUtils.downloadTextFile.firstCall.args;
            expect(downloadArgs[0].replace(/\s/g, '')).to.equal(item.fileData);
            expect(downloadArgs[1]).to.equal(fileType);
            expect(downloadArgs[2]).to.equal('data.' + fileType);
          });
        }

      });

    });

  });

  describe('#exportTargetView', function () {

    var childViews = [
        {
          p0: 'v0'
        },
        {
          p1: 'v1'
        }
      ],
      title = 'should take last child view';

    beforeEach(function () {
      view.get('childViews').pushObjects(childViews);
      view.propertyDidChange('exportTargetView');
    });

    it(title, function () {
      expect(view.get('exportTargetView')).to.eql(childViews[1]);
    });
  });

  describe("#timeRange()", function () {
    var testCases = [
      {
        time_range: null,
        currentTimeIndex: 1,
        customTimeRange: null,
        expected: 3600
      },
      {
        time_range: null,
        currentTimeIndex: 1,
        customTimeRange: 2,
        expected: 2
      },
      {
        time_range: null,
        currentTimeIndex: 8,
        customTimeRange: 2,
        expected: 0
      },
      {
        time_range: 2,
        currentTimeIndex: 1,
        customTimeRange: null,
        expected: 7200
      }
    ];

    testCases.forEach(function(test) {
      it("time_range=" + test.time_range +
         " currentTimeIndex=" + test.currentTimeIndex +
         " customTimeRange=" + test.customTimeRange, function() {
        view.set('content.properties.time_range', test.time_range);
        view.set('customTimeRange', test.customTimeRange);
        view.reopen({
          exportTargetView: Em.Object.create({
            currentTimeIndex: test.currentTimeIndex
          })
        });
        view.propertyDidChange('timeRange');
        expect(view.get('timeRange')).to.be.equal(test.expected);
      });
    });
  });

  describe("#drawWidget()", function () {

    beforeEach(function() {
      sinon.stub(view, 'calculateValues').returns({});
      view.set('data', null);
    });

    afterEach(function() {
      view.calculateValues.restore();
    });

    it("isLoaded = false", function() {
      view.set('isLoaded', false);
      view.drawWidget();
      expect(view.get('data')).to.be.null;
    });

    it("isLoaded = true", function() {
      view.set('isLoaded', true);
      view.drawWidget();
      expect(view.get('data')).to.be.eql({});
    });
  });

  describe("#calculateValues()", function () {
    beforeEach(function() {
      this.mockExtract = sinon.stub(view, 'extractExpressions');
      this.mockCompute = sinon.stub(view, 'computeExpression');
    });

    afterEach(function() {
      this.mockExtract.restore();
      this.mockCompute.restore();
    });

    var testCases = [
      {
        metrics: {},
        values: [],
        expression: [],
        computed: {},
        expected: []
      },
      {
        metrics: {},
        values: [{}],
        expression: [],
        computed: {},
        expected: []
      },
      {
        metrics: {},
        values: [{
          value: '${m1}'
        }],
        expression: ['${m1}'],
        computed: {
          '${m1}': []
        },
        expected: []
      },
      {
        metrics: {},
        values: [{
          value: '${m1}',
          name: 'v1'
        }],
        expression: ['${m1}'],
        computed: {
          '${m1}': [{
            m1: {}
          }]
        },
        expected: [
          {
            name: 'v1',
            data: [{
              m1: {}
            }]
          }
        ]
      }
    ];

    testCases.forEach(function(test) {
      it("metrics=" + JSON.stringify(test.metrics) +
         " values=" + JSON.stringify(test.values) +
         " expression=" + test.expression +
         " computed=" + test.computed, function() {
        view.set('metrics', test.metrics);
        view.set('content.values', test.values);
        this.mockCompute.returns(test.computed);
        this.mockExtract.returns(test.expression);
        expect(view.calculateValues()).to.be.eql(test.expected);
      });
    });
  });

  describe("#computeExpression()", function () {

    beforeEach(function() {
      sinon.stub(view, 'adjustData', function (dataLinks) {
        dataLinks.m1[1] = [3, 1112];
      });
    });

    afterEach(function() {
      view.adjustData.restore();
    });

    var testCases = [
      {
        expression: '1',
        metrics: [],
        expected: {
          '${1}': []
        },
        adjustDataCalled: false
      },
      {
        expression: 'm1',
        metrics: [],
        expected: {
          '${m1}': []
        },
        adjustDataCalled: false
      },
      {
        expression: 'm1',
        metrics: [{
          name: 'm1',
          data: []
        }],
        expected: {
          '${m1}': []
        },
        adjustDataCalled: false
      },
      {
        expression: 'm1',
        metrics: [{
          name: 'm1',
          data: [
            [null, 1111]
          ]
        }],
        expected: {
          '${m1}': [
            [null, 1111]
          ]
        },
        adjustDataCalled: false
      },
      {
        expression: 'm1',
        metrics: [{
          name: 'm1',
          data: [
            [1, 1111]
          ]
        }],
        expected: {
          '${m1}': [
            [1, 1111]
          ]
        },
        adjustDataCalled: false
      },
      {
        expression: 'm1+1',
        metrics: [{
          name: 'm1',
          data: [
            [1, 1111]
          ]
        }],
        expected: {
          '${m1+1}': [
            [2, 1111]
          ]
        },
        adjustDataCalled: false
      },
      {
        expression: 'm1/m2',
        metrics: [
          {
            name: 'm1',
            data: [
              [0, 1111]
            ]
          },
          {
            name: 'm2',
            data: [
              [0, 1111]
            ]
          }
        ],
        expected: {
          '${m1/m2}': [
            [0, 1111]
          ]
        },
        adjustDataCalled: false
      },
      {
        expression: 'm1+m2',
        metrics: [
          {
            name: 'm1',
            data: [
              [1, 1111]
            ]
          },
          {
            name: 'm2',
            data: [
              [1, 1111],
              [2, 1112]
            ]
          }],
        expected: {
          '${m1+m2}': [
            [2, 1111],
            [5, 1112]
          ]
        },
        adjustDataCalled: true
      }
    ];

    testCases.forEach(function(test) {
      it("expression=" + test.expression +
         " metrics=" + JSON.stringify(test.metrics), function() {
        expect(view.computeExpression(test.expression, test.metrics)).to.be.eql(test.expected);
        expect(view.adjustData.calledOnce).to.be.equal(test.adjustDataCalled);
      });
    });
  });

  describe("#addTimeProperties()", function () {

    beforeEach(function() {
      sinon.stub(App, 'dateTime').returns(10000);
      view.set('timeStep', 15);
    });

    afterEach(function() {
      App.dateTime.restore();
    });

    it("targetView is null", function() {
      view.reopen({
        exportTargetView: null
      });
      view.set('parentView', null);
      expect(view.addTimeProperties([{}])).to.be.empty;
    });

    it("empty metricPaths", function() {
      expect(view.addTimeProperties([])).to.be.empty;
    });

    it("timeRange=5", function() {
      view.reopen({
        timeRange: 5,
        exportTargetView: Em.Object.create({
          isPopup: true
        })
      });
      expect(view.addTimeProperties(['m1'])).to.be.eql([
        "m1[5,10,15]"
      ]);
    });

    it("timeRange=0, customStartTime=null", function() {
      view.reopen({
        timeRange: 0,
        exportTargetView: Em.Object.create({
          isPopup: true,
          customStartTime: null
        })
      });
      expect(view.addTimeProperties(['m1'])).to.be.eql([
        "m1[10,10,15]"
      ]);
    });

    it("timeRange=0, customStartTime=1000, customEndTime=null", function() {
      view.reopen({
        timeRange: 0,
        exportTargetView: Em.Object.create({
          isPopup: true,
          customStartTime: 1000,
          customEndTime: null
        })
      });
      expect(view.addTimeProperties(['m1'])).to.be.eql([
        "m1[10,10,15]"
      ]);
    });

    it("timeRange=0, customStartTime=1000, customEndTime=10000", function() {
      view.reopen({
        timeRange: 0,
        exportTargetView: Em.Object.create({
          isPopup: true,
          customStartTime: 1000,
          customEndTime: 10000
        })
      });
      expect(view.addTimeProperties(['m1'])).to.be.eql([
        "m1[1,10,15]"
      ]);
    });
  });

  describe("#graphView", function () {
    var graphView;

    beforeEach(function () {
      graphView = view.get('graphView').create({
        parentView: view,
        _refreshGraph: Em.K,
        $: function() {
          return {
            closest: function() {
              return {on: Em.K}
            }
          }
        }
      });
    });

    describe("#setYAxisFormatter()", function () {

      beforeEach(function () {
        sinon.stub(App.ChartLinearTimeView, 'DisplayUnitFormatter');
        graphView.set('yAxisFormatter', null);
      });

      afterEach(function () {
        App.ChartLinearTimeView.DisplayUnitFormatter.restore();
      });

      it("yAxisFormatter should not be set", function () {
        graphView.reopen({
          displayUnit: null
        });
        graphView.setYAxisFormatter();
        expect(graphView.get('yAxisFormatter')).to.be.null;
      });

      it("yAxisFormatter should be set", function () {
        graphView.reopen({
          displayUnit: 'u1'
        });
        graphView.setYAxisFormatter();
        expect(graphView.get('yAxisFormatter')).to.be.function;
      });
    });

    describe("#setTimeRange", function () {

      beforeEach(function() {
        sinon.stub(graphView.get('parentView'), 'propertyDidChange');
      });

      afterEach(function() {
        graphView.get('parentView').propertyDidChange.restore();
      });

      it("isPopup=false", function() {
        graphView.set('isPopup', false);
        graphView.setTimeRange();
        expect(graphView.get('parentView.customTimeRange')).to.be.null;
      });

      it("isPopup=true, currentTimeIndex=8", function() {
        graphView.set('isPopup', true);
        graphView.set('currentTimeIndex', 8);
        graphView.setTimeRange();
        expect(graphView.get('parentView').propertyDidChange.calledWith('customTimeRange')).to.be.true;
      });

      it("isPopup=true, currentTimeIndex=1", function() {
        graphView.set('isPopup', true);
        graphView.set('currentTimeIndex', 1);
        graphView.set('timeUnitSeconds', 10);
        expect(graphView.get('parentView.customTimeRange')).to.be.equal(10);
      });
    });

    describe("#id", function () {

      it("should return id", function() {
        graphView.set('parentView.content.id', 'g1');
        graphView.propertyDidChange('id');
        expect(graphView.get('id')).to.be.equal('widget_g1_graph');
      });
    });

    describe("#renderer", function () {

      it("should return area", function() {
        graphView.set('parentView.content.properties.graph_type', 'STACK');
        graphView.propertyDidChange('renderer');
        expect(graphView.get('renderer')).to.be.equal('area');
      });

      it("should return line", function() {
        graphView.set('parentView.content.properties.graph_type', '');
        graphView.propertyDidChange('renderer');
        expect(graphView.get('renderer')).to.be.equal('line');
      });
    });

    describe("#transformToSeries()", function () {

      beforeEach(function() {
        sinon.stub(graphView, 'transformData').returns({});
      });

      afterEach(function() {
        graphView.transformData.restore();
      });

      it("empty data", function() {
        expect(graphView.transformToSeries([])).to.be.empty;
      });

      it("should return series", function() {
        expect(graphView.transformToSeries([{}])).to.be.eql([{}]);
      });
    });

    describe("#loadData()", function () {

      beforeEach(function() {
        sinon.stub(Em.run, 'next', Em.clb);
        sinon.stub(graphView, '_refreshGraph');
      });

      afterEach(function() {
        Em.run.next.restore();
        graphView._refreshGraph.restore();
      });

      it("_refreshGraph should be called", function() {
        graphView.loadData();
        expect(graphView._refreshGraph.calledOnce).to.be.true;
      });
    });

    // todo: fix test

    //describe("#didInsertElement()", function () {
    //
    //  beforeEach(function() {
    //    sinon.stub(graphView, 'loadData');
    //    sinon.stub(Em.run, 'next', Em.clb);
    //    sinon.stub(App, 'tooltip');
    //  });
    //
    //  afterEach(function() {
    //    graphView.loadData.restore();
    //    Em.run.next.restore();
    //    App.tooltip.restore();
    //  });
    //
    //  it("loadData should be called", function() {
    //    graphView.didInsertElement();
    //    expect(graphView.loadData.calledOnce).to.be.true;
    //  });
    //
    //  it("App.tooltip should be called, isPreview=false", function() {
    //    graphView.didInsertElement();
    //    expect(App.tooltip.getCall(0).args[1]).to.be.eql({
    //      placement: 'left',
    //      template: '<div class="tooltip"><div class="tooltip-arrow"></div><div class="tooltip-inner graph-tooltip"></div></div>'
    //    });
    //  });
    //
    //  it("App.tooltip should be called, isPreview=true", function() {
    //    graphView.reopen({
    //      isPreview: true
    //    });
    //    graphView.didInsertElement();
    //    expect(App.tooltip.getCall(0).args[1]).to.be.equal('disable');
    //  });
    //});
  });
});