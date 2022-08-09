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

module.exports = {

  pad: function(str, len, pad, dir) {

    var STR_PAD_LEFT = 1;
    var STR_PAD_RIGHT = 2;
    var STR_PAD_BOTH = 3;

    if (typeof(len) == "undefined") { len = 0; }
    if (typeof(pad) == "undefined") { pad = ' '; }
    if (typeof(dir) == "undefined") { dir = STR_PAD_RIGHT; }

    if (len + 1 >= str.length) {

      switch (dir){

        case STR_PAD_LEFT:
          str = Array(len + 1 - str.length).join(pad) + str;
          break;

        case STR_PAD_BOTH:
          var padlen = len - str.length;
          var right = Math.ceil((padlen) / 2);
          var left = padlen - right;
          str = Array(left+1).join(pad) + str + Array(right+1).join(pad);
          break;

        default:
          str = str + Array(len + 1 - str.length).join(pad);
          break;

      } // switch

    }
    return str;

  },
  underScoreToCamelCase: function(name){
    function replacer(str, p1, p2, offset, s) {
      return str[1].toUpperCase();
    }
    return name.replace(/_\w/g,replacer);
  },

  /**
   * Forces given string into upper camel-case representation. The first
   * character of each word will be capitalized with the rest in lower case.
   */
  getCamelCase : function(name) {
    if (name != null) {
      return name.toLowerCase().replace(/(\b\w)/g, function(f) {
        return f.toUpperCase();
      })
    }
    return name;
  },

  /**
   * Compare two versions by following rules:
   * first higher than second then return 1
   * first lower than second then return -1
   * first equal to second then return 0
   * @param first {string}
   * @param second {string}
   * @return {number}
   */
  compareVersions: function (first, second) {
    if (!(typeof first === 'string' && typeof second === 'string')) {
      return -1;
    }
    if (first === '' || second === '') {
      return -1;
    }
    var firstNumbers = first.split(/[\.-]/);
    var secondNumbers = second.split(/[\.-]/);
    var length = 0;
    var i = 0;
    var result = false;
    if (firstNumbers.length === secondNumbers.length) {
      length = firstNumbers.length;
    } else if (firstNumbers.length < secondNumbers.length) {
      length = secondNumbers.length;
    } else {
      length = firstNumbers.length;
    }

    while (i < length && !result) {
      firstNumbers[i] = (firstNumbers[i] === undefined) ? 0 : window.parseInt(firstNumbers[i]);
      secondNumbers[i] = (secondNumbers[i] === undefined) ? 0 : window.parseInt(secondNumbers[i]);
      if (firstNumbers[i] > secondNumbers[i]) {
        result = 1;
        break;
      } else if (firstNumbers[i] === secondNumbers[i]) {
        result = 0;
      } else if (firstNumbers[i] < secondNumbers[i]) {
        result = -1;
        break;
      }
      i++;
    }
    return result;
  },

  isSingleLine: function(string){
    return String(string).trim().indexOf("\n") == -1;
  },
  /**
   * transform array of objects into CSV format content
   * @param array
   * @return {Array}
   */
  arrayToCSV: function(array){
    var content = "";
    array.forEach(function(item){
      var row = [];
      for(var i in item){
        if(item.hasOwnProperty(i)){
          row.push(item[i]);
        }
      }
      content += row.join(',') + '\n';
    });
    return content;
  },

  /**
   * Extracts filename from linux/unix path
   * @param path
   * @return {string}: filename
   */
  getFileFromPath: function(path) {
    if (!path || typeof path !== 'string') {
      return '';
    }
    return path.replace(/^.*[\/]/, '');
  },

  getPath: function(path) {
    if (!path || typeof path !== 'string' || path[0] != '/') {
      return '';
    }
    var last_slash = path.lastIndexOf('/');
    return (last_slash!=0)?path.substr(0,last_slash):'/';
  },

  /**
   * @method getFormattedStringFromArray Get formatted string of elements to display on the UI
   * Example:
   * var arr = [ambari, bigdata, hadoop]
   * getFormattedStringFromArray(arr);  // ambari, bigdata and hadoop
   * @param array {Array}  Array of elements
   * @param [endSeparator=Em.I18n.t('and')] {String}
   * @returns {String}
   */
  getFormattedStringFromArray: function (array, endSeparator) {
    var label = '';
    endSeparator = endSeparator || Em.I18n.t('and');
    array.forEach(function (_arrElement) {
      if (array.length === 1) {
        label = _arrElement;
      }
      else {
        if (_arrElement !== array[array.length - 1]) {           // [clients.length - 1]
          label = label + ' ' + _arrElement;
          if (_arrElement !== array[array.length - 2]) {
            label = label + ',';
          }
        }
        else {
          label = label + ' ' + endSeparator + ' ' + _arrElement;
        }
      }
    }, this);
    return label.trim();
  },
  /**
   * Get plural|singular value of string by related count.
   *
   * @param {Number} count
   * @param {String} singular
   * @param {String} [plural]
   * @return {String}
   * @method pluralize
   */
  pluralize: function(count, singular, plural) {
    var _plural = plural || pluralize(singular);
    return count > 1 ? _plural : singular;
  },

  /**
   * decode html entities
   * @param {string} string
   * @returns {string}
   */
  htmlEntities: function (string) {
    if (typeof string !== 'string') return "";
    return $("<div/>").text(string).html();
  },

  /**
   * Escaping user input to be treated as a literal string within a regular expression
   * get from https://developer.mozilla.org/en/docs/Web/JavaScript/Guide/Regular_Expressions
   * @param {string} str
   * @returns {*}
   */
  escapeRegExp: function (str) {
    return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  },

  /**
   * Generates random string using upper and lower letters and digits
   *
   * @param {number} len
   * @param {String} [allowed]
   * @returns {String}
   * @method getRandomString
   */
  getRandomString: function(len, allowed) {
    Em.assert('len should be defined and more than 0', len > 0);
    var text = '';
    allowed = typeof allowed === 'string' ? allowed : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    for( var i=0; i < len; i++ ) {
      text += allowed.charAt(Math.floor(Math.random() * allowed.length));
    }
    return text;
  },

  /**
   * @param {string} string
   * @returns {string}
   * @method upperUnderscoreToText
   */
  upperUnderscoreToText: function(string) {
    if (typeof(string) !== 'string') {
      return '';
    }
    return string.split('_').map(function(word) {
      return word.toLowerCase().capitalize();
    }).join(' ');
  },

  /**
   *
   * @param {string} string
   * @param {RegExp} regexp
   */
  unicodeEscape: function (string, regexp = /[\s\S]/g) {
    return string.replace(regexp, function (escape) {
      return '\\u' + ('0000' + escape.charCodeAt().toString(16)).slice(-4);
    });
  }
};
