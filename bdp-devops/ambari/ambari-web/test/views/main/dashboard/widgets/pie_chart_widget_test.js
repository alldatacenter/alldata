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

require('views/common/chart/pie');
require('utils/helper');
require('views/main/dashboard/widget');
require('views/main/dashboard/widgets/pie_chart_widget');

describe('App.PieChartDashboardWidgetView', function() {

  var model = Em.Object.create({
    used: null,
    max: null
  });
  var pieChartDashboardWidgetView = App.PieChartDashboardWidgetView.create({
    model_type: null,
    model: model,
    modelValueUsed: Em.computed.alias('model.used'),
    modelValueMax: Em.computed.alias('model.max'),
    widgetHtmlId: 'fake'
  });

  pieChartDashboardWidgetView.calc();

  describe('#getUsed', function() {
    var tests = [
      {
        model: Em.Object.create({
          used: 1
        }),
        e: 1,
        m: '"Used" is set'
      },
      {
        model: Em.Object.create({
          used: null
        }),
        e: 0,
        m: '"Used" is not set'
      },
      {
        model: Em.Object.create({}),
        e: 0,
        m: '"Used" is not defined'
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        pieChartDashboardWidgetView.set('model', test.model);
        expect(pieChartDashboardWidgetView.getUsed()).to.equal(test.e);
      });
    });
  });

  describe('#getMax', function() {
    var tests = [
      {
        model: Em.Object.create({
          max: 1
        }),
        e: 1,
        m: '"Max" is set'
      },
      {
        model: Em.Object.create({
          max: null
        }),
        e: 0,
        m: '"Max" is not set'
      },
      {
        model: Em.Object.create({}),
        e: 0,
        m: '"Max" is not defined'
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        pieChartDashboardWidgetView.set('model', test.model);
        expect(pieChartDashboardWidgetView.getMax()).to.equal(test.e);
      });
    });
  });

  describe('#calcIsPieExists', function() {
    var tests = [
      {
        model: Em.Object.create({
          max: 1
        }),
        e: true,
        m: 'Exists'
      },
      {
        model: Em.Object.create({
          max: 0
        }),
        e: false,
        m: 'Not exists'
      },
      {
        model: Em.Object.create({}),
        e: false,
        m: 'Not exists'
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        pieChartDashboardWidgetView.set('model', test.model);
        expect(pieChartDashboardWidgetView.calcIsPieExists()).to.equal(test.e);
      });
    });
  });

  describe('calcDataForPieChart', function() {
    var tests = [
      {
        model: Em.Object.create({
          max: 10,
          used: 0
        }),
        e: ['0', '0.0'],
        m: 'Nothing is used'
      },
      {
        model: Em.Object.create({
          max: 10,
          used: 10
        }),
        e: ['100', '100.0'],
        m: 'All is used'
      },
      {
        model: Em.Object.create({
          max: 10,
          used: 5
        }),
        e: ['50', '50.0'],
        m: 'Half is used'
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        pieChartDashboardWidgetView.set('model', test.model);
        expect(pieChartDashboardWidgetView.calcDataForPieChart()).to.eql(test.e);
      });
    });
  });

});
