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

export default Ember.Route.extend({
    workflowManagerConfigs : Ember.inject.service('workflow-manager-configs'),
    afterModel(){
      let workflowManagerConfigsPromise = this.getWorkflowManagerConfigs();
      workflowManagerConfigsPromise.then(function(data){
        var jsonData = JSON.parse(data);
        this.get('workflowManagerConfigs').setWfmConfigs(jsonData);
        this.invokeServiceChecksPromises(jsonData);
      }.bind(this)).catch(function(errors){
        this.controllerFor('index').set('errors', errors);
      }.bind(this));
    },
    invokeServiceChecksPromises(workflowManagerConfigs) {
      let ooziePromise = this.checkOozie();
      let hdfsPromise = this.checkHdfs();
      this.controllerFor('index').get('issues').clear();
      let serviceChecks = this.controllerFor('index').get('serviceChecks');
      this.processServiceCheckPromise(ooziePromise, serviceChecks.findBy('name', 'oozie'));
      this.processServiceCheckPromise(hdfsPromise, serviceChecks.findBy('name', 'hdfs'));

      if (workflowManagerConfigs.checkHomeDir === 'true') {
        serviceChecks.pushObject({'name':'homeDir', 'checkCompleted':false, isAvailable : true, 'displayName' : 'User Home Directory Test', 'errorMessage' : 'User home directory not found'})
        let homeDirPromise = this.checkUserHome();
        this.processServiceCheckPromise(homeDirPromise, serviceChecks.findBy('name', 'homeDir'));
        Ember.RSVP.Promise.all([ooziePromise, hdfsPromise, homeDirPromise]).then(()=>{
          this.controllerFor('index').set('serviceChecksComplete', true);
          Ember.run.later(()=>{
            this.transitionTo('design');
        }, 2000);
        }).catch((errors)=>{
          this.controllerFor('index').set('serviceChecksComplete', true);
          this.controllerFor('index').set('errors', errors);
        });
      } else {
        Ember.RSVP.Promise.all([ooziePromise, hdfsPromise]).then(()=>{
          this.controllerFor('index').set('serviceChecksComplete', true);
          Ember.run.later(()=>{
            this.transitionTo('design');
        }, 2000);
        }).catch((errors)=>{
          this.controllerFor('index').set('serviceChecksComplete', true);
          this.controllerFor('index').set('errors', errors);
        });
      }
    },
    processServiceCheckPromise(promise, serviceCheck){
      promise.then(()=>{
        Ember.set(serviceCheck, 'isAvailable', true);
      }).catch((e)=>{
        console.error(e);
        Ember.set(serviceCheck, 'isAvailable', false);
        var response = typeof e.responseText === "string"? JSON.parse(e.responseText) : e.responseText;
        Ember.set(serviceCheck, 'stackTrace', response.stackTrace);
      }).finally(()=>{
        Ember.set(serviceCheck, 'checkCompleted', true);
      });
    },
    checkOozie(){
      return new Ember.RSVP.Promise((resolve, reject) => {
        var url = Ember.ENV.API_URL + "/v1/admin/configuration";
          Ember.$.ajax({
          url: url,
          method: "GET",
          dataType: "text",
          contentType: "text/plain;charset=utf-8",
          beforeSend: function(request) {
            request.setRequestHeader("X-Requested-By", "workflow-designer");
          },
          success : function(response){
            resolve(true);
          },
          error : function(response){
            reject(response);
          }
        });
      });
    },
    checkHdfs(){
      return new Ember.RSVP.Promise((resolve, reject) => {
        var url = Ember.ENV.API_URL + "/hdfsCheck";
          Ember.$.ajax({
          url: url,
          method: "GET",
          dataType: "text",
          contentType: "text/plain;charset=utf-8",
          beforeSend: function(request) {
            request.setRequestHeader("X-Requested-By", "workflow-designer");
          },
          success : function(response){
            resolve(true);
          },
          error : function(response){
            reject(response);
          }
        });
      });
    },
    checkUserHome(){
      return new Ember.RSVP.Promise((resolve, reject) => {
        var url = Ember.ENV.API_URL + "/homeDirCheck";
          Ember.$.ajax({
          url: url,
          method: "GET",
          dataType: "text",
          contentType: "text/plain;charset=utf-8",
          beforeSend: function(request) {
            request.setRequestHeader("X-Requested-By", "workflow-designer");
          },
          success : function(response){
            resolve(true);
          },
          error : function(response){
            reject(response);
          }
        });
      });
    },
    getWorkflowManagerConfigs(){
      return new Ember.RSVP.Promise((resolve, reject) => {
        var url = Ember.ENV.API_URL + "/getWorkflowManagerConfigs";
          Ember.$.ajax({
          url: url,
          method: "GET",
          dataType: "text",
          contentType: "text/plain;charset=utf-8",
          beforeSend: function(request) {
            request.setRequestHeader("X-Requested-By", "workflow-designer");
          },
          success : function(response){
            resolve(response);
          },
          error : function(response){
            reject(response);
          }
        });
      });
    },
    actions : {
      showDetails (check){
        Ember.set(check, 'showingDetails', !check.showingDetails);
      }
    }
});
