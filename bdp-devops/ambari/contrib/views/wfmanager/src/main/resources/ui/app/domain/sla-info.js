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
var SlaInfo = Ember.Object.extend(Ember.Copyable,{
  copy (){
    var slaInfo = {};
    for (let key in this) {
      slaInfo[key] = Ember.copy(this[key]) ;
    }
    return slaInfo;
  },
  init (){
    this.nominalTime={
      value : undefined,
      displayValue : undefined,
      type : 'date'
    };
    this.shouldStart = {
      time : undefined,
      unit : undefined
    };
    this.shouldEnd = {
      time : undefined,
      unit : undefined
    };
    this.maxDuration = {
      time : undefined,
      unit : undefined
    };
    this.alertEvents = undefined;
    this.alertContacts = undefined;
  },
  nominalTime:{
    value : undefined,
    displayValue : undefined,
    type : 'date'
  },
  shouldStart : {
    time : undefined,
    unit : undefined
  },
  shouldEnd : {
    time : undefined,
    unit : undefined
  },
  maxDuration : {
    time : undefined,
    unit : undefined
  },
  alertEvents : undefined,
  alertContacts : undefined
});
export {SlaInfo};
