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
 * Default input control
 * @type {*}
 */

var App = require('app');
require('views/common/controls_view');

//TODO should use only "serviceConfig" binding instead of "config"
App.PlainConfigTextField = Ember.View.extend(App.SupportsDependentConfigs, App.WidgetPopoverSupport, App.WidgetValueObserver, {
  templateName: require('templates/common/configs/widgets/plain_config_text_field'),
  valueBinding: 'config.value',
  classNames: ['widget-config-plain-text-field'],
  placeholderBinding: 'config.placeholder',

  disabled: Em.computed.not('config.isEditable'),

  configLabel: Em.computed.firstNotBlank('config.stackConfigProperty.displayName', 'config.displayName', 'config.name'),

  /**
   * @type {string|boolean}
   */
  unit: function() {
    return Em.getWithDefault(this, 'config.stackConfigProperty.valueAttributes.unit', false);
  }.property('config.stackConfigProperty.valueAttributes.unit'),

  /**
   * @type {string}
   */
  displayUnit: function() {
    var unit = this.get('unit');
    if ('milliseconds' == unit) {
      unit = 'ms';
    }
    return unit;
  }.property('unit'),

  insertNewline: function() {
    this.get('parentView').trigger('toggleWidgetView');
  },

  didInsertElement: function() {
    this._super();
    this.initPopover();
    this.set('config.displayType', Em.getWithDefault(this, 'config.stackConfigProperty.valueAttributes.type', 'string'));
  }

});
