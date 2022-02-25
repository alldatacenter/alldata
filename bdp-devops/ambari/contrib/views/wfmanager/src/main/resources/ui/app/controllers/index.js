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
  progressWidth : '10%',
  serviceChecksComplete : false,
  issues : Ember.A([]),
  serviceChecks : Ember.A([
    {'name':'oozie', 'checkCompleted':false, isAvailable : true, 'displayName' : 'Oozie Test', 'errorMessage' : 'Oozie service check failed'},
    {'name':'hdfs', 'checkCompleted':false, isAvailable : true, 'displayName' : 'HDFS Test', 'errorMessage' : 'HDFS service check failed'}
  ]),
  width : Ember.computed('serviceChecks.@each.checkCompleted','serviceChecks.@each.isAvailable', function(){
    let width = 10;
    this.get('serviceChecks').forEach((check)=>{
      if(check.checkCompleted && check.isAvailable){
        width += (90/this.get('serviceChecks').length);
      }
    });
    return Ember.String.htmlSafe(`${width}%`);
  })

});
