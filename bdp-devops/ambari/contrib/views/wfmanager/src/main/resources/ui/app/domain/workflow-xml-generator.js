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
import {WorkflowXmlMapper} from '../domain/workflow_xml_mapper';
import {NodeVisitor} from '../domain/node-visitor';
import CustomMappingHandler from "../domain/custom-mapping-handler";

var WorkflowGenerator= Ember.Object.extend({
  workflowMapper:null,
  x2js : new X2JS({useDoubleQuotes:true,escapeMode:false}),
  workflow:null,
  workflowContext : {},
  nodeVisitor:null,
  ignoreErrors:false,
  init(){
    this.workflowMapper=WorkflowXmlMapper.create({schemaVersions:this.workflow.schemaVersions});
    this.nodeVisitor=NodeVisitor.create({});
  },
  process(){
    if (!this.ignoreErrors && (!this.workflow.get("name") || this.workflow.get("name").trim()==="")){
      this.workflowContext.addError({message : "Workflow name is mandatory"});
      return;
    }
    var workflowObj={"workflow-app":{}};
    this.get("workflowMapper").getGlobalConfigHandler().handle(this.workflow.get("globalSetting"),workflowObj["workflow-app"]);
    this.visitNode(workflowObj,this.workflow.startNode);
    if (this.workflow.slaEnabled===true){
      this.get("workflowMapper").handleSLAMapping(this.workflow.sla,workflowObj["workflow-app"]);
    }
    this.get("workflowMapper").handleCredentialsGeneration(this.workflow.credentials,workflowObj["workflow-app"]);
    this.get("workflowMapper").hanldeParametersGeneration(this.workflow.parameters,workflowObj["workflow-app"]);
    var reordered={"workflow-app":{}};
    var srcWorkflowApp=workflowObj["workflow-app"];
    var targetWorkflowApp=reordered["workflow-app"];
    targetWorkflowApp["_name"]=this.workflow.get("name");
    this.copyProp(srcWorkflowApp,targetWorkflowApp,["parameters","global","credentials","start","decision","fork","join","action","kill","end","info"]);
    targetWorkflowApp["_xmlns"]="uri:oozie:workflow:"+this.workflow.schemaVersions.workflowVersion;
    if (this.slaInfoExists(targetWorkflowApp)){
      targetWorkflowApp["_xmlns:sla"]="uri:oozie:sla:0.2";
    }
    var xmlAsStr = this.get("x2js").json2xml_str(reordered);
    return xmlAsStr;
  },

  getActionNodeXml(actionNodeName, actionNodeType) {
    var workflowObj={"workflow-app":{}};
    this.visitNode(workflowObj, this.workflow.startNode);
    var workflowActions = workflowObj["workflow-app"].action;
    var actionNodes = workflowActions.filter(function (workflowActionNode) {
        return workflowActionNode._name === actionNodeName;
      });
    if (actionNodes.length>0) {
      var actionNode = {};
      actionNode[actionNodeType] = actionNodes[0][actionNodeType];
      return this.get("x2js").json2xml_str(actionNode);
    }
    return "";
  },
  slaInfoExists(workflowApp){
    if (workflowApp.info){
      return true;
    }
    var slaExists= false;
    if (!workflowApp.action){
      return false;
    }
    workflowApp.action.forEach(function(action){
      if (action.info){
        slaExists=true;
      }
    });
    return slaExists;
  },
  copyProp(src,dest,props){
    props.forEach(function(prop){
      if (src[prop]){
        dest[prop]=src[prop];
      }
    });
  },

  visitNode(workflowObj,node,visitedNodes){
    if (!visitedNodes){
      visitedNodes=[];
    }
    if (visitedNodes.contains(node.get("name"))){
      return;
    }
    visitedNodes.push(node.get("name"));
    var self=this;
    var workflowApp=workflowObj["workflow-app"];
    if (node.isPlaceholder()){
      return self.visitNode(workflowObj,node.transitions[0].targetNode,visitedNodes);
    }
    var nodeHandler=this.get("workflowMapper").getNodeHandler(node.type);
    nodeHandler.setContext(this.workflowContext);
    var nodeObj=nodeHandler.handleNode(node);
    if (node.type==='action'){
      var jobHandler=this.get("workflowMapper").getActionJobHandler(node.actionType);
      if (jobHandler){

        jobHandler.setContext(this.workflowContext);
        if(/\s/g.test(node.name)) {
          this.workflowContext.addError({node : node, message : "Action name cannot contain white space."});
        }
        if (!self.ignoreErrors && !node.get("domain")){
            this.workflowContext.addError({node : node, message : "Action Properties are empty"});
        }else{
          if(node.customMapping){
            CustomMappingHandler.setMapping(node.name, node.customMapping);
          }
          jobHandler.handle(node.get("domain"),nodeObj,node.get("name"));
          if (!self.ignoreErrors){
            var errors=jobHandler.validate(node.get("domain"));
            if (errors && errors.length>0){
              errors.forEach(function(err){
                this.workflowContext.addError({node : node, message : err.message});
              }.bind(this));
            }
          }
        }
      }else{
        if (!self.ignoreErrors){
          this.workflowContext.addError({node : node, message : "Unknown action:"+node.actionType});
        }
      }
    }
    if (nodeHandler.hasMany()){
        if (!workflowApp[node.type]){
            workflowApp[node.type]=[];
        }
      workflowApp[node.type].push(nodeObj);
    }else{
      workflowApp[node.type]=nodeObj;
    }
    nodeHandler.handleTransitions(node.transitions,nodeObj);
    if (node.transitions){
      node.transitions.forEach(function(tran){
        self.visitNode(workflowObj,tran.targetNode,visitedNodes);
      });
    }
    if (node.isActionNode()){
      nodeHandler.handleSla(node.domain,nodeObj);
    }
  }
});
export {WorkflowGenerator};
