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
import * as actionJobHandler from '../domain/actionjob_hanlder';
import CommonUtils from "../utils/common-utils";

var ActionTypeResolver=Ember.Object.extend({
  actionJobHandlerMap:null,
  validStandardActionProps:["ok","error","info"],
  init(){
    var settings={schemaVersions:this.schemaVersions};
    this.actionJobHandlerMap=new Map();
    this.actionJobHandlerMap.set("java",actionJobHandler.JavaActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("pig",actionJobHandler.PigActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("hive",actionJobHandler.HiveActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("hive2",actionJobHandler.Hive2ActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("sqoop",actionJobHandler.SqoopActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("shell",actionJobHandler.ShellActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("spark",actionJobHandler.SparkActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("map-reduce",actionJobHandler.MapRedActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("sub-workflow",actionJobHandler.SubWFActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("distcp",actionJobHandler.DistCpJobHandler.create(settings));
    this.actionJobHandlerMap.set("ssh",actionJobHandler.SshActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("email",actionJobHandler.EmailActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("fs",actionJobHandler.FSActionJobHandler.create(settings));
  },
  getActionType(json){
    var self=this;
    var resolvedType=null;
    var problaleActionsTypes=[];
    Object.keys(json).forEach(function functionName(key) {
      if (!self.validStandardActionProps.contains(key) && !CommonUtils.startsWith(key,"_")){
        problaleActionsTypes.push(key);
      }
    });
    if (problaleActionsTypes.length===1){
      return problaleActionsTypes[0];
    }else{
      console.error("Invalid Action spec..");
    }
    return resolvedType;
  },
  getActionJobHandler(jobType){
    if(this.actionJobHandlerMap.has(jobType)) {
      return this.actionJobHandlerMap.get(jobType);
    }else{
      var customActionJobHandler = actionJobHandler.CustomActionJobHandler.create({
        actionType : jobType
      });
      this.actionJobHandlerMap.set(jobType,customActionJobHandler);
      return customActionJobHandler;
    }
  }
});

export {ActionTypeResolver};
