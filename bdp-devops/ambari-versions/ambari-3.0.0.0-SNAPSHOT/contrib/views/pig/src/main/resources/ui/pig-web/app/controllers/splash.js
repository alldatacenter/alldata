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
"use strict";

var App = require('app');

App.SplashController = Ember.ObjectController.extend({
  actions: {
    toggleStackTrace:function () {
      var value = this.get('isExpanded');
      this.set('isExpanded', !value);
    }
  },
  isExpanded: false,

  errors: "",
  stackTrace: "",
  startTests: function(model) {
    var url = App.getNamespaceUrl() + '/resources/pig/help/';
    var self = this;
    var processResponse = function(name, data) {

      if( data != undefined ){
          data = data;
      } else {
        data = Ember.Object.create( {trace: null, message: "Server Error", status: "500"});
      }

      model.set(name + 'Test', data.status == 200);

      if (data.status != 200) {
        var checkFailedMessage = "Service '" + name + "' check failed";
        var errors = self.get("errors");
        errors += checkFailedMessage;
        errors += (data.message)?(': <i>' + data.message + '</i><br>'):'<br>';
        self.set("errors", errors);
      }

      if (data.trace != null) {
        var stackTrace = self.get("stackTrace");
        stackTrace += checkFailedMessage + ':\n' + data.trace;
        self.set("stackTrace", stackTrace);
      }
      model.set(name + 'TestDone', true);
      var percent = model.get('percent');
      model.set('percent', percent + (100/model.get("numberOfChecks")));
    };

    var checks = [];
    if(model.get("serviceCheckPolicy").checkHdfs){
      checks.push("hdfs");
    }else{
      model.set("hdfs" + 'TestDone', true);
      model.set("hdfs" + 'Test', true);
    }
    if(model.get("serviceCheckPolicy").checkStorage){
      checks.push("storage");
    }else{
      model.set("storage" + 'TestDone', true);
      model.set("storage" + 'Test', true);
    }

    if(model.get("serviceCheckPolicy").checkWebhcat){
      checks.push("webhcat");
    }else{
      model.set("webhcat" + 'TestDone', true);
      model.set("webhcat" + 'Test', true);
    }

    if(model.get("serviceCheckPolicy").checkHomeDirectory){
      checks.push("userhome");
    }else{
      model.set("userhome" + 'TestDone', true);
      model.set("userhome" + 'Test', true);
    }

    var promises = checks.map(function(name) {
      return Ember.$.getJSON('/' + url + name + 'Status')
               .then(
                 function(data) {
                   processResponse(name, data);
                 },
                 function(reason) {
                   processResponse(name, reason.responseJSON);
                 }
               );
    });

    return Ember.RSVP.all(promises);
  },
  fetchServiceCheckPolicy: function(){
    var url = App.getNamespaceUrl() + '/resources/pig/help/';

    return Ember.$.getJSON('/' + url + "service-check-policy");
  },

  progressBarStyle: function() {
    return 'width: ' + this.get("model").get("percent") + '%;';
  }.property("model.percent"),

  allTestsCompleted: function(){
    return this.get("model").get("hdfsTestDone") && this.get("model").get("webhcatTestDone") && this.get("model").get("storageTestDone") && this.get("model").get("userhomeTestDone");
  }.property('model.hdfsTestDone', 'model.webhcatTestDone', 'model.storageTestDone', 'model.userhomeTestDone')

});
