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
  tabsInfo : {},
  workInProgress : {},
  userInfo : Ember.inject.service('user-info'),
  userName : null,
  setLastActiveTab(tabId){
    this.get("userInfo").getUserData().then(function(data){
       localStorage.setItem(data+"-lastActiveTab", tabId);
      }.bind(this)).catch(function(e){
        console.error(e);
      });
  },
  getLastActiveTab(){
      this.get("userInfo").getUserData().then(function(data){
        return localStorage.getItem(data+"-lastActiveTab");
      }.bind(this)).catch(function(e){
        console.error(e);
      });
  },
  restoreTabs(){
      var deferred = Ember.RSVP.defer();
      this.get("userInfo").getUserData().then(function(data){
        this.set("userName", data);
        var tabs = localStorage.getItem(data+'-tabsInfo');
        deferred.resolve(JSON.parse(tabs));
      }.bind(this)).catch(function(e){
        deferred.resolve("");
        console.error(e);
      });
    return deferred;
  },
  saveTabs(tabs){
    if(!tabs){
      return;
    }
    var tabArray = [];
    tabs.forEach((tab)=>{
      tabArray.push({
        type : tab.type,
        id : tab.id,
        name : tab.name,
        filePath : tab.filePath
      });
    });
    this.get("userInfo").getUserData().then(function(data){
      localStorage.setItem(data+'-tabsInfo', JSON.stringify(tabArray));
    }.bind(this)).catch(function(e){
      console.error(e);
    });
  },
  restoreWorkInProgress(id){
    var deferred = Ember.RSVP.defer();
    this.get("userInfo").getUserData().then(function(data){
       deferred.resolve(localStorage.getItem(data+"-"+id));
    }.bind(this)).catch(function(data){
       deferred.resolve("");
    });
    return deferred;
  },
  saveWorkInProgress(id, workInProgress){
    this.get("userInfo").getUserData().then(function(data){
      localStorage.setItem(data+"-"+id, workInProgress);
    }.bind(this)).catch(function(e){
      console.error(e);
    });
  },
  deleteWorkInProgress(id){
    this.get("userInfo").getUserData().then(function(data){
      localStorage.removeItem(data+"-"+id);
    }.bind(this)).catch(function(e){
      console.error(e);
    });
  },
  getUserName(){
   return this.get("userName");
  }
});
