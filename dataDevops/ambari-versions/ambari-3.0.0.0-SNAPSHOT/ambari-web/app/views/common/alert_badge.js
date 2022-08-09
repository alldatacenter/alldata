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

App.AlertBadgeView = Em.View.extend({
  layoutName: require('templates/common/alert_badge'),
  classNames: ['alert-badge'],

  /**
   * if true - supports only CRITICAL and WARNING alerts
   * if false - supports CRITICAL, WARNING and SUGGESTION alerts
   * @type {boolean}
   */
  isTwoLevelBadge: true,

  /**
   * @type {boolean}
   */
  shouldShowNoneCount: false,

  /**
   * @type {number}
   */
  criticalCount: 0,
  /**
   * @type {number}
   */
  warningCount: 0,
  /**
   * @type {number}
   */
  suggestionCount: 0,
  /**
   * @type {string}
   */
  criticalCountDisplay: function() {
    return this.get('criticalCount') > 99 ? "99+" : String(this.get('criticalCount'));
  }.property('criticalCount'),
  /**
   * @type {string}
   */
  warningCountDisplay: function() {
    return this.get('warningCount') > 99 ? "99+" : String(this.get('warningCount'));
  }.property('warningCount'),
  /**
   * @type {string}
   */
  suggestionCountDisplay: function() {
    return this.get('suggestionCount') > 99 ? "99+" : String(this.get('suggestionCount'));
  }.property('suggestionCount'),
  /**
   * @type {boolean}
   */
  showNoneCount: Em.computed.and('!criticalCount', '!warningCount', '!suggestionCount', 'shouldShowNoneCount')
});
