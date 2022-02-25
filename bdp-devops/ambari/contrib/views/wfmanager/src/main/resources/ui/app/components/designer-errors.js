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
  showingStackTrace: false,
  hasErrorMsg : Ember.computed('errorMsg', function() {
    return !Ember.isBlank(this.get("errorMsg"));
  }),
  errorMsgDetails : Ember.computed('data.responseText', function() {
    var jsonResponse = this.getparsedResponse();
    if (jsonResponse.message) {
      if (jsonResponse.message.indexOf('Permission denied') >= 0) {
        return "Permission Denied";
      }
      return jsonResponse.message;
    }
    return "";
  }),
  stackTrace : Ember.computed('data.responseText', function() {
      var jsonResponse = this.getparsedResponse();
      var stackTraceMsg = jsonResponse.stackTrace;
      if(!stackTraceMsg){
        return "";
      }
      if (stackTraceMsg instanceof Array) {
        return stackTraceMsg.join("").replace(/\tat /g, '&nbsp;&nbsp;&nbsp;&nbsp;at&nbsp;');
      } else {
        return stackTraceMsg.replace(/\tat /g, '<br/>&nbsp;&nbsp;&nbsp;&nbsp;at&nbsp;');
      }
  }),
  isStackTraceAvailable : Ember.computed('stackTrace', function(){
    return this.get('stackTrace') && this.get('stackTrace').length ? true : false;
  }),
  getparsedResponse() {
    var response = this.get('data.responseText');
    if (response) {
      try {
        return JSON.parse(response);
      } catch(err){
        return "";
      }
    }
    return "";
  },

  actions: {
    showStackTrace(){
      this.set("showingStackTrace", !this.get("showingStackTrace"));
    },
    closeStackTrace(){
      this.set("showingStackTrace", false);
    },
    dismissError(idx){
      this.get("errors").removeAt(idx);
    }
  }
});
