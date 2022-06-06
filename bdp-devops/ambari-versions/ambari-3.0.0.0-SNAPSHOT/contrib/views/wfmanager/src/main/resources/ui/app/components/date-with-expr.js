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
  'dateField.displayValue': {
    validators: [
      validator('presence', true),
      validator('date', {
        format: 'MM/DD/YYYY hh:mm A',
        disabled(model, attribute) {
          return model.get('dateField.type') === 'expr';
        }
      })
    ]
  },
});
export default Ember.Component.extend(Validations, {
  initialize : function(){
    this.sendAction('register', this, this);
  }.on('init'),
  elementsInserted : function(){
    if(this.get('dateField.type') === 'date'){
      this.$('input[name="'+this.get('inputName')+'"]').datetimepicker({
        format: 'MM/DD/YYYY hh:mm A',
        useCurrent: false,
        showClose : true
      });
    }
    Ember.addObserver(this, 'dateField.type', this, this.typeObserver);
    Ember.addObserver(this, 'dateField.displayValue', this, this.timeObserver);
  }.on('didInsertElement'),
  onDestroy : function(){
    this.sendAction('deregister', this);
    Ember.removeObserver(this, 'dateField.type', this, this.typeObserver);
    Ember.removeObserver(this, 'dateField.displayValue', this, this.timeObserver);
  }.on('willDestroyElement'),
  typeObserver : function(){
    if(this.get('dateField.type') === 'date'){
      this.$('input[name="'+this.get('inputName')+'"]').datetimepicker({
        format: 'MM/DD/YYYY hh:mm A',
        useCurrent: false,
        showClose : true
      });
    }else{
      var dateTimePicker = this.$('input[name="'+this.get('inputName')+'"]').data("DateTimePicker");
      if(dateTimePicker){
        dateTimePicker.destroy();
      }
    }
  },
  timeObserver : function(){
    if(this.get('dateField.type') === 'date'){
      var date = new Date(this.get('dateField.displayValue'));
      if(isNaN(date.getTime())){
        this.set('dateField.value', undefined);
        return;
      }
      this.set('dateField.value', moment(date).format("YYYY-MM-DDTHH:mm")+'Z');
    }else{
      this.set('dateField.value', Ember.copy(this.get('dateField.displayValue')));
    }
  }
});
