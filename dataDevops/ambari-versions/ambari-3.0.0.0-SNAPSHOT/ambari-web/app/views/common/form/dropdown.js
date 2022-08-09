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

App.DropdownView = Em.View.extend({

  templateName: require('templates/common/form/dropdown'),

  qaAttr: '',

  selection: null,

  value: '',

  optionValuePath: '',

  optionLabelPath: '',

  /**
   * Used to prevent infinite loop because of cyclic updating of value and selection
   * @type {Boolean}
   */
  isUpdating: false,

  change: Em.K,

  didInsertElement: function() {
    this.observeEmptySelection();
  },

  /**
   * value should be updated after updating selection and vise versa
   */
  onValueOrSelectionUpdate: function (context, property) {
    var selection = this.get('selection');
    var value = this.get('value');
    var content = this.get('content');
    var optionValuePath = this.get('optionValuePath');
    this.set('isUpdating', true);
    if (property === 'value') {
      this.set('selection', optionValuePath ? content.findProperty(optionValuePath, value) : value);
    } else if (property === 'selection') {
      this.set('value', selection && Em.getWithDefault(selection, optionValuePath, selection) || '');
    }
    this.set('isUpdating', false);
  }.observes('selection', 'value'),

  selectOption: function (option) {
    this.set('selection', option.context);
    this.change();
  },

  /**
   * Set default selection
   */
  observeEmptySelection: function () {
    if (this.get('content.length') && !this.get('selection')) this.set('selection', this.get('content')[0]);
  }.observes('content')

});

App.DropdownOptionView = Em.View.extend({

  tagName: 'span',

  template: Em.Handlebars.compile('{{view.optionLabel}}'),

  optionLabel: function () {
    var optionLabelPath = this.get('optionLabelPath');
    var option = this.get('option');
    if (!option) return '';
    if (optionLabelPath) return Em.get(option, optionLabelPath);
    return option;
  }.property('option', 'optionLabelPath')

});
