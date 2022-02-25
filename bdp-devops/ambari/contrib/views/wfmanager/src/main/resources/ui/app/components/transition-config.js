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
import {FindNodeMixin} from '../domain/findnode-mixin';

const Validations = buildValidations({
  'transition.errorNode.name': validator('presence', {
    presence : true
  })
});

export default Ember.Component.extend(FindNodeMixin, Validations, {
  selectedKillNode : '',
  initialize : function(){
    this.set('descendantNodes', this.get('currentNode.validErrorToNodes'));
    if(!this.get('transition.okToNode')){
      var defaultOkToNode = this.getOKToNode(this.get('currentNode'));
      this.set('transition.okToNode', defaultOkToNode);
      this.set('defaultOkToNode', defaultOkToNode);
    }
    this.sendAction('register','transition', this);
    if(Ember.isBlank(this.get('transition.errorNode.name'))){
      this.set('transition.errorNode', this.get('killNodes').objectAt(0));
    }
  }.on('init'),
  actions : {
    errorToHandler (value){
      this.set('selectedKillNode', value);
      if(this.get('selectedKillNode') === 'createNew'){
        this.set('transition.errorNode.name', "");
        this.set('transition.errorNode.message', "");
        this.set('transition.errorNode.isNew', true);
      }else if(value === ""){
        this.set('transition.errorNode', null);
      }else{
        this.set('transition.errorNode.isNew',false);
        var node = this.get('descendantNodes').findBy('name',value);
        if(node){
          this.set('transition.errorNode', node);
        }else{
          node = this.get('killNodes').findBy('name',value);
          this.set('transition.errorNode', node);
        }
      }
    },
    okToHandler (name){
      var validOkToNodes = this.get('currentNode.validOkToNodes');
      var node = validOkToNodes.findBy('name',name);
      if(!node){
        node = this.get('killNodes').findBy('name',name);
      }
      if(node.id !== this.get('defaultOkToNode').id){
        this.set('showWarning', true);
      }else{
        this.set('showWarning', false);
      }
      this.set('transition.okToNode', node);
    },
    undoChangeOkTo(){
      this.set('transition.okToNode', this.get('defaultOkToNode'));
      this.set('showWarning', false);
    }
  }
});
