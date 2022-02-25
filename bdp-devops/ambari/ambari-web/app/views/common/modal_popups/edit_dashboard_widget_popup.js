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
const {isValidFloat} = require('utils/validator');

/**
 * Thresholds manager for sliders with single value
 * Usage:
 * <pre>
 * SingleHandler.create({
 *    thresholdMin: 12,
 *    minValue: 10,
 *    maxValue: 100
 * });
 * </pre>
 *
 * @class SingleHandler
 */
const SingleHandler = Em.Object.extend({

  /**
   * @type {number}
   */
  thresholdMin: null,

  /**
   * @type {number}
   * @default 0
   */
  minValue: 0,

  /**
   * @type {number}
   */
  maxValue: null,

  /**
   * Is <code>thresholdMin</code> invalid
   * true - invalid
   * false - valid
   *
   * @type {boolean}
   */
  thresholdMinError: Em.computed.bool('thresholdMinErrorMessage'),

  /**
   * Alias for <code>thresholdMinError</code>
   *
   * @type {boolean}
   */
  hasErrors: Em.computed.alias('thresholdMinError'),

  /**
   * Error message for <code>thresholdMin</code>
   * <ul>
   *  <li>Value is not a number</li>
   *  <li>Value is out of range <code>(minValue - maxValue)</code></li>
   * </ul>
   * Empty message means that <code>thresholdMin</code> has valid value
   *
   * @type {string}
   */
  thresholdMinErrorMessage: function () {
    var thresholdMin = this.get('thresholdMin');
    var maxValue = this.get('maxValue');
    var minValue = this.get('minValue');
    if (!isValidFloat(thresholdMin) || thresholdMin > maxValue || thresholdMin < minValue) {
      return Em.I18n.t('dashboard.widgets.error.invalid').format(minValue, maxValue);
    }
    return '';
  }.property('thresholdMin', 'maxValue'),

  /**
   * Formatted threshold value
   *
   * @type {number[]}
   */
  preparedThresholds: function () {
    return [parseFloat(this.get('thresholdMin'))];
  }.property('thresholdMin'),

  /**
   * Force set new values for threshold
   *
   * @param {number[]} newValues
   */
  updateThresholds(newValues) {
    this.set('thresholdMin', newValues[0]);
  }

});

/**
 * Thresholds manager for sliders with double values
 * Usage:
 * <pre>
 * SingleHandler.create({
 *    thresholdMin: 12,
 *    thresholdMax: 40,
 *    minValue: 10,
 *    maxValue: 100
 * });
 * </pre>
 *
 * @class DoubleHandlers
 */
const DoubleHandlers = SingleHandler.extend({

  /**
   * @type {number}
   */
  thresholdMax: null,

  /**
   * Is <code>thresholdMax</code> invalid
   * true - invalid
   * false - valid
   *
   * @type {boolean}
   */
  thresholdMaxError: Em.computed.bool('thresholdMaxErrorMessage'),

  /**
   * Is some threshold invalid
   * true - some one is invalid
   * false - thresholds are valid
   *
   * @type {boolean}
   */
  hasErrors: Em.computed.or('thresholdMinError', 'thresholdMaxError'),

  /**
   * Error message for <code>thresholdMin</code>
   * <ul>
   *  <li>Value is not a number</li>
   *  <li>Value is out of range <code>(minValue - maxValue)</code></li>
   *  <li><code>thresholdMin</code>-value greater than <code>thresholdMax</code>-value</li>
   * </ul>
   * Empty message means that <code>thresholdMin</code> has valid value
   *
   * @type {string}
   */
  thresholdMinErrorMessage: function () {
    var thresholdMin = this.get('thresholdMin');
    var thresholdMax = this.get('thresholdMax');
    var maxValue = this.get('maxValue');
    var minValue = this.get('minValue');
    if (!isValidFloat(thresholdMin) || thresholdMin > maxValue || thresholdMin < minValue) {
      return Em.I18n.t('dashboard.widgets.error.invalid').format(minValue, maxValue);
    }
    if (this.get('thresholdMaxError') === false && thresholdMax <= thresholdMin) {
      return Em.I18n.t('dashboard.widgets.error.smaller');
    }
    return '';
  }.property('thresholdMin', 'thresholdMax'),

  /**
   * Error message for <code>thresholdMax</code>
   * <ul>
   *  <li>Value is not a number</li>
   *  <li>Value is out of range <code>(minValue - maxValue)</code></li>
   * </ul>
   * Empty message means that <code>thresholdMax</code> has valid value
   *
   * @type {string}
   */
  thresholdMaxErrorMessage: function () {
    var thresholdMax = this.get('thresholdMax');
    var maxValue = this.get('maxValue');
    var minValue = this.get('minValue');
    if (!isValidFloat(thresholdMax) || thresholdMax > maxValue || thresholdMax < minValue) {
      return Em.I18n.t('dashboard.widgets.error.invalid').format(minValue, maxValue);
    }
    return '';
  }.property('thresholdMax'),

  /**
   * Threshold values ready to save
   *
   * @type {number[]}
   */
  preparedThresholds: function () {
    return [parseFloat(this.get('thresholdMin')), parseFloat(this.get('thresholdMax'))];
  }.property('thresholdMin', 'thresholdMax'),

  /**
   * Force set new values for threshold
   *
   * @param {number[]} newValues
   */
  updateThresholds(newValues) {
    this.set('thresholdMin', newValues[0]);
    this.set('thresholdMax', newValues[1]);
  }

});

