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

App.ScriptController = Em.ObjectController.extend({
  init: function(){
    this.pollster.set('target',this);
    this._super();
  },
  actions :{
    deactivateJob:function (jobId) {
      var rejected = this.get('activeJobs').rejectBy('id',jobId);
      this.set('activeJobs',rejected);
      if (!this.get('controllers.pig.activeScript')) {
        this.transitionToRoute('pig.history');
      }
      else if (this.get('activeTab') == jobId) {
        this.transitionToRoute('script.edit',this.get('controllers.pig.activeScript.id'));
      }
    },
    deleteJob:function (job) {
      job.deleteRecord();
      job.save().then(this.deleteJobSuccess.bind(this),Em.run.bind(this,this.deleteJobFailed,job));
      this.get('activeJobs').removeObject(job);
    }
  },

  deleteJobSuccess:function (data) {
    this.send('showAlert', {message:Em.I18n.t('job.alert.job_deleted'),status:'info'});
  },
  deleteJobFailed:function (job,error) {
    var trace = (error.responseJSON)?error.responseJSON.trace:null;
    job.rollback();
    this.send('showAlert', {message:Em.I18n.t('job.alert.delete_filed'),status:'error',trace:trace});
  },

  needs:['pig'],

  activeTab: 'script',

  isScript:true,

  activeJobs:Em.A(),
  activeJobsIds:Em.computed.mapBy('activeJobs','id'),
  activeScriptId:Em.computed.alias('controllers.pig.activeScript.id'),

  staticTabs:function () {
    return [
      {label:'Script',name:'script',url:'script.edit',target:this.get('activeScriptId')},
      {label:'History',name:'history',url:'script.history',target:this.get('activeScriptId')}
    ];
  }.property('activeScriptId'),


  jobTabs:function () {
    var jobTabs = [];
    this.get('activeJobs').forEach(function (job) {
      jobTabs.push({
        label:job.get('title') + ' - ' + job.get('status').decamelize().capitalize(),
        name:job.get('id'),
        url:'script.job',
        target:job.get('id')
      });
    });
    return jobTabs;
  }.property('activeJobs.[]','activeJobs.@each.status'),

  tabs:Em.computed.union('staticTabs','jobTabs'),

  pollster:Em.Object.createWithMixins(Ember.ActionHandler,{
    jobs:[],
    timer:null,
    start: function(jobs){
      this.stop();
      this.set('jobs',jobs);
      this.onPoll();
    },
    stop: function(){
      Em.run.cancel(this.get('timer'));
    },
    onPoll: function() {
      this.get('jobs').forEach(function (job) {
        if (job.get('jobInProgress')) {
          job.reload().catch(function (error) {
            this.send('showAlert',{
              message:Em.I18n.t('job.alert.delete_filed'),
              status:'error',
              trace:(error.responseJSON)?error.responseJSON.trace:null
            });
            this.jobs.removeObject(job);
          }.bind(this));
        } else {
          this.jobs.removeObject(job);
        }
      }.bind(this));

      if (this.get('jobs.length') > 0) {
        this.set('timer', Ember.run.later(this, function() {
          this.onPoll();
        }, 10000));
      }
    }
  }),

  pollingWatcher:function () {
    var pollster = this.get('pollster');
    if (this.get('activeJobs.length') > 0) {
      pollster.start(this.get('activeJobs').copy());
    } else {
      pollster.stop();
    }
  }.observes('activeJobs.@each'),

  activeJobsWatcher:function () {
    if (this.get('activeJobs.firstObject.scriptId') != this.get('controllers.pig.activeScriptId')) {
      this.set('activeJobs',[]);
    }
  }.observes('controllers.pig.activeScriptId')
});
