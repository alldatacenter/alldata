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

var cmp = Ember.computed;

App.CapschedAdvancedController = Ember.Controller.extend({
  needs: ['capsched'],

  actions: {
    rollbackQueueMappingProps: function() {
      var tempRefreshNeeded = this.get('isRefreshOrRestartNeeded');
      var sched = this.get('scheduler'),
      attributes = sched.changedAttributes(),
      props = this.queueMappingProps;
      props.forEach(function(prop) {
        if (attributes.hasOwnProperty(prop)) {
          sched.set(prop, attributes[prop][0]);
        }
      });
      this.set('isRefreshOrRestartNeeded', tempRefreshNeeded);
    },
    rollbackProp: function(prop, item) {
      var tempRefreshNeeded = this.get('isRefreshOrRestartNeeded');
      var attributes = item.changedAttributes();
      if (attributes.hasOwnProperty(prop)) {
        item.set(prop, attributes[prop][0]);
      }
      this.set('isRefreshOrRestartNeeded', tempRefreshNeeded);
      this.afterRollbackProp();
    },
    showSaveConfigDialog: function(mode) {
      if (mode) {
        this.set('saveMode', mode);
      } else {
        this.set('saveMode', '');
      }
      this.set('isSaveConfigDialogOpen', true);
    },
    showConfirmDialog: function() {
      this.set('isConfirmDialogOpen', true);
    }
  },

  initEvents: function() {
    this.get('eventBus').subscribe('beforeSavingConfigs', function() {
      this.set("tempRefreshNeed", this.get('isRefreshOrRestartNeeded') || false);
    }.bind(this));

    this.get('eventBus').subscribe('afterConfigsSaved', function(refresh) {
      this.set('isRefreshOrRestartNeeded', refresh !== undefined? refresh:this.get('tempRefreshNeed'));
    }.bind(this));
  }.on('init'),

  teardownEvents: function() {
    this.get('eventBus').unsubscribe('beforeSavingConfigs');
    this.get('eventBus').unsubscribe('afterConfigsSaved');
  }.on('willDestroy'),

  isOperator: cmp.alias('controllers.capsched.isOperator'),
  scheduler: cmp.alias('controllers.capsched.content'),
  queues: cmp.alias('controllers.capsched.queues'),

  isQueueMappingsDirty: false,
  queueMappingProps: ['queue_mappings', 'queue_mappings_override_enable'],
  isRefreshOrRestartNeeded: false,
  isQueueMappignsNeedSaveOrRefresh: cmp.or('isQueueMappingsDirty', 'isRefreshOrRestartNeeded'),

  saveMode: '',

  isConfirmDialogOpen: false,
  isSaveConfigDialogOpen: false,

  configNote: cmp.alias('store.configNote'),

  queueMappingsDidChange: function() {
    var sched = this.get('scheduler'),
    attributes = sched.changedAttributes(),
    props = this.queueMappingProps;
    var isDirty = props.any(function(prop){
      return attributes.hasOwnProperty(prop);
    });
    this.set('isQueueMappingsDirty', isDirty);
    this.set('isRefreshOrRestartNeeded', isDirty);
  }.observes('scheduler.queue_mappings', 'scheduler.queue_mappings_override_enable'),

  forceRefreshRequired: function() {
    return !this.get('isQueueMappingsDirty') && this.get('isRefreshOrRestartNeeded');
  }.property('isQueueMappingsDirty', 'isRefreshOrRestartNeeded'),

  afterRollbackProp: function() {
    if (this.get('isQueueMappingsDirty')) {
      this.set('isRefreshOrRestartNeeded', true);
    } else {
      this.set('isRefreshOrRestartNeeded', false);
    }
  }
});
