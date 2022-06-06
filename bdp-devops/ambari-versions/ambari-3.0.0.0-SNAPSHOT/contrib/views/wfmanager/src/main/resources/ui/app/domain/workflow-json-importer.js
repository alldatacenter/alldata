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
import {Workflow} from '../domain/workflow';
import {NodeFactory} from '../domain/node-factory';
var WorkflowJsonImporter= Ember.Object.extend({
    nodeFactory:NodeFactory.create({}),
    importWorkflow(workflowJsonStr){
      if (!workflowJsonStr){
        return null;
      }
      try{
        var workflowJson=  JSOG.parse(workflowJsonStr);
        var workflow=Workflow.create({});
        workflow.initialize();
        workflow.set("name",workflowJson.name);
        workflow.schemaVersions.workflowVersion = workflowJson.schemaVersions.workflowVersion;
        workflow.schemaVersions.actionVersions = new Map(JSON.parse(workflowJson.schemaVersions.actionVersions));
        workflow.sla = workflowJson.sla;
        workflow.slaEnabled = workflowJson.slaEnabled;
        workflow.credentials = workflowJson.credentials;
        workflow.globalSetting = workflowJson.globalSetting;
        workflow.parameters = workflowJson.parameters;
        this.restoreKillNodes(workflowJson.killNodes,workflow);
        var nodeMap= new Map();
        var startNode=this.visitNode(workflowJson.startNode,nodeMap);
        workflow.set("startNode",startNode);
        var maxId=0;
        for(let value of nodeMap.keys()){
            var id=Number.parseInt(value.substr(5));
            if (id>maxId){
              maxId=id;
            }
        }
        this.nodeFactory.resetNodeIdTo(maxId+1);
        return workflow;
      }catch(e){
        console.error(e);
        return null;
      }
    },
    visitNode(nodeJson,nodeMap){
      var self=this;
      if (!nodeJson){
        return;
      }
      var node;
      if (!nodeMap.has(nodeJson.id)){
        node=this.nodeFactory.createNode({id:nodeJson.id, type:nodeJson.type,name:nodeJson.name,actionType:nodeJson.actionType,killMessage:nodeJson.killMessage});
        node.set("domain",nodeJson.domain);
        node.set("errorMsgs",nodeJson.errorMsgs);
        node.set("errors",nodeJson.errors);
        if(nodeJson.customMapping){
          node.set('customMapping', nodeJson.customMapping);
        }
        nodeMap.set(node.id,node);
        if (nodeJson.transitions){
          nodeJson.transitions.forEach(function(nodeTran){
            var transitions=nodeTran;
            if (!Ember.isArray(nodeTran)){
              transitions=[nodeTran];
            }
            transitions.forEach(function(tran){
              var targetNodeJson=tran.targetNode;
              var targetNode=self.visitNode(targetNodeJson,nodeMap);
              node.addTransitionTo(targetNode,tran.condition);
            });
          });
        }
      }else{
        node=nodeMap.get(nodeJson.id);
      }
      return node;
    },
    restoreKillNodes(killnodesJson,workflow){
      if (!killnodesJson){
        return;
      }
      workflow.resetKillNodes();
      killnodesJson.forEach(function(killNodeJson){
        workflow.createKillNode(killNodeJson.name,killNodeJson.killMessage);
      });
    
    }
});
export {WorkflowJsonImporter};
