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

var numberUtils = require('utils/number_utils');

/**
 * Template for list-option
 * @type {Em.object}
 */
var configOption = Em.Object.extend({
  label: '',
  value: '',
  description: '',
  isSelected: false,
  isDisabled: false,
  order: 0
});

/**
 * Config Widget for List
 * Usage:
 *  <code>{{view App.ListConfigWidgetView configBinding="someObject"}}</code>
 * @type {App.ConfigWidgetView}
 */
App.ListConfigWidgetView = App.ConfigWidgetView.extend({
  classNames: ['widget-config', 'list-widget'],
  supportSwitchToTextBox: true,

  /**
   * Counter used to determine order of options selection (<code>order<code>-field in the <code>configOption</code>)
   * Greater number - later selection
   * @type {number}
   */
  orderCounter: 1,

  /**
   * Maximum length of the <code>displayVal</code>
   * If its length is greater, it will cut to current value and ' ...' will be added to the end
   * @type {number}
   */
  maxDisplayValLength: 30,

  /**
   * <code>options</code> where <code>isSelected</code> is true
   * @type {configOption[]}
   */
  val: [],

  /**
   * List of options for <code>config.value</code>
   * @type {configOption[]}
   */
  options: [],

  /**
   * String with selected options labels separated with ', '
   * If result string is too long (@see maxDisplayValLength) it's cut and ' ...' is added to the end
   * If nothing is selected, default placeholder is used
   * @type {string}
   */
  displayVal: function () {
    var v = this.get('val').sortProperty('order').mapProperty('label');
    if (v.length > 0) {
      var output = v.join(', '),
        maxDisplayValLength = this.get('maxDisplayValLength');
      if (output.length > maxDisplayValLength - 3) {
        return output.substring(0, maxDisplayValLength - 3) + ' ...';
      }
      return output;
    }
    return Em.I18n.t('services.service.widgets.list-widget.nothingSelected');
  }.property('val.[]'),

  /**
   * Config-object bound on the template
   * @type {App.StackConfigProperty}
   */
  config: null,

  /**
   * Maximum number of options allowed to select (based on <code>config.valueAttributes.selection_cardinality</code>)
   * @type {number}
   */
  allowedToSelect: 1,

  /**
   * Minimum number of options needed to select (based on <code>config.valueAttributes.selection_cardinality</code>)
   * @type {number}
   */
  neededToSelect: 0,

  templateName: require('templates/common/configs/widgets/list_config_widget'),

  willInsertElement: function () {
    this._super();
    this.parseCardinality();
    this.calculateOptions();
    this.calculateInitVal();
  },

  didInsertElement: function () {
    this.initPopover();
    this._super();
    this.toggleWidgetState();
    this.addObserver('options.@each.isSelected', this, this.calculateVal);
    this.addObserver('options.@each.isSelected', this, this.checkSelectedItemsCount);
    if (this.isValueCompatibleWithWidget()) {
      this.calculateVal();
    }
    this.checkSelectedItemsCount();
    Em.run.next(function () {
      App.tooltip(this.$('[rel="tooltip"]'));
    });
  },

  /**
   * Get list of <code>options</code> basing on <code>config.valueAttributes</code>
   * <code>configOption</code> is used
   * @method calculateOptions
   */
  calculateOptions: function () {
    var valueAttributes = this.get('config.stackConfigProperty.valueAttributes'),
      options = [];
    valueAttributes.entries.forEach(function (entryValue) {
      options.pushObject(configOption.create({
        value: entryValue.value,
        label: entryValue.label || entryValue.value,
        description: entryValue.description || ''
      }));
    });
    this.set('options', options);
  },

  /**
   * Update options list by recommendations
   * @method updateList
   */
  updateList: function() {
    this.removeObserver('options.@each.isSelected', this, this.calculateVal);
    this.removeObserver('options.@each.isSelected', this, this.checkSelectedItemsCount);
    /**
     * This method should update options only. Observes should be removed
     * until new options will be applies, to avoid changing of config value.
     */
    this.calculateOptions();

    this.addObserver('options.@each.isSelected', this, this.calculateVal);
    this.addObserver('options.@each.isSelected', this, this.checkSelectedItemsCount);
    this.set('config.showAsTextBox', !this.isValueCompatibleWithWidget());
  }.observes('config.stackConfigProperty.valueAttributes.entries.[]', 'controller.forceUpdateBoundaries'),

  /**
   * Get initial value for <code>val</code> using calculated earlier <code>options</code>
   * Used on <code>willInsertElement</code> and when user click on "Undo"-button (to restore default value)
   * @method calculateInitVal
   */
  calculateInitVal: function (configValue) {
    var config = this.get('config'),
      options = this.get('options'),
      value = configValue || config.get('value'),
      self = this,
      val = [];
    if (value !== '' && this.isOptionExist(value)) {
      if ('string' === Em.typeOf(value)) {
        value = value.split(',');
      }
      options.invoke('setProperties', {isSelected: false, isDisabled: false});
      val = value.map(function (v) {
        var option = options.findProperty('value', v.trim());
        option.setProperties({
          order: self.get('orderCounter'),
          isSelected: true
        });
        self.incrementProperty('orderCounter');
        return option;
      });
    }
    this.set('val', val);
  },

  /**
   * Get config-value basing on selected <code>options</code> sorted by <code>order</code>-field
   * Triggers on each option select/deselect
   * @method calculateVal
   */
  calculateVal: function () {
    var val = this.get('options').filterProperty('isSelected').sortProperty('order');
    this.set('val', val);
    this.set('config.value', val.mapProperty('value').join(','));
  },

  /**
   * If user already selected maximum of allowed options, disable other options
   * If user selected less than minimum of needed options, mark config.value as invalid
   * If user deselect some option, all disabled options become enabled
   * Triggers on each option select/deselect
   * @method checkSelectedItemsCount
   */
  checkSelectedItemsCount: function () {
    var allowedToSelect = this.get('allowedToSelect'),
      neededToSelect = this.get('neededToSelect'),
      currentlySelected = this.get('options').filterProperty('isSelected').length,
      selectionDisabled = allowedToSelect <= currentlySelected;
    this.get('options').filterProperty('isSelected', false).setEach('isDisabled', selectionDisabled);
    if (currentlySelected < neededToSelect) {
      this.set('config.errorMessage', 'You should select at least ' + neededToSelect + ' item(s)');
    } else {
      this.get('config').validate();
    }
  },

  /**
   * Get maximum number of options allowed to select and needed to select basing on config cardinality value
   * @method parseCardinality
   */
  parseCardinality: function () {
    var cardinality = this.get('config.stackConfigProperty.valueAttributes.selection_cardinality');
    this.set('allowedToSelect', numberUtils.getCardinalityValue(cardinality, true) || 1);
    this.set('neededToSelect', numberUtils.getCardinalityValue(cardinality, false) || 1);
  },

  /**
   * Option click-handler
   * toggle selection for current option and increment <code>orderCounter</code> for proper options selection order
   * @param {{context: Object}} e
   * @returns {boolean} always returns false to avoid list hiding
   */
  toggleOption: function (e) {
    if (e.context.get('isDisabled')) return false;
    var orderCounter = this.get('orderCounter'),
      option = this.get('options').findProperty('value', e.context.get('value'));
    option.set('order', orderCounter);
    option.toggleProperty('isSelected');
    this.incrementProperty('orderCounter');
    this.sendRequestRorDependentConfigs(this.get('config'));
    return false;
  },

  /**
   * Restore config value
   * @method restoreValue
   */
  restoreValue: function() {
    this._super();
    this.setValue(this.get('config.value'));
  },

  /**
   * @method setRecommendedValue
   */
  setRecommendedValue: function () {
    this._super();
    this.setValue(this.get('config.value'));
  },

  /**
   * Just a small checkbox-wrapper with modified click-handler
   * Should call <code>parentView.toggleOption</code>
   * User may click on the checkbox or on the link which wraps it, but action in both cases should be the same (<code>toggleOption</code>)
   * @type {App.CheckboxView}
   */
  checkBoxWithoutAction: App.CheckboxView.extend({
    init: function() {
      this._super();
      this.off('change', this, this._updateElementValue);
    },
    _updateElementValue: function () {
      var option = this.get('parentView.options').findProperty('value', this.get('value'));
      this.get('parentView').toggleOption({context: option});
    }
  }),

  setValue: function(value) {
    if (value && this.isOptionExist(value)) {
      this.calculateInitVal(value);
    } else {
      this.calculateInitVal();
    }
    this.set('config.showAsTextBox', !this.isValueCompatibleWithWidget());
  },

  isValueCompatibleWithWidget: function() {
    var res = this._super() && this.isOptionExist(this.get('config.value'));
    if (!res) {
      this.updateWarningsForCompatibilityWithWidget(Em.I18n.t('config.infoMessage.wrong.value.for.widget'));
      return false;
    }
    this.updateWarningsForCompatibilityWithWidget('');
    return true;
  },

  isOptionExist: function(value) {
    var isExist = true;
    if (Em.isNone(value)) {
      return !isExist;
    } else {
      value = Em.typeOf(value) == 'string' ? value.split(',') : value;
      value.forEach(function(item) {
        isExist = isExist && this.get('options').mapProperty('value').contains(item);
      }, this);
      return isExist;
    }
  }
});
