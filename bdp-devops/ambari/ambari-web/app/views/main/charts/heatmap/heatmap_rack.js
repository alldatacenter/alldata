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
var lazyloading = require('utils/lazy_loading');

App.MainChartsHeatmapRackView = Em.View.extend({
  templateName: require('templates/main/charts/heatmap/heatmap_rack'),
  classNames: ['rack', 'panel', 'panel-default'],
  classNameBindings: ['visualSchema'],

  /** rack status block class */
  statusIndicator: 'statusIndicator',
  /** loaded hosts of rack */
  hosts: [],

  /**
   * display hosts of rack
   */
  displayHosts: function () {
    var rackHosts = this.get('rack.hosts');
    var rackCount = this.get('controller.racks.length');

    if (this.get('hosts.length') === 0) {
      if (rackHosts.length > 100 && rackCount == 1) {
        lazyloading.run({
          initSize: 100,
          chunkSize: 200,
          delay: 100,
          destination: this.get('hosts'),
          source: rackHosts,
          context: this.get('rack')
        });
      } else {
        this.set('hosts', rackHosts);
        this.set('rack.isLoaded', true);
      }
    }
  },

  didInsertElement: function () {
    this.set('hosts', []);
    this.get('controller').addRackView(this);
  },
  /**
   * Provides the CSS style for an individual host.
   * This can be used as the 'style' attribute of element.
   */
  hostCssStyle: function () {
    var rack = this.get('rack');
    var hostCount = rack.get('hosts.length');
    if (hostCount >= 12) {
      return 'col-md-1';
    }
    if (hostCount === 1) {
      return 'col-md-12';
    }
    if (hostCount === 2) {
      return 'col-md-6';
    }
    if (hostCount === 3) {
      return 'col-md-4';
    }
    if (hostCount === 4) {
      return 'col-md-3';
    }
    return 'col-md-2';
  }.property('rack.isLoaded')
});
