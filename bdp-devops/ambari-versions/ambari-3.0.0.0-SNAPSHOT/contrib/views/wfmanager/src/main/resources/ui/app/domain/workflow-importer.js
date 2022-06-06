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
import CommonUtils from "../utils/common-utils";
import {Workflow} from '../domain/workflow';
import {WorkflowXmlMapper} from '../domain/workflow_xml_mapper';
import SchemaVersions from '../domain/schema-versions';
import Constants from '../utils/constants';

var WorkflowImporter= Ember.Object.extend({
  workflowMapper:null,
  x2js : new X2JS(),
  schemaVersions : SchemaVersions.create({}),
  importWorkflow(workflowXml){
    var workflow=Workflow.create({});
    var errors=[];
    workflow.initialize();
    this.workflowMapper=WorkflowXmlMapper.create({schemaVersions:workflow.schemaVersions});
    return this.processWorkflowXml(workflowXml,workflow, errors);
  },
  processWorkflowXml(workflowXml,workflow, errors){
    var xmlDoc=  Ember.$.parseXML( workflowXml );
    if (xmlDoc && xmlDoc.getElementsByTagName('action').length>Constants.flowGraphMaxNodeCount){
      errors.push({message: "Workflow is too large to be imported",dismissable:true});
      return {workflow:null, errors: errors};
    }
    var workflowJson= this.get("x2js").xml_str2json(workflowXml);
    if (!workflowJson || !workflowJson["workflow-app"]){
      errors.push({message: "Could not import invalid workflow",dismissable:true});
      return {workflow:null, errors: errors};
    }
    var workflowAppJson=workflowJson["workflow-app"];
    var workflowVersion=CommonUtils.extractSchemaVersion(workflowAppJson._xmlns);
    var maxWorkflowVersion = Math.max.apply(Math, this.get('schemaVersions').getSupportedVersions('workflow'));
    if (workflowVersion > maxWorkflowVersion) {
      errors.push({message: "Unsupported workflow version - " + workflowVersion});
    } else {
      workflow.schemaVersions.workflowVersion = workflowVersion;
    }
    this.processWorkflowActionVersions(workflowAppJson, workflow, errors);

    if (workflowAppJson.info && workflowAppJson.info.__prefix==="sla") {
      workflow.slaEnabled=true;
      this.workflowMapper.handleSLAImport(workflow,workflowAppJson.info);
    }
    this.workflowMapper.handleCredentialImport(workflow,workflowAppJson.credentials);
    this.workflowMapper.handleParametersImport(workflow,workflowAppJson.parameters);
    var nodeMap=this.setupNodeMap(workflowAppJson,workflow,Ember.$(xmlDoc));
    this.setupTransitions(workflowAppJson,nodeMap);
    workflow.set("startNode",nodeMap.get("start").node);
    let globalProperties = workflowJson["workflow-app"].global.configuration.property;
    if(workflowJson["workflow-app"].global) {
      if(Ember.isArray(globalProperties)) {
        workflow.set("globalSetting", workflowJson["workflow-app"].global);
      } else  {
        workflow.set("globalSetting", {configuration : { property:[globalProperties] }} );
      }
    }

    this.populateKillNodes(workflow,nodeMap);
    return {workflow: workflow, errors: errors};
  },
  processWorkflowActionVersions(workflowAppJson, workflow, errors) {
    var importedWfActionVersions = Ember.Map.create();
    var actions=workflowAppJson.action ? (workflowAppJson.action.length?workflowAppJson.action:[workflowAppJson.action]):[];
    actions.forEach(function(wfAction) {
      var wfActionType = Object.keys(wfAction)[0];
      var wfActionXmlns = wfAction[wfActionType]._xmlns;
      if (!wfActionXmlns) {
        return;
      }
      var wfActionVersion = CommonUtils.extractSchemaVersion(wfActionXmlns);
      if (importedWfActionVersions.get(wfActionType)) {
        importedWfActionVersions.get(wfActionType).push(wfActionVersion);
      } else {
        importedWfActionVersions.set(wfActionType, [wfActionVersion]);
      }
    });

    importedWfActionVersions._keys.forEach(function(wfActionType){
      if(!CommonUtils.isSupportedAction(wfActionType)){
        return;
      }
      var maxImportedActionVersion = Math.max.apply(null,importedWfActionVersions.get(wfActionType));
      var supportedVersions = this.get('schemaVersions').getSupportedVersions(wfActionType);
      importedWfActionVersions.get(wfActionType).forEach((version)=>{
        if(supportedVersions.indexOf(version) === -1){
          errors.push({message: "Unsupported " + wfActionType + " version - " + maxImportedActionVersion});
        }else{
          workflow.schemaVersions.actionVersions.set(wfActionType, maxImportedActionVersion);
        }
      }, this);
    }, this);
  },
  processActionNode(nodeMap,action){
    var actionMapper=this.get("workflowMapper").getNodeHandler("action");
    var actionNode=actionMapper.handleImportNode(action);
    nodeMap.set(actionNode.getName(),actionNode);
  },
  setupNodeMap(workflowAppJson,workflow,xmlDoc){
    var self=this;
    workflow.set("name",workflowAppJson["_name"]);
    var nodeMap=new Map();
    Object.keys(workflowAppJson).forEach(function (key) {
      var nodeHandler=self.workflowMapper.getNodeHandler(key);
      if (nodeHandler){
        if (Ember.isArray(workflowAppJson[key])){
          workflowAppJson[key].forEach(function(jsonObj){
            var actionDom = xmlDoc.find("action[name='" + jsonObj._name + "']");
            var node = nodeHandler.handleImportNode(key,jsonObj,workflow,actionDom);
            nodeMap.set(jsonObj._name,{json:jsonObj,node:node});
          });
        }else{
          if ('action'===key){//action handler.
          	var actionDom=xmlDoc.find("action[name='"+workflowAppJson[key]._name+ "']");
          }
          var node=nodeHandler.handleImportNode(key,workflowAppJson[key],workflow,actionDom);
          if (!workflowAppJson[key]._name){
            nodeMap.set(key,{json:workflowAppJson[key],node:node});
          }else{
            nodeMap.set(workflowAppJson[key]._name,{json:workflowAppJson[key],node:node});
          }
        }
      }
    });
    return nodeMap;
  },
  setupTransitions(workflowAppJson,nodeMap){
    var self=this;
    nodeMap.forEach(function(entry,key){
      var node=entry.node;
      if (!node){
        console.error("could not process:",key);//TODO error handling...
        return;
      }
      var json=entry.json;
      var nodeHandler=self.workflowMapper.getNodeHandler(node.get("type"));
      if (!nodeHandler){
        console.error("could not process:",node.get("type"));//TODO error handling...
      }
      nodeHandler.handleImportTransitions(node,json,nodeMap);
    });
  },
  getNodeIds(nodeMap){
    var ids=[];
    nodeMap.forEach(function(entry){
      var node=entry.node;
      ids.push(node.id);
    });
    return ids;
  },
  getNodeNames(nodeMap){
    var names=[];
    nodeMap.forEach(function(entry){
      var node=entry.node;
      names.push(node.id);
    });
    return names;
  },
  populateKillNodes(workflow,nodeMap){
    if (this.containsKillNode(nodeMap)){
      workflow.resetKillNodes();
    }
    nodeMap.forEach(function(entry){
      var node=entry.node;
      if (node.isKillNode()){
        workflow.get("killNodes").pushObject(node);
      }
    });
  },
  containsKillNode(nodeMap){
    var containsKillNode=false;
    nodeMap.forEach(function(entry){
      var node=entry.node;
      if (node.isKillNode()){
        containsKillNode=true;
      }
    });
    return containsKillNode;
  }
});
export {WorkflowImporter};
