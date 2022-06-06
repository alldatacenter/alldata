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
  'actionModel.command': validator('presence', {
    presence : true,
    disabled(model, attribute) {
      return model.get('isArg');
    },
    dependentKeys : ['isArg']
  }),
  'actionModel.arg': {
    validators: [
      validator('arg-length', {
        min : 1,
        dependentKeys: ['isArg','actionModel.arg.@each.value'],
        message : 'At least one arg should be non-empty',
        disabled(model, attribute) {
          return !model.get('isArg');
        }
      })
    ]
  }
});

export default Ember.Component.extend(Validations, {
  initialize : function(){
    this.sendAction('register','sqoopAction', this);
    this.on('fileSelected',function(fileName){
      this.set(this.get('filePathModel'), fileName);
    }.bind(this));
    if(this.get('actionModel.jobXml') === undefined){
      this.set("actionModel.jobXml", Ember.A([]));
    }
    if(this.get('actionModel.arg') === undefined && !this.get('actionModel.command')){
      this.set("actionModel.arg", Ember.A([]));
      this.set('isArg', false);
    }else if(this.get('actionModel.arg') && this.get('actionModel.arg').length > 0){
      this.set('isArg', true);
    }else{
      this.set('isArg', false);
    }
    if(this.get('actionModel.files') === undefined){
      this.set("actionModel.files", Ember.A([]));
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
  observeError :function(){
    if(this.$('#collapseOne label.text-danger').length > 0 && !this.$('#collapseOne').hasClass("in")){
      this.$('#collapseOne').collapse('show');
    }
  }.on('didUpdate'),
  elementsInserted : function(){
    this.send('setIsArg', this.get('isArg'));
  }.on('didInsertElement'),
  onDestroy : function(){
    if(this.get('isArg')){
      this.set('actionModel.command', undefined);
      if(Ember.isBlank(this.get('actionModel.arg').get('lastObject').value)){
        this.get('actionModel.arg').removeAt(this.get('actionModel.arg').length - 1);
      }
    }else{
      this.set("actionModel.arg", Ember.A([]));
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
    setIsArg(value){
      this.set('isArg', value);
      if(value){
        this.$('#command-option').hide();
        this.$('#arg-option').show();
        if(!this.get("actionModel.arg") || this.get('actionModel.arg').length === 0){
          this.set("actionModel.arg", Ember.A([]));
          this.get("actionModel.arg").pushObject({value : ""})
        }
      }else{
        this.$('#arg-option').hide();
        this.$('#command-option').show();
      }
    },
    addArg () {
      this.get("actionModel.arg").pushObject({value : ""})
    },
    deleteArg (index) {
      this.get('actionModel.arg').removeAt(index);
    }
  }
});
