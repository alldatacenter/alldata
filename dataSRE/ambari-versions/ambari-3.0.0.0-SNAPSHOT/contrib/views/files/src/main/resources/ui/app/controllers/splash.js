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

import Ember from 'ember';

export default Ember.Controller.extend({
  store: Ember.inject.service('store'),
  isExpanded: false,
  errors: "",
  stackTrace: "",
  startTests: function() {

    var model = this.get('model');

    var adapter = this.get('store').adapterFor('application');
    var baseURL = adapter.buildURL()
    var baseURLArr = baseURL.split('/');

    baseURLArr[baseURLArr.length - 1] = undefined;
    baseURL = baseURLArr.join('/');

    var url = baseURL + 'help/'

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
      model.set('percent', percent + 100);
    };

    var promises = ['hdfs'].map(function(name) {

      return Ember.$.getJSON( url + name + 'Status')
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

  progressBarStyle: function() {
    return 'width: ' + this.get("model").get("percent") +  '%;';
  }.property("model.percent"),

  allTestsCompleted: function(){
  return this.get("model").get("hdfsTestDone");
  }.property('model.hdfsTestDone'),

  modelhdfsTestDone: function() {
    return this.get('model.hdfsTestDone');
  }.property('model.hdfsTestDone' ),

  modelhdfsTest: function() {
    return this.get('model.hdfsTest');
  }.property('model.hdfsTest' ),

  actions: {
    toggleStackTrace:function () {
      var value = this.get('isExpanded');
      this.set('isExpanded', !value);
    }
  }
});
