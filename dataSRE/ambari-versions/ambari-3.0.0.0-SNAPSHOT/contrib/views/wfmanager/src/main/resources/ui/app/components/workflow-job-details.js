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
import CommonUtils from "../utils/common-utils";

export default Ember.Component.extend({
  dagUrl:Ember.computed('model.id',function(){
    return Ember.ENV.API_URL+'/getDag?jobid='+this.get('model.id');
  }),
  validTrackerUrl:Ember.computed('model.actionInfo.consoleUrl',function(){
    var trackerUrl = this.get("model.actionInfo.consoleUrl");
    if (trackerUrl && CommonUtils.startsWith(trackerUrl.trim(), "http")) {
      return true;
    }
    return false;
  }),
  actions :{
    getJobLog (params) {
      this.sendAction('getJobLog', params);
    },
    getActionDetails(action){
      this.sendAction('getActionDetails',action);
    }
  }
});
