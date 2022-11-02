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

App.CreateScriptController = Ember.ObjectController.extend({
  needs:['pigScripts'],
  filePath:'',
  buttons: [
    {
      title: Em.I18n.t('common.cancel'),
      action: "cancel",
      classBindings:[':btn',':btn-default']
    },
    {
      title: Em.I18n.t('common.create'),
      action: "ok",
      classBindings:[':btn',':btn-success','isValid::disabled']
    }
  ],
  clearFilePath:function () {
    this.set('filePath','');
    this.set('titleErrorMessage','');
  }.observes('content'),
  actions:{
    confirm:function () {
      this.get('controllers.pigScripts').send('confirmcreate',this.get('content'),this.get('filePath'));
    },
    cancel:function (script) {
      this.get('content').deleteRecord();
    }
  },
  titleErrorMessage:'',
  clearAlert:function () {
    if (!this.get('content.isBlankTitle')) {
      this.set('titleErrorMessage','');
    }
  }.observes('content.title'),
  titleChange:function () {
    var target = this.get('targetObject');
    var message = (Ember.isBlank(target.get('content.title')))?Em.I18n.t('scripts.modal.error_empty_title'):'';

    target.set('titleErrorMessage',message);
  },
  isValid:function () {
    return !this.get('content.isBlankTitle');
  }.property('content.isBlankTitle')
});
