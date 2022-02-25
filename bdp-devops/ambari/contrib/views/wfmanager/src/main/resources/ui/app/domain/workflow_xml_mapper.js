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
import * as nodeHandler from '../domain/node-handler';
import {SLAMapper} from "../domain/mapping-utils";
import {ActionTypeResolver} from "../domain/action-type-resolver";

import {MappingMixin,ConfigurationMapper} from "../domain/mapping-utils";
var WorkflowXmlMapper= Ember.Object.extend({
  nodeHandlerMap:null,
  globalConfigHandler:null,
  actionTypeResolver:null,
  slaMapper: SLAMapper.create({}),
  schemaVersions:null,
  init: function() {
    this.actionTypeResolver=ActionTypeResolver.create({schemaVersions:this.schemaVersions});
    this.set("globalConfigHandler",GlobalConfigHandler.create({}));
    this.set("slaMapper",SLAMapper.create({}));
    this.nodeHandlerMap=new Map();
    this.nodeHandlerMap.set("start",nodeHandler.StartNodeHandler.create({}));
    this.nodeHandlerMap.set("end",nodeHandler.EndNodeHandler.create({}));
    this.nodeHandlerMap.set("action",nodeHandler.ActionNodeHandler.create({actionTypeResolver:this.actionTypeResolver}));
    this.nodeHandlerMap.set("decision",nodeHandler.DecisionNodeHandler.create({}));
    this.nodeHandlerMap.set("fork",nodeHandler.ForkNodeHandler.create({}));
    this.nodeHandlerMap.set("join",nodeHandler.JoinNodeHandler.create({}));
    this.nodeHandlerMap.set("kill",nodeHandler.KillNodeHandler.create({}));
  },
  getNodeHandler(type){
    return this.nodeHandlerMap.get(type);
  },
  getGlobalConfigHandler(){
    return this.globalConfigHandler;
  },
  getActionJobHandler(jobType){
    return this.actionTypeResolver.getActionJobHandler(jobType);
  },
  handleSLAMapping(sla,workflowObj){
    this.get("slaMapper").hanldeGeneration(sla,workflowObj);
  },
  handleSLAImport(workflow,infoJson){
    this.slaMapper.handleImport(workflow,infoJson,"sla");
  },
  handleCredentialsGeneration(credentials,workflowObj){
    if (credentials && credentials.length>0){
      workflowObj["credentials"]={"credential":[]};
      credentials.forEach(function(credential){
        var credJson={_name:credential.name,_type:credential.type,property:[]};
        if (credential.property){
          credential.property.forEach(function(prop){
            credJson.property.push({"name":prop.name,"value":prop.value});
          });
        }
        workflowObj.credentials.credential.push(credJson);
      });
    }
  },
  handleCredentialImport(workflow,credJson){
    if (!credJson || !credJson.credential){
      return;
    }
    workflow.credentials.clear();
    var credntialsJson=Ember.isArray(credJson.credential)?credJson.credential:[credJson.credential];
    credntialsJson.forEach(function(cred){
      var credential={
        name:cred._name,
        type:cred._type,
        property:Ember.A([])
      };
      if (cred.property){
        if (!Ember.isArray(cred.property)){
          cred.property=[cred.property];
        }
        cred.property.forEach(function(property){
          credential.property.push({"name":property.name,"value":property.value});
        });
      }
      workflow.credentials.push(credential);
    });
  },
  hanldeParametersGeneration(parameters,workflowObj){
    if (!parameters || !parameters.configuration){
      return;
    }
    if (parameters.configuration.property.length>0){
      workflowObj.parameters={property:[]};
    }
    parameters.configuration.property.forEach(function(prop){
      workflowObj.parameters.property.push({"name":prop.name,"value":prop.value});
    });
  },
  handleParametersImport(workflow,parameters){
    if (!parameters|| !parameters.property){
      return;
    }
    workflow.parameters={"configuration":{property:[]}};
    parameters.property.forEach(function(prop){
      workflow.parameters.configuration.property.push({"name":prop.name,"value":prop.value});
    });
  }
});
var GlobalConfigHandler=Ember.Object.extend(MappingMixin,{
  mapping:null,
  configurationMapper:ConfigurationMapper.create({}),
  init(){
    this.mapping=[
      {xml:"job-tracker",domain:"jobTracker"},
      {xml:"name-node",domain:"nameNode"},
      {xml:"configuration",customHandler:this.configurationMapper}
    ];
  },

  handle(domainObject,nodeObj){
    if (!domainObject){
      return;
    }
    var globalObj={};
    nodeObj["global"]=globalObj;
    this.handleMapping(domainObject,globalObj,this.mapping);
  }
});
export {WorkflowXmlMapper};
