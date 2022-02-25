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
import {Node} from '../domain/node';
import {idGen} from '../domain/id-gen';
var NodeFactory= Ember.Object.extend({
  createStartNode(){
    return this.createNode({id:this.generateNodeId(), type:'start',name:"Start"});
  },
  createEndNode(name){
    return this.createNode({id:this.generateNodeId(), type:'end', name:name});
  },
  createKillNode(name,message){
    return this.createNode({id:this.generateNodeId(), type:"kill", name:name,killMessage:message});
  },
  createPlaceholderNode(target,errorPath){
    var placeholderNode= this.createNode({
      id:this.generateNodeId(), type:"placeholder",
      name:"placeholder" + this.generateName(),
      errorPath:errorPath
    });
    placeholderNode.addTransitionTo(target);
    return placeholderNode;
  },
  createActionNode(actionType,name){
    if (!name){
      name=actionType;
    }
    return this.createNode({id:this.generateNodeId(),type:"action",actionType:actionType, name:name});
  },
  generateDecisionNodeWithJoinPlaceHolder(target){
    var id=this.generateNodeId();
    var decisionNode=this.createEmptyDecisionNode("decision" + this.generateName(),id);
    var joinPlaceholder = this.createNode({id:"decision_end_"+id, type:"decision_end", name:"decision_end" + this.generateName()});
    joinPlaceholder.addTransitionTo(target);
    var leftPlaceholder =this.createPlaceholderNode(joinPlaceholder) ;
    decisionNode.addTransitionTo(leftPlaceholder,"default");
    var rightPlaceholder = this.createPlaceholderNode(joinPlaceholder);
    decisionNode.addTransitionTo(rightPlaceholder);
    return decisionNode;
  },
  generateDecisionNodeWithoutJoinPlaceHolder(target){
    var decisionNode=this.createEmptyDecisionNode("decision");
    var leftPlaceholder =this.createPlaceholderNode(target) ;
    decisionNode.addTransitionTo(leftPlaceholder,"default");
    if (Constants.globalSetting.useAdditionalPlaceholderFlowForDecision){
      var rightPlaceholder = this.createPlaceholderNode(target);
      decisionNode.addTransitionTo(rightPlaceholder);
    }
    return decisionNode;
  },
  generateDecisionNode(target){
    if (Constants.globalSetting.useJoinNodeForDecision){
      return this.generateDecisionNodeWithJoinPlaceHolder(target);
    }else{
      return this.generateDecisionNodeWithoutJoinPlaceHolder(target);
    }
  },
  createEmptyDecisionNode(name,id){
    if (!id){
      id=this.generateNodeId();
    }
    var decisionNode = this.createNode({id:id, type:"decision", name:name});
    return decisionNode;
  },
  createEmptyForkNode(name,id){
    if (!id){
      id=this.generateNodeId();
    }
    var forkNode = this.createNode({id:id, type:"fork", name:name});
    return forkNode;
  },
  createEmptyJoinNode(name,id){
    if (!id){
      id=this.generateNodeId();
    }
    var joinNode=this.createNode({id:id, type:"join", name:name});
    return joinNode;
  },
  generateForkNode(target){
    var forkId=this.generateNodeId();
    var forkNode= this.createEmptyForkNode("fork" + this.generateName(),forkId);
    var joinNode= this.createEmptyJoinNode("join" + this.generateName(),"join_"+forkId);
    joinNode.addTransitionTo(target);

    var leftPlaceholder =this.createPlaceholderNode(joinNode) ;
    forkNode.addTransitionTo(leftPlaceholder);

    var rightPlaceholder = this.createPlaceholderNode(joinNode);
    forkNode.addTransitionTo(rightPlaceholder);
    return forkNode;
  },
  createNode(settings){
    settings.factory=this;
	if (!settings.id){
      settings.id=this.generateNodeId();
    }
    return Node.create(settings);
  },
  generateNodeId(){
    return idGen.generateNodeId();
  },
  generateName(){
    return idGen.generateNodeName();
  },
  resetNodeIdTo(id){
    return idGen.resetTo(id);
  }
});
export{NodeFactory};
