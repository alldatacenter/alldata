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
import Constants from '../utils/constants';
import { validator, buildValidations } from 'ember-cp-validations';

const Validations = buildValidations({
  'workflowStream': validator('presence', {
    presence : true
  }),
  'configMap': {
    validators: [
      validator('job-params-validator', {
        dependentKeys: ['configMap.@each.value', 'showErrorMessage']
      })
    ]
  }
});


export default Ember.Component.extend(Validations, {
  workflowStream : "",
  displayName : Ember.computed('type', function(){
    if(this.get('type') === 'wf'){
      return "Workflow";
    }else if(this.get('type') === 'coord'){
      return "Coordinator";
    }else{
      return "Bundle";
    }
  }),
  initialize :function(){

  }.on('init'),
  rendered : function(){
    this.$("#configureJob").modal("show");    
  }.on('didInsertElement'),

  getParsedErrorResponse (response){

  },
  actions: {
    save(){
      this.$("#configureJob").modal("hide");		
      this.sendAction("importWorkflowStream", this.$('.CodeMirror')[0].CodeMirror.getValue());
    },
    close(){
      this.$("#configureJob").modal("hide");	
      this.sendAction("hideStreamImport");
    }
  }
});
