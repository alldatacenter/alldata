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

App.PluralizeView = Em.View.extend({
  tagName: 'span',
  template: Em.Handlebars.compile('{{view.wordOut}}'),

  wordOut: function() {
    var count, singular, plural;
    count = this.get('content');
    singular = this.get('singular');
    plural = this.get('plural');
    return this.getWord(count, singular, plural);
  }.property('content'),
  /**
   * Get computed word.
   *
   * @param {Number} count
   * @param {String} singular
   * @param {String} [plural]
   * @return {String}
   * @method getWord
   */
  getWord: function(count, singular, plural) {
    singular = this.parseValue(singular);
    // if plural not passed
    if (!plural) plural = singular + 's';
    else plural = this.parseValue(plural);
    if (singular && plural) {
      if (count == 1) {
        return singular;
      } else {
        return plural;
      }
    }
    return '';
  },
  /**
   * Detect and return value from its instance.
   *
   * @param {String} value
   * @return {*}
   * @method parseValue
   **/
  parseValue: function(value) {
    switch (value[0]) {
      case '@':
        value = this.getViewPropertyValue(value);
        break;
      case 't':
        value = this.tDetect(value);
        break;
      default:
        break;
    }
    return value;
  },
  /*
   * Detect for Em.I18n.t reference call
   * @params word {String}
   * return {String}
   */
  tDetect: function(word) {
    var splitted = word.split(':');
    if (splitted.length > 1 && splitted[0] == 't') {
      return Em.I18n.t(splitted[1]);
    } else {
      return splitted[0];
    }
  },
  /**
   * Get property value from view|controller by its key path.
   *
   * @param {String} value - key path
   * @return {*}
   * @method getViewPropertyValue
   **/
  getViewPropertyValue: function(value) {
    value = value.substr(1);
    var keyword = value.split('.')[0]; // return 'controller' or 'view'
    switch (keyword) {
      case 'controller':
        return Em.get(this, value);
      case 'view':
        return Em.get(this, value.replace(/^view/, 'parentView'));
      default:
        break;
    }
  }
});