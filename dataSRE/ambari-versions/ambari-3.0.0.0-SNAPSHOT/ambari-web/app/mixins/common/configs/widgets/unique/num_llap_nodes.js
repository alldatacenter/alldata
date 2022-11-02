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

/**
 * Slider for `num_llap_nodes` should have unique widget
 *
 * @type {Em.Mixin}
 */
App.NumLlapNodesWidgetMixin = Em.Mixin.create({

  /**
   * @type {boolean}
   */
  readOnly: Em.computed.alias('config.stackConfigProperty.valueAttributes.read_only'),

  /**
   * @type {boolean}
   */
  doNotShowWidget: function () {
    var self = this;
    Em.run.next(function () {
      self.handleReadOnlyAttribute();
    });
    if (this.get('readOnly')) {
      return false;
    }
    return this.get('isPropertyUndefined') || this.get('config.showAsTextBox');
  }.property('isPropertyUndefined', 'config.showAsTextBox', 'readOnly'),

  handleReadOnlyAttribute: function () {
    var readOnly = this.get('readOnly') || false;
    this.set('disabled', readOnly);
    this.set('supportSwitchToTextBox', !readOnly);
    var action = readOnly ? 'disable' : 'enable';
    this.toggleSlider(action);
  },

  toggleWidgetView: function() {
    this._super();
    var action = !this.get('config.showAsTextBox') && this.get('readOnly') ? 'disable' : 'enable';
    this.toggleSlider(action);
  },

  toggleWidgetState: function () {
    this.set('disabled', !this.get('config.isEditable'));
  }.observes('config.isEditable'),

  toggleSlider: function (action) {
    var self = this;
    Em.run.next(function () {
      Em.tryInvoke(self.get('slider'), action);
    });
  }

});