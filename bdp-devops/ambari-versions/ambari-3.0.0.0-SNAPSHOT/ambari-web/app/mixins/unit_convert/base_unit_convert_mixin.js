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

App.BaseUnitConvertMixin = Em.Mixin.create({
  /**
   * Which type of units should be used e.g. size, time.
   * Conversion dimensions will be used according this type.
   * For more info regarding dimension table @see convertMapTable property.
   *
   * This property can be used in mixed object to specify which dimension table map will
   * be used for conversion. If type is not specified we try to find correct table by input unit type.
   *
   * Note: You should specify dimension for `percentage` conversion with specified property unit type.
   *
   * @property currentDimensionType
   * @type {String}
   */
  currentDimensionType: null,

  units: ['b', 'kb', 'mb', 'gb', 'tb', 'pb'],

  /**
   * Labels related to units. Specify desired display names here that not much actual unit name.
   */
  unitLabelMap: {
    percent: '%',
    int: '',
    float: ''
  },

  convertMapTable: Em.Object.create({
    size: {
      b: 1024,
      kb: 1024,
      mb: 1024,
      gb: 1024,
      tb: 1024,
      pb: 1024
    },
    time: {
      milliseconds: 1,
      seconds: 1000,
      minutes: 60,
      hours: 60,
      days: 24
    },
    /**
     * Percent dimension type should be specified directly through `currentDimensionType` property.
     * For example:
     * 'percent.percent_int' if widget `unit-name` is "percent" and config property `type` is "int"
     * 'percent.percent_float' if widget `unit-name` is "percent" and config property `type` is "float"
     */
    percent: {
      percent_int: {
        int: 1,
        percent: 1
      },
      percent_float: {
        float: 1,
        percent: 0.01
      }
    }
  }),

  /**
   * Convert value between different unit types. Conversion works for following directions:
   *  single unit -> single unit e.g. from "s" -> "s".
   *  single unit -> multi unit e.g. from "ms" -> "days,hours"
   *  multi unit -> single unit e.g. from "days,hours" -> "ms"
   * For single -> single unit conversion returned value will be as is or an array of objects if
   * isObjectOutput flag passed.
   * If any passed unit is multi dimensions like "days,hours" array of objects will return always.
   * For example:
   *  <code>
   *     convertValue(1024, 'kb', 'kb') // returns 1024
   *     convertValue(1024, 'kb', 'kb', true) // returns [{ type: 'kb', value: 1024 }]
   *     convertValue(60000, 'ms', 'hours,minutes') // returns [{type: 'hours', value: 0 }, {type: 'minutes', value: 1}]
   *  </code>
   * For more examples see unit tests.
   *
   * @method convertValue
   * @param {String|Number|Object[]} value - value to convert
   * @param {String|String[]} fromUnit - specified value unit type(s) e.g. "mb"
   *    Form multi dimensional format pass units separated with "," or list of unit types
   *    e.g. "gb,mb", ['gb', 'mb']
   * @param {Boolean} [isObjectOutput=false] - 'returned' value should be object this option useful for widgets where its
   *    value should be an Object
   * @param {String|String[]} toUnit - desired unit(s) to convert specified value. Same format as for `fromUnit`
   * @returns {Number|Object[]} returns single value or array of objects according to multi unit format
   */
  convertValue: function(value, fromUnit, toUnit, isObjectOutput) {
    var self = this;
    var valueIsArray = Em.isArray(value);
    if (!valueIsArray) {
      value = +value;
    }
    // if desired unit not passed or fromUnit and toUnit are the same
    // just return the value
    if ((fromUnit == toUnit || !toUnit) && !isObjectOutput) {
      return value;
    }
    if (isNaN(value) && !valueIsArray) {
      return null;
    }
    // convert passed toUnit string to array of units by ','
    // for example "mb,kb" to ['mb','kb']
    if (!Em.isArray(toUnit)) {
      toUnit = toUnit.split(',');
    }
    // process multi unit format
    if (toUnit.length > 1 || isObjectOutput) {
      // returned array has following structure
      //  .value - value according to unit
      //  .type - unit name
      return toUnit.map(function(unit) {
        var convertedValue = Math.floor(self._convertToSingleValue(value, fromUnit, unit));
        value -= self._convertToSingleValue(convertedValue, unit, fromUnit);
        return {
          value: convertedValue,
          type: unit
        };
      });
    }
    // for single unit format just return single value
    else {
      if (!valueIsArray) {
        return this._convertToSingleValue(value, fromUnit, toUnit[0]);
      }
      else {
        return value.map(function(item) {
          return self._convertToSingleValue(item.value, item.type, toUnit[0]);
        }).reduce(Em.sum, 0);
      }
    }
  },

  /**
   * Get dimension table map. `currentDimensionType` will be used if specified or
   * detected by property unit type.
   *
   * @method _converterGetUnitTable
   * @private
   * @param {String|String[]} unit - unit type
   * @return {Object}
   */
  _converterGetUnitTable: function(unit) {
    var unitType;
    if (this.get('currentDimensionType')) {
      unitType = this.get('currentDimensionType');
    }
    else {
      unitType = Em.keys(this.get('convertMapTable')).filter(function(item) {
        return Em.keys(this.get('convertMapTable.' + item)).contains(Em.isArray(unit) ? unit[0].toLowerCase() : unit.toLowerCase());
      }, this)[0];
    }
    return this.get('convertMapTable.' + unitType);
  },

  /**
   * @method _convertToSingleValue
   * @private
   * @param {Number} value - value to convert
   * @param {String} fromUnit - unit type for current value
   * @param {String} toUnit - desired unit type
   * @return {Number}
   */
  _convertToSingleValue: function(value, fromUnit, toUnit) {
    var convertTable = this._converterGetUnitTable(fromUnit);
    var units = Em.keys(convertTable);
    var fromUnitIndex = units.indexOf(fromUnit.toLowerCase());
    var toUnitIndex = units.indexOf(toUnit.toLowerCase());
    var isInt = function(val) {
      return parseInt(val) === val;
    };
    Em.assert("Invalid value unit type " + fromUnit, fromUnitIndex > -1);
    Em.assert("Invalid desired unit type " + toUnit, toUnitIndex > -1);
    if (fromUnitIndex == toUnitIndex) {
      return value;
    }
    var range = [fromUnitIndex + 1, toUnitIndex + 1];
    var processedUnits = Array.prototype.slice.apply(units, range[0] < range[1] ? range : range.slice().reverse());
    var factor = processedUnits.map(function(unit) {
      return Em.get(convertTable, unit);
    }, this).reduce(function(p,c) { return p*c; });
    if (range[0] < range[1]) {
      value /= factor;
    }
    else {
      value *= factor;
    }
    return isInt(value) ? value : parseFloat(value.toFixed(3));
  }

});
