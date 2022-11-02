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

App.EditLabelCapacityComponent = Ember.Component.extend({
  layoutName: 'components/editLabelCapacity',

  label: null,
  queue: null,
  parentQueue: null,

  actions: {
    rollbackProp: function(prop, target) {
      var attrs = target.changedAttributes();
      if (attrs.hasOwnProperty(prop)) {
        target.set(prop, attrs[prop][0]);
      }
    },
    enableAccess: function() {
      this.get('queue').recursiveAddChildQueueLabels(this.get('label'));
    },
    disableAccess: function() {
      this.get('queue').recursiveRemoveChildQueueLabels(this.get('label'));
    }
  },

  isAccessEnabled: function() {
    if (this.get('queue.labels').findBy('name',this.get('label.name'))) {
      return true;
    } else {
      return false;
    }
  }.property('queue', 'queue.labels.[]'),

  isAccessDisabled: Ember.computed.not('isAccessEnabled'),

  isDisabledByParent: function() {
    if (Ember.isEmpty(this.get('parentQueue'))) {
      return false;
    } else {
      return !this.get('parentQueue.labels').findBy('name', this.get('label.name'));
    }
  }.property('parentQueue', 'parentQueue.labels.[]'),

  isLabelCapacityDirty: function() {
    var isDirty = this.get('label').changedAttributes().hasOwnProperty('capacity');
    this.set('label.isDirtyLabelCapacity', isDirty);
    return isDirty;
  }.property('label.capacity'),

  isLabelMaxCapacityDirty: function() {
    var isDirty = this.get('label').changedAttributes().hasOwnProperty('maximum_capacity');
    this.set('label.isDirtyLabelMaxCapacity', isDirty);
    return isDirty;
  }.property('label.maximum_capacity'),

  isInvalidLabelMaxCapacity: function() {
    var invalid = this.get('label.maximum_capacity') < this.get('label.capacity');
    this.get('queue').set('isInvalidLabelMaxCapacity', invalid);
    return invalid;
  }.property('label.capacity', 'label.maximum_capacity')
});
