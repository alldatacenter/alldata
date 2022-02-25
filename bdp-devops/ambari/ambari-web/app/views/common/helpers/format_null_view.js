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

App.FormatNullView = Em.View.extend({
  tagName: 'span',
  template: Em.Handlebars.compile('{{view.result}}'),

  result: function() {
    var emptyValue = this.get('empty') ? this.get('empty') : Em.I18n.t('services.service.summary.notAvailable');
    emptyValue = emptyValue.startsWith('t:') ? Em.I18n.t(emptyValue.substr(2, emptyValue.length)) : emptyValue;
    return (this.get('content') || this.get('content') == 0) ? this.get('content') : emptyValue;
  }.property('content')
});