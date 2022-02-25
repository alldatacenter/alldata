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

App.CapschedController = Ember.Controller.extend({
  queues: null,
  precision: 2,
  selectedQueue: null,
  allQueueLabels: null,
  allNodeLabels: Ember.computed.alias('store.nodeLabels.content'),

  actions: {
    loadTagged: function (tag) {
      this.transitionToRoute('capsched.scheduler').then(function() {
         this.store.fetchTagged(App.Queue, tag);
       }.bind(this));
    },
    clearAlert: function () {
      this.set('alertMessage',null);
    }
  },

  /**
   * User admin status.
   * @type {Boolean}
   */
  isOperator: false,

  /**
   * Inverted isOperator value.
   * @type {Boolean}
   */
  isNotOperator: Ember.computed.not('isOperator'),

  queuesWatcher: function() {
    var allQueues = this.get('queues') || [];
    allQueues.forEach(function(queue) {
      var absCap = this.getAbsoluteCapacityForQueue(queue);
      queue.set('absolute_capacity', absCap);
    }, this);
  }.observes('queues', 'queues.[]', 'queues.@each.capacity').on('init'),

  getAbsoluteCapacityForQueue: function(queue) {
    var allQueues = this.get('queues');
    var effectCapRatio = 1;
    while (queue !== null) {
      effectCapRatio *= queue.get('capacity') / 100;
      queue = allQueues.findBy('id', queue.get('parentPath').toLowerCase()) || null;
    }
    var effectCapPercent = effectCapRatio * 100,
    absoluteCap = parseFloat(effectCapPercent).toFixed(this.get('precision'));
    return parseFloat(absoluteCap);
  },

  labelsWatcher: function() {
    var allQueues = this.get('queues') || [];
    allQueues.forEach(function(queue) {
      var qLabels = queue.get('labels');
      if (!Ember.isEmpty(qLabels)) {
        qLabels.forEach(function(label) {
          var absCap = this.getAbsoluteCapacityForLabel(label, queue);
          label.set('absolute_capacity', absCap);
        }, this);
      }
    }, this);
  }.observes('queues.@each.labels', 'queues.@each.labels.[]', 'allQueueLabels.@each.capacity').on('init'),

  getAbsoluteCapacityForLabel: function(label, queue) {
    var allQueues = this.get('queues'),
    allQueueLabels = this.get('allQueueLabels') || [],
    labelName = label.get('name'),
    effectCapRatio = 1;
    while (queue !== null) {
      if (queue.get('labels').findBy('name', labelName)) {
        var qlabel = queue.get('labels').findBy('id', [queue.get('id'), labelName].join('.'));
        effectCapRatio *= qlabel.get('capacity') / 100;
        queue = allQueues.findBy('id', queue.get('parentPath').toLowerCase()) || null;
      } else {
        return 0;
      }
    }
    var effectCapPercent = effectCapRatio * 100,
    absoluteCap = parseFloat(effectCapPercent).toFixed(this.get('precision'));
    return parseFloat(absoluteCap);
  },

  alertMessage: null,

  tags: function () {
    return this.store.find('tag');
  }.property('store.current_tag'),

  sortedTags: Ember.computed.sort('tags', function(a, b){
    return (+a.id > +b.id)? (+a.id < +b.id)? 0 : -1 : 1;
  }),

  showSpinner: false,

  startSpinner: function() {
    this.set('showSpinner', true);
  },

  stopSpinner: function() {
    this.set('showSpinner', false);
  },

  diffXmlConfig: null,

  viewConfigXmlDiff: function(config) {
    this.set('diffXmlConfig', config);
  },

  viewXmlConfig: null,

  viewCapSchedXml: function(config) {
    this.set('viewXmlConfig', config);
  }
});
