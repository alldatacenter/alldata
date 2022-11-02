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
 * Toggle widget view for config property.
 * @type {Em.View}
 */
App.ToggleConfigWidgetView = App.ConfigWidgetView.extend({
  templateName: require('templates/common/configs/widgets/toggle_config_widget'),
  classNames: ['widget-config', 'toggle-widget'],

  /**
   * Saved switcher for current config.
   *
   * @property switcher
   */
  switcher: null,

  /**
   * Minimum required handle width for toggle widget in pixels.
   * @type {Number}
   */
  minHandleWidth: 30,

  /**

  /**
   * Value used in the checkbox.
   * <code>config.value</code> can't be used because it's string.
   *
   * @property switcherValue
   * @type {boolean}
   */
  switcherValue: false,

  /**
   * when value is changed by recommendations we don't need to send
   * another request for recommendations.
   *
   * @type {boolean}
   */
  skipRequestForDependencies: false,

  supportSwitchToTextBox: true,

  /**
   * Update config value using <code>switcherValue</code>.
   * switcherValue is boolean, but config value should be a string.
   *
   * @method updateConfigValue
   */
  updateConfigValue: function () {
    this.set('config.value', this.get('config.stackConfigProperty.valueAttributes.entries')[this.get('switcherValue') ? 0 : 1].value);
  },

  /**
   * Get value for <code>switcherValue</code> (boolean) using <code>config.value</code> (string).
   *
   * @param configValue
   * @returns {boolean}
   * @method getNewSwitcherValue
   */
  getNewSwitcherValue: function (configValue) {
    return this.get('config.stackConfigProperty.valueAttributes.entries')[0].value === '' + configValue;
  },

  didInsertElement: function () {
    this.set('switcherValue', this.getNewSwitcherValue(this.get('config.value')));
    // plugin should be initiated after applying binding for switcherValue
    Em.run.later('sync', function() {
      this.initSwitcher();
      this.toggleWidgetState();
      this.initPopover();
    }.bind(this), 10);
    this.addObserver('switcherValue', this, this.updateConfigValue);
    this._super();
  },

  /**
   * Value may be changed after recommendations are received
   * So, switcher should be updated too
   *
   * @method setValue
   */
  setValue: function (configValue) {
    var value = this.getNewSwitcherValue(configValue);
    if (this.get('switcherValue') !== value) {
      this.set('skipRequestForDependencies', true);
      this.get('switcher').bootstrapSwitch('toggleState', value);
      this.set('skipRequestForDependencies', false);
      this.set('switcherValue', value);
    }
  },

  /**
   * Init switcher plugin.
   *
   * @method initSwitcher
   */
  initSwitcher: function () {
    var labels = this.get('config.stackConfigProperty.valueAttributes.entries'),
      self = this;
    Em.assert('toggle for `' + this.get('config.name') + '` should contain two entries', labels.length === 2);
    if (this.$()) {
      var switcher = this.$("input:eq(0)").bootstrapSwitch({
        onText: labels[0].label,
        offText: labels[1].label,
        offColor: 'default',
        onColor: 'success',
        handleWidth: self.getHandleWidth(labels.mapProperty('label.length')),
        onSwitchChange: function (event, state) {
          self.set('switcherValue', state);
          if (!self.get('skipRequestForDependencies')) {
            self.sendRequestRorDependentConfigs(self.get('config'));
          }
        }
      });
      this.set('switcher', switcher);
    }
  },

  /**
   * Calculate handle width by longest label.
   *
   * @param {String[]} labels
   * @returns {Number}
   */
  getHandleWidth: function(labels) {
    var labelWidth = Math.max.apply(null, labels) * 8;
    return labelWidth < this.get('minHandleWidth') ? this.get('minHandleWidth') : labelWidth;
  },


  /**
   * Restore default config value and toggle switcher.
   *
   * @override App.ConfigWidgetView.restoreValue
   * @method restoreValue
   */
  restoreValue: function () {
    this._super();
    this.setValue(this.get('config.value'));
  },

  /**
   * @method setRecommendedValue
   */
  setRecommendedValue: function () {
    this._super();
    this.setValue(this.get('config.recommendedValue'));
  },

  /**
   * Enable/disable switcher basing on config isEditable value
   * @method toggleWidgetState
   */
  toggleWidgetState: function () {
    if (this.get('switcher')){
      this.get('switcher').bootstrapSwitch('disabled', !this.get('config.isEditable'));
    }
    this._super();
  }.observes('config.isEditable'),

  /**
   * Check if value provided by user in the textbox may be used in the toggle
   * @returns {boolean}
   * @method isValueCompatibleWithWidget
   */
  isValueCompatibleWithWidget: function () {
    if (this._super()) {
      var res = this.get('config.stackConfigProperty.valueAttributes.entries').mapProperty('value').contains(this.get('config.value'));
      if (!res) {
        this.updateWarningsForCompatibilityWithWidget(Em.I18n.t('config.infoMessage.wrong.value.for.widget'));
        return false;
      }
      this.updateWarningsForCompatibilityWithWidget('');
      return true;
    }
    return false;
  }

});
