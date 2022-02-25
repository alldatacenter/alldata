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

App.ScriptJobRoute = Em.Route.extend({
    actions: {
      error: function(error, transition) {
        Em.warn(error.stack);
        var trace = null;
        if (error && error.responseJSON.trace)
          trace = error.responseJSON.trace;
        transition.send('showAlert', {'message':Em.I18n.t('job.alert.load_error',{message:error.message}), status:'error', trace:trace});
        this.transitionTo('pig');
      },
      navigate:function (argument) {
        return this.transitionTo(argument.route)
      },
      killjob:function (job) {
        var self = this;
        job.kill(function () {
          job.reload();
          self.send('showAlert', {'message': Em.I18n.t('job.alert.job_killed',{title:self.get('title')}), status:'info'});
        },function (reason) {
          var trace = (reason && reason.responseJSON.trace)?reason.responseJSON.trace:null;
          self.send('showAlert', {'message': Em.I18n.t('job.alert.job_kill_error'), status:'error', trace:trace});
        });
      }
    },
    model:function (q,w) {
      return this.store.find('job',q.job_id);
    },
    setupController: function(controller, model) {
      controller.set('model', model);
    },
    afterModel:function (job) {
      this.controllerFor('pig').set('activeScriptId', job.get('scriptId'));
      this.controllerFor('script').get('activeJobs').addObject(job);
      this.controllerFor('script').set('activeTab',job.get('id'));
    }
});
