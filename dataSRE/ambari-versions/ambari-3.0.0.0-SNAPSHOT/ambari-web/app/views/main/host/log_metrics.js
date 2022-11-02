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
 * @typedef {Ember.Object} LogLevelItemObject
 * @property {string} level level name
 * @property {number} counter
 */
/**
 * @typedef {Object} ServiceLogMetricsObject
 * @property {App.Service} service model instance
 * @property {LogLevelItemObject[]} logs
 */
App.MainHostLogMetrics = Em.View.extend({
  templateName: require('templates/main/host/log_metrics'),
  classNames: ['host-log-metrics'],

  /**
   * @type {ServiceLogMetricsObject[]}
   */
  logsData: function() {
    var services = this.get('content.hostComponents').mapProperty('service').uniq();
    var logLevels = ['fatal', 'critical', 'error', 'warning', 'info', 'debug'];
    return services.map(function(service) {
      var levels = logLevels.map(function(level) {
        return Em.Object.create({
          level: level,
          counter: Math.ceil(Math.random()*10)
        });
      });
      return Em.Object.create({
        service: service,
        logs: levels
      });
    });
  }.property('content'),

  /**
   * @type  {Ember.View} Pie Chart view
   * @extends App.PieChartView
   */
  chartView: App.ChartPieView.extend({
    classNames: ['log-metrics-chart'],
    w: 150,
    h: 150,
    stroke: '#fff',
    strokeWidth: 1,
    levelColors: {
      FATAL: '#B10202',
      CRITICAL: '#E00505',
      ERROR: App.healthStatusRed,
      INFO: App.healthStatusGreen,
      WARNING: App.healthStatusOrange,
      DEBUG: '#1e61f7'
    },
    innerR: 36,
    donut: d3.layout.pie().sort(null).value(function(d) { return d.get('counter'); }),

    prepareChartData: function(content) {
      this.set('data', content.get('logs'));
    },

    didInsertElement: function() {
      this.prepareChartData(this.get('content'));
      this._super();
      this.appendLabels();
      this.formatCenterText();
      this.attachArcEvents();
      this.colorizeArcs();
    },

    attachArcEvents: function() {
      var self = this;
      this.get('svg').selectAll('.arc')
        .on('mouseover', function(d) {
          self.get('svg').select('g.center-text').select('text')
            .text(d.data.get('level').capitalize() + ": " + d.data.get('counter'));
        })
        .on('mouseout', function() {
          self.get('svg').select('g.center-text').select('text').text('');
        });
    },

    formatCenterText: function() {
      this.get('svg')
        .append('svg:g')
        .attr('class', 'center-text')
        .attr('render-order', 1)
        .append('svg:text')
        .attr('transform', "translate(0,0)")
        .attr('text-anchor', 'middle')
        .attr('stroke', '#000')
        .attr('stroke-width', 0)
    },

    appendLabels: function() {
      var labelArc = d3.svg.arc()
        .outerRadius(this.get('outerR') - 15)
        .innerRadius(this.get('outerR') - 15);
      this.get('svg').selectAll('.arc')
        .append('text')
        .attr('transform', function(d) { return "translate(" + labelArc.centroid(d) + ")"; })
        .attr('stroke', '#000')
        .attr('stroke-width', 0)
        .attr('font-size', '12px')
        .attr('dy', '.50em')
        .text(function(d) { return d.data.get('counter'); });
    },

    colorizeArcs: function() {
      var self = this;
      this.get('svg').selectAll('.arc path')
        .attr('fill', function(d) {
          return self.get('levelColors')[d.data.get('level').toUpperCase()];
        });
    }
  }),


  transitionByService: function(e) {
    var service = e.context;
    App.router.transitionTo('logs', {query: '?service_name=' + service.get('service.serviceName')});
  }
});
