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

App.PigHistoryView = Em.View.extend({
  initTooltips:function () {
    if ( this.$('td:last-child a')) {
      Em.run.next(this.addTooltip.bind(this));
    }
  }.on('didInsertElement').observes('controller.page','controller.content.@each'),
  addTooltip:function () {
    this.$('td:last-child a').tooltip({placement:'bottom'});
  },
  scriptLink:Em.Component.extend({
    tagName:'a',
    classNames:['scriptLink'],
    classNameBindings:['hasScript::inactive'],
    action:'goToScript',
    hasScript:function () {
      var all = this.get('allIds'),
          current = (this.get('scriptId'))?this.get('scriptId').toString():'';
      return all.contains(current);
    }.property('scriptId'),
    click:function () {
      if (this.get('hasScript')) {
        this.sendAction('action',this.get('scriptId'));
      }
    }
  })
});
