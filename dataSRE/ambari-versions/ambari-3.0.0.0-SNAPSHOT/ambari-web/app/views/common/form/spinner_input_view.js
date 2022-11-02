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

App.SpinnerInputView = Em.View.extend({
  templateName: require('templates/common/form/spinner_input'),
  classNames: ['spinner-input'],

  /**
   * Bind content directly from Handlebars template or any another way.
   * For example:
   * {{view App.SpinnerInputView contentBinding="someContent"}}
   *
   * @property content
   * @type {Object}
   */
  content: {

    /**
     * Minimal value that can be set. (not required)
     *
     * @property [minValue]
     * @type {Number}
     */
    minValue: null,

    /**
     * Maximum value that can be set. (not required)
     *
     * @property [maxValue]
     * @type {Number}
     */
    maxValue: null,

    /**
     * Input value. (required)
     *
     * @property value
     * @type {String|Number}
     */
    value: null,

    /**
     * Config display name. Will be shown beneath the input. (not required)
     *
     * @type {String}
     * @property [label]
     */
    label: null,

    /**
     * If set to true value will toggle with max|min on property exceed value.
     *
     * @property [invertOnOverflow]
     * @type {Boolean}
     */
    invertOnOverflow: false,

    /**
     * Determines if this control should be enabled or disabled
     * It's based on increment_step value
     *
     * @property [enabled]
     * @type {Boolean}
     */
    enabled: true,

    /**
     * Increment value
     *
     * @property [incrementStep]
     * @type {Number}
     */
    incrementStep: 1
  },

  /**
   * Input should be disabled, if whole widget is disabled or if its unit is less than step_increment value
   * @type {boolean}
   */
  computedDisabled: Em.computed.or('!content.enabled', 'disabled'),

  incrementValue: function () {
    this.setValue(true);
  },

  decrementValue: function () {
    this.setValue(false);
  },

  setValue: function (increment) {
    var incrementStep = this.get('content.incrementStep');
    var value = parseInt(this.get('content.value')) + ((increment) ? incrementStep : -incrementStep);
    // if minimal required value is specified and decremented property value is less than minimal
    if (!Em.isEmpty(this.get('content.minValue')) && !increment && (value < this.get('content.minValue') )) {
      // set maximum value if value invert accepted
      value = (this.get('content.invertOnOverflow')) ? (this.get('content.maxValue') + 1 - incrementStep) : this.get('content.minValue');
    }
    // if maximum require value is specified and incremented value is more than maximum
    else if (!Em.isEmpty(this.get('content.maxValue')) && increment && (value > this.get('content.maxValue'))) {
      // set minimum if value invert accepted
      value = (this.get('content.invertOnOverflow')) ? this.get('content.minValue') : this.get('content.maxValue');
    }
    this.set('content.value', value);
  },

  /**
   * Handle keyboard event. Allow digits only and some key maps.
   */
  keyDown: function (e) {
    var charCode = (e.charCode) ? e.charCode : e.which;
    if ([46, 8, 9, 27, 13, 110].contains(charCode) ||
      (charCode == 65 && e.ctrlKey === true) ||
      (charCode == 67 && e.ctrlKey === true) ||
      (charCode == 88 && e.ctrlKey === true) ||
      (charCode >= 35 && charCode <= 39)) {
      return;
    }
    if (charCode == 190 || (e.shiftKey || (charCode < 48 || charCode > 57)) && (charCode < 96 || charCode > 105)) {
      e.preventDefault();
    }
  },

  /**
   * Change value to maximum if exceeded.
   */
  keyUp: function (e) {
    if (!Em.isEmpty(this.get('content.maxValue')) && this.get('content.value') > this.get('content.maxValue')) {
      this.set('content.value', this.get('content.maxValue'));
    }
  },

  /**
   * Set value to 0 if user removed it and changed focus to another element.
   */
  focusOut: function (e) {
    if (Em.isEmpty(this.get('content.value'))) {
      this.set('content.value', 0);
    }
  }
});
