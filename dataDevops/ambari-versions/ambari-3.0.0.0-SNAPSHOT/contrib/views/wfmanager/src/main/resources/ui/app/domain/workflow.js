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
import {FindNodeMixin} from '../domain/findnode-mixin';
import {NodeFactory} from '../domain/node-factory';
import SchemaVersions from '../domain/schema-versions';
import {NodeVisitor} from '../domain/node-visitor';
import {SlaInfo} from '../domain/sla-info';
var Workflow= Ember.Object.extend(FindNodeMixin,{
  name:"",
  startNode:null,
  globalSetting:null,
  parameters: null,
  usePlaceholders: true,
  killNodes : null,
  nodeVisitor : null,
  nodeFactory:NodeFactory.create({}),
  credentials : Ember.A([]),
  initialize(){
    this.nodeVisitor=NodeVisitor.create({});
    var schemaVersions=SchemaVersions.create({});
    this.schemaVersions = {};
    this.schemaVersions.workflowVersion = schemaVersions.getDefaultVersion('workflow');
    this.set("xmlns","uri:oozie:workflow:"+this.schemaVersions.workflowVersion);
    var actionsMap = new Map();
    Constants.actions.forEach((action)=>{
      if(action.supportsSchema){
        actionsMap.set(action.name, schemaVersions.getDefaultVersion(action.name));
      }
    });
    this.schemaVersions.actionVersions = actionsMap;
    var src =this.nodeFactory.createStartNode();
    var dest =this.nodeFactory.createEndNode("end");
    this.set("startNode", src);
    this.set("killNodes",Ember.A([]));
    this.set("globalSetting",null);
    this.set("name","");
    this.set("draftVersion", "v1");
    this.set("parameters", null);
    this.set("credentials", Ember.A([]));
    this.set("sla", SlaInfo.create({}));
    this.appendDefaultKillNode();
    src.addTransitionTo(dest);
  },
  appendDefaultKillNode(){
    this.createKillNode(Constants.defaultKillNodeName,"${wf:errorMessage(wf:lastErrorNode())}");
  },
  createKillNode(name, message){
    var killNode=this.nodeFactory.createKillNode(name,message);
    this.get("killNodes").pushObject(killNode);
  },
  resetKillNodes(){
    this.set("killNodes",Ember.A([]));
  },
  resetWorfklow(){
    //TODO idGen.reset();
    this.initialize();
  },

  findJoinNode(node){
    if (node.isDecisionNode()){
      return this.findCommonTargetNode(this.startNode,node);
    }else if (node.isForkNode()) {
      return node.getDefaultTransitionTarget();
    }else{
      return null;
    }
  },
  addBranch(sourceNode){
    var target=this.findJoinNode(sourceNode);
    if (this.get("usePlaceholders")){
      var placeholderNode=this.nodeFactory.createPlaceholderNode(target) ;
      sourceNode.addTransitionTo(placeholderNode);
    }else{
      sourceNode.addTransitionTo(target);
    }
  },
  addDecisionBranch(settings){
    if (!settings.targetNode){
      return;
    }
    var sourceNode=settings.sourceNode;
    var insertNodeOnPath=settings.newNodeType?true:false;
    var target=settings.targetNode;
    if (!insertNodeOnPath){
      if (this.get("usePlaceholders")){
        var placeholderNode=this.nodeFactory.createPlaceholderNode(target) ;
        sourceNode.addTransitionTo(placeholderNode,settings.condition);
      }else{
        sourceNode.addTransitionTo(target,settings.condition);
      }
    }else{
      //not yet implemented
    }
  },
  getDefaultKillNode(){
    var defaultKillNode = this.get('killNodes').findBy('name',Constants.defaultKillNodeName);
    return defaultKillNode ? defaultKillNode : this.get('killNodes').objectAt(0);
  },
  generatedNode(target,type,settings){
    var generatedNode=null;
    if ("decision" === type){
      generatedNode=this.nodeFactory.generateDecisionNode(target);
    }else  if ("fork" === type){
      generatedNode=this.nodeFactory.generateForkNode(target);
    }else  if ("kill" === type){
      generatedNode = this.nodeFactory.createKillNode(settings.name);
      //source.deleteCurrentKillNode();//TODO how to get source...
    }else{
      generatedNode = this.nodeFactory.createActionNode(type);
      generatedNode.addTransitionTo(target);
      generatedNode.addTransitionTo(this.getDefaultKillNode(), "error");
    }
    return generatedNode;
  },

  addKillNode(source,settings){
    var generatedNode=this.generatedNode(null,"kill",settings);
    return source.addTransitionTo(generatedNode,"error");
  },
  addNode(transition,type,settings, id) {
    var target=transition.targetNode;
    var computedTarget=target;
    if (target && target.isPlaceholder()){
      computedTarget=target.getTargets()[0];
    }
    var generatedNode=this.generatedNode(computedTarget,type,settings);
    if(id){
      generatedNode.name = generatedNode.name+ "_"+ id;
    }
    var sourceNode=transition.source;
    if (sourceNode && sourceNode.isPlaceholder()) {
      var orignalTransition=this.findTransitionTo(this.startNode,sourceNode.id);
      orignalTransition.targetNode=generatedNode;
      if (orignalTransition.isOnError()){
        orignalTransition.source.set("errorNode",generatedNode);
      }
    } else {
      transition.targetNode=generatedNode;
      if (transition.isOnError()){
        transition.source.set("errorNode",generatedNode);
      }
    }
    return generatedNode;
  },
  deleteKillNode(node){
    let killNodes = this.get("killNodes");
    var killNodeReferenced=false;
    this.nodeVisitor.process(this.startNode,function(n){
      if (n.errorNode && n.errorNode.name===node.name){
        killNodeReferenced=true;
      }
    });
    if (killNodeReferenced){
      return{
        status: false,
        message: "Kill node is being referenced by other nodes."
      };
    }
    for(var i=0; i<killNodes.length; i++){
      if(node.id === killNodes[i].id){
        this.get("killNodes").removeObject(killNodes[i]);
        break;
      }
    }
    return {
      status:true
    };
  },
  deleteNode(node,transitionslist){
    var self=this;
    var target=node.getDefaultTransitionTarget();
    if (target && !target.isEndNode() && (node.isForkNode()|| node.isDecisionNode())){
      target=this.findJoinNode(node);
      if (!target){//A bug will give target as null if the decision has single path.
        target=node.getDefaultTransitionTarget();
      }
      if (target.isJoinNode()){
        target=target.getDefaultTransitionTarget();
      }
    }
    transitionslist.forEach(function(tran){
      var sourceNode=tran.sourceNode;
      var joinNode;
      if (sourceNode.isDecisionNode()){
        joinNode=self.findJoinNode(sourceNode);
        if (joinNode===target){
          if (tran.isDefaultCasePath()){
            tran.targetNode=self.nodeFactory.createPlaceholderNode(target);
          }else   if (sourceNode.getOkTransitionCount()>2){
            sourceNode.removeTransition(tran);
          }else{
            tran.targetNode=self.nodeFactory.createPlaceholderNode(target);
          }
        }else{
          tran.targetNode=target;
        }
      }else if (sourceNode.isForkNode()){
        joinNode=self.findJoinNode(sourceNode);
        if (joinNode===target){
          if (sourceNode.getOkTransitionCount()>2){
            sourceNode.removeTransition(tran);
          }else{
            tran.targetNode=self.nodeFactory.createPlaceholderNode(target);
          }
        }else{
          tran.targetNode=target;
        }
      }else{
        tran.targetNode=target;
        if (tran.isOnError()){
          tran.sourceNode.set("errorNode",target );
        }
      }
    });
  },
  deleteTransition(transition){
    transition.sourceNode.removeTransition(transition);
  },
  deleteEmptyTransitions(transitionslist){
    var self=this;
    transitionslist.forEach(function(tran){
      var sourceNode=this.findNodeById(self.startNode,tran.sourceNodeId);
      if (sourceNode.isForkNode()&& tran.getTargetNode().isJoinNode()){
        sourceNode.removeTransition(tran);
      }
    });
  },
  findTransistionsToNode(matchingNode){
    var transitionslist=[];
    this.findTransistionsToNodeInternal(this.startNode,matchingNode,transitionslist);
    return transitionslist;
  },
  findTransistionsToNodeInternal(node,matchingNode,transitionslist){
    var self=this;
    if (node.transitions){
      node.transitions.forEach(function(tran){
        if (tran.getTargetNode()===matchingNode){
          transitionslist.push(tran);
        }
        self.findTransistionsToNodeInternal(tran.getTargetNode(),matchingNode,transitionslist);
      });
    }
  }
});
export {Workflow};
