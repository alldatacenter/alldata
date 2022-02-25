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

require('utils/helper');
require('views/common/chart/pie');
require('views/main/dashboard/widget');
require('views/main/dashboard/widgets/pie_chart_widget');
require('views/main/dashboard/widgets/namenode_cpu');

describe('App.NameNodeCpuPieChartView', function() {

  var model;
  var nameNodeCpuPieChartView;

  beforeEach(function () {
    model = Em.Object.create({
      used: null,
      max: null
    });
    nameNodeCpuPieChartView = App.NameNodeCpuPieChartView.create({
      model_type: null,
      model: model,
      modelFieldUsed: 'used',
      modelFieldMax: 'max',
      widgetHtmlId: 'fake'
    });
    nameNodeCpuPieChartView.calc();
  });

  afterEach(function () {
    nameNodeCpuPieChartView.destroy();
    clearTimeout(nameNodeCpuPieChartView.get('intervalId'));
  });

  describe('#calcIsPieExists', function() {
    var tests = [
      {
        cpuWio: 1,
        e: true,
        m: 'Exists'
      },
      {
        cpuWio: null,
        e: false,
        m: 'Not exists'
      },
      {
        cpuWio: undefined,
        e: false,
        m: 'Not exists'
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        nameNodeCpuPieChartView.set('cpuWio', test.cpuWio);
        expect(nameNodeCpuPieChartView.calcIsPieExists()).to.equal(test.e);
      });
    });
  });

  describe('calcDataForPieChart', function () {
    var tests = [
      {
        cpuWio: 0,
        e: ['0.0', '0.00'],
        m: 'Nothing is used'
      },
      {
        cpuWio: 100,
        e: ['100.0', '100.00'],
        m: 'All is used'
      },
      {
        cpuWio: 50,
        e: ['50.0', '50.00'],
        m: 'Half is used'
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        nameNodeCpuPieChartView.set('cpuWio', test.cpuWio);
        expect(nameNodeCpuPieChartView.calcDataForPieChart()).to.eql(test.e);
      });
    });
  });

});
