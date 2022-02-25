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
import {NodeFactory} from '../domain/node-factory';
import {SLAMapper} from "../domain/mapping-utils";

var NodeHandler=Ember.Object.extend({
  nodeFactory:NodeFactory.create({}),
  context : {},
  setContext(context){
    this.context = context;
  },
  getContext(){
    return this.context;
  },
  hasMany(){
    return true;
  },
  handleNode(node){
    return {"_name":node.get("name")};
  },
  /* jshint unused:vars */
  handleTransitions(transitions,nodeObj){

  },
  /* jshint unused:vars */
  handleImportNode(type,node){
  },
    /* jshint unused:vars */
  handleImportTransitions(node,json,nodeMap){
  }
});
var StartNodeHandler= NodeHandler.extend({
  hasMany(){
    return false;
  },
  handleNode(node){
    return {};
  },
  handleTransitions(transitions,nodeObj){
    if (transitions.length!==1){
      this.context.addError({node:nodeObj, message:"Invalid Start Node"});
    }
    nodeObj["_to"]=transitions[0].targetNode.getName();
  },
  handleImportNode(type,node,workflow){
    return this.nodeFactory.createStartNode();
  },
  handleImportTransitions(node,json,nodeMap){
    node.addTransitionTo(nodeMap.get(json._to).node);
  }
});
var EndNodeHandler= NodeHandler.extend({
  hasMany(){
    return false;
  },
  handleImportNode(type,node,workflow){
    return this.nodeFactory.createEndNode("end");
  },

});
var KillNodeHandler= NodeHandler.extend({
  type:"kill",
  handleImportNode(type,node,workflow){
    return this.nodeFactory.createKillNode(node._name,node.message);
  },
  handleNode(node){
    var obj= {"_name":node.get("name")};
    if (!Ember.isBlank(node.get("killMessage"))){
      obj["message"]=node.get("killMessage");
    }
    return obj;
  }
});
var ActionNodeHandler= NodeHandler.extend({
  type:"action",
  actionTypeResolver:null,
  schemaVersions: null,
  slaMapper: SLAMapper.create({}),
  init(){
  },
  handleNode(node){
    var nodeObj=this._super(node);
    if (node.domain && !Ember.isBlank(node.domain.credentials)){
      nodeObj._cred=node.domain.credentials;
    }
    return nodeObj;
  },
  handleSla(domain,nodeObj){
    if (domain && domain.slaEnabled){
      return this.slaMapper.hanldeGeneration(domain.slaInfo,nodeObj);
    }
  },

  handleTransitions(transitions,nodeObj){
    transitions.forEach(function(tran){
      if (!tran.condition){
        nodeObj["ok"]={"_to":tran.getTargetNode().getName()};
      }else if (tran.condition="error"){
        nodeObj["error"]={"_to":tran.getTargetNode().getName()};
      }
    });
  },
  handleImportNode(type,nodeJson,workflow,xmlDoc){
    var actionType=this.get("actionTypeResolver").getActionType(nodeJson);

    var actionNode = this.nodeFactory.createActionNode(actionType,nodeJson._name);
    if (actionType===null){
      console.error("cannot handle unsupported node:"+nodeJson);//TODO error handling...
      return actionNode;
    }
    var actionJobHandler=this.get("actionTypeResolver").getActionJobHandler(actionType);
    if(actionJobHandler){
      actionJobHandler.handleImport(actionNode,nodeJson[actionType],xmlDoc);
    }
    if (nodeJson.info && nodeJson.info.__prefix==="sla") {
      actionNode.domain.slaEnabled=true;
      this.slaMapper.handleImport(actionNode.domain,nodeJson.info,"slaInfo");
    }
    if(nodeJson._cred){
      actionNode.domain.credentials=nodeJson._cred;
    }
    return actionNode;
  },
  handleImportTransitions(node,json,nodeMap){
    node.addTransitionTo(nodeMap.get(json.ok._to).node);
    if (json.error && json.error._to){
      node.addTransitionTo(nodeMap.get(json.error._to).node,"error");
    }
  }
});
var DecisionNodeHandler= NodeHandler.extend({
  type:"decision",
  handleTransitions(transitions,nodeObj){
    var swithCaseObj={"case":[]};
    nodeObj["switch"]=swithCaseObj;
    var caseObjects=swithCaseObj["case"];
    transitions.forEach(function(tran){
      if (tran.condition!=="default"){
        caseObjects.push({"_to":tran.getTargetNode().getName(),"__text":tran.condition});
      }else{
        swithCaseObj['default']={};
        swithCaseObj['default']["_to"]=tran.getTargetNode().getName();
      }

    });
  },
  handleImportNode(type,node,workflow){
    return this.nodeFactory.createEmptyDecisionNode(node._name);
  },
  handleImportTransitions(node,json,nodeMap){
    var self=this;
    var defaultPath=json.switch.default._to;
    var placeholder=self.nodeFactory.createPlaceholderNode(nodeMap.get(defaultPath).node);
    node.addTransitionTo(placeholder,"default");
    var cases=[];
    if (Ember.isArray(json.switch.case)){
      cases=json.switch.case;
    }else{
      cases.push(json.switch.case);
    }
    cases.forEach(function(caseExpr){
      var placeholder=self.nodeFactory.createPlaceholderNode(nodeMap.get(caseExpr._to).node);
      node.addTransitionTo(placeholder,caseExpr.__text);
    });
  }
});
var ForkNodeHandler= NodeHandler.extend({
  type:"fork",
  handleTransitions(transitions,nodeObj){
    var pathObjects=[];
    nodeObj["path"]=pathObjects;
    transitions.forEach(function(tran){
      pathObjects.push({"_start":tran.getTargetNode().getName()});
    });
  },
  handleImportNode(type,node,workflow){
    return this.nodeFactory.createEmptyForkNode(node._name);
  },
  handleImportTransitions(node,json,nodeMap){
    var paths=Ember.isArray(json.path)?json.path:[json.path];
    paths.forEach(function(path){
      node.addTransitionTo(nodeMap.get(path._start).node);
    });
  }
});
var JoinNodeHandler= NodeHandler.extend({
  type:"join",
  handleTransitions(transitions,nodeObj){
    transitions.forEach(function(tran){
      nodeObj["_to"]=tran.getTargetNode().getName();
    });
  },
  handleImportNode(type,node,workflow){
    return this.nodeFactory.createEmptyJoinNode(node._name);
  },
  handleImportTransitions(node,json,nodeMap){
    node.addTransitionTo(nodeMap.get(json._to).node);
  }
});
export{NodeHandler,StartNodeHandler,EndNodeHandler,KillNodeHandler,ActionNodeHandler,DecisionNodeHandler,ForkNodeHandler,JoinNodeHandler};
