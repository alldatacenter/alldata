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

App.RadioButtonConfigWidgetView = App.ConfigWidgetView.extend({
  templateName: require('templates/common/configs/widgets/radio_button_config'),
  classNames: ['widget-config', 'radio-button-widget'],

  /**
   * Content for radio buttons has following structure:
   *    .value {String} - radio value attribute
   *    .label {String} - label to display along with radio button
   *    .description {String} - description for specified value
   * @type {Em.Object[]}
   * @property content
   */

  didInsertElement: function() {
    this.generateContent();
  },

  generateContent: function() {
    this.set('content', this.get('config.stackConfigProperty.valueAttributes.entries').map(function(item, index, collection) {
      return Em.Object.create({
        value: item,
        descripton: this.get('config.stackConfigProperty.valueAttributes.entry_descriptions.' + index),
        label: this.get('config.stackConfigProperty.valueAttributes.entry_labels.' + index)
      });
    }, this));
  }

});
