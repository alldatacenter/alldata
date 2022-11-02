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

App.ConfirmAwayController = Ember.ObjectController.extend({
  needs:['pig'],
  buttons: [
    {
      title: Em.I18n.t('common.cancel'),
      action: "cancel",
      classBindings:[':btn',':btn-default']
    },
    {
      title: Em.I18n.t('common.discard_changes'),
      action: "option",
      classBindings:[':btn',':btn-danger']
    },
    {
      title: Em.I18n.t('common.save'),
      action: "ok",
      classBindings:[':btn',':btn-success']
    }
  ],
  waitingTransition:null,
  actions:{
    confirm:function () {
      var transition = this.get('content');
      var script = this.get('controllers.pig.activeScript');
      this.get('controllers.pig').send('saveScript',script,this.saveCallback.bind(this));
      this.set('waitingTransition',transition);
    },
    discard:function () {
      var script = this.get('controllers.pig.activeScript');
      var filePromise = script.get('pigScript');
      var transition = this.get('content');
      this.set('waitingTransition',transition);
      filePromise.then(function (file) {
        script.rollback();
        file.rollback();
        this.get('waitingTransition').retry();
      }.bind(this));
    }
  },
  saveCallback:function(response){
    this.get('waitingTransition').retry();
    this.send('showAlert', {'message':Em.I18n.t('scripts.alert.script_saved',{title: response[1].get('title')}),status:'success'});
  }
});
