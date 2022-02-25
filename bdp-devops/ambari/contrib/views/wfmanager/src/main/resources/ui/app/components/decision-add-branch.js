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
import {FindNodeMixin} from '../domain/findnode-mixin';
import { validator, buildValidations } from 'ember-cp-validations';

const Validations = buildValidations({
  'condition': validator('presence', {
    presence : true,
    disabled(model) {
      return !model.get('canValidate');
    },
    'message' : 'Required',
    dependentKeys : ['canValidate']
  }),
  'targetNode': validator('presence', {
    presence : true,
    disabled(model) {
      return !model.get('canValidate');
    },
    'message' : 'Required',
    dependentKeys : ['canValidate']
  }),
});
export default Ember.Component.extend(Validations, FindNodeMixin,{
  isInsertAction: false,
  condition:"",
  targetNode:"",
  newNodeType:null,
  initialize : function(){
    var self=this;

    this.on("showBranchOptions",function(node){
      if (self.$("#selector-content").is(":visible")){
        self.$("#selector-content").hide();
      }else{
        if (node) {
          self.set("node", node);
        }
        self.set("isInsertAction",false);
        this.set("newNodeType",null);
        this.get('flowRenderer').populateOkToandErrorTONodes(node);
        var descendantNodes= Ember.A([]);
        descendantNodes.pushObjects(this.get('node.validOkToNodes'));
        this.set('descendantNodes',descendantNodes);
        self.$("#selector-content").show();
      }
    });
  }.on('init'),
  setup : function(){
    this.sendAction('registerAddBranchAction',this);
  }.on('didInsertElement'),
  actions:{
    addNewNode(type){
      this.set("newNodeType",type);
    },
    onTargetNodeChange(value){
      var node = this.get('descendantNodes').findBy('id',value) || this.get('killNodes').findBy('id',value);
      this.set('targetNode', node);
    },
    save(){
      this.set('canValidate', true);
      if(this.get('validations.isInvalid')){
        this.set('showErrorMessage', true);
        return;
      }
      this.sendAction("addDecisionBranch",{
        sourceNode: this.get("node"),
        condition:this.get("condition"),
        targetNode:this.get("targetNode"),
        newNodeType:this.get("newNodeType")
      });
      this.$("#selector-content").hide();
      this.set('canValidate', false);
      this.set('condition',"");
      this.set('targetNode',"");
      this.$('#target-node-select').prop('selectedIndex', 0);
    },
    cancel(){
      this.$("#selector-content").hide();
    }
  }
});
