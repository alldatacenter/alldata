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
var validator = require('utils/validator');
var numericUtils = require('utils/number_utils');

App.AlertConfigProperty = Ember.Object.extend({

  /**
   * label to be shown for config property
   * @type {String}
   */
  label: '',

  /**
   * PORT|METRIC|AGGREGATE
   * @type {String}
   */
  type: '',

  /**
   * config property value
   * @type {*}
   */
  value: null,

  /**
   * property value cache to realise undo function
   * @type {*}
   */
  previousValue: null,

  /**
   * define either input is disabled or enabled
   * @type {Boolean}
   */
  isDisabled: false,

  /**
   * options that Select list will have
   * @type {Array}
   */
  options: [],

  /**
   * input displayType
   * one of 'textFields', 'textArea', 'select' or 'threshold'
   * @type {String}
   */
  displayType: '',

  /**
   * unit to be shown with value
   * @type {String}
   */
  unit: null,

  /**
   * space separated list of css class names to use
   * @type {String}
   */
  classNames: '',

  /**
   * define whether row with property should be shifted right
   * @type {Boolean}
   */
  isShifted: false,

  /**
   * name or names of properties related to config
   * may be either string for one property or array of strings for multiple properties
   * if this property is array, then <code>apiFormattedValue</code> should also be an array
   * example: <code>apiProperty[0]</code> relates to <code>apiFormattedValue[0]</code>
   * @type {String|Array}
   */
  apiProperty: '',

  /**
   * for some metrics properties may be set true or false
   * depending on what property is related to (JMX or Ganglia)
   */
  isJMXMetric: null,

  /**
   * define place to show label
   * if true - label is shown before input
   * if false - label is shown after input
   * @type {Boolean}
   */
  isPreLabeled: Em.computed.notExistsIn('displayType', ['radioButton']),

  /**
   * value converted to appropriate format for sending to server
   * should be computed property
   * should be defined in child class
   * @type {*}
   */
  apiFormattedValue: Em.computed.alias('value'),

  /**
   * define if property was changed by user
   * @type {Boolean}
   */
  wasChanged: function () {
    return this.get('previousValue') !== null && this.get('value') !== this.get('previousValue');
  }.property('value', 'previousValue'),

  /**
   * view class according to <code>displayType</code>
   * @type {Em.View}
   */
  viewClass: function () {
    var displayType = this.get('displayType');
    switch (displayType) {
      case 'textField':
        return App.AlertConfigTextFieldView;
      case 'textArea':
        return App.AlertConfigTextAreaView;
      case 'select':
        return App.AlertConfigSelectView;
      case 'threshold':
        return App.AlertConfigThresholdView;
      case 'radioButton':
        return App.AlertConfigRadioButtonView;
      case 'parameter':
        return App.AlertConfigParameterView;
      default:
    }
  }.property('displayType'),

  /**
   * Define whether property is valid
   * Computed property
   * Should be defined in child class
   * @type {Boolean}
   */
  isValid: function () {
    return true;
  }.property()

});

