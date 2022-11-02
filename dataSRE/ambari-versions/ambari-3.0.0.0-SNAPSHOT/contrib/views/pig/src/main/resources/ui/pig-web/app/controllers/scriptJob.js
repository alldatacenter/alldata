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

App.ScriptJobController = Em.ObjectController.extend(App.FileHandler,{
  fullscreen:false,
  scriptContents:function () {
    var job = this.get('content'),
        controller = this,
        promise = new Ember.RSVP.Promise(function (resolve,reject){
          var file = (job.get('jobType') !== 'explain') ? job.get('pigScript') : job.store.find('file',[job.get('statusDir'),'source.pig'].join('/'));

          return file.then(function (data) {
            resolve(data);
          },function (error) {
            var response = (error.responseJSON)?error.responseJSON:{};
            reject(response.message);
            if (error.status != 404) {
              controller.send('showAlert', {'message': Em.I18n.t('job.alert.promise_error',
                {status:response.status, message:response.message}), status:'error', trace: response.trace});
            }
          });
        });
    return Ember.ObjectProxy.extend(Ember.PromiseProxyMixin).create({
      promise: promise
    });
  }.property('content'),



  jobResultsHidden:true,

  jobResults:function (output) {
    var jobId = this.get('content.id');
    var url = ['jobs', jobId, 'results', 'stdout'].join('/');

    return this.fileProxy(url);
  }.property('content'),

  showJobResults:function () {
    if (!Em.isEmpty(this.get('jobResults.content.fileContent'))) {
      this.set('jobResultsHidden',false);
    }
  }.observes('jobResults.content.fileContent'),

  jobLogsHidden:true,

  jobLogs:function (output) {
    var jobId = this.get('content.id');
    var url = ['jobs', jobId, 'results', 'stderr'].join('/');

    return this.fileProxy(url);
  }.property('content'),

  showJobLogs:function () {
    if (!Em.isEmpty(this.get('jobLogs.content.fileContent')) && this.get('jobResults.isFulfilled') && Em.isEmpty(this.get('jobResults.content.fileContent'))) {
      this.set('jobLogsHidden',false);
    }
  }.observes('jobLogs.content.fileContent','jobResults.isFulfilled','jobResults.content.fileContent'),

  hasErrorInLogs:false,

  resetLogsErrors:function () {
    if (this.get('jobLogs.isPending')) this.set('hasErrorInLogs',false);
  }.observes('jobLogs.isPending'),

  suggestedFilenamePrefix: function() {
    return this.get("content.jobId").toLowerCase().replace(/\W+/g, "_");
  }.property("content.jobId"),

  reloadOutputs: function(){
    Em.run.later(this,function () {
      if (this.get('content.jobInProgress')) {
        Em.run.debounce(this,'notifyPropertyChange','content',5000);
      };
    },10000);
   }.observes('content'),

  actions:{
    download:function (opt) {
      var file = (opt == 'results')?'jobResults.content.fileContent':'jobLogs.content.fileContent';
      var suffix = (opt == 'results')?'_results.txt':'_logs.txt';
      return this.downloadFile(this.get(file), this.get("suggestedFilenamePrefix")+suffix);
    },
    fullscreen:function () {
      this.toggleProperty('fullscreen');
    }
  }
});
