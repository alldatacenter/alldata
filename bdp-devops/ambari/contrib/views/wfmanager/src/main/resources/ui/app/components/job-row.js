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
  tagName: 'tr',
  targetParentId: Ember.computed('job', function() {
    var parentID = this.get('job').parentId;
    return parentID.split("@")[0];
  }),
  onRender : function(){
    this.$('#actions').hide();
  }.on('didInsertElement'),
  getJobDetails : function(id){
    var deferred = Ember.RSVP.defer();
    Ember.$.get(Ember.ENV.API_URL+'/v2/job/'+id+'?show=info&timezone=GMT&offset=1&len='+Ember.ENV.PAGE_SIZE).done(function(response){
      if (typeof response === "string") {
        response = JSON.parse(response);
      }
      deferred.resolve(response);
    }).fail(function(){
      deferred.reject();
    });
    return deferred.promise;
  },
  jobType : Ember.computed('job', function(){
    if(this.get('job').bundleJobId){
      return 'bundle';
    }else if(this.get('job').coordJobId){
      return 'coord';
    }else{
      return 'wf';
    }

  }),
  actions : {
    doAction(action, id) {
      this.set('showError', false);
      this.set('showLoader', true);
      var deferred = Ember.RSVP.defer();
      deferred.promise.then(function(){
        this.set('showLoader', false);
        if(action === 'start'){
          this.set('job.status','RUNNING');
        }else if(action === 'suspend' && this.get('job.status') === 'RUNNING'){
          this.set('job.status','SUSPENDED');
        }else if(action === 'suspend' && this.get('job.status') === 'PREP'){
          this.set('job.status','PREPSUSPENDED');
        }else if(action === 'resume' && this.get('job.status') === 'SUSPENDED'){
          this.set('job.status','RUNNING');
        }else if(action === 'resume' && this.get('job.status') === 'PREPSUSPENDED'){
          this.set('job.status','PREP');
        }else if(action === 'stop'){
          this.set('job.status','STOPPED');
        }else if(action === 'rerun'){
          this.set('job.status','RUNNING');
        }else if(action === 'kill'){
          this.set('job.status','KILLED');
        }
        this.sendAction('showMessage', {type:'success', message:`${action.toUpperCase()} action request complete. Job is ${this.get('job.status')}`});
      }.bind(this)).catch(function(e){
        this.set('showError', true);
        this.set('showLoader', false);
        var message = `${action.toUpperCase()} action for could not be completed`;
        if(this.get('userName') !== this.get('job.user')){
          message = `${message}. ${this.get('userName')} is not the job owner.`
        }
        this.sendAction('showMessage', {type:'error', message:message});
        console.error(e);
      }.bind(this));
      if(action === 'rerun' && this.get('job').bundleJobId){
        action = 'bundle-'+action;
      }else if(action === 'rerun' && this.get('job').coordJobId){
        action = 'coord-'+action;
      }
      var params = {id: id, action:action };
      if(action.indexOf('rerun') > -1){
        var jobDetailsPromise = this.getJobDetails(id);
        jobDetailsPromise.then(function(jobInfo){
          params.conf = jobInfo.conf;
          this.sendAction("onAction", params, deferred);
        }.bind(this));
      }else{
        this.sendAction("onAction", params, deferred);
      }
    },
    showJobDetails : function(jobId){
      this.sendAction('showJobDetails', jobId);
    },
    showActions () {
      this.$('#actions-div').hide();
      this.$('#actions').show();
    },
    hideActions () {
      this.$('#actions-div').show();
      this.$('#actions').hide();
    },
    rowSelected(){
      this.sendAction('rowSelected');
    }
  }
});
