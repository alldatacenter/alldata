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
import {Transition} from '../domain/transition';
import Constants from '../utils/constants';
var Node = Ember.Object.extend(FindNodeMixin,{
  id:null,
  name:null,
  type:null,
  actionType:null,/*for action nodes*/
  transitions:null,
  domain:null,
  errorNode:null,
  killnodeName:"",
  factory:null,
  errorMsgs:[],
  init(){
    this.transitions = [];
  },
  onSave(){
    var self=this;
    if (this.isDecisionNode()){
      var i=0;
      this.get("domain").forEach(function(tran){
        self.get("transitions")[i].condition=tran.condition;
        if(self.get("transitions")[i].targetNode.id !== tran.node.id){
          self.get("transitions")[i].targetNode = tran.node;
        }
        i++;
      });
    }else if (this.isActionNode()){
      var errorNode=this.get("errorNode");
      if(errorNode && errorNode.id){
        this.setErrorTransitionTo(errorNode);
      }
    }
  },
  validateCustom(){
    this.errorMsgs=[];
    //TODO Custom validations
    return this.errorMsgs;
  },
  deleteCurrentKillNode(){
    var self=this;
    this.get("transitions").forEach(function(tran){
      if (tran.condition==="error"){
        self.removeTransition(tran);
      }
    });
  },
  getNodeDetail(){
    var domain={};
    if (this.isDecisionNode()){
      var flows=[];
      this.get("transitions").forEach(function(tran){
        flows.push({condition: tran.condition, node: tran.getTargetNode(true)});
      });
      this.set("domain",flows);
      return this.get("domain");
    }else if (this.get("domain")){
      return this.get("domain");
    }else{
      return domain;
    }
  },
  getName(){
    return this.get("name")?this.get("name"):this.get("id");
  },
  findTransitionTo(target){
    var oldTrans = null;
    var transitions=this.get("transitions");
    transitions.forEach(function(tran){
      if (tran.targetNode === target) {
        oldTrans = tran;
        return false;
      }
    });
    return oldTrans;
  },
  addTransition(transition) {
    var transitions=this.get("transitions");
    if (transitions && transitions.indexOf(transition) === -1) {
      if (transition.condition==="error"){
        this.set("errorNode",transition.getTargetNode());
      }
      transitions.push(transition);
    }
  },
  addTransitionTo(target,condition){
    var transition = Transition.create({targetNode:target,sourceNodeId:this.id,condition:condition});
    this.addTransition(transition);
    return transition;
  },
  setErrorTransitionTo(target){
    var errorTrans=this.getErrorTransition();
    if (!errorTrans){
      if (target){
        if (target.isKillNode()){
          this.addTransitionTo(target,"error");
        }else{
          this.addTransitionTo(target,"error");
        }
      }
    }else{
      errorTrans.set("targetNode",target);
    }
  },

  removeTransition(transition){
    if (transition && this.transitions.indexOf(transition) > -1) {
      this.transitions.splice(this.transitions.indexOf(transition), 1);
    }
  },
  hasTransition(){
    var transitions=this.get("transitions");
    if (!transitions){
      return false;
    }else{
      return transitions.length>0;
    }
  },
  getErrorTransition(){
    var errorTrans=null;
    this.get("transitions").forEach(function(tran){
      if (tran.condition==="error"){
        errorTrans=tran;
        return;
      }
    });
    return errorTrans;
  },
  getOkTransitionCount(){
    var count=0;
    this.get("transitions").forEach(function(tran){
      if (tran.condition!=="error"){
        count++;
      }
    });
    return count;
  },
  getDefaultConditionTarget(){
    var target;
    var transitions=this.get("transitions");
    transitions.forEach(function(tran){
      if (tran.condition==="default"){
        target=tran.targetNode;
      }
    });
    return target;
  },
  getNonKillNodeTarget(){
    var nonKillNodeCount=0;
    var nonKillNode;
    var transitions=this.get("transitions");
    transitions.forEach(function(tran){
      if (!tran.targetNode.isKillNode()){
        nonKillNodeCount++;
        nonKillNode=tran.targetNode;
      }
    });
    if (nonKillNodeCount>0){
      return nonKillNode;
    }else{
      console.log("no non kill nodes transitions.");
      return null;
    }
  },
  getDefaultTransitionTarget(){
    if (this.isForkNode()){
      return this.findNodeByType(this,"join");
    }
    var transitions=this.get("transitions");
    if (transitions.length===0){
      return this;
    }else if (transitions.length===1){
      return transitions[0].targetNode;
    }
    var target;
    if (this.isDecisionNode()){
      var defaultTarget=this.getDefaultConditionTarget();
      if (defaultTarget.isKillNode()){
        target=this.getNonKillNodeTarget();
      }else{
        target=defaultTarget;
      }
    }else{
      target=transitions[0].targetNode;
    }
    if (target && target.isPlaceholder()){
      return target.getDefaultTransitionTarget();
    }
    return target;
  },
  getTargets(){
    var targets=[];
    var transitions=this.get("transitions");
    transitions.forEach(function(tran){
      targets.push(tran.get("targetNode"));
    });
    return targets;
  },
  addError(obj){
    this.errorMsgs.push(obj);
  },
  isPlaceholder(){
    return this.get("type")==="placeholder";
  },
  isDecisionNode(){
    return this.get("type")==="decision";
  },
  isForkNode(){
    return this.get("type")==="fork";
  },
  isJoinNode(){
    return this.get("type")==="join";
  },
  isActionNode(){
    return this.get("type")==="action";
  },
  isKillNode(){
    return this.get("type")==="kill";
  },
  isDecisionEnd(){
    return this.get("type")==="decision_end";
  },
  isDefaultKillNode(){
    return this.isKillNode() && this.get("name")===Constants.defaultKillNodeName;
  },
  isEndNode(){
    return this.get("type")==="end";
  }
});
export {Node};
