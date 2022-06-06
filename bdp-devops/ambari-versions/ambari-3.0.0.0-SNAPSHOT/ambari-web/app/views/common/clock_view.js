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
var dateUtils = require('utils/date/date');

App.ClockView = Em.View.extend({
  classNames: ['clock-view'],
  template: Em.Handlebars.compile('<span>{{view.currentTime}}'),
  dateFormat: 'HH:mm:ss',
  updateInterval: 1000,
  /**
   * Interval function id
   * @type {Number}
   */
  intervalId: null,

  didInsertElement: function() {
    var self = this;
    this.set('intervalId', setInterval(function() {
      self.set('currentTime', dateUtils.dateFormat(new Date().getTime() + App.get('clockDistance'), self.get('dateFormat')));
    }, this.get('updateInterval')))
  },

  willDestroyElement: function() {
    if (this.get('intervalId')) {
      clearInterval(this.get('intervalId'));
      this.set('intervalId', null);
    }
  }
});
