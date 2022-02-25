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

var App = require('app');

App.UserGroupInputComponent = Em.Component.extend({
  layoutName:'components/userGroupInput',

  ug:'',

  users:function (key, value, previousValue) {
    if (value || value === "") {
      this.set('ug',[value,this.get('groups')].join(' '));
    }
    var ug = this.get('ug');
    return (ug == '*')?'*':(ug || '').split(' ')[0];
  }.property('ug'),

  groups:function (key, value, previousValue) {
    if (value || value === "") {
      this.set('ug',[this.get('users'),value].join(' '));
    }
    var ug = this.get('ug');
    return (ug == '*')?'*':(ug || '').split(' ')[1] || '';
  }.property('ug'),

  noSpace:function (e) {
    return (e.keyCode != 32);
  }
});