App.AlertConfigProperties = {

  AlertName: App.AlertConfigProperty.extend({
    name: 'alert_name',
    label: 'Alert Name',
    displayType: 'textField',
    apiProperty: 'name'
  }),

  AlertNameSelected: App.AlertConfigProperty.extend({
    name: 'alert_name',
    label: 'Alert Name',
    displayType: 'select',
    apiProperty: 'name'
  }),

  Service: App.AlertConfigProperty.extend({
    name: 'service',
    label: 'Service',
    displayType: 'select',
    apiProperty: 'service_name',
    apiFormattedValue: function () {
      return this.get('value') === 'CUSTOM' ? this.get('value') : App.StackService.find().findProperty('displayName', this.get('value')).get('serviceName');
    }.property('value'),
    change: function () {
      this.set('property.value', true);
      this.get('parentView.controller').changeService(this.get('property.name'));
    }
  }),

  Component: App.AlertConfigProperty.extend({
    name: 'component',
    label: 'Component',
    displayType: 'select',
    apiProperty: 'component_name',
    apiFormattedValue: function () {
      return this.get('value') === 'No component' ? this.get('value') : App.StackServiceComponent.find().findProperty('displayName', this.get('value')).get('componentName');
    }.property('value')
  }),

  Scope: App.AlertConfigProperty.extend({
    name: 'scope',
    label: 'Scope',
    displayType: 'select',
    apiProperty: 'scope',
    apiFormattedValue: function () {
      return this.get('value').toUpperCase();
    }.property('value')
  }),

  Description: App.AlertConfigProperty.extend({
    name: 'description',
    label: 'Description',
    displayType: 'textArea',
    // todo: check value after API will be provided
    apiProperty: 'description'
  }),

  Interval: App.AlertConfigProperty.extend({
    name: 'interval',
    label: 'Check Interval',
    displayType: 'textField',
    unit: 'Minute',
    colWidth: 'col-md-4',
    apiProperty: 'interval',
    isValid: function () {
      var value = this.get('value');
      if (!value) return false;
      return String(value) === String(parseInt(value, 10)) && value >= 1;
    }.property('value')
  }),

  /**
   * Implements threshold
   * Main difference from other alertConfigProperties:
   * it has two editable parts - <code>value</code> and <code>text</code>
   * User may configure it to edit only one of them (use flags <code>showInputForValue</code> and <code>showInputForText</code>)
   * This flags also determines update value and text in the API-request or not (see <code>App.AlertConfigProperties.Thresholds</code> for more examples)
   *
   * @type {App.AlertConfigProperty.Threshold}
   */
  Threshold: App.AlertConfigProperty.extend({

    name: 'threshold',

    /**
     * Property text cache to realise undo function
     * @type {*}
     */
    previousText: null,

    label: '',

    /**
     * OK|WARNING|CRITICAL
     * @type {string}
     */
    badge: '',

    /**
     * threshold-value
     * @type {string}
     */
    value: '',

    /**
     * Type of value. This will be a fixed set of types (like %).
     */
    valueMetric: null,

    /**
     * Value actually displayed to the user. This value is transformed
     * based on the limited types of 'valueMetric's. Mappings from
     * 'value' to 'displayValue' is handled by observers.
     */
    displayValue: '',

    /**
     * threshold-text
     * @type {string}
     */
    text: '',

    displayType: 'threshold',

    apiProperty: [],

    init: function () {
      this.set('displayValue', this.getNewValue());
      this._super();
    },

    /**
     * @type {string[]}
     */
    apiFormattedValue: function () {
      var ret = [];
      if (this.get('showInputForValue')) {
        ret.push(this.get('value'));
      }
      if (this.get('showInputForText')) {
        ret.push(this.get('text'));
      }
      return ret;
    }.property('value', 'text', 'showInputForValue', 'showInputForText'),

    /**
     * Determines if <code>value</code> should be visible and editable (if not - won't update API-value)
     * @type {bool}
     */
    showInputForValue: true,

    /**
     * Determines if <code>text</code> should be visible and editable (if not - won't update API-text)
     * @type {bool}
     */
    showInputForText: true,

    /**
     * Custom css-class for different badges
     * type {string}
     */
    badgeCssClass: Em.computed.format('alert-state-{0}', 'badge'),

    /**
     * Determines if <code>value</code> or <code>text</code> were changed
     * @type {bool}
     */
    wasChanged: function () {
      return this.get('previousValue') !== null && this.get('value') !== this.get('previousValue') ||
      this.get('previousText') !== null && this.get('text') !== this.get('previousText');
    }.property('value', 'text', 'previousValue', 'previousText'),

    /**
     * May be redefined in child-models, mixins etc
     * @method getValue
     * @returns {string}
     */
    getNewValue: function () {
      return this.get('value');
    },

    valueWasChanged: function () {
      var displayValue = this.get('displayValue');
      var newDisplayValue = this.getNewValue();
      if (newDisplayValue !== displayValue && !(isNaN(newDisplayValue) ||isNaN(displayValue))) {
        this.set('displayValue', newDisplayValue);
      }
    }.observes('value'),

    /**
     * May be redefined in child-models, mixins etc
     * @method getDisplayValue
     * @returns {string}
     */
    getNewDisplayValue: function () {
      return this.get('displayValue');
    },

    displayValueWasChanged: function () {
      var value = this.get('value');
      var newValue = this.getNewDisplayValue();
      if (newValue !== value && !(isNaN(newValue) ||isNaN(value))) {
        this.set('value', newValue);
      }
    }.observes('displayValue'),

    /**
     * Check if <code>displayValue</code> is valid float number
     * If this value isn't shown (see <code>showInputForValue</code>), result is always true
     * @return {boolean}
     */
    isValid: function () {
      if (!this.get('showInputForValue')) {
        return true;
      }

      var value = this.get('displayValue');

      if (Em.isNone(value)) {
        return false;
      }

      value = ('' + value).trim();

      //only allow 1/10th of a second
      if (numericUtils.getFloatDecimals(value) > 1) {
        return false;
      }

      return validator.isValidFloat(value);
    }.property('displayValue', 'showInputForValue')

  }),

  URI: App.AlertConfigProperty.extend({
    name: 'uri',
    label: 'Host',
    displayType: 'textField',
    apiProperty: 'source.uri'
  }),

  URIExtended: App.AlertConfigProperty.extend({
    name: 'uri',
    label: 'URI',
    displayType: 'textArea',
    apiProperty: 'source.uri',
    apiFormattedValue: function () {
      var result = {};
      try {
        result = JSON.parse(this.get('value'));
      } catch (e) {
        console.error('Wrong format of URI');
      }
      return result;
    }.property('value')
  }),

  DefaultPort: App.AlertConfigProperty.extend({
    name: 'default_port',
    label: 'Default Port',
    displayType: 'textField',
    classNames: 'alert-port-input',
    apiProperty: 'source.default_port',
    isValid: function () {
      var value = this.get('value');
      if (!value) return false;
      return String(value) === String(parseInt(value, 10)) && value >= 1;
    }.property('value')
  }),

  Path: App.AlertConfigProperty.extend({
    name: 'path',
    label: 'Path',
    displayType: 'textField',
    apiProperty: 'source.path'
  }),

  Metrics: App.AlertConfigProperty.extend({
    name: 'metrics',
    label: 'JMX/Ganglia Metrics',
    displayType: 'textArea',
    apiProperty: Em.computed.ifThenElse('isJMXMetric', 'source.jmx.property_list', 'source.ganglia.property_list'),
    apiFormattedValue: function () {
      return this.get('value').split(',\n');
    }.property('value')
  }),

  FormatString: App.AlertConfigProperty.extend({
    name: 'metrics_string',
    label: 'Format String',
    displayType: 'textArea',
    apiProperty: Em.computed.ifThenElse('isJMXMetric', 'source.jmx.value', 'source.ganglia.value')
  }),

  Parameter: App.AlertConfigProperty.extend({

    name: 'parameter',

    displayType: 'parameter',

    badge: Em.computed.alias('threshold'),

    thresholdExists: Em.computed.bool('threshold'),

    thresholdNotExists: Em.computed.empty('threshold'),

    /**
     * Custom css-class for different badges
     * type {string}
     */
    badgeCssClass: Em.computed.format('alert-state-{0}', 'badge')

  })

};
App.AlertConfigProperties.Parameters = {

  StringMixin: Em.Mixin.create({
    isValid: function () {
      var value = this.get('value');
      return String(value).trim() !== '';
    }.property('value')
  }),
  NumericMixin: Em.Mixin.create({
    isValid: function () {
      var value = this.get('value');
      if (!value) {
        return false;
      }
      value = ('' + value).trim();
      if (!numericUtils.isPositiveNumber(value)) {
        return false;
      }
      value = parseFloat(value);
      return !isNaN(value);
    }.property('value')
  }),
  PercentageMixin: Em.Mixin.create({
    isValid: function () {
      var value = this.get('value');
      if (!value) {
        return false;
      }
      if (!validator.isValidFloat(value) || !numericUtils.isPositiveNumber(value)) {
        return false;
      }
      value = String(value).trim();
      value = parseFloat(value);

      return !isNaN(value) && value > 0;
    }.property('value')
  })

};
App.AlertConfigProperties.Thresholds = {

  OkThreshold: App.AlertConfigProperties.Threshold.extend({

    badge: 'OK',

    name: 'ok_threshold',

    apiProperty: function () {
      var ret = [];
      if (this.get('showInputForValue')) {
        ret.push('source.reporting.ok.value');
      }
      if (this.get('showInputForText')) {
        ret.push('source.reporting.ok.text');
      }
      return ret;
    }.property('showInputForValue', 'showInputForText')

  }),

  WarningThreshold: App.AlertConfigProperties.Threshold.extend({

    badge: 'WARNING',

    name: 'warning_threshold',

    apiProperty: function () {
      var ret = [];
      if (this.get('showInputForValue')) {
        ret.push('source.reporting.warning.value');
      }
      if (this.get('showInputForText')) {
        ret.push('source.reporting.warning.text');
      }
      return ret;
    }.property('showInputForValue', 'showInputForText')

  }),

  CriticalThreshold: App.AlertConfigProperties.Threshold.extend({

    badge: 'CRITICAL',

    name: 'critical_threshold',

    apiProperty: function () {
      var ret = [];
      if (this.get('showInputForValue')) {
        ret.push('source.reporting.critical.value');
      }
      if (this.get('showInputForText')) {
        ret.push('source.reporting.critical.text');
      }
      return ret;
    }.property('showInputForValue', 'showInputForText')

  }),

  /**
   * Mixin for <code>App.AlertConfigProperties.Threshold</code>
   * Used to validate values in percentage range (0..1]
   * @type {Em.Mixin}
   */
  PercentageMixin: Em.Mixin.create({

    isValid: function () {
      var value = this.get('displayValue');

      if (!value) {
        return false;
      }

      value = ('' + value).trim();
      if (numericUtils.getFloatDecimals(value)) {
        return false;
      }

      value = parseFloat(value);

      //do not allow float values
      if (parseInt(value, 10) !== value) {
        return false;
      }

      return this.get('showInputForValue') ? !isNaN(value) && value > 0 : true;
    }.property('displayValue', 'showInputForValue')

  }),

  /**
   * Mixin for <code>App.AlertConfigProperties.Threshold</code>
   * Used to validate values that should be greater than 0
   * @type {Em.Mixin}
   */
  PositiveMixin: Em.Mixin.create({

    isValid: function () {
      if (!this.get('showInputForValue')) {
        return true;
      }
      var value = this.get('displayValue');

      if (!value) {
        return false;
      }

      //only allow 1/10th of a second
      if (numericUtils.getFloatDecimals(value) > 1) {
        return false;
      }

      value = ('' + value).trim();
      value = parseFloat(value);

      return !isNaN(value) && value > 0;
    }.property('displayValue', 'showInputForValue')

  })

};
