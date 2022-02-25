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
import Constants from '../utils/constants';
const Validations = buildValidations({
  'actionModel.master': validator('presence', {
    presence : true
  }),
  'actionModel.jar': validator('presence', {
    presence : true
  }),
  'actionModel.sparkName': validator('presence', {
    presence : true
  })    
});
export default Ember.Component.extend(Validations,{
  setup : function(){
    if(this.get('actionModel.jobXml') === undefined){
      this.set("actionModel.jobXml", Ember.A([]));
    }
    if(this.get('actionModel.args') === undefined){
      this.set("actionModel.args", Ember.A([]));
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
    this.set('mastersList',Ember.copy(Constants.sparkMasterList));
    this.set('isJar', this.get('actionModel.jar') && this.get('actionModel.jar').endsWith('.jar'));
    this.sendAction('register','sparkAction', this);
  }.on('init'),
  appendNameNode: false,
  initialize : function(){
    this.on('fileSelected',function(fileName){
      this.set(this.get('filePathModel'), fileName);
    }.bind(this));
  }.on('didInsertElement'),
  rendered : function(){
    if(this.get('actionModel.master')){
      var master = Constants.sparkMasterList.findBy('value',this.get('actionModel.master'));
      if(master){
        this.$("input[name=master][value=" + this.get('actionModel.master') + "]").prop('checked','checked');
        this.set('disableCustomMaster','disabled');
      }else{
        this.$("input[name=master][value=other]").prop('checked','checked');
        this.set('customMaster',Ember.copy(this.get('actionModel.master')));
        this.set('disableCustomMaster', false);
      }
    }
  }.on('didInsertElement'),
  jarObserver : Ember.observer('actionModel.jar',function(){
    var isJar =  this.get('actionModel.jar') && this.get('actionModel.jar').endsWith('.jar');
    this.set('isJar', isJar);
    if(!isJar){
      this.set('actionModel.class', undefined);
    }
  }),
  // validations : {
  //   'actionModel.master': {
  //     presence: {
  //       'message' : 'You need to provide a value for Runs on (Master)'
  //     }
  //   },
  //   'actionModel.jar': {
  //     presence: {
  //       'message' : 'You need to provide a value for Application'
  //     },
  //     format : {
  //       'with' : /\.jar$|\.py$/i,
  //       'message' : 'You need to provide a .jar or .py file'
  //     }
  //   },
  //   'actionModel.sparkName': {
  //     presence: {
  //       'message' : 'You need to provide a value for Name'
  //     }
  //   }
  // },
  observeError :function(){
    if(this.$('#collapseOne label.text-danger').length > 0 && !this.$('#collapseOne').hasClass("in")){
      this.$('#collapseOne').collapse('show');
    }
  }.on('didUpdate'),
  validateJarPathAndAppend() {
      let nameNode = this.get('actionModel.nameNode'), jar = this.get('actionModel.jar');
      if(!jar) {
        return;
      }
      this.toggleProperty('appendNameNode');
      if(!jar.startsWith('${nameNode}') && this.get('appendNameNode')) {
        this.set('actionModel.jar', `${nameNode}${jar}`);
      } else if(jar.startsWith('${nameNode}') && this.get('appendNameNode')) {
        this.set('actionModel.jar', `${jar}`);
      } else {
        this.set('actionModel.jar', jar.replace('${nameNode}', ''));
      }
  },
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
    appendNamenode() {
      this.validateJarPathAndAppend();
    },
    onMasterChange (elt){
      var value = this.$(elt).val();
      if(value !== 'other'){
        this.set('actionModel.master',value);
        this.set('customMaster','');
        this.set('disableCustomMaster','disabled');
      }else{
        this.set('disableCustomMaster', false);
      }
    }
  }
});
