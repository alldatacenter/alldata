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
  history: Ember.inject.service(),
  queryParams: {
      jobType: { refreshModel: true },
      id: { refreshModel: true },
      fromBundleId: {refreshModel: true},
      fromCoordId: {refreshModel: true}
  },
  fromBundleId: null,
  fromCoordId: null,
  getJobInfo (url){
    var deferred = Ember.RSVP.defer();
    Ember.$.get(url).done(function(res){
      deferred.resolve(res);
    }).fail(function(){
      deferred.reject();
    });
    return deferred.promise;
  },
  model : function(params){
    return this.getJobInfo(Ember.ENV.API_URL+'/v2/job/'+params.id+'?show=info&timezone=GMT&offset=1&len='+1000).catch(function(){
        return {error : "Remote API Failed"};
      }).then(function(response){
      if (typeof response === "string") {
          response = JSON.parse(response);
      }
      response.jobType = params.jobType;
      return response;
    });
  },
  afterModel : function (model, transition){
    if(transition.queryParams.fromBundleId){
      this.set('fromBundleId', transition.queryParams.fromBundleId);
    }else{
      this.set('fromBundleId', null);
    }
    if(transition.queryParams.fromCoordId){
      this.set('fromCoordId', transition.queryParams.fromCoordId);
    }else{
      this.set('fromCoordId', null);
    }
  },
  actions : {
    didTransition (){
      if (this.get('fromBundleId')) {
        this.controller.set('fromBundleId', this.get('fromBundleId'));
      }
      if (this.get('fromCoordId')) {
        this.controller.set('fromCoordId',this.get('fromCoordId'));
      }
    },
    onTabChange : function(tab){
      this.set('currentTab', tab);
      this.controller.set('currentTab',tab);
    },
    backToSearch : function(){
      var params = this.get('history').getSearchParams();
      if(null != params){
        this.transitionTo('design.dashboardtab', {
            queryParams: {
                jobType: params.type,
                start: params.start,
                end: params.end,
                filter: params.filter
            }
        });
      }else{
        this.transitionTo('design.dashboardtab');
      }
    },
    doRefresh : function(){
      this.refresh();
    }
  }
});