/**
 * Common body-view for popup with sliders
 *
 * @class EditDashboardWidgetPopupBody
 */
const EditDashboardWidgetPopupBody = Em.View.extend({
  templateName: require('templates/main/dashboard/edit_widget_popup')
});

/**
 * Popup with slider to edit dashboard widget
 * Usage:
 * <pre>
 *   App.EditDashboardWidgetPopup.show({
 *    widgetView: this,
 *    sliderHandlersManager: App.EditDashboardWidgetPopup.DoubleHandlers.create({
 *      maxValue: 100,
 *      thresholdMin: this.get('thresholdMin'),
 *      thresholdMax: this.get('thresholdMax')
 *    })
 *  });
 * </pre>
 *
 * <code>widgetView</code> should be set to view with widget.
 * Usually you will use <code>App.EditDashboardWidgetPopup</code> inside of some <code>App.DashboardWidgetView</code> instance,
 * so <code>widgetView</code> may be set to <code>this</code>
 * <code>sliderHandlersManager</code> should be set to some of the <code>App.EditDashboardWidgetPopup.SingleHandler</code>
 * or <code>App.EditDashboardWidgetPopup.DoubleHandler</code>
 *
 * You can't use <code>App.EditDashboardWidgetPopup</code> without setting this two properties!
 *
 * @class App.EditDashboardWidgetPopup
 */
