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

export default Ember.Service.extend({
  data:null,
  userName: null,
  init() {
  },
  getUserData(){
    var url = Ember.ENV.API_URL + "/getCurrentUserName", self = this;
    var deferred = Ember.RSVP.defer();
    if(self.get("userName") !== null && self.get("userName") !== undefined){
      deferred.resolve(self.get("userName"));
    } else{
      Ember.$.ajax({
        url: url,
        method: 'GET',
        dataType: "text",
        beforeSend: function (xhr) {
          xhr.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
          xhr.setRequestHeader("X-Requested-By", "Ambari");
        }
      }).done(function(data){
        let uname = JSON.parse(data).username;
        self.set("userName", JSON.parse(data).username);
        deferred.resolve(uname);
      }).fail(function(data){
        self.set("userName", "");
        deferred.reject(data);
      });
    }
    return deferred.promise;
  },
  setUserData(data){
    this.set("data", data);
  },
  getUserName(){
    var res = this.getUserData();
    return res;
  }
});
