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

App.ProgressBarView = Em.View.extend({
  template: Ember.Handlebars.compile('<div {{bindAttr class="barClass :progress-bar" style="view.progressWidth"}}></div>'),
  classNameBindings: ['generalClass'],

  /**
   * @type {number}
   * @default null
   */
  progress: null,

  /**
   * @type {string}
   * @default null
   */
  status: null,

  /**
   * @type {string}
   */
  generalClass: 'progress',

  /**
   * string format: width:<number>%;
   * @type {string}
   */
  progressWidth: Em.computed.format('width:{0}%;', 'progress'),

  /**
   * @type {string}
   */
  barClass: function () {
    switch (this.get('status')) {
      case 'FAILED':
        return 'progress-bar-danger';
      case 'ABORTED':
      case 'TIMED_OUT':
        return 'progress-bar-warning';
      case 'COMPLETED':
        return 'progress-bar-success';
      case 'SUSPENDED':
        return 'progress-bar-info';
      case 'QUEUED':
      case 'PENDING':
      case 'IN_PROGRESS':
        return 'progress-bar-info active progress-bar-striped';
      default:
        return 'progress-bar-info'
    }
  }.property('status')
});
