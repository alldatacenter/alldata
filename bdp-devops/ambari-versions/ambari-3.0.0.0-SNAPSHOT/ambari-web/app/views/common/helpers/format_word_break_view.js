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

App.FormatWordBreakView = Em.View.extend({

  attributeBindings: ["data-original-title"],

  tagName: 'span',

  template: Em.Handlebars.compile('{{{view.result}}}'),

  _splitLongSubStrings: function (str) {
    var maxBlockLength = 20;
    if (str.length <= maxBlockLength || str.contains('-') || str.contains(' ')) {
      return str;
    }
    return str.split(/(?=[A-Z])/).join('<wbr>');
  },

  /**
   * @type {string}
   */
  result: function() {
    var content = this.get('content') || '';
    var self = this;
    ['.', '_', '/'].forEach(function (delimiter) {
      if (content.contains(delimiter)) {
        content = content.split(delimiter).map(function (substr) {
          return self._splitLongSubStrings(substr);
        }).join(delimiter + '<wbr>');
      }
    });
    return content.replace(/(<wbr>){2,}/g, '<wbr>'); // no need for double <wbr>
  }.property('content')
});