App.EditDashboardWidgetPopup = App.ModalPopup.extend({

  header: Em.I18n.t('dashboard.widgets.popupHeader'),
  classNames: ['modal-edit-widget'],
  modalDialogClasses: ['modal-lg'],
  primary: Em.I18n.t('common.apply'),
  disablePrimary: Em.computed.alias('sliderHandlersManager.hasErrors'),

  /**
   * Can't be null or undefined
   *
   * @type {SingleHandler|DoubleHandlers}
   */
  sliderHandlersManager: null,

  /**
   * Widget view
   * Can't be not a view. Used to save Thresholds. Normally it's an instance of <code>App.DashboardWidgetView</code>
   *
   * @type {Em.View}
   */
  widgetView: null,

  /**
   * Determines if slider is enabled for slide
   * true - don't enabled
   * false - enabled
   *
   * Determines if slider handlers should be updated with Threshold values
   * true - don't update
   * false - update
   *
   * Used as option <code>disabled</code> for $.ui.slider
   *
   * @type {boolean}
   */
  sliderDisabled: false,

  /**
   * Slider "ticks"
   * Used as option <code>values</code> for $.ui.slider
   *
   * @type {number[]}
   */
  sliderHandlers: function () {
    return this.get('sliderHandlersManager.preparedThresholds');
  }.property('sliderHandlersManager.preparedThresholds.[]'),

  /**
   * Colors used for slider ranges
   * @type {string[]}
   */
  sliderColors: [App.healthStatusRed, App.healthStatusOrange, App.healthStatusGreen],

  /**
   * Maximum value for slider
   * Used as option <code>max</code> for $.ui.slider
   *
   * @type {number}
   */
  sliderMaxValue: Em.computed.alias('sliderHandlersManager.maxValue'),

  /**
   * Minimum value for slider
   * Used as option <code>min</code> for $.ui.slider
   *
   * @type {number}
   */
  sliderMinValue: 0,

  /**
   * Check how many handlers has slider
   * true - 2 handlers
   * false - 1 handler
   *
   * Used as option <code>range</code> for $.ui.slider
   *
   * @type {boolean}
   */
  sliderIsRange: true,

  bodyClass: EditDashboardWidgetPopupBody,

  init() {
    Em.assert('`widgetView` should be valid view', this.get('widgetView.isView'));
    Em.assert('`sliderHandlersManager` should be set', !!this.get('sliderHandlersManager'));
    return this._super(...arguments);
  },

  /**
   * Save new threshold value on popup-close (means Primary click)
   * Use <code>widgetView</code> to get <code>widgetsView</code> and save new values
   * Current widget is updated too (without redrawing)
   */
  saveThreshold () {
    let preparedThresholds = this.get('sliderHandlersManager.preparedThresholds');
    this.get('widgetView').saveWidgetThresholds(preparedThresholds);
  },

  /**
   * Update slider values when new threshold values are provided
   * Don't do anything if some value is invalid or slider is disabled
   *
   * @private
   */
  _updateSliderValues: function() {
    var sliderHandlersManager = this.get('sliderHandlersManager');
    if (!sliderHandlersManager.get('hasErrors') && !this.get('sliderDisabled')) {
      $('#slider-range').slider('values', sliderHandlersManager.get('preparedThresholds'));
    }
  }.observes('sliderDisabled', 'sliderHandlersManager.preparedThresholds.[]', 'sliderHandlersManager.hasErrors'),

  onPrimary () {
    let sliderHandlersManager = this.get('sliderHandlersManager');
    if (!sliderHandlersManager.get('hasErrors')) {
      this.saveThreshold();
      this.hide();
    }
  },

  /**
   * Create slider in the popup when it's opened
   */
  createSlider() {
    var self = this;
    let sliderHandlersManager = this.get('sliderHandlersManager');
    var handlers = this.get('sliderHandlers');

    $('#slider-range').slider({
      range: this.get('sliderIsRange'),
      min: this.get('sliderMinValue'),
      max: this.get('sliderMaxValue'),
      disabled: this.get('sliderDisabled'),
      values: handlers,
      create: function () {
        self.updateSliderColors(handlers);
      },
      slide: function (event, ui) {
        self.updateSliderColors(ui.values);
        sliderHandlersManager.updateThresholds(ui.values);
      },
      change: function (event, ui) {
        self.updateSliderColors(ui.values);
      }
    });
  },

  didInsertElement: function () {
    this._super();
    this.createSlider();
  },

  /**
   * Update colors on slider using <code>sliderColors</code> theme when user interacts with it
   *
   * @param {number[]} handlers
   */
  updateSliderColors(handlers) {
    let gradient = this._getGradientStr(handlers);
    $('#slider-range')
      .css('background-image', '-webkit-' + gradient)
      .css('background-image', '-ms-' + gradient)
      .css('background-image', '-moz-' + gradient)
      .find('.ui-widget-header').css({
        'background-color': App.healthStatusOrange,
        'background-image': 'none'
      });
  },

  /**
   * @param {number[]} handlers
   * @returns {string}
   * @private
   */
  _getGradientStr(handlers) {
    let maxValue = this.get('sliderMaxValue');
    let colors = this.get('sliderColors');
    let gradient = colors[0] + ', ' + handlers.map((handler, i) => {
        return `${colors[i]}  ${handlers[i] * 100 / maxValue}%, ${colors[i + 1]} ${handlers[i] * 100 / maxValue}%,`;
      }).join('') + colors[colors.length - 1];
    return `linear-gradient(left,${gradient})`;
  }

});

App.EditDashboardWidgetPopup.reopenClass({
  SingleHandler,
  DoubleHandlers,
  EditDashboardWidgetPopupBody
});