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

App.PathInputComponent = Em.Component.extend({
  layoutName:'components/pathInput',
  actions:{
    add:function () {
      var currentBasedir = this.get('currentBasedir'),
          path = this.get('path'),
          basedir = path.substr(0,path.lastIndexOf('.')) || currentBasedir,
          queuePath = [basedir,path.substr(path.lastIndexOf('.')+1)].join('.'),
          queueName = path.substr(path.lastIndexOf('.')+1),
          deletedQueues = this.get('queues.firstObject.store').get('deletedQueues'),
          alreadyExists = (this.get('queues').findBy('name',queueName)||deletedQueues.findBy('name',queueName))?true:false;

      if (!path || !queueName) {
        return this.setProperties({'isError':true,'errorMessage':'Enter queue name.'});
      }
      if (alreadyExists) {
        return this.setProperties({'isError':true,'errorMessage':'Queue already exists.'});
      }

      if (this.get('pathMap').contains(basedir) && !alreadyExists) {
        this.sendAction('action',basedir,queueName);
        this.set('activeFlag',false);
      }
    },
    cancel:function () {
      this.set('activeFlag',false);
    }
  },
  queues:[],
  activeFlag:false,
  pathMap:Em.computed.mapBy('queues','path'),
  currentBasedir:Em.computed(function() {
    return this.get('queues').filterBy('isCurrent',true).get('firstObject.path') || "root";
  }),
  path:'',
  isError:false,
  errorMessage:'',
  didChangePath:function () {
    return this.set('isError',false);
  }.observes('path'),
  inputFieldView: Em.TextField.extend({
    classNames:['form-control newQPath'],
    action:'add',
    pathSource:[],
    placeholder:"Enter queue path...",
    typeaheadInit:function () {
      this.$().typeahead({
          source: this.get('pathSource'),
          matcher: function (item) {
            return ~item.toLowerCase().indexOf(this.query.toLowerCase());
          },
          minLength:2,
          items:100,
          scrollHeight:5
      }).focus();
    }.observes('pathSource').on('didInsertElement'),
    tooltipInit:function () {
      this.$().tooltip({
        title:function() {
          return this.get('parentView.errorMessage');
        }.bind(this),
        placement:'bottom',
        trigger:'manual'
      });
    }.on('didInsertElement').observes('parentView.errorMessage'),
    tooltipToggle:function (e,o) {
      this.$().tooltip((e.get(o)?'show':'hide'));
    }.observes('parentView.isError'),
    willClearRender:function () {
      this.$().typeahead('destroy');
      this.$().tooltip('destroy');
    }
  })
});
