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

App.ScriptRoute = Em.Route.extend({
  actions:{
    willTransition:function (transition) {
      if (this.controllerFor('script.edit').get('isExec')) {
        return transition.abort();
      }
      if (this.controllerFor('script.edit').get('isRenaming')) {
        this.controllerFor('script.edit').set('titleWarn',true);
        return transition.abort();
      }
      var isAway = this.isGoingAway(transition);
      var scriptDirty = this.controllerFor('pig').get('scriptDirty');
      if (isAway && scriptDirty) {
        transition.abort();
        this.send('openModal','confirmAway',transition);
      }
    },
    error:function (error) {
      var msg, trace = (error && error.responseJSON.trace)?error.responseJSON.trace:null;
      if (error.status = 404) {
        this.store.all('script').filterBy('isLoaded',false).forEach(function (notLoaded) {
          notLoaded.unloadRecord();
        });
        msg = Em.I18n.t('scripts.not_found');
      } else {
        msg = Em.I18n.t('scripts.load_error_single');
      }
      this.send('showAlert', {'message': msg, status:'error', trace:trace});
      this.transitionTo('pig');
    }
  },
  enter:function () {
    this.controllerFor('pig').set('category', '');
  },
  deactivate: function() {
    this.controllerFor('pig').set('activeScriptId', null);
    this.controllerFor('script').set('activeJobs',[]);
  },
  isGoingAway:function (transition) {
    var isScriptAway = !transition.targetName.match(/^script./);
    if (!isScriptAway) {
      var targetParams = transition.params[transition.targetName];
      if (targetParams['script_id']) {
        return targetParams['script_id'] != this.controllerFor('pig').get('activeScriptId');
      }
      if (targetParams['job_id'] && this.modelFor('script.history')) {
        return this.modelFor('script.history').get('content').filterBy('id',targetParams['job_id']).length == 0;
      }
    }
    return isScriptAway;
  }
});
