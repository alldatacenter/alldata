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

export default Ember.Component.extend(Ember.Evented, {
  credentialsList : Ember.A([]),
  credentialsInfo : {},
  childComponents : new Map(),
  initialize : function(){
    this.get('credentialsList').clear();
    this.get('childComponents').clear();
    this.set('credentialsList', this.get('workflowCredentials'));
  }.on('init'),
  rendered : function(){
    this.$('#workflow_credentials_dialog').modal({
      backdrop: 'static',
      keyboard: false
    });
    this.$('#workflow_credentials_dialog').modal('show');
    this.$('#workflow_credentials_dialog').modal().on('hidden.bs.modal', function() {
      this.sendAction('showCredentials', false);
    }.bind(this));
  }.on('didInsertElement'),
  processMultivaluedComponents(){
    this.get('childComponents').forEach((childComponent)=>{
      if(childComponent.get('multivalued')){
        childComponent.trigger('bindInputPlaceholder');
      }
    });
  },
  actions : {
    register(component, context){
      this.get('childComponents').set(component, context);
    },
    createCredentials(){
      this.set('editMode', false);
      this.set('createMode',true);
    },
    editCredentials(index){
      this.set('createMode', false);
      this.set('editMode',true);
      this.set('currentCredentialIndex', index);
      this.set('currentCredentials', Ember.copy(this.get('credentialsList').objectAt(index)));
    },
    updateCredentials(){
      this.set('editMode', false);
      this.get('credentialsList').replace(this.get('currentCredentialIndex'), 1, Ember.copy(this.get('currentCredentials')));
    },
    addCredentials (credentialsInfo){
      this.get('credentialsList').pushObject(credentialsInfo);
      this.set('createMode', false);
    },
    deleteCredentials(index){
      this.get('credentialsList').removeAt(index);
      if(index === this.get('currentCredentialIndex')){
        this.set('editMode', false);
      }
    },
    cancelCreateMode(){
      this.set('createMode', false);
    },
    cancelEditMode(){
      this.set('editMode', false);
    },
    saveCredentials (){
      this.processMultivaluedComponents();
      this.set('workflowCredentials', Ember.copy(this.get('credentialsList')));
      this.$('#workflow_credentials_dialog').modal('hide');
    }
  }
});
