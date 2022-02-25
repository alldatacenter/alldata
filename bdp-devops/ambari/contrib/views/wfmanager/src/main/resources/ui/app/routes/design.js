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
import SchemaVersions from '../domain/schema-versions';

export default Ember.Route.extend(Ember.Evented, {
  currentDraft: null,
  beforeModel: function(transition){
    this.set("xmlAppPath", transition.queryParams.appPath);
    this.controllerFor('design').set("xmlAppPath", transition.queryParams.appPath);
  },
  model(){
    if(!this.get('failedSchemaVersions')){
      return this.importAdminConfigs();
    }else{
      return [];
    }
  },
  afterModel(model){
    if(!this.get('failedSchemaVersions')){
      SchemaVersions.reopen({
        adminConfig : JSON.parse(model)
      });
    }
  },
  importAdminConfigs(){
    return new Ember.RSVP.Promise((resolve, reject) => {
      var url = Ember.ENV.API_URL + "/v1/admin/configuration";
      var deferred = Ember.$.ajax({
        url: url,
        method: "GET",
        dataType: "text",
        contentType: "text/plain;charset=utf-8",
        beforeSend: function(request) {
          request.setRequestHeader("X-Requested-By", "workflow-designer");
        },
        success : function(response){
          try {
            resolve(JSON.parse(response));
          } catch (e) {
            console.error(e);
            reject(response);
          }
        },
        error : function(response){
          reject(response);
        }
      });
    });
  },
  actions : {
    error(error, transition){
      SchemaVersions.reopen({
        useDefaultSettings : true
      });
      this.set('failedSchemaVersions', true);
      transition.retry();
    },
    editWorkflow(path, type, isImportedFromDesigner, configuration){
      this.trigger('openNewTab', path, type, isImportedFromDesigner, configuration);
    },
    showDashboard(){
      this.controller.set('dashboardShown', true);
      this.transitionTo('design.dashboardtab');
    },
    getAllRecentWorks(deferred){
      this.store.findAll("wfproject", { reload: true }).then((data)=>{
        deferred.resolve(data);
      }).catch((e)=>{
        deferred.reject(e);
      });
    },
    hideDashboard(){
      this.controller.set('dashboardShown', false);
      this.transitionTo('design');
    }
  }
});
