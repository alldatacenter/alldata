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
  'actionModel.host': validator('presence', {
    presence : true
  }),
  'actionModel.command': validator('presence', {
    presence : true
  })
});
export default Ember.Component.extend(Validations, {
  fileBrowser : Ember.inject.service('file-browser'),
  useArg : false,
  setUp : function(){
    if(this.get('actionModel.args') === undefined){
      this.set("actionModel.args", Ember.A([]));
    }else if(this.get('actionModel.args').length > 0){
      this.set('useArg', false);
    }
    if(this.get('actionModel.arg') === undefined){
      this.set("actionModel.arg", Ember.A([]));
    }else if(this.get('actionModel.arg').length > 0){
      this.set('useArg', true);
    }
  }.on('init'),
  initialize : function(){
    this.on('fileSelected',function(fileName){
      this.set(this.get('filePathModel'), fileName);
    }.bind(this));
    this.sendAction('register','sshAction', this);
    this.send('argTypeChanged', this.get('useArg'));
  }.on('didInsertElement'),
  observeError :function(){
    if(this.$('#collapseOne label.text-danger').length > 0 && !this.$('#collapseOne').hasClass("in")){
      this.$('#collapseOne').collapse('show');
    }
  }.on('didUpdate'),
  onDestroy : function(){
    if(this.get('useArg')){
      this.set("actionModel.args", Ember.A([]));
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
    argTypeChanged(useArg){
      this.set('useArg', useArg);
      if(useArg){
        this.$('#args-option').hide();
        this.$('#arg-option').show();
      }else{
        this.$('#arg-option').hide();
        this.$('#args-option').show();
      }
    }
  }
});
