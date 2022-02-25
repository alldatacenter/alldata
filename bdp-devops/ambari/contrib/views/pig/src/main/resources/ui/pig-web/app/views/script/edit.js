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

App.ScriptEditView = Em.View.extend({
  didInsertElement:function () {
    $('.file-path, .fullscreen-toggle').tooltip();
  },
  willClearRender:function () {
    this.set("controller.fullscreen", false);
  },
  showTitleWarn:function () {
    if (this.get('controller.titleWarn')) {
      this.$('#title').tooltip({
        trigger:"manual",
        placement:"bottom",
        title:Em.I18n.t('scripts.alert.rename_unfinished')
      }).tooltip('show');
    }
  }.observes('controller.titleWarn'),
  actions:{
    insertUdf:function (udf) {
      var code = this.get('controller.content.pigScript.fileContent'),
      registered = 'REGISTER ' + udf.get('path') + '\n' + code;
      this.set('controller.content.pigScript.fileContent',registered);
    }
  },
  focusInput:Em.TextField.extend({
    becomeFocused: function () {
      this.$().focus().val(this.$().val());
    }.on('didInsertElement'),
    cancel:function (argument) {
      this.sendAction('action','cancel');
    }
  })
});
