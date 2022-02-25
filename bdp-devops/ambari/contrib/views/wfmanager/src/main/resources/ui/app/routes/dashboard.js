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
  errorMessage : "Error",
  queryParams: {
    jobType: { refreshModel: true },
    start: { refreshModel: true },
    filter: { refreshModel: true }
  },
  actions: {
    onShowJobDetails : function(params){
      this.transitionTo('job',{
        queryParams : {
          id : params.id,
          jobType : params.type,
          fromBundleId: null,
          fromCoordId: null
        }
      });
    },
    loading: function ( /*transition, originRoute*/ ) {
      Ember.$("#loading").css("display", "block");
      var app = this.controllerFor('application');
      if (app.get('currentRouteName') !== "dashboard") {
        return true;
      }
      return false;
    },
    error: function() {

    },
    doSearch (params){
      this.get('history').setSearchParams(params);
      Ember.$("#loading").css("display", "block");
      this.search(params);
    }
  },
  fetchJobs (url){
    var deferred = Ember.RSVP.defer();
    Ember.$.get(url).done(function(res){
      deferred.resolve(res);
    }).fail(function(){
      deferred.reject();
    });
    return deferred.promise;
  },
  setPageResultLen(){
    /* 
      setting the no of jobs to be displayed to multiple of 5
    */
    var relHeight = parseInt(Ember.$(window).width()/100); 
    return relHeight - relHeight%5;
  },
  search(params){
    params = params || {};
    var type = params.type || "wf",
    start = Number(params.start || 1),
    //len = Number(params.len || Ember.ENV.PAGE_SIZE),
    len = this.setPageResultLen(),
    index = 0,
    filter = params.filter || "",
    API_URL = Ember.ENV.API_URL,
    url = [API_URL,
      "/v2/jobs?jobtype=", type,
      "&offset=", start,
      "&len=", len,
      "&filter=", filter
    ].join(""),
    page = (start - 1) / len + 1;
    return this.fetchJobs(url).catch(function(){
      this.controllerFor('dashboard').set('model',{error : "Remote API Failed"});
      Ember.$("#loading").css("display", "none");
    }.bind(this)).then(function (res) {
      if(!res){
        return;
      }
      if (typeof res === "string") {
        res = JSON.parse(res);
      }
      res.jobs = [];

      if (res.workflows) {
        res.areWfs = true;
        res.type = "wf";
        res.workflows.forEach(function (job) {
          job.type = "wf";
          res.jobs.push(job);
        });
      }
      if (res.coordinatorjobs) {
        res.areCoords = true;
        res.type = "coords";
        res.coordinatorjobs.forEach(function (job) {
          job.type = "coordinatorjobs";
          job.id = job.coordJobId;
          job.appName = job.coordJobName;
          res.jobs.push(job);
        });
      }
      if (res.bundlejobs) {
        res.areBundles = true;
        res.type = "bundles";
        res.bundlejobs.forEach(function (job) {
          job.type = "bundlejobs";
          job.id = job.bundleJobId;
          job.appName = job.bundleJobName;
          res.jobs.push(job);
        });
      }
      res.pageSize = len;
      res.pages = [];

      while (index++ < (res.total / len)) {
        res.pages.push({ index: index, active: page === index });
      }

      res.jobTypeValue = type;
      res.filterValue = filter;
      res.pageSize = len;
      res.totalValue = res.total;
      res.page = page;
      res.start = start;
      res.end = (start + res.jobs.length - 1);
      res.time = new Date().getTime();
      this.controllerFor('dashboard').set('model', res);
      Ember.$("#loading").css("display", "none");
      return res;
    }.bind(this));
  },
  afterModel: function (model) {
    Ember.$("#loading").css("display", "none");
  },
  model: function (params) {

  }
});
