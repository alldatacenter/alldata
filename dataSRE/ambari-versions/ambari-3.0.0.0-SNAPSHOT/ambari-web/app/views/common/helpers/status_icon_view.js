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

App.StatusIconView = Em.View.extend({
  tagName: 'i',

  /**
   * relation map between status and icon class
   * @type {object}
   */
  statusIconMap: {
    'INIT': 'icon-cogs in_progress',
    'COMPLETED': 'glyphicon glyphicon-ok completed',
    'WARNING': 'glyphicon glyphicon-warning-sign',
    'FAILED': 'glyphicon glyphicon-exclamation-sign failed',
    'HOLDING_FAILED': 'glyphicon glyphicon-exclamation-sign failed',
    'SKIPPED_FAILED': 'glyphicon glyphicon-share-alt failed',
    'PENDING': 'glyphicon glyphicon-cog pending',
    'QUEUED': 'glyphicon glyphicon-cog queued',
    'IN_PROGRESS': 'icon-cogs in_progress',
    'HOLDING': 'glyphicon glyphicon-pause',
    'SUSPENDED': 'glyphicon glyphicon-pause',
    'ABORTED': 'glyphicon glyphicon-minus aborted',
    'TIMEDOUT': 'glyphicon glyphicon-time timedout',
    'HOLDING_TIMEDOUT': 'glyphicon glyphicon-time timedout',
    'SUBITEM_FAILED': 'glyphicon glyphicon-remove failed'
  },

  classNameBindings: ['iconClass'],
  attributeBindings: ['data-original-title'],

  didInsertElement: function () {
    App.tooltip($(this.get('element')));
  },

  'data-original-title': function() {
    return this.get('content').toCapital();
  }.property('content'),

  /**
   * @type {string}
   */
  iconClass: function () {
    return this.get('statusIconMap')[this.get('content')] || 'glyphicon glyphicon-question-sign';
  }.property('content')
});