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
  'data.name': validator('presence', {
    presence : true
  }),
  'data.dataset': validator('presence', {
    presence : true
  })
});

export default Ember.Component.extend(Validations, {
  initialize : function(){
    if(!this.get('data.start')){
      this.set('data.start',{
        type : 'date',
        value :''
      });
    }
    if(!this.get('data.end')){
      this.set('data.end',{
        type : 'date',
        value :''
      });
    }
    if(this.get('type') === 'output'){
      if(!this.get('data.instance')){
        this.set('data.instance',{
          type : 'date',
          value :''
        });
      }
    }
    if(this.get('type') === 'input' && !this.get('data.instances')){
      this.set('data.instances', Ember.A([]));
      this.set('data.isList', true);
    }
    this.set('childComponents', new Map());
  }.on('init'),
  validateChildComponents(){
    var isChildComponentsValid = true;
    this.get('childComponents').forEach((context)=>{
      if(context.get('validations') && context.get('validations.isInvalid')){
        isChildComponentsValid =  false;
        context.set('showErrorMessage', true);
      }
    }.bind(this));
    return isChildComponentsValid;
  },
  actions : {
    registerChild(key, context){
      this.get('childComponents').set(key, context);
    },
    deregisterChild(key){
      this.get('childComponents').delete(key);
    },
    onInstanceTypeChange(isList) {
      this.set('data.isList', isList);
    },
    add(){
      var isChildComponentsValid = this.validateChildComponents();
      if(this.get('validations.isInvalid') || !isChildComponentsValid) {
        this.set('showErrorMessage', true);
        return;
      }
      this.sendAction('add');
    },
    update(){
      var isChildComponentsValid = this.validateChildComponents();
      if(this.get('validations.isInvalid') || !isChildComponentsValid) {
        this.set('showErrorMessage', true);
        return;
      }
      this.sendAction('update');
    },
    cancel(){
      this.sendAction('cancel');
    }
  }
});
