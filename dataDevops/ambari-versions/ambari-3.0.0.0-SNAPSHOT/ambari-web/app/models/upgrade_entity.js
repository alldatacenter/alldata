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


/**
 * @type {Ember.Object}
 * @class
 */
App.upgradeEntity = Em.Object.extend({

  /**
   * type of entity "GROUP", "ITEM", "TASK"
   * @type {string}
   */
  type: null,

  /**
   * @type {boolean}
   */
  isExpanded: false,

  /**
   * @type {boolean}
   */
  hasExpandableItems: false,

  /**
   * @type {boolean}
   */
  isVisible: Em.computed.notEqual('status', 'PENDING'),

  /**
   * status of tasks/items/groups which should be grayed out and disabled
   * @type {Array}
   */
  nonActiveStates: ['PENDING', 'ABORTED'],

  /**
   * @type {boolean}
   */
  isRunning: Em.computed.existsIn('status', ['IN_PROGRESS']),

  /**
   * @type {boolean}
   */
  isCompleted: Em.computed.existsIn('status', ['COMPLETED', 'FAILED', 'TIMEDOUT', 'ABORTED']),

  /**
   * @type {number}
   */
  progress: function () {
    return Math.floor(this.get('progress_percent'));
  }.property('progress_percent'),

  /**
   * indicate whether entity has active link
   * @type {boolean}
   */
  isActive: function () {
    return !this.get('nonActiveStates').contains(this.get('status'));
  }.property('status'),

  /**
   * indicate whether upgrade group should be expanded
   * @type {boolean}
   */
  isExpandableGroup: function () {
    return this.get('type') === 'GROUP' && (this.get('isActive') || this.get('hasExpandableItems'));
  }.property('isActive', 'hasExpandableItems'),

  upgradeItemStatus: Em.computed.firstNotBlank('display_status', 'status'),

  /**
   * @type {string}
   */
  upgradeGroupStatus: function () {
    if (App.get('upgradeSuspended') && this.get('status') === 'ABORTED') {
      return 'SUSPENDED';
    }
    if (this.get('type') === 'GROUP' && !this.get('isActive') && this.get('hasExpandableItems')) {
      return 'SUBITEM_FAILED';
    }
    return this.get('display_status') || this.get('status');
  }.property('isExpandableGroup', 'display_status', 'status', 'App.upgradeSuspended')
});