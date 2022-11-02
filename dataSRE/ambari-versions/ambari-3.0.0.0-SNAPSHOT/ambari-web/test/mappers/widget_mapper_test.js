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
var testHelpers = require('test/helpers');
require('mappers/widget_mapper');

describe('App.widgetMapper', function () {
  var widgetModel = App.Widget,
    layoutModel = App.WidgetLayout,
    widgetRecords = widgetModel.find(),
    layoutRecords = layoutModel.find(),
    clearData = function () {
      widgetRecords.clear();
      layoutRecords.clear();
    };

  beforeEach(function () {
    clearData();
    App.store.safeLoad(layoutModel, {
      id: 1,
      section_name: 's0'
    });
    App.widgetMapper.map({
      id: 1,
      section_name: 's0',
      widgets: [
        {
          WidgetInfo: {
            id: 11,
            widget_name: 'wn0',
            widget_type: 'GRAPH',
            service_name: 'HDFS',
            time_created: 1,
            author: 'a0',
            properties: '{"graph_type":"LINE","time_range":"1"}',
            metrics: '[{"name":"m0","metric_path":"p0/m0"},{"name":"m1","metric_path":"p1/m1"}]',
            values: '[{"name":"n0","value":"v0"},{"name":"n1","value":"v1"}]',
            description: 'd0',
            scope: 'CLUSTER',
            tag: 't0'
          }
        },
        {
          WidgetInfo: {
            id: 12,
            widget_name: 'wn1',
            widget_type: 'NUMBER',
            service_name: 'YARN',
            time_created: 2,
            author: 'a1',
            properties: '{"warning_threshold":"1","error_threshold":"2"}',
            metrics: '[{"name":"m2","metric_path":"p2/m2"},{"name":"m3","metric_path":"p3/m3"}]',
            values: '[{"name":"n2","value":"v2"},{"name":"n3","value":"v3"}]',
            description: 'd1',
            scope: 'CLUSTER',
            tag: 't1'
          }
        }
      ]
    });
  });

  afterEach(clearData);

  describe('#map', function () {
    it('should load mapped data to App.Widget', function () {
      testHelpers.nestedExpect([
        {
          id: 11,
          widgetName: 'wn0',
          widgetType: 'GRAPH',
          serviceName: 'HDFS',
          timeCreated: 1,
          author: 'a0',
          properties: {
            graph_type: 'LINE',
            time_range: '1'
          },
          metrics: [
            {
              name: 'm0',
              metric_path: 'p0/m0'
            }, {
              name: 'm1',
              metric_path: 'p1/m1'
            }
          ],
          values: [
            {
              name: 'n0',
              value: 'v0'
            },
            {
              name: 'n1',
              value: 'v1'
            }
          ],
          description: 'd0',
          scope: 'CLUSTER',
          tag: 't0',
          defaultOrder: 1
        },
        {
          id: 12,
          widgetName: 'wn1',
          widgetType: 'NUMBER',
          serviceName: 'YARN',
          timeCreated: 2,
          author: 'a1',
          properties: {
            warning_threshold: '1',
            error_threshold: '2'
          },
          metrics: [
            {
              name: 'm2',
              metric_path: 'p2/m2'
            },
            {
              name: 'm3',
              metric_path: 'p3/m3'
            }
          ],
          values: [
            {
              name: 'n2',
              value: 'v2'
            },
            {
              name: 'n3',
              value: 'v3'
            }
          ],
          description: 'd1',
          scope: 'CLUSTER',
          tag: 't1',
          defaultOrder: 2
        }
      ], widgetRecords.toArray());
    });

    it('should set relations to App.WidgetLayout', function () {
      testHelpers.nestedExpect(widgetRecords.mapProperty('layout').uniq(), layoutRecords.toArray());
    });
  });
});
