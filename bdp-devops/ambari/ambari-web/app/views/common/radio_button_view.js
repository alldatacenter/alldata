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
 * Create custom styled radio button. Extends from App.CheckboxView with few additional properties:
 * - selection
 * - value
 *
 * @extends App.CheckboxView
 * @see App.CheckboxView
 * @type {Ember.View}
 */
App.RadioButtonView = App.CheckboxView.extend({

  /**
   * Selected value
   * @type {*}
   */
  selection: null,

  classNames: ['radio'],

  /**
   * <code>value</code> html attribute of radio button
   * @type {[type]}
   */
  value: null,

  checked: Em.computed.equalProperties('selection', 'value'),

  checkboxView: Ember.RadioButton.extend({
    selectionBinding: 'parentView.selection',
    valueBinding: 'parentView.value',
    disabledBinding: 'parentView.disabled',
    checked: Em.computed.alias('parentView.checked'),
    nameBinding: 'parentView.name',
    didInsertElement: function() {
      this.set('parentView.checkboxId', this.get('elementId'));
      this._super();
    },
    change: function() {
      if (typeof this.get('parentView.change') === 'function') {
        this.get('parentView.change').apply(this.get('parentView'), Array.prototype.slice.call(arguments));
      }
    }
  })
});
