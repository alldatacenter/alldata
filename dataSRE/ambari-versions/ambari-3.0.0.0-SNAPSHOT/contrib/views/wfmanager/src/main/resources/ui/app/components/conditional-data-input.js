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
  'condition.operator': validator('presence', {
    presence : true
  }),
  'condition.operands': {
    validators: [
      validator('operand-length', {
        min : 2,
        dependentKeys: ['condition.operands.[]','condition.operator']
      })
    ]
  }
});

export default Ember.Component.extend(Validations, {
  initialize : function(){
    this.set('conditionsList', Ember.A([]));
    this.get('conditionsList').pushObjects([
      {value : 'and', displayName: 'All'},
      {value : 'or', displayName: 'Any'}
    ]);
    if(!this.get('isToplevel')){
      this.get('conditionsList').pushObject({value : 'combine', displayName: 'Combine'});
    }
    this.sendAction('register', this, this);
  }.on('init'),
  onDestroy : function(){
    this.sendAction('deregister', this);
  }.on('willDestroyElement'),
  actions : {
    registerChild (key, context){
      this.sendAction('register', key, context);
    },
    deregisterChild(key){
      this.sendAction('deregister', key);
    },
    addCondition(){
      if(!this.get('condition.operands')){
        this.set('condition.operands', Ember.A([]));
      }
      this.get('condition.operands').pushObject({operands : Ember.A([]), type:'condition'});
    },
    addDataInput(){
      if(!this.get('condition.operands')){
        this.set('condition.operands', Ember.A([]));
      }
      this.get('condition.operands').pushObject({type:'dataInput'});
    },
    deleteOperand(index){
      this.get('condition.operands').removeAt(index);
    },
    showAdvanced(){
      this.set('showAdvanced', true);
    },
    hideAdvanced(){
      this.set('showAdvanced', false);
    }
  }
});
