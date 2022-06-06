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

export default Ember.Controller.extend({
  actions: {
    launchDesign: function () {
      this.transitionToRoute('design');
    },
    doRefresh : function(){
      this.get('target.router').refresh();
    },
    onJobAction: function (params, deferred) {
      if (Ember.ENV.API_FAILED) {
        return { error: "Remote API Failed." };
      }
      var url = [Ember.ENV.API_URL,
        "/v2/job/", params.id, "?action=", params.action,'&user.name=oozie'
      ].join("");
      var jobActionParams = {
        url: url,
        method: 'PUT',
        beforeSend: function (xhr) {
          xhr.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
          xhr.setRequestHeader("X-Requested-By", "Ambari");
          if(params.action.indexOf('rerun') > -1){
            xhr.setRequestHeader("Content-Type","application/xml");
          }
        }
      };
      if(params.action.indexOf('rerun') > -1){
        jobActionParams.data = params.conf;
      }
      Ember.$.ajax(jobActionParams).done(function(response){
        deferred.resolve(response);
      }).fail(function(error){
        deferred.reject(error);
      });
    },
    onBulkAction : function(params, deferred){
      if (Ember.ENV.API_FAILED) {
        return { error: "Remote API Failed." };
      }
      var url = [Ember.ENV.API_URL,
        "/v2/jobs?jobtype=", params.jobType,
        "&offset=", params.start,
        "&len=", params.len,
        "&filter=", params.filter,
        "&action=", params.action,
        "&user.name=oozie"
      ].join("");
      Ember.$.ajax({
        url: url,
        method: 'PUT',
        beforeSend: function (xhr) {
          xhr.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
          xhr.setRequestHeader("X-Requested-By", "Ambari");
        }
      }).done(function(response){
        deferred.resolve(response);
      }).fail(function(response){
        deferred.reject(response);
      });
    }
  }
});
