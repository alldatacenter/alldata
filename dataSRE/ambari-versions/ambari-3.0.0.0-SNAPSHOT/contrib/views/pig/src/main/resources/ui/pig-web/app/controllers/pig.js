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

App.PigController = Em.ArrayController.extend({
  needs:['scriptEdit'],
  category: 'scripts',
  navs: [
    {name:'scripts',url:'pig',label: Em.I18n.t('scripts.scripts'),icon:'fa-file-code-o'},
    {name:'udfs',url:'pig.udfs',label:Em.I18n.t('udfs.udfs'),icon:'fa-plug'},
    {name:'history',url:'pig.history',label:Em.I18n.t('common.history'),icon:'fa-clock-o'}
  ],
  actions:{
    closeScript:function () {
      this.transitionToRoute('pig');
    },
    saveScript: function (script,onSuccessCallback) {
      return script.get('pigScript').then(function (file){
        return Ember.RSVP.all([file.save(),script.save()]);
      }).then(onSuccessCallback || Em.run.bind(this,'saveScriptSuccess'), Em.run.bind(this,'saveScriptFailed'));
    },
    deletescript:function (script) {
      return this.send('openModal','confirmDelete',script);
    },
    confirmdelete:function (script) {
      script.deleteRecord();
      return script.save().then(Em.run.bind(this,'deleteScriptSuccess'),Em.run.bind(this,'deleteScriptFailed'));
    },
    copyScript:function (script) {
      var newScript = this.store.createRecord('script',{
        title:script.get('title')+' (copy)',
        templetonArguments:script.get('templetonArguments')
      });
      newScript.save().then(function (savedScript) {
        return Em.RSVP.all([savedScript.get('pigScript'),script.get('pigScript.fileContent')]);
      }).then(function (data) {
        return data.objectAt(0).set('fileContent',data.objectAt(1)).save();
      }).then(function () {
        this.send('showAlert', {'message':script.get('title') + ' is copied.',status:'success'});
        if (this.get('activeScript')) {
          this.send('openModal','gotoCopy',newScript);
        }
      }.bind(this));
    }
  },

  activeScriptId:null,

  disableScriptControls:Em.computed.alias('controllers.scriptEdit.isRenaming'),

  activeScript:function () {
    return (this.get('activeScriptId'))?this.get('content').findBy('id',this.get('activeScriptId').toString()):null;
  }.property('activeScriptId',"content.[]"),

  /*
   *Is script or script file is dirty.
   * @return {boolean}
   */
  scriptDirty:function () {
    return this.get('activeScript.isDirty') || this.get('activeScript.pigScript.isDirty');
  }.property('activeScript.pigScript.isDirty','activeScript.isDirty'),

  saveEnabled:function () {
    return this.get('scriptDirty') && !this.get('disableScriptControls');
  }.property('scriptDirty','disableScriptControls'),

  saveScriptSuccess: function (script) {
    this.send('showAlert', {
      'message': Em.I18n.t('scripts.alert.script_saved', { 'title' : script.get('title') } ),
      'status': 'success'
    });
  },

  saveScriptFailed: function (error) {
    this.send('showAlert', {
      'message': Em.I18n.t('scripts.alert.save_error'),
      'status': 'error',
      'trace': (error && error.responseJSON.trace) ? error.responseJSON.trace : null
    });
  },

  deleteScriptSuccess: function (script) {
    this.transitionToRoute('pig.scripts');
    this.send('showAlert', {
      'message': Em.I18n.t('scripts.alert.script_deleted', { 'title': script.get('title') } ),
      'status': 'success'
    });
  },

  deleteScriptFailed: function (error) {
    this.send('showAlert', {
      'message': Em.I18n.t('scripts.alert.delete_failed'),
      'status': 'error',
      'trace': (error && error.responseJSON.trace) ? error.responseJSON.trace : null
    });
  }
});
