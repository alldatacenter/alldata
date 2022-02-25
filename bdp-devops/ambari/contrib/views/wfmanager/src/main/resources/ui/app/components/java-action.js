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
import { validator, buildValidations } from 'ember-cp-validations';

const Validations = buildValidations({
  'actionModel.mainClass': validator('presence', {
    presence : true
  })
});

export default Ember.Component.extend(Validations, {
  fileBrowser : Ember.inject.service('file-browser'),
  isSingle : false,
  setUp : function(){
    if(this.get('actionModel.args') === undefined){
      this.set("actionModel.args", Ember.A([]));
    }
    if(this.get('actionModel.javaOpt') === undefined){
      this.set("actionModel.javaOpt", Ember.A([]));
    }else if(this.get('actionModel.javaOpt').length > 0){
      this.set('isSingle', false);
    }else if(Ember.isBlank(this.get('actionModel.javaOpts')) && this.get('actionModel.javaOpt').length == 0){
      this.set('isSingle', false);
    }else{
      this.set('isSingle', true);
    }
    if(this.get('actionModel.files') === undefined){
      this.set("actionModel.files", Ember.A([]));
    }
    if(this.get('actionModel.jobXml') === undefined){
      this.set("actionModel.jobXml", Ember.A([]));
    }
    if(this.get('actionModel.archives') === undefined){
      this.set("actionModel.archives", Ember.A([]));
    }
    if(this.get('actionModel.prepare') === undefined){
      this.set("actionModel.prepare", Ember.A([]));
    }
    if(this.get('actionModel.configuration') === undefined){
      this.set("actionModel.configuration",{});
      this.set("actionModel.configuration.property", Ember.A([]));
    }
  }.on('init'),
  initialize : function(){
    this.on('fileSelected',function(fileName){
      this.set(this.get('filePathModel'), fileName);
    }.bind(this));
    this.sendAction('register','javaAction', this);
    this.send('setIsSingle', this.get('isSingle'));
  }.on('didInsertElement'),
  observeError :function(){
    if(this.$('#collapseOne label.text-danger').length > 0 && !this.$('#collapseOne').hasClass("in")){
      this.$('#collapseOne').collapse('show');
    }
  }.on('didUpdate'),
  onDestroy : function(){
    if(this.get('isSingle')){
      this.set('actionModel.javaOpt', Ember.A([]));
    }else{
      this.set("actionModel.javaOpts", undefined);
    }
  }.on('willDestroyElement'),
  actions : {
    openFileBrowser(model, context){
      if(undefined === context){
        context = this;
      }
      this.set('filePathModel', model);
      this.sendAction('openFileBrowser', model, context);
    },
    register (name, context){
      this.sendAction('register',name , context);
    },
    setIsSingle(value){
      this.set('isSingle', value);
      if(this.get('isSingle')){
        this.$('#single-option').show();
        this.$('#arg-option').hide();
      }else{
        this.$('#single-option').hide();
        this.$('#arg-option').show();
      }
    }
  }
});
