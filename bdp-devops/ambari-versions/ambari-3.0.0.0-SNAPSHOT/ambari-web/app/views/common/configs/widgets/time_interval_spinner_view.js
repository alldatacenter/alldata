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

App.TimeIntervalSpinnerView = App.ConfigWidgetView.extend({
  templateName: require('templates/common/configs/widgets/time_interval_spinner'),
  classNames: ['widget-config', 'spinner-input-widget'],

  supportSwitchToTextBox: true,

  /**
   * @property isValid
   * @type {Boolean}
   */
  isValid: true,

  /**
   * @property disabled
   * @type {Boolean}
   */
  disabled: false,

  /**
   * @property valueIsChanged
   * @type {Boolean}
   */
  valueIsChanged: false,

  /**
   * Default property value in widget format.
   *
   * @property savedValue
   * @type {Object[]}
   */
  savedValue: null,

  /**
   * Maximum property value in widget format.
   *
   * @property maxValue
   * @type {Object[]}
   */
  maxValue: function() {
    return this.generateWidgetValue(this.get('config.stackConfigProperty.valueAttributes.maximum'));
  }.property('config.stackConfigProperty.valueAttributes.maximum'),

  /**
   * Minimum property value in widget format.
   *
   * @property minValue
   * @type {Object[]}
   */
  minValue: function() {
    return this.generateWidgetValue(this.get('config.stackConfigProperty.valueAttributes.minimum'));
  }.property('config.stackConfigProperty.valueAttributes.minimum'),

  propertyUnitBinding: 'config.stackConfigProperty.valueAttributes.unit',

  /**
   * @TODO move it to unit conversion view mixin?
   * Map with maximum value per unit.
   *
   * @property timeMaxValueOverflow
   * @type {Object}
   */
  timeMaxValueOverflow: {
    milliseconds: 999,
    seconds: 59,
    minutes: 59,
    hours: 23,
    days: 365
  },

  didInsertElement: function () {
    this._super();
    this.prepareContent();
    this.toggleWidgetState();
    this.initPopover();
  },

  /**
   * Content setter.
   * Affects to view attributes:
   *  @see savedValue
   *       content
   * @method prepareContent
   */
  prepareContent: function() {
    var property = this.get('config');
    this.setValue(!isNaN(parseInt(property.get('value'))) ? property.get('value') : 0);
    this.parseIncrement();
  },

  /**
   * If <code>config.stackConfigProperty.valueAttributes.increment_step</code> exists, it should be used as increment
   * for controls. Also if it's value greater than maximum value for last unit, it (unit) should be disabled
   * @method parseIncrement
   */
  parseIncrement: function () {
    var property = this.get('config');
    var step = property.get('stackConfigProperty.valueAttributes.increment_step');
    if (step) {
      var type = this.get('content.lastObject.type');
      step = this.convertValue(step, property.get('stackConfigProperty.valueAttributes.unit'), type);
      this.set('content.lastObject.incrementStep', step);
      if (step > Em.get(this, 'timeMaxValueOverflow.' + type)) {
        this.set('content.lastObject.enabled', false);
      }
    }
  },

  /**
   * @TODO move it to unit conversion view mixin?
   * Generate formatted value for widget.
   *
   * @param {String|Number} value
   * @returns {Object[]}
   * @method generateWidgetValue
   */
  generateWidgetValue: function(value) {
    return this.widgetValueByConfigAttributes(value, true).map(function(item) {
      item.label = Em.I18n.t('common.' + item.type);
      item.minValue = 0;
      item.maxValue = Em.get(this, 'timeMaxValueOverflow.' + item.type);
      item.incrementStep = 1;
      item.enabled = true;
      item.invertOnOverflow = true;
      return item;
    }, this);
  },

  /**
   * Handle changes for widget value.
   *  * Capture validation errors.
   *  * Set original config value converted from widget format
   *  * Send recommendations.
   */
  widgetValueObserver: function() {
    this.checkModified();
    this.setConfigValue();
    this.checkErrors();
    this.sendRequestRorDependentConfigs(this.get('config'));
  }.observes('content.@each.value'),

  /**
   * Handle config value changes in raw mode.
   * Check specific validation errors regarding widget format value.
   */
  configValueObserverForAttributes: function() {
    if (this.get('config.showAsTextBox')) {
      this.checkErrors();
    }
  }.observes('config.value', 'config.showAsTextBox'),

  /**
   * Check for property modification.
   * @method checkModified
   */
  checkModified: function() {
    this.set('valueIsChanged', this.configValueByWidget(this.get('content')) != parseInt(this.get('config.savedValue')));
  },

  /**
   * Check for validation errors like minimum or maximum required value
   * @param {*} [val]
   * @method checkErrors
   */
  checkErrors: function(val) {
    val = val || this.get('content');
    // in raw mode we should use config value instead of converted from widget
    var convertedValue = this.get('config.showAsTextBox') ? parseInt(this.get('config.value'), 10) : this.configValueByWidget(val);
    var warnMessage = '';
    var warn = false;
    // if not a number ignore check since it validation failed in config model as well.
    if (isNaN(convertedValue)) {
      return;
    }
    if (convertedValue < parseInt(this.get('config.stackConfigProperty.valueAttributes.minimum'))) {
      warnMessage = Em.I18n.t('config.warnMessage.outOfBoundaries.less').format(this.dateToText(this.get('minValue')));
      warn = true;
    }
    else if (convertedValue > parseInt(this.get('config.stackConfigProperty.valueAttributes.maximum'))) {
      warnMessage = Em.I18n.t('config.warnMessage.outOfBoundaries.greater').format(this.dateToText(this.get('maxValue')));
      warn = true;
    }
    this.setProperties({
      isValid: !warnMessage,
      warnMessage: warnMessage
    });
    this.get('config').setProperties({
      warnMessage: warnMessage,
      warn: warn
    });
  },

  /**
   * set appropriate attribute for configProperty model
   * @method setConfigValue
   */
  setConfigValue: function(value) {
    this.set('config.value', '' + (value || this.configValueByWidget(this.get('content'))));
  },

  setValue: function(value) {
    if ((value && !isNaN(value)) || !isNaN(this.get('config.value'))) {
      this.set('content', this.generateWidgetValue(value || this.get('config.value')));
    }
  },

  setRecommendedValue: function() {
    this._super();
    this.setValue();
    this.parseIncrement();
  },

  /**
   * Convert value to readable format using widget value.
   *
   * @param {Object[]} widgetFormatValue - value formatted for widget @see convertToWidgetUnits
   * @return {String}
   * @method dateToText
   */
  dateToText: function(widgetFormatValue) {
    return widgetFormatValue.map(function(item) {
      if (Em.get(item, 'value') > 0) {
        return Em.get(item, 'value') + ' ' + Em.get(item, 'label');
      }
      else {
        return null;
      }
    }).compact().join(' ');
  },

  /**
   * Restore value to default.
   * @method restoreValue
   */
  restoreValue: function() {
    this._super();
    this.setValue(this.get('config.savedValue'));
    this.parseIncrement();
  },

  isValueCompatibleWithWidget: function() {
    if (this._super()) {
      var configValue = parseInt(this.get('config.value'));
      if (isNaN(configValue)) return false;
      if (this.get('config.stackConfigProperty.valueAttributes.minimum')) {
        var min = parseInt(this.get('config.stackConfigProperty.valueAttributes.minimum'));
        if (configValue < min) {
          this.updateWarningsForCompatibilityWithWidget(Em.I18n.t('config.warnMessage.outOfBoundaries.less').format(this.dateToText(this.get('minValue'))));
          return false;
        }
      }
      if (this.get('config.stackConfigProperty.valueAttributes.maximum')) {
        var max = parseInt(this.get('config.stackConfigProperty.valueAttributes.maximum'));
        if (configValue > max) {
          this.updateWarningsForCompatibilityWithWidget(Em.I18n.t('config.warnMessage.outOfBoundaries.greater').format(this.dateToText(this.get('maxValue'))));
          return false;
        }
      }
      if (this.get('config.stackConfigProperty.valueAttributes.increment_step')) {
        if (configValue % this.get('config.stackConfigProperty.valueAttributes.increment_step') != 0) return false;
      }
      this.updateWarningsForCompatibilityWithWidget('');
      return true;
    }
    return false;
  }

});
