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

App.Script = DS.Model.extend({
  title:DS.attr('string'),
  pigScript:DS.belongsTo('file', { async: true }),
  dateCreated:DS.attr('scriptdate', { defaultValue: moment()}),
  templetonArguments:DS.attr('string'),
  pythonScript:DS.attr('string'),
  owner:DS.attr('string'),
  opened:DS.attr('string'),
  // nav item identifier
  name:function (){
    return this.get('title') + this.get('id');
  }.property('title'),
  label:function (){
    return this.get('title');
  }.property('title'),

  argumentsArray:function (key,val) {
    if (arguments.length >1) {
      var oldargs = (this.get('templetonArguments'))?this.get('templetonArguments').w():[];
      if (val.length != oldargs.length) {
        this.set('templetonArguments',val.join('\t'));
      }
    }
    var args = this.get('templetonArguments');
    return (args && args.length > 0)?args.w():[];
  }.property('templetonArguments'),

  dateCreatedUnix:function () {
    return moment(this.get('dateCreated')).unix();
  }.property('dateCreated'),

  isBlankTitle:function () {
    return Ember.isBlank(this.get('title'));
  }.property('title')
});
