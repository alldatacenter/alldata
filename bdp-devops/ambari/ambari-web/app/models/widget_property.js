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

App.WidgetProperty = Ember.Object.extend({

  /**
   * label to be shown for property
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
   * input displayType
   * one of 'textFields', 'textArea', 'select' or 'threshold'
   * @type {String}
   */
  displayType: '',


  /**
   * space separated list of css class names to use
   * @type {String}
   */
  classNames: '',

  /**
   * view class according to <code>displayType</code>
   * @type {Em.View}
   */
  viewClass: function () {
    var displayType = this.get('displayType');
    switch (displayType) {
      case 'textField':
        return App.WidgetPropertyTextFieldView;
      case 'threshold':
        return App.WidgetPropertyThresholdView;
      case 'select':
        return App.WidgetPropertySelectView;
      default:
    }
  }.property('displayType'),

  /**
   * Define whether property is valid
   * Computed property should be defined in child class
   * @type {Boolean}
   */
  isValid: true,

  /**
   * Define whether property is required by user
   * @type {Boolean}
   */
  isRequired: true
});

App.WidgetPropertyTypes = [

  {
    name: 'display_unit',
    label: 'Unit',
    displayType: 'textField',
    classNames: 'widget-property-unit',
    isValid: function () {
      return this.get('isRequired') ? Boolean(this.get('value')) : true;
    }.property('value'),
    valueMap: {
     "value": "display_unit"
    }
  },

  {
    name: 'graph_type',
    label: 'Graph Type',
    displayType: 'select',
    options: [
      {
        label: "LINE",
        value: "LINE"
      },
      {
        label: "STACK",
        value: "STACK"
      }
    ],
    valueMap: {
      "value": "graph_type"
    }
  },
  {
    name: 'time_range',
    label: 'Time Range',
    displayType: 'select',
    options: [
      {
        label: "Last 1 hour",
        value: "1"
      },
      {
        label: "Last 2 hours",
        value: "2"
      },
      {
        label: "Last 4 hour",
        value: "4"
      },
      {
        label: "Last 12 hour",
        value: "12"
      },
      {
        label: "Last 24 hour",
        value: "24"
      },
      {
        label: "Last 1 week",
        value: "168"
      },
      {
        label: "Last 1 month",
        value: "720"
      },
      {
        label: "Last 1 year",
        value: "8760"
      }
    ],
    valueMap: {
      "value": "time_range"
    }
  },

  {
    name: 'threshold',
    label: 'Thresholds',

    MIN_VALUE: 0,

    MAX_VALUE: 1,

    valueMap: {
      "smallValue": "warning_threshold",
      "bigValue": "error_threshold"
    },

    /**
     * threshold-value
     * @type {string}
     */
    smallValue: '',
    bigValue: '',
    badgeOK: 'OK',
    badgeWarning: 'WARNING',
    badgeCritical: 'CRITICAL',

    displayType: 'threshold',

    classNames: 'widget-property-threshold',

    /**
     * Check if <code>smallValue</code> is valid float number
     * @return {boolean}
     */
    isSmallValueValid: function () {
      return this.validate(this.get('smallValue'));
    }.property('smallValue', 'bigValue'),

    /**
     * Check if <code>bigValue</code> is valid float number
     * @return {boolean}
     */
    isBigValueValid: function () {
      return this.validate(this.get('bigValue'));
    }.property('bigValue', 'smallValue'),

    /**
     * validate
     * @param {string} value
     * @returns {boolean}
     */
    validate: function (value) {
      value = Number(('' + value).trim());
      if (!value && !isNaN(value)) {
        return true;
      }
      return validator.isValidFloat(value) && value > this.get('MIN_VALUE') && value <= this.get('MAX_VALUE');
    },

    isValid: Em.computed.and('isSmallValueValid', 'isBigValueValid')
  }
];