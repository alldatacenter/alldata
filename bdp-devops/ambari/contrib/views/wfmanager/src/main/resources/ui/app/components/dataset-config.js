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
  'dataset.name': validator('presence', {
    presence : true
  }),
  'dataset.frequency.value': validator('presence', {
    presence : true
  }),
  'dataset.frequency.type': validator('presence', {
    presence : true
  }),
  'dataset.timezone': validator('presence', {
    presence : true
  }),
  'dataset.uriTemplate': validator('presence', {
    presence : true
  })
});

export default Ember.Component.extend(Validations, {
  childComponents : new Map(),
  initialize : function(){
    if(!this.get('dataset.initialInstance')){
      this.set('dataset.initialInstance', {
        type : 'date',
        value : ''
      });
    }
    if(!this.get('dataset.timezone')){
      this.set('dataset.timezone','UTC');
    }
    if(!this.get('dataset.frequency')){
      this.set('dataset.frequency',{ type : undefined, value : undefined });
    }
    this.set('timeUnitOptions',Ember.A([]));
    this.get('timeUnitOptions').pushObject({value:'',displayName:'Select'});
    this.get('timeUnitOptions').pushObject({value:'months',displayName:'Months'});
    this.get('timeUnitOptions').pushObject({value:'endOfMonths',displayName:'End of Months'});
    this.get('timeUnitOptions').pushObject({value:'days',displayName:'Days'});
    this.get('timeUnitOptions').pushObject({value:'endOfDays',displayName:'End of Days'});
    this.get('timeUnitOptions').pushObject({value:'hours',displayName:'Hours'});
    this.get('timeUnitOptions').pushObject({value:'minutes',displayName:'Minutes'});
    this.get('timeUnitOptions').pushObject({value:'cron',displayName:'Cron'});
    this.set('childComponents', new Map());
    this.set('timezoneList', Ember.copy(Constants.timezoneList));
    if(!this.get('dataset.doneFlagType')) {
      this.set('dataset.doneFlagType', 'default');
    }

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
    addDataset(){
      var isChildComponentsValid = this.validateChildComponents();
      if(this.get('validations.isInvalid') || !isChildComponentsValid) {
        this.set('showErrorMessage', true);
        return;
      }
      this.sendAction('add');
    },
    updateDataset(){
      var isChildComponentsValid = this.validateChildComponents();
      if(this.get('validations.isInvalid') || !isChildComponentsValid) {
        this.set('showErrorMessage', true);
        return;
      }
      this.sendAction('update');
    },
    cancelDatasetOperation(){
      this.sendAction('cancel');
    },
    clearDoneFlag(type){
      this.set('dataset.doneFlag', '');
      this.set('dataset.doneFlagType', type);
    }
  }
});
