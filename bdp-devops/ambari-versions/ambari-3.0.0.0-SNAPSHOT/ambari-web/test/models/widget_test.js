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
require('models/widget');

describe('App.Widget', function () {

  var widget;

  beforeEach(function () {
    widget = App.Widget.createRecord();
  });

  describe('#viewClass', function () {

    var cases = [
      {
        widgetType: 'GRAPH',
        viewClass: App.GraphWidgetView
      },
      {
        widgetType: 'TEMPLATE',
        viewClass: App.TemplateWidgetView
      },
      {
        widgetType: 'NUMBER',
        viewClass: App.NumberWidgetView
      },
      {
        widgetType: 'GAUGE',
        viewClass: App.GaugeWidgetView
      },
      {
        widgetType: 'HEATMAP',
        viewClass: App.HeatmapWidgetView
      },
      {
        widgetType: 'NONE',
        viewClass: Em.View
      }
    ];

    cases.forEach(function (item) {

      it(item.widgetType, function () {
        widget.set('widgetType', item.widgetType);
        expect(widget.get('viewClass')).to.eql(item.viewClass);
      });

    });

  });

});