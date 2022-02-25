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

App.Widget = DS.Model.extend({
  widgetName: DS.attr('string'),

  /**
   * types:
   *  - GAUGE (shown as a percentage dial)
   *  - HEATMAP
   *  - GRAPH (Line graph and stack graph)
   *  - NUMBER (e.g., “1 ms” for RPC latency)
   *  - LINKS
   *  - TEMPLATE
   */
  widgetType: DS.attr('string'),
  layout: DS.belongsTo('App.WidgetLayout'),
  description: DS.attr('string'),
  serviceName: DS.attr('string'),
  timeCreated: DS.attr('number'),
  sectionName: DS.attr('string'),
  author: DS.attr('string'),
  scope: DS.attr('string'),
  properties: DS.attr('object'),
  expression: DS.attr('array'),
  metrics: DS.attr('array'),
  values: DS.attr('array'),
  tag: DS.attr('string'),
  isVisible: DS.attr('boolean', {defaultValue: true}),
  /**
   * This field is not derived from API but needs to be filled in the mapper on the client side
   * @type {number}
   * @default 0
   */
  defaultOrder: DS.attr('number', {
    defaultValue: 0
  }),

  /**
   * @type Em.View
   * @class
   */
  viewClass: function () {
    switch (this.get('widgetType')) {
      case 'GRAPH':
        return App.GraphWidgetView;
      case 'TEMPLATE':
        return App.TemplateWidgetView;
      case 'NUMBER':
        return App.NumberWidgetView;
      case 'GAUGE':
        return App.GaugeWidgetView;
      case 'HEATMAP':
        return App.HeatmapWidgetView;
      default:
        return Em.View;
    }
  }.property('widgetType')
});

App.Widget.FIXTURES = [];

App.WidgetType = DS.Model.extend({
  name: DS.attr('string'),
  displayName: DS.attr('string'),
  iconPath: DS.attr('string'),
  description: DS.attr('string'),
  properties: DS.attr('array')
});


App.WidgetType.FIXTURES = [
  {
    id: 'GAUGE',
    name: 'GAUGE',
    icon_path: '/img/widget-gauge.png',
    display_name: 'Gauge',
    description: Em.I18n.t('widget.type.gauge.description'),
    properties: [
      {
        name : 'threshold',
        isRequired: false,
        smallValue: '0.7',
        bigValue: '0.9'
      }
    ]
  },
  {
    id: 'NUMBER',
    name: 'NUMBER',
    icon_path: '/img/widget-number.png',
    display_name: 'Number',
    description: Em.I18n.t('widget.type.number.description'),
    properties: [
      {
        name : 'threshold',
        isRequired: false,
        smallValue: '10',
        bigValue: '20',
        MAX_VALUE: Infinity
      },
      {
        name : 'display_unit',
        isRequired: false,
        value: '',
        placeholder: 'Optional: MB, ms, etc.'
      }
    ]
  },
  {
    id: 'GRAPH',
    name: 'GRAPH',
    display_name: 'Graph',
    icon_path: '/img/widget-graph.png',
    description: Em.I18n.t('widget.type.graph.description'),
    properties: [
      {
        name : 'graph_type',
        isRequired: true,
        value: 'LINE'
      },
      {
        name : 'display_unit',
        isRequired: false,
        value: '',
        placeholder: 'Optional: MB, ms, etc.'
      }
    ]
  },
  {
    id: 'TEMPLATE',
    name: 'TEMPLATE',
    icon_path: '/img/widget-template.png',
    display_name: 'Template',
    description: Em.I18n.t('widget.type.template.description'),
    properties: []
  }
];
