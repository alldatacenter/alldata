/*
*    Licensed to the Apache Software Foundation (ASF) under one or more
*    contributor license agreements.  See the NOTICE file distributed with
*    this work for additional information regarding copyright ownership.
*    The ASF licenses this file to You under the Apache License, Version 2.0
*    (the "License"); you may not use this file except in compliance with
*    the License.  You may obtain a copy of the License at
*
*        http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS,
*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*    See the License for the specific language governing permissions and
*    limitations under the License.
*/

import Ember from 'ember';

export default Ember.Component.extend({
  showBulkAction : false,
  history: Ember.inject.service(),
  userInfo : Ember.inject.service('user-info'),
  currentPage : Ember.computed('jobs.start',function(){
    if(Ember.isBlank(this.get('jobs.start'))){
      return 1;
    }
    var roundedStart = this.get('jobs.start') - this.get('jobs.start') % this.get('jobs.pageSize');
    return (roundedStart / this.get('jobs.pageSize'))+1;
  }),
  userName : Ember.computed.alias('userInfo.userName'),
  rendered : function(){
    this.sendAction('onSearch', this.get('history').getSearchParams());
  }.on('didInsertElement'),
  isUpdated : function(){
    if(this.get('showActionError')){
      this.$('#error-alert').fadeOut(5000, ()=>{
        this.set("showActionError", false);
      });
    }
    if(this.get('showActionSuccess')){
      this.$('#success-alert').fadeOut(5000, ()=>{
        this.set("showActionSuccess", false);
      });
    }
  }.on('didUpdate'),
  handleBulkActionResponse(response){
    if (typeof response === "string") {
      response = JSON.parse(response);
    }
    if(response.workflows){
      this.updateJobs(response.workflows, 'id');
    }else if(response.coordinatorjobs){
      this.updateJobs(response.coordinatorjobs, 'coordJobId');
    }else if(response.bundlejobs){
      this.updateJobs(response.bundlejobs, 'bundleJobId');
    }
  },
  updateJobs(jobsToUpdate, queryField){
    jobsToUpdate.forEach(job =>{
      var existing = this.get('jobs.jobs').findBy('id', job[queryField]);
      Ember.set(existing, 'status', job.status);
    });
  },
  actions: {
    selectAll() {
      this.$(".cbox").click();
      this.$('#ba_buttons').toggleClass('shown');
    },
    onAction(params, deferred) {
      this.sendAction("onAction", params, deferred);
    },
    refresh (){
      this.sendAction("doRefresh");
    },
    doBulkAction(action){
      this.set('showBulkActionLoader', true);
      var filter = '';
      var deferred = Ember.RSVP.defer();
      this.$('.cbox:checked').each(function(index, element){
        filter = filter + "id=" + this.$(element).attr('id')+ ";";
      }.bind(this));
      var params = {};
      params.action = action;
      params.filter = filter;
      params.jobType = this.get('jobs.jobTypeValue');
      params.start = this.get('jobs.start');
      params.len = this.get('jobs.pageSize');
      this.sendAction('onBulkAction', params, deferred);
      deferred.promise.then(function(response){
        this.set('showBulkActionLoader', false);
        this.handleBulkActionResponse(response);
        this.set('showBulkAction', false);
        this.send('showMessage', {type:'success', message:`${action.toUpperCase()} action request complete`});
      }.bind(this), function(){
        this.set('showBulkActionLoader', false);
        this.send('showMessage', {type:'error', message: `${action.toUpperCase()} action for could not be completed`});
      }.bind(this));
    },
    page: function (page) {
      var size = this.get('jobs.pageSize'),
      jobType = this.get('jobs.jobTypeValue'),
      filter = this.get('jobs.filterValue'),
      start = (size * (page - 1) + 1);
      start = start || 1;
      this.sendAction("onSearch", { type: jobType, start: start, filter: filter });
    },
    prev: function (page) {
      page = page - 1;
      var size = this.get('jobs.pageSize'),
      jobType = this.get('jobs.jobTypeValue'),
      filter = this.get('jobs.filterValue'),
      start = (size * (page - 1) + 1);

      if (page >= 0) {
        start = start || 1;
        this.sendAction("onSearch", { type: jobType, start: start, filter: filter });
      }
    },
    next: function (page) {
      page = page + 1;
      var size = this.get('jobs.pageSize'),
      jobType = this.get('jobs.jobTypeValue'),
      filter = this.get('jobs.filterValue'),
      total = this.get('jobs.totalValue'),
      start = (size * (page - 1) + 1);
      if (start < total) {
        start = start || 1;
        this.sendAction("onSearch", { type: jobType, start: start, filter: filter });
      }
      this.sendAction("onSearch", { type: jobType, start: start, filter: filter });
    },
    showJobDetails : function(jobId){
      this.sendAction('onShowJobDetails',{type:this.get('jobs.jobTypeValue'), id:jobId});
    },
    rowSelected : function(){
      if(this.$('.cbox:checked').length > 0){
        this.set('showBulkAction', true);
        var status = [];
        this.$('.cbox:checked').each((index, element)=>{
          status.push(this.$(element).attr('data-status'));
        }.bind(this));
        var isSame = status.every(function(value, idx, array){
          return idx === 0 || value === array[idx - 1];
        });
        if(isSame && status.get('firstObject') === 'SUSPENDED'){
          this.set('toggleResume', 'enabled');
          this.set('toggleStart', 'disabled');
          this.set('toggleSuspend', 'disabled');
        }else if(isSame && status.get('firstObject') === 'PREP'){
          this.set('toggleStart', 'enabled');
          this.set('toggleSuspend', 'disabled');
          this.set('toggleResume', 'disabled');
        }else if(isSame && status.get('firstObject') === 'RUNNING'){
          this.set('toggleSuspend', 'enabled');
          this.set('toggleResume', 'disabled');
          this.set('toggleStart', 'disabled');
        }else if(isSame && status.get('firstObject') === 'KILLED'){
          this.set('toggleKill', 'disabled');
          this.set('toggleSuspend', 'disabled');
          this.set('toggleResume', 'disabled');
          this.set('toggleStart', 'disabled');
        }
      }else{
        this.set('showBulkAction', false);
      }
    },
    showMessage(messageInfo){
      if(messageInfo.type === 'error'){
        this.set('showActionError', true);
        this.set('errorMessage', messageInfo.message);
      }else{
        this.set('showActionSuccess', true);
        this.set('successMessage', messageInfo.message);
      }
    }
  }
});
