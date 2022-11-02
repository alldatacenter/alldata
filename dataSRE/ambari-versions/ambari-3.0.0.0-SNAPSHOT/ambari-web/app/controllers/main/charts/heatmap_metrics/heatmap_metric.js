/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
var date = require('utils/date/date');

const numberUtils = require('utils/number_utils');

/**
 * Base class for any heatmap metric.
 * 
 * This class basically provides the following for each heatmap metric.
 * <ul>
 * <li> Provides number of slots in which temperature can fall.
 * <li> Maintains the maximum value so as to scale slot ranges.
 * <li> Gets JSON data from server and maps response for all hosts into above
 * slots.
 * </ul>
 * 
 */
App.MainChartHeatmapMetric = Em.Object.extend({
  /**
   * Name of this metric
   */
  name: null,

  /**
   * Number of slots this metric will be mapped into. When changing this value,
   * the color count in 'slotColors' should also be changed.
   */
  numberOfSlots: 5,

  /**
   * Colors for the each of the number of slots defined above. When changing the
   * number of slots, the number of colors also should be updated.
   * 
   * @type {Array}
   */
  slotColors: [
    '#1EB475',
    '#1FB418',
    '#E9E23F',
    '#E9A840',
    '#EF6162'
  ],

  /**
   * Minimum value of this metric. Default is 0.
   */
  minimumValue: 0,

  /**
   * Maximum value of this metric. This has to be specified by extending classes
   * so that the range from 'minimumValue' to 'maximumValue' can be split among
   * 'numberOfSlots'. It is recommended that this value be a multiple of
   * 'numberOfSlots'.
   */
  maximumValue: 100,

  /**
   * Units of the maximum value which is shown in UI {String}
   */
  units: '',


  /**
   * Provides following information about slots in an array of objects.
   * <ul>
   * <li> from: {number} Slot starts from this value
   * <li> to: {number} Slot ends at this value (inclusive)
   * <li> label: {String} Slot name to be shown
   * <li> cssStyle: {String} style to be embedded on hosts which fall into this
   * slot.
   * </ul>
   * 
   * Slot count will be the same as specified in 'numberOfSlots'. Slot
   * definitions will be given in increasing temperature from 'minimumValue' to
   * 'maximumValue'.
   * 
   */
  slotDefinitions: function () {
    var max = this.get('maximumValue') ? parseFloat(this.get('maximumValue')) : 0;
    var slotCount = this.get('numberOfSlots');
    var units = this.get('units');
    var delta = (max - this.get('minimumValue')) / slotCount;
    var defs = [];
    var slotColors = this.get('slotColors');
    var slotColorIndex = 0;
    for ( var c = 0; c < slotCount - 1; c++) {
      var slotColor = slotColors[slotColorIndex++];
      var start = (c * delta);
      var end = ((c + 1) * delta);
      defs.push(this.generateSlot(start, end, units, slotColor));
    }
    slotColor = slotColors[slotColorIndex++];
    start = ((slotCount - 1) * delta);
    defs.push(this.generateSlot(start, max, units, slotColor));

    defs.push(Em.Object.create({
      invalidData: true,
      index: defs.length,
      label: Em.I18n.t('charts.heatmap.label.invalidData'),
      cssStyle: this.getHatchStyle()
    }));
    defs.push(Em.Object.create({
      notAvailable: true,
      index: defs.length,
      label: Em.I18n.t('charts.heatmap.label.notAvailable'),
      cssStyle: "background-color: #666"
    }));
    defs.push(Em.Object.create({
      notApplicable: true,
      index: defs.length,
      label: Em.I18n.t('charts.heatmap.label.notApplicable'),
      cssStyle: "background-color:#ccc"
    }));
    return defs;
  }.property('minimumValue', 'maximumValue', 'numberOfSlots'),
  /**
   * genarate slot by given parameters
   *
   * @param min
   * @param max
   * @param units
   * @param slotColor
   * @return {object}
   * @method generateSlot
   */
  generateSlot: function (min, max, units, slotColor) {
    const fromLabel = this.formatLegendLabel(min, units);
    const toLabel = this.formatLegendLabel(max, units);
    const from = this.convertNumber(min, units);
    const to = this.convertNumber(max, units);

    return Em.Object.create({
      hasBoundaries: true,
      from: from,
      to: to,
      label: fromLabel + " - " + toLabel,
      cssStyle: "background-color:" + slotColor
    });
  },

  /**
   * calculate hatch style of slot according to browser version used
   * @return {String}
   */
  getHatchStyle: function () {
    var hatchStyle = "background-color:#666";
    if (jQuery.browser.webkit) {
      hatchStyle = "background-image:-webkit-repeating-linear-gradient(-45deg, #666, #666 6px, #fff 6px, #fff 7px)";
    } else if (jQuery.browser.mozilla) {
      hatchStyle = "background-image:repeating-linear-gradient(-45deg, #666, #666 6px, #fff 6px, #fff 7px)";
    } else if (jQuery.browser.msie && jQuery.browser.version) {
      var majorVersion = parseInt(jQuery.browser.version.split('.')[0]);
      if (majorVersion > 9) {
        hatchStyle = "background-image:repeating-linear-gradient(-45deg, #666, #666 6px, #fff 6px, #fff 7px)";
      }
    }
    return hatchStyle;
  },

  /**
   * In slot definitions this value is used to construct the label by appending
   * it to slot min-max values. For example giving '%' here would result in slot
   * definition label being '0% - 10%'.
   */
  slotDefinitionLabelSuffix: '',

  hostToValueMap: null,

  hostToSlotMap: function () {
    var hostToValueMap = this.get('hostToValueMap');
    var hostNames = this.get('hostNames');
    var hostToSlotMap = {};
    if (hostToValueMap && hostNames) {
      hostNames.forEach(function (hostName) {
        var slot = this.calculateSlot(hostToValueMap, hostName);
        if (slot > -1) {
          hostToSlotMap[hostName] = slot;
        }
      }, this);
    }
    return hostToSlotMap;
  }.property('hostToValueMap', 'slotDefinitions'),

  /**
   * calculate slot position in hostToSlotMap by hostname
   * @param hostToValueMap
   * @param hostName
   * @return {Number}
   */
  calculateSlot: function (hostToValueMap, hostName) {
    const slotDefinitions = this.get('slotDefinitions');
    const slotWithBoundaries = slotDefinitions.filterProperty('hasBoundaries');
    const invalidDataSlot = slotDefinitions.findProperty('invalidData').get('index');
    const notAvailableDataSlot = slotDefinitions.findProperty('notAvailable').get('index');
    let slot = slotDefinitions.findProperty('notApplicable').get('index');

    if (hostName in hostToValueMap) {
      let value = hostToValueMap[hostName];
      if (Em.isNone(value)) {
        slot = notAvailableDataSlot;
      } else if (isNaN(value) || !isFinite(value) || value < 0) {
        slot = invalidDataSlot;
      } else {
        value = Number(value);
        slotWithBoundaries.forEach((slotDef, slotIndex, array) => {

          if ((value >= slotDef.from && value <= slotDef.to) ||
            // If value exceeds maximum then it pushed to the last/maximal slot
            (value > slotDef.to && slotIndex === array.length - 1)) {
            slot = slotIndex;
          }
        });
      }
    }
    return slot;
  },

  /**
   * Turns numbers into displayable values. For example 24.345432425 into 24.3
   * etc.
   * 
   * @private
   */
  formatLegendLabel: function (num, units) {
    const fraction = num % 1;
    if (num >= 100) {
      num = Math.round(num);
    } else if (num >= 10 && fraction > 0) {
      num = parseFloat(num.toFixed(1));
    } else if (fraction > 0) {
      num = parseFloat(num.toFixed(2));
    }
    if (units === 'ms') {
      return date.timingFormat(num);
    }
    return num + units;
  },

  /**
   * return converted value
   *
   * @param {number} number
   * @param {string} units
   */
  convertNumber: function(number, units) {
    if (units === 'MB') {
      return Math.round(number * numberUtils.BYTES_IN_MB);
    }
    return number;
  }

});
