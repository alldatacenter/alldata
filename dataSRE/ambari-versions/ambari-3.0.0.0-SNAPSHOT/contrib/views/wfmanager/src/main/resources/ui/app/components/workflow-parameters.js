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
  'parameters.configuration.property': {
    validators: [
      validator('unique-name', {
        dependentKeys: ['parameters.configuration.property.[]']
      })
    ]
  }
});
export default Ember.Component.extend(Validations, {
  saveClicked : false,
  initialize : function(){
    if(this.get('parameters') === undefined || this.get('parameters') === null){
      this.set('parameters',{});
    }
    if(this.get('parameters.configuration') === undefined){
      this.set("parameters.configuration",{});
      this.set("parameters.configuration.property", Ember.A([]));
    }
    this.sendAction('register','workflowParameters',this);

  }.on('init'),
  displayName : Ember.computed('type', function(){
    if(this.get('type') === 'wf'){
      return "Workflow";
    }else if(this.get('type') === 'coord'){
      return "Coordinator";
    }else{
      return "Bundle";
    }
  }),
  rendered : function(){
    this.$('#workflow_parameters_dialog').modal({
      backdrop: 'static',
      keyboard: false
    });
    this.$('#workflow_parameters_dialog').modal('show');
    this.$('#workflow_parameters_dialog').modal().on('hidden.bs.modal', function() {
    if(this.get('saveClicked')){
      this.sendAction('saveWorkFlowParam');
    }else{
      this.sendAction('closeWorkFlowParam');
    }
    }.bind(this));
  }.on('didInsertElement'),
  actions : {
    register(component, context){
      this.set('nameValueContext', context);
    },
    close (){
      this.$('#workflow_parameters_dialog').modal('hide');
      this.set('saveClicked', false);
    },
    saveParameters (){
      if(!this.get('validations.isInvalid')){
        this.get("nameValueContext").trigger("bindInputPlaceholder");
        this.set('saveClicked', true);
        this.$('#workflow_parameters_dialog').modal('hide');
        return ;
      }
    }
  }
});
