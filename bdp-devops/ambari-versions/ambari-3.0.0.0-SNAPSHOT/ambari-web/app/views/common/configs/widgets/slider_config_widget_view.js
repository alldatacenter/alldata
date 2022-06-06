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

/**
 * Slider-view for configs
 * Used to numeric values
 * Config value attributes should contain minimum and maximum limits for value
 * @type {App.ConfigWidgetView}
 */
App.SliderConfigWidgetView = App.ConfigWidgetView.extend({

  classNames: ['widget-config'],

  templateName: require('templates/common/configs/widgets/slider_config_widget'),

  supportSwitchToTextBox: true,

  /**
   * Slider-object created on the <code>initSlider</code>
   * @type {Object}
   */
  slider: null,

  /**
   * Mirror of the config-value shown in the input on the left of the slider
   * @type {number}
   */
  mirrorValue: 0,

  /**
   * Previous mirrorValue
   * @type {number}
   */
  prevMirrorValue: 0,

  /**
   * Determines if used-input <code>mirrorValue</code> is valid
   * Calculated on the <code>mirrorValueObs</code>
   * @type {boolean}
   */
  isMirrorValueValid: true,

  /**
   * Unit label to display.
   * @type {String}
   */
  unitLabel: '',

  /**
   * List of widget's properties which <code>changeBoundaries</code>-method should observe
   * @type {string[]}
   */
  changeBoundariesProperties: ['maxMirrorValue', 'widgetRecommendedValue','minMirrorValue', 'mirrorStep'],

  /**
   * Flag to check if value should be changed to recommended or saved.
   * @type {boolean}
   */
  isRestoring: false,

  /**
   * max allowed value transformed form config unit to widget unit
   * @type {Number}
   */
  maxMirrorValue: function() {
    var parseFunction = this.get('mirrorValueParseFunction');
    var maximum = this.getValueAttributeByGroup('maximum');
    var max = this.widgetValueByConfigAttributes(maximum);
    return parseFunction(max);
  }.property('config.stackConfigProperty.valueAttributes.maximum', 'controller.forceUpdateBoundaries'),

  /**
   * min allowed value transformed form config unit to widget unit
   * @type {Number}
   */
  minMirrorValue: function() {
    var parseFunction = this.get('mirrorValueParseFunction');
    var minimum = this.getValueAttributeByGroup('minimum');
    var min = this.widgetValueByConfigAttributes(minimum);
    return parseFunction(min);
  }.property('config.stackConfigProperty.valueAttributes.minimum', 'controller.forceUpdateBoundaries'),

  /**
   *
   * if group is default look into (ex. for maximum)
   * <code>config.stackConfigProperty.valueAttributes.maximum<code>
   * if group is not default look into
   * <code>config.stackConfigProperty.valueAttributes.{group.name}.maximum<code>
   * @param {String} attribute - name of attribute, for current moment
   * can be ["maximum","minimum","increment_step"] but allows to use other it there will be available
   * @returns {string}
   */
  getValueAttributeByGroup: function(attribute) {
    var parseFunction = this.get('parseFunction');
    var configValue = this.get('config.value');
    var defaultGroupAttr = this.get('config.stackConfigProperty.valueAttributes');
    var groupAttr = this.get('configGroup') && defaultGroupAttr[this.get('configGroup.name')];
    var usedGroupAttr = (groupAttr && !Em.isNone(groupAttr[attribute])) ? groupAttr : defaultGroupAttr;
    var boundary = usedGroupAttr[attribute];

    if (!this.get('referToSelectedGroup')) {
      if (attribute === 'minimum') {
        if (parseFunction(configValue) < parseFunction(boundary)) {
          return configValue;
        }
      } else if (attribute === 'maximum') {
        if (parseFunction(configValue) > parseFunction(boundary)) {
          return configValue;
        }
      }
    }
    if (isNaN(boundary) && !isNaN(configValue)) {
      if (attribute === 'minimum') {
        return isNaN(usedGroupAttr['maximum']) ? configValue : Math.min(usedGroupAttr['maximum'], configValue).toString();
      }
      if (attribute === 'maximum') {
        return isNaN(usedGroupAttr['minimum']) ? configValue : Math.max(usedGroupAttr['minimum'], configValue).toString();
      }
    }
    return boundary;
  },
  /**
   * step transformed form config units to widget units
   * @type {Number}
   */
  mirrorStep: function() {
    var parseFunction = this.get('mirrorValueParseFunction');
    var step = this.widgetValueByConfigAttributes(this.get('config.stackConfigProperty.valueAttributes.increment_step'));
    return step ? parseFunction(step) : this.get('unitType') === 'int' ? 1 : 0.1;
  }.property('config.stackConfigProperty.valueAttributes.increment_step'),

  /**
   * Default value of config property transformed according widget format
   * @returns {Number}
   */
  widgetDefaultValue: function () {
    var parseFunction = this.get('mirrorValueParseFunction');
    return parseFunction(this.widgetValueByConfigAttributes(this.get('config.savedValue')));
  }.property('config.savedValue'),

  /**
   * Default value of config property transformed according widget format
   * @returns {Number}
   */
  widgetRecommendedValue: function () {
    var parseFunction = this.get('mirrorValueParseFunction');
    return parseFunction(this.widgetValueByConfigAttributes(this.get('config.recommendedValue')));
  }.property('config.recommendedValue'),

  /**
   * unit type of widget
   * @type {String}
   */
  unitType: function () {
    var widgetUnit = this.get('config.stackConfigProperty.widget.units.length') && this.get('config.stackConfigProperty.widget.units')[0]['unit-name'].toLowerCase();
    var configUnit = this.get('config.stackConfigProperty.valueAttributes.type').toLowerCase();
    if (widgetUnit) {
      return this.get('units').indexOf(widgetUnit) > this.get('units').indexOf(configUnit) ? 'float' : this.get('config.stackConfigProperty.valueAttributes.type')
    } else {
      return 'float';
    }
  }.property('config.stackConfigProperty.widget.units.@each.unit-name'),
  /**
   * Function used to parse widget mirror value
   * For integer - parseInt, for float - parseFloat
   * @type {Function}
   */
  mirrorValueParseFunction: function () {
    return this.get('unitType') === 'int' ? parseInt : parseFloat;
  }.property('unitType'),

  /**
   * Function used to validate widget mirror value
   * For integer - validator.isValidInt, for float - validator.isValidFloat
   * @type {Function}
   */
  mirrorValueValidateFunction: function () {
    return this.get('unitType') === 'int' ? validator.isValidInt : validator.isValidFloat;
  }.property('unitType'),

  /**
   * Function used to parse config value (based on <code>config.stackConfigProperty.valueAttributes.type</code>)
   * For integer - parseInt, for float - parseFloat
   * @type {Function}
   */
  parseFunction: function () {
    return this.get('config.stackConfigProperty.valueAttributes.type') === 'int' ? parseInt : parseFloat;
  }.property('config.stackConfigProperty.valueAttributes.type'),

  /**
   * Function used to validate config value (based on <code>config.stackConfigProperty.valueAttributes.type</code>)
   * For integer - validator.isValidInt, for float - validator.isValidFloat
   * @type {Function}
   */
  validateFunction: function () {
    return this.get('config.stackConfigProperty.valueAttributes.type') === 'int' ? validator.isValidInt : validator.isValidFloat;
  }.property('config.stackConfigProperty.valueAttributes.type'),

  /**
   * Enable/disable slider state
   * @method toggleWidgetState
   */
  toggleWidgetState: function () {
    var slider = this.get('slider');
    this.get('config.isEditable') ? slider.enable() : slider.disable();
    this._super();
  }.observes('config.isEditable'),

  willInsertElement: function () {
    this._super();
    this.prepareValueConverter();
    this.addObserver('mirrorValue', this, this.mirrorValueObs);
  },

  didInsertElement: function () {
    this._super();
    this.setValue();
    this.initSlider();
    this.toggleWidgetState();
    this.initPopover();
    var self = this;
    this.get('changeBoundariesProperties').forEach(function(property) {
      self.addObserver(property, self, self.changeBoundaries);
    });
  },

  willDestroyElement: function() {
    this.$('[data-toggle=tooltip]').tooltip('destroy');
    var self = this;
    this.get('changeBoundariesProperties').forEach(function(property) {
      self.removeObserver(property, self, self.changeBoundaries);
    });
    this.removeObserver('mirrorValue', this, this.mirrorValueObs);
    if (this.get('slider')) {
      try {
        if (self.get('slider')) {
          self.get('slider').destroy();
        }
      } catch (e) {
        console.error('error while clearing slider for config: ' + self.get('config.name'));
      }
    }
  },

  mirrorValueObs: function () {
    Em.run.once(this, 'mirrorValueObsOnce');
  },

  /**
   * Check if <code>mirrorValue</code> was updated by user
   * Validate it. If value is correct, set it to slider and config.value
   * @method mirrorValueObs
   */
  mirrorValueObsOnce: function () {
    var mirrorValue = this.get('mirrorValue'),
      slider = this.get('slider'),
      min = this.get('minMirrorValue'),
      max = this.get('maxMirrorValue'),
      validationFunction = this.get('mirrorValueValidateFunction'),
      parseFunction = this.get('mirrorValueParseFunction');
    if (validationFunction(mirrorValue)) {
      var parsed = parseFunction(mirrorValue);
      if (parsed > max) {
        this.set('isMirrorValueValid', false);
        this.get('config').setProperties({
          warnMessage: Em.I18n.t('config.warnMessage.outOfBoundaries.greater').format(this.formatTickLabel(max, ' ')),
          warn: true
        });
      } else if (parsed < min) {
        this.set('isMirrorValueValid', false);
        this.get('config').setProperties({
          warnMessage: Em.I18n.t('config.warnMessage.outOfBoundaries.less').format(this.formatTickLabel(min, ' ')),
          warn: true
        });
      } else {
        this.set('isMirrorValueValid', !this.get('config.error'));
        this.set('config.value', '' + this.configValueByWidget(parsed));
        if (slider) {
          slider.setValue(parsed);
        }
      }
      // avoid precision during restore value
      if (!Em.isNone(this.get('config.savedValue')) && parsed == parseFunction(this.widgetValueByConfigAttributes(this.get('config.savedValue')))) {
        this.set('config.value', this.get('config.savedValue'));
      }
      // ignore precision during set recommended value
      if (!Em.isNone(this.get('config.recommendedValue')) && parsed == parseFunction(this.widgetValueByConfigAttributes(this.get('config.recommendedValue')))) {
        this.set('config.value', this.get('config.recommendedValue'));
      }
    } else {
      this.set('isMirrorValueValid', false);
      this.set('config.errorMessage', 'Invalid value');
    }
  },

  /**
   * set widget value same as config value
   * @override
   * @method setValue
   */
  setValue: function(value) {
    var parseFunction = this.get('parseFunction');
    value = value || parseFunction(this.get('config.value'));
    this.set('mirrorValue', this.widgetValueByConfigAttributes(value));
    this.set('prevMirrorValue', this.get('mirrorValue'));
  },

  /**
   * Setup convert table according to widget unit-name and property type.
   * Set label for unit to display.
   * @method prepareValueConverter
   */
  prepareValueConverter: function() {
    var widgetUnit = this._converterGetWidgetUnits();
    if (['int', 'float'].contains(this._converterGetPropertyAttributes()) && widgetUnit == 'percent') {
      this.set('currentDimensionType', 'percent.percent_' + this._converterGetPropertyAttributes());
    }
    this.set('unitLabel', Em.getWithDefault(this.get('unitLabelMap'), widgetUnit, widgetUnit));
  },

  /**
   * Draw slider for current config
   * @method initSlider
   */
  initSlider: function () {
    var self = this,
      config = this.get('config'),
      valueAttributes = config.get('stackConfigProperty.valueAttributes'),
      parseFunction = this.get('parseFunction'),
      ticks = [this.valueForTick(this.get('minMirrorValue'))],
      ticksLabels = [],
      maxMirrorValue = this.get('maxMirrorValue'),
      minMirrorValue = this.get('minMirrorValue'),
      mirrorStep = this.get('mirrorStep'),
      recommendedValue = this.valueForTick(+this.get('widgetRecommendedValue')),
      range = Math.floor((maxMirrorValue - minMirrorValue) / mirrorStep) * mirrorStep,
      isOriginalSCP = config.get('isOriginalSCP'),
      // for little odd numbers in range 4..23 and widget type 'int' use always 4 ticks
      isSmallInt = this.get('unitType') == 'int' && range > 4 && range < 23 && range % 2 == 1,
      recommendedValueMirroredId,
      recommendedValueId;

    // ticks and labels
    for (var i = 1; i <= 3; i++) {
      var val = minMirrorValue + this.valueForTickProportionalToStep(range * (i / (isSmallInt ? 3 : 4)));
      // if value's type is float, ticks may be float too
      ticks.push(this._extraRound(val));
    }

    ticks.push(this.valueForTick(maxMirrorValue));
    ticks = ticks.uniq();
    ticks.forEach(function (tick, index, items) {
      var label = '';
      if ((items.length < 5 || index % 2 === 0 || items.length - 1 == index)) {
        label = this.formatTickLabel(tick, ' ');
      }
      ticksLabels.push(label);
    }, this);

    ticks = ticks.uniq();

    if (!(this.get('controller.isCompareMode') && !isOriginalSCP)) {
      // default marker should be added only if recommendedValue is in range [min, max]
      if (recommendedValue <= maxMirrorValue && recommendedValue >= minMirrorValue && recommendedValue != '') {
        // process additional tick for default value if it not defined in previous computation
        if (!ticks.contains(recommendedValue)) {
          // push default value
          ticks.push(recommendedValue);
          // and resort array
          ticks = ticks.sort(function (a, b) {
            return a - b;
          });
          recommendedValueId = ticks.indexOf(recommendedValue);
          // to save nice tick labels layout we should add new tick value which is mirrored by index to default value
          recommendedValueMirroredId = ticks.length - recommendedValueId;
          // push mirrored default value behind default
          if (recommendedValueId == recommendedValueMirroredId) {
            recommendedValueMirroredId--;
          }
          // push empty label for default value tick
          ticksLabels.insertAt(recommendedValueId, '');
          // push empty to mirrored position
          ticksLabels.insertAt(recommendedValueMirroredId, '');
          // for saving correct sliding need to add value to mirrored position which is average between previous
          // and next value
          ticks.insertAt(recommendedValueMirroredId, this.valueForTick((ticks[recommendedValueMirroredId] + ticks[recommendedValueMirroredId - 1]) / 2));
          // get new index for default value
          recommendedValueId = ticks.indexOf(recommendedValue);
        }
        else {
          recommendedValueId = ticks.indexOf(recommendedValue);
        }
      }
    }

    /**
     * Slider some times change config value while being created,
     * this may happens when slider recreating couple times during small period.
     * To cover this situation need to reset config value after slider initializing
     * @type {String}
     */
    var correctConfigValue = this.get('config.value');

    // workaround for cases when slider input is hidden in DOM
    try {
      $(this.get('element')).find('.ui-slider-wrapper').removeClass('hide');
    } catch (e) {
      console.error('Error when trying to show slider input');
    }

    var slider = new Slider(this.$('input.slider-input')[0], {
      value: this.get('mirrorValue'),
      ticks: ticks,
      tooltip: 'always',
      ticks_labels: ticksLabels,
      step: mirrorStep,
      formatter: function (val) {
        var labelValue = Em.isArray(val) ? val[0] : val;
        return self.formatTickLabel(labelValue, ' ');
      }
    });

    /**
     * Resetting config value, look for <code>correctConfigValue<code>
     * for more info
     */
    this.set('config.value', correctConfigValue);

    slider.on('change', this.onSliderChange.bind(this))
    .on('slideStop', function() {
      /**
       * action to run sendRequestRorDependentConfigs when
       * we have changed config value within slider
       */
      if (self.get('prevMirrorValue') != self.get('mirrorValue')) {        
        self.sendRequestRorDependentConfigs(self.get('config'));
      }
    });
    this.set('slider', slider);
    var sliderTicks = this.$('.ui-slider-wrapper:eq(0) .slider-tick');

    if (recommendedValueId) {
      sliderTicks.eq(recommendedValueId).addClass('slider-tick-default').on('mousedown', function(e) {
        if (self.get('disabled')) return false;
        self.setRecommendedValue();
        e.stopPropagation();
        return false;
      });
      // create label for default value and align it
      // defaultSliderTick.append('<span>{0}</span>'.format(recommendedValue + this.get('unitLabel')));
      // defaultSliderTick.find('span').css('marginLeft', -defaultSliderTick.find('span').width()/2 + 'px');
      // if mirrored value was added need to hide the tick for it
      if (recommendedValueMirroredId) {
        sliderTicks.eq(recommendedValueMirroredId).hide();
      }
    }
    // mark last tick to fix it style
    sliderTicks.last().addClass('last');
  },

  /**
   * Callback function triggered on slider change event.
   * Set config property and widget value with new one, or ignore changes in case value restoration executed by
   * <code>restoreValue</code>, <code>setRecommendedValue</code>.
   *
   * @param {Object} e - object that contains <code>oldValue</code> and <code>newValue</code> attributes.
   * @method onSliderChange
   */
  onSliderChange: function(e) {
    if (!this.get('isRestoring')) {
      var val = this.get('mirrorValueParseFunction')(e.newValue);
      this.set('config.value', '' + this.configValueByWidget(val));
      this.set('mirrorValue', val);
    } else {
      this.set('isRestoring', false);
    }
  },
  /**
   * Convert value according to property attribute unit.
   *
   * @method valueForTick
   * @param {Number} val
   * @private
   * @returns {Number}
   */
  valueForTick: function(val) {
    return this.get('unitType') === 'int' ? Math.round(val) : this._extraRound(val);
  },

  /**
   * Convert value according to property attribute unit
   * Also returned value is proportional to the <code>mirrorStep</code>
   *
   * @param {Number} val
   * @private
   * @returns {Number}
   */
  valueForTickProportionalToStep: function (val) {
    if (this.get('unitType') === 'int') {
      return Math.round(val);
    }
    var mirrorStep = this.get('mirrorStep');
    var r = Math.round(val / mirrorStep);
    return this._extraRound(r * mirrorStep);
  },

  /**
   * Round number to 3 digits after "."
   * Used for all slider's ticks
   * @param {Number} v
   * @returns {Number} number with 3 digits after "."
   * @private
   * @method _extraRound
   */
  _extraRound: function(v) {
    return parseFloat(v.toFixed(3));
  },

  /**
   * Restore <code>savedValue</code> for config
   * Restore <code>mirrorValue</code> too
   * @method restoreValue
   */
  restoreValue: function () {
    this._super();
    this.set('isRestoring', true);
    this.get('slider').setValue(this.get('widgetDefaultValue'));
    if (this.get('config.value') === this.get('config.savedValue')) {
      this.set('isRestoring', false);
    }
  },

  /**
   * @method setRecommendedValue
   */
  setRecommendedValue: function () {
    this._super();
    this.set('isRestoring', true);
    this.get('slider').setValue(this.get('widgetRecommendedValue'));
    if (this.get('config.value') === this.get('config.recommendedValue')) {
      this.set('isRestoring', false);
    }
  },

  /**
   * Determines if config-value was changed
   * @type {boolean}
   */
  valueIsChanged: function () {
    return !Em.isNone(this.get('config.savedValue')) && this.get('parseFunction')(this.get('config.value')) != this.get('parseFunction')(this.get('config.savedValue'));
  }.property('config.value', 'config.savedValue'),

  /**
   * Run changeBoundariesOnce only once
   * @method changeBoundaries
   */
  changeBoundaries: function() {
    if (this.get('config.stackConfigProperty.widget')) {
      Em.run.once(this, 'changeBoundariesOnce');
    }
  },

  /**
   * Recreate widget in case max or min values were changed
   *
   * @method changeBoundariesOnce
   */
  changeBoundariesOnce: function () {
    if ($.mocho) {
      //temp fix as it can broke test that doesn't have any connection with this method
      return;
    }
    if (this.get('config')) {
      try {
        if (this.get('slider')) {
          this.get('slider').destroy();
        }
        this.initIncompatibleWidgetAsTextBox();
        this.initSlider();
        this.toggleWidgetState();
        // arguments exists - means method is called as observer and not directly from other method
        // so, no need to call <code>refreshSliderObserver</code> and this prevent recursive calls
        // like changeBoundariesOnce -> refreshSliderObserver -> changeBoundariesOnce -> ...
        if (arguments.length) {
          this.refreshSliderObserver();
        }
      } catch (e) {
        console.error('error while rebuilding slider for config: ' + this.get('config.name'));
      }
    }
  },

  /**
   * Method used for initializing sliders in the next Ember run-loop
   * It's useful for overrides, because they are redrawn a little bit later than origin config
   *
   * If this method is called without arguments, it will call itself recursively using <code>changeBoundariesOnce</code>
   * and <code>refreshSliderObserver</code>
   * If not - it will just call <code>changeBoundariesOnce</code> in the next run-loop
   * @method changeBoundariesOnceLater
   */
  _changeBoundariesOnceLater: function() {
    var self = this;
    Em.run.later('sync', function() {
      self.changeBoundariesOnce();
    }, 10);
  },

  /**
   * Workaround for bootstrap-slider widget that was initiated inside hidden container.
   * @method refreshSliderObserver
   */
  refreshSliderObserver: function() {
    var self = this;
    var sliderTickLabel = this.$('.ui-slider-wrapper:eq(0) .slider-tick-label:first');
    if (sliderTickLabel.width() == 0 && this.isValueCompatibleWithWidget()) {
      Em.run.next(function() {
        self._changeBoundariesOnceLater();
      });
    }
  }.observes('parentView.content.isActive', 'parentView.parentView.tab.isActive'),

  /**
   * Check if value provided by user in the textbox may be used in the slider
   * @returns {boolean}
   * @method isValueCompatibleWithWidget
   */
  isValueCompatibleWithWidget: function() {
    if (this._super()) {
      if (!this.get('validateFunction')(this.get('config.value'))) {
        return false;
      }
      var configValue = this.get('parseFunction')(this.get('config.value'));
      if (this.get('config.stackConfigProperty.valueAttributes.minimum')) {
        var min = this.get('parseFunction')(this.getValueAttributeByGroup('minimum'));
        if (configValue < min) {
          min = this.widgetValueByConfigAttributes(min);
          this.updateWarningsForCompatibilityWithWidget(Em.I18n.t('config.warnMessage.outOfBoundaries.less').format(this.formatTickLabel(min, ' ')));
          return false;
        }
      }
      if (this.get('config.stackConfigProperty.valueAttributes.maximum')) {
        var max = this.get('parseFunction')(this.getValueAttributeByGroup('maximum'));
        if (configValue > max) {
          max = this.widgetValueByConfigAttributes(max);
          this.updateWarningsForCompatibilityWithWidget(Em.I18n.t('config.warnMessage.outOfBoundaries.greater').format(this.formatTickLabel(max, ' ')));
          return false;
        }
      }

      this.updateWarningsForCompatibilityWithWidget('');
      return true;
    }
    return false;
  },

  /**
   * Returns formatted value of slider label
   * @param tick - starting value
   * @param separator - will be inserted between value and unit
   * @returns {string}
   */
  formatTickLabel: function (tick, separator) {
    var label,
      valueLabel = tick,
      units = ['B', 'KB', 'MB', 'GB', 'TB'],
      unitLabel = this.get('unitLabel'),
      unitLabelIndex = units.indexOf(unitLabel);
    if (unitLabelIndex > -1) {
      while (tick > 9999 && unitLabelIndex < units.length - 1) {
        tick /= 1024;
        unitLabelIndex++;
      }
      unitLabel = units[unitLabelIndex];
      valueLabel = this._extraRound(tick);
    }
    label = valueLabel + ((separator && unitLabel) ? separator : '') + unitLabel;
    return label;
  }

});
