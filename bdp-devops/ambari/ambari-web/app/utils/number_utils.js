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
module.exports = {

  /**
   * @const
   */
  BYTES_IN_MB: 1024 * 1024,

  /**
   * Convert byte size to other metrics.
   *
   * @param {Number} bytes to convert to string
   * @param {Number} precision Number to adjust precision of return value. Default is 0.
   * @param {String} parseType
   *           JS method name for parse string to number. Default is "parseInt".
   * @param {Number} multiplyBy bytes by this number if given. This is needed
   *          as <code>null * 1024 = 0</null>
   * @remarks The parseType argument can be "parseInt" or "parseFloat".
   * @return {String} Returns converted value with abbreviation.
   */
  bytesToSize: function (bytes, precision, parseType, multiplyBy) {
    if (bytes === null || bytes === undefined) {
      return 'n/a';
    } else {
      if (arguments[2] === undefined) {
        parseType = 'parseInt';
      }
      if (arguments[3] === undefined) {
        multiplyBy = 1;
      }
      var value = bytes * multiplyBy;
      var sizes = [ 'Bytes', 'KB', 'MB', 'GB', 'TB', 'PB' ];
      var posttxt = 0;
      while (value >= 1024) {
        posttxt++;
        value = value / 1024;
      }
      if (value === 0) {
        precision = 0;
      }
      var parsedValue = window[parseType](value);
      return parsedValue.toFixed(precision) + " " + sizes[posttxt];
    }
  },

  /**
   * Validates if the given string or number is an integer between the
   * values of min and max (inclusive). The minimum and maximum
   * checks are ignored if their valid is NaN.
   *
   * @method validateInteger
   * @param {string|number} str - input string
   * @param {string|number} [min]
   * @param {string|number} [max]
   */
  validateInteger : function(str, min, max) {
    if (str==null || str==undefined || (str + "").trim().length < 1) {
      return Em.I18n.t('number.validate.empty');
    } else {
      str = (str + "").trim();
      var number = parseInt(str);
      if (isNaN(number)) {
        return Em.I18n.t('number.validate.notValidNumber');
      } else {
        if (str.length != (number + "").length) {
          // parseInt("1abc") returns 1 as integer
          return Em.I18n.t('number.validate.notValidNumber');
        }
        if (!isNaN(min) && number < min) {
          return Em.I18n.t('number.validate.lessThanMinimum').format(min);
        }
        if (!isNaN(max) && number > max) {
          return Em.I18n.t('number.validate.moreThanMaximum').format(max);
        }
      }
    }
    return null;
  },
  /**
   * @param {String|Number} cardinality - value to parse
   * @param {Boolean} isMax - return maximum count
   * @return {Number}
   **/
  getCardinalityValue: function(cardinality, isMax) {
    if (cardinality) {
      var isOptional = cardinality.toString().split('-').length > 1;
      if (isOptional) {
        return parseInt(cardinality.split('-')[(isMax) ? 1 : 0]);
      } else {
        if (isMax) return /^\d+\+/.test(cardinality) || cardinality == 'ALL' ? Infinity : parseInt(cardinality);
        return cardinality == 'ALL' ? Infinity : parseInt(cardinality.toString().replace('+', ''))
      }
    } else {
      return 0;
    }
  },

  getFloatDecimals: function (number, separator) {
    separator = separator || '.';

    var value = '' + number;
    var decimals = value.split(separator);

    decimals = decimals[1] ? decimals[1].length : 0;

    return decimals;
  },

  /**
   * @param n
   * @return {boolean}
   */
  isPositiveNumber: function(n) {
    var number = Number(n);
    return !isNaN(number) && isFinite(number) && (number > 0);
  }
};
