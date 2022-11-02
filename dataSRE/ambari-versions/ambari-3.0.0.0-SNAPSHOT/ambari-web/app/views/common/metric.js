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

/**
 * use: {{view App.MetricFilteringWidget controllerBinding="App.router.mainChartsController"}}
 * set controller.metric field with metric value
 * widget assign itself to controller like metricWidget (controller.get('metricWidget'))
 * @type {*}
 */
App.MetricFilteringWidget = Em.View.extend({
  classNames:['metric-filtering-widget'],
  /**
   * chosen metric value
   */
  chosenMetric:null,
  chosenMoreMetric:null,
  showMore:0, // toggle more metrics indicator
  /**
   * metrics
   */
  metrics:[
    Em.Object.create({ label:Em.I18n.t('metric.default'), value:null}),
    Em.Object.create({ label:Em.I18n.t('metric.cpu'), value:'cpu'}),
    Em.Object.create({ label:Em.I18n.t('metric.memory'), value:'memory'}),
    Em.Object.create({ label:Em.I18n.t('metric.network'), value:'network'}),
    Em.Object.create({ label:Em.I18n.t('metric.io'), value:'io'})
  ],


  moreMetrics:[
    Em.Object.create({ label:Em.I18n.t('metric.more.cpu'), code:'cpu', items:[
      Em.Object.create({value:"cpu_nice"}),
      Em.Object.create({value:"cpu_wio"}),
      Em.Object.create({value:"cpu_user"}),
      Em.Object.create({value:"cpu_idle"}),
      Em.Object.create({value:"cpu_system"}),
      Em.Object.create({value:"cpu_aidle"})
    ] }),

    Em.Object.create({ label:Em.I18n.t('metric.more.disk'), code:'disk',
      items:[
        Em.Object.create({value:'disk_free'}),
        Em.Object.create({value:'disk_total'}),
        Em.Object.create({value:'part_max_used'})
      ]
    }),

    Em.Object.create({ label:Em.I18n.t('metric.more.load'), code:'load',
      items:[
        Em.Object.create({value:'load_one'}),
        Em.Object.create({value:'load_five'}),
        Em.Object.create({value:'load_fifteen'})
      ]
    }),

    Em.Object.create({ label:Em.I18n.t('metric.more.memory'), code:'memory',
      items:[
        Em.Object.create({value:'swap_free'}),
        Em.Object.create({value:'cpu'})
      ]
    }),

    Em.Object.create({ label:Em.I18n.t('metric.more.network'), code:'network',
      items:[
        Em.Object.create({value:'bytes_out'}),
        Em.Object.create({value:'bytes_in'}),
        Em.Object.create({value:'pkts_in'}),
        Em.Object.create({value:'pkts_out'})
      ]
    }),

    Em.Object.create({ label:Em.I18n.t('metric.more.process'), code:'process',
      items:[
        Em.Object.create({value:'proc_run'}),
        Em.Object.create({value:'proc_total'})
      ]
    })

  ],

  /**
   * return array of chosen metrics
   */
  chosenMetrics:function () {
    return this.get('chosenMetric') ? [this.get('chosenMetric')] : this.get('defaultMetrics');
  }.property('chosenMetric'),

  /**
   * metric item view
   */
  itemView:Em.View.extend({
    tagName:'li',
    classNameBindings:['disabled'],
    disabled:Em.computed.ifThenElse('isActive', 'disabled', false),
    isActive:Em.computed.equalProperties('metric.value', 'widget.chosenMetric'),
    label:Em.computed.alias('metric.label'),
    template:Em.Handlebars.compile('<a {{action activate view.metric.value target="view.widget" href="#" }}>{{unbound view.label}}</a>')
  }),

  moreItemView:function () {
    return this.get('itemView').extend({
      label:Em.computed.alias('metric.value')
    });
  }.property(),

  /**
   * return default selected metrics (currently - all)
   */
  defaultMetrics:function () {
    var values = [];
    $.each(this.get('metrics'), function () {
      if (this.value) {
        values.push(this.value);
      }
    });
    return values;
  }.property(),

  bindToController:function () {
    var thisW = this;
    var controller = this.get('controller');
    controller.set('metricWidget', thisW);
  },

  toggleMore:function () {
    this.set('showMore', 1 - this.get('showMore'));
  },

  /**
   * assign this widget to controller, prepare items by metricsConfig
   */
  init:function () {
    this._super();
    this.bindToController();
  },

  /**
   * write active metric to widget
   * @param event
   */
  activate:function (event) {
    this.set('chosenMetric', event.context);
  },

  templateName:require('templates/common/metric')
});