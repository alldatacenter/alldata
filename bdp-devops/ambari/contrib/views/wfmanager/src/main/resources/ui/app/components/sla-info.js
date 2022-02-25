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
import { v1, v4 } from "ember-uuid";

const Validations = buildValidations({
  'slaInfo.nominalTime.value': {
     validators: [
       validator('presence', {
         presence : true,
         disabled(model, attribute) {
           return !model.get('slaEnabled');
         },
         dependentKeys : ['slaEnabled']
       })
     ]
   },
  'slaInfo.shouldEnd.time': {
    validators: [
      validator('presence', {
        presence : true,
        disabled(model, attribute) {
          return !model.get('slaEnabled');
        },
        dependentKeys : ['slaEnabled']
      }),
      validator('number', {
        disabled(model) {
          return !model.get('slaEnabled');
        },
        allowString : true,
        allowBlank : true,
        integer : true,
        dependentKeys : ['slaEnabled']
      }),
    ]
  },
  'slaInfo.shouldStart.time': validator('number', {
    disabled(model) {
      return !model.get('slaEnabled');
    },
    allowString : true,
    allowBlank : true,
    integer : true,
    dependentKeys : ['slaEnabled']
  }),
  'slaInfo.maxDuration.time': validator('number', {
    disabled(model) {
      return !model.get('slaEnabled');
    },
    allowString : true,
    allowBlank : true,
    integer : true,
    dependentKeys : ['slaEnabled']
  }),
  'slaInfo.shouldStart.unit':validator('presence', {
    presence : true,
    disabled(model) {
      return !model.get('slaEnabled') || !model.get('slaInfo.shouldStart.time');
    },
    dependentKeys : ['slaEnabled', 'slaInfo.shouldStart.time']
  }),
  'slaInfo.shouldEnd.unit':validator('presence', {
    presence : true,
    disabled(model) {
      return !model.get('slaEnabled') || !model.get('slaInfo.shouldEnd.time');
    },
    dependentKeys : ['slaEnabled', 'slaInfo.shouldEnd.time']
  }),
  'slaInfo.maxDuration.unit':validator('presence', {
    presence : true,
    disabled(model) {
      return !model.get('slaEnabled') || !model.get('slaInfo.maxDuration.time');
    },
    dependentKeys : ['slaEnabled', 'slaInfo.maxDuration.time']
  })
});

export default Ember.Component.extend(Validations, {
  alertEvents: Ember.A([]),
  timeUnitOptions : Ember.A([]),
  initialize : function(){
    this.set('alertEvents', Ember.A([]));
    this.get('alertEvents').pushObject({eventType:'start_miss', alertEnabled:false, displayName :'Start Miss'});
    this.get('alertEvents').pushObject({eventType:'end_miss', alertEnabled:false, displayName : 'End Miss'});
    this.get('alertEvents').pushObject({eventType:'duration_miss', alertEnabled:false, displayName:'Duration Miss'});

    Ember.addObserver(this, 'alertEvents.@each.alertEnabled', this, this.alertEventsObserver);

    this.set('timeUnitOptions',Ember.A([]));
    this.get('timeUnitOptions').pushObject({value:'',displayName:'Select'});
    this.get('timeUnitOptions').pushObject({value:'MINUTES',displayName:'Minutes'});
    this.get('timeUnitOptions').pushObject({value:'HOURS',displayName:'Hours'});
    this.get('timeUnitOptions').pushObject({value:'DAYS',displayName:'Days'});

    if(this.get('slaInfo.alertEvents')){
      var alertsFor = this.get('slaInfo.alertEvents').split(",");
      alertsFor.forEach((alert)=>{
        Ember.set(this.get('alertEvents').findBy('eventType', alert),'alertEnabled', true);
      });
    }
    if(this.get('slaEnabled') === undefined){
      this.set('slaEnabled', false);
    }
    Ember.addObserver(this, 'slaEnabled', this, this.slaObserver);
    this.set('collapseId', v1());
  }.on('init'),
  alertEventsObserver : function(){
    var alerts = this.get('alertEvents').filterBy('alertEnabled',true).mapBy('eventType');
    this.set('slaInfo.alertEvents', alerts.join());
  },
  onDestroy : function(){
    Ember.removeObserver(this, 'alertEvents.@each.alertEnabled', this, this.alertEventsObserver);
    Ember.removeObserver(this, 'slaEnabled', this, this.slaObserver);
  }.on('willDestroyElement'),
  elementsInserted : function() {
    this.sendAction('register','slaInfo', this);
    if(this.get('slaEnabled')){
      this.$('#slaCollapse').collapse('show');
    }
  }.on('didInsertElement'),
  slaObserver : function(){
    if(this.get('slaEnabled')){
      this.$('#slaCollapse').collapse('show');
    }else{
      this.$('#slaCollapse').collapse('hide');
    }
  }
});
