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

App.Job = DS.Model.extend({
  pigScript:DS.belongsTo('file', { async: true }),
  //pythonScript:DS.belongsTo('script'),
  scriptId: DS.attr('number'),
  title: DS.attr('string'),
  templetonArguments:DS.attr('string'),
  owner: DS.attr('string'),
  forcedContent:DS.attr('string'),
  duration: DS.attr('number'),
  durationTime:function () {
    return moment.duration(this.get('duration'), "seconds").format("h [hrs], m [min], s [sec]");
  }.property('duration'),

  sourceFile:DS.attr('string'),
  sourceFileContent:DS.attr('string'),

  statusDir: DS.attr('string'),
  status: DS.attr('string'),
  dateStarted:DS.attr('isodate'),
  jobId: DS.attr('string'),
  jobType: DS.attr('string'),
  percentComplete: DS.attr('number'),
  percentStatus:function () {
    if (this.get('isTerminated')) {
      return 100;
    }
    return (this.get('status')==='COMPLETED')?100:(this.get('percentComplete')||0);
  }.property('status','percentComplete'),

  isTerminated:function(){
    return (this.get('status')=='KILLED'||this.get('status')=='FAILED');
  }.property('status'),
  isKilling:false,
  kill:function(success,error){
    var self = this;
    var host = self.store.adapterFor('application').get('host');
    var namespace = self.store.adapterFor('application').get('namespace');
    var url = [host, namespace,'jobs',self.get('id')].join('/');

    self.set('isKilling',true);
    return Em.$.ajax(url, {
      type:'DELETE',
      contentType:'application/json',
      beforeSend:function(xhr){
        xhr.setRequestHeader('X-Requested-By','ambari');
      }
    }).always(function() {
      self.set('isKilling',false);
    }).then(success,error);
  },

  jobSuccess:function () {
    return this.get('status') == 'COMPLETED';
  }.property('status'),

  jobError:function () {
    return this.get('status') == 'SUBMIT_FAILED' || this.get('status') == 'KILLED' || this.get('status') == 'FAILED';
  }.property('status'),

  jobInProgress:function () {
    return this.get('status') == 'SUBMITTING' || this.get('status') == 'SUBMITTED' || this.get('status') == 'RUNNING';
  }.property('status'),

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

  notFound: function() {
    if (this.get('isLoaded')) {
      this.set('status','FAILED');
    }
  }
});
