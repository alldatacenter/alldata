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

App.CapschedQueuesconfEditqueueView = Ember.View.extend({
  isCapacityPanelCollapsed: true,
  isLabelsPanelCollapsed: true,
  isPreemptionPanelCollapsed: true,
  isAclPanelCollapsed: true,
  isResourcesPanelCollapsed: true,

  doExpandCollapsePanel: function() {
    var that = this;
    this.$('#collapseQueueAclPanelBtn').on('click', function(e) {
      Ember.run.next(that, function() {
        this.toggleProperty('isAclPanelCollapsed');
      });
    });
    this.$('#collapseResourcesPanelBtn').on('click', function(e) {
      Ember.run.next(that, function() {
        this.toggleProperty('isResourcesPanelCollapsed');
      });
    });
    this.$('#collapseQueueCapacityPanelBtn').on('click', function(e) {
      Ember.run.next(that, function() {
        this.toggleProperty('isCapacityPanelCollapsed');
      });
    });
    this.$('#collapseLabelCapacityPanelBtn').on('click', function(e) {
      Ember.run.next(that, function() {
        this.toggleProperty('isLabelsPanelCollapsed');
      });
    });
    this.$('#collapseQueuePreemptionPanelBtn').on('click', function(e) {
      Ember.run.next(that, function() {
        this.toggleProperty('isPreemptionPanelCollapsed');
      });
    });
  }.on('didInsertElement'),

  destroyEventListeners: function() {
    this.$('#collapseQueueAclPanelBtn').off('click');
    this.$('#collapseResourcesPanelBtn').off('click');
    this.$('#collapseQueueCapacityPanelBtn').off('click');
    this.$('#collapseLabelCapacityPanelBtn').off('click');
    this.$('#collapseQueuePreemptionPanelBtn').off('click');
  }.on('willDestroyElement')
});
