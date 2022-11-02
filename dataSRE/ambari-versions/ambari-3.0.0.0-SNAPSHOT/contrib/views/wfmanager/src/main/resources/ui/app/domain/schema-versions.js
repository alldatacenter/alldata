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
export default Ember.Object.extend({

  supportedVersions : new Map(),
  defaultVersions : new Map(),
  actionVersions: new Map(),
  currentActionVersion:new Map(),
  workflowVersions: ["0.5","0.4.5","0.4","0.3","0.2.5","0.2","0.1"],
  bundleVersions: ["0.2","0.1"],
  coordinatorVersions: ["0.5", "0.4","0.3", "0.2","0.1"],
  actionSchemas:{
    "hive":["0.6","0.5","0.4","0.3","0.2","0.1"],
    "hive2":["0.2","0.1"],
    "sqoop":["0.4","0.3","0.2","0.1"],
    "shell":["0.3","0.2","0.1"],
    "spark":["0.2","0.1"],
    "distcp":["0.2","0.1"],
    "email":["0.2","0.1"]
  },
  init(){
    if(this.supportedVersions.size === 0 && !this.useDefaultSettings){
      this.parseWorkflowSchemaConfig();
      this.parseCoordSchemaConfig();
      this.parseBundleSchemaConfig();
      this.initializeDefaultVersions();
    }else if(this.supportedVersions.size === 0 && this.useDefaultSettings){
      this.loadDefaultSettings()
      this.initializeDefaultVersions();
    }
  },
  loadDefaultSettings(){
    this.supportedVersions.set('workflow', []);
    this.supportedVersions.set('coordinator', []);
    this.supportedVersions.set('bundle', []);
    this.workflowVersions.forEach((value)=>{
      this.supportedVersions.get('workflow').pushObject(value);
    }, this);
    this.coordinatorVersions.forEach((value)=>{
      this.supportedVersions.get('coordinator').pushObject(value);
    }, this);
    this.bundleVersions.forEach((value)=>{
      this.supportedVersions.get('bundle').pushObject(value);
    }, this);
    Object.keys(this.actionSchemas).forEach((key)=>{
      this.supportedVersions.set(key , this.actionSchemas[key]);
    }, this);
  },
  getSupportedVersions(type){
    return this.supportedVersions.get(type);
  },

  getDefaultVersion(type){
    return this.defaultVersions.get(type);
  },

  setDefaultVersion(type, version){
    this.defaultVersions.set(type, version);
  },

  restoreDefaultVersionSettings(){
    this.initializeDefaultVersions();
  },

  initializeDefaultVersions(){
    this.supportedVersions.forEach((value, key) =>{
      var max = Math.max.apply(null,value);
      if(isNaN(max)){
        max = value.reduce((a, b) => a > b?a:b);
      }
      this.defaultVersions.set(key, String(max));
    }, this);
  },
  parseWorkflowSchemaConfig() {
    this.workflowVersions = [];
    var wfSchemaVersions = this.adminConfig["oozie.service.SchemaService.wf.schemas"].trim().split(",");
    wfSchemaVersions = wfSchemaVersions.map(Function.prototype.call, String.prototype.trim).sort();
    wfSchemaVersions.forEach(function(wfSchemaVersion) {
      var wfSchema = wfSchemaVersion.split("-");
      var wfSchemaName = wfSchema[0];
      var wfSchemaType = wfSchema[1];
      var wfSchemaVersionNumber = wfSchema[2].replace(".xsd", "");
      if (wfSchemaType === "action") {
        if(this.supportedVersions.get(wfSchemaName)){
          this.supportedVersions.get(wfSchemaName).pushObject(wfSchemaVersionNumber);
        }else{
          this.supportedVersions.set(wfSchemaName, [wfSchemaVersionNumber]);
        }
      } else if (wfSchemaType === "workflow") {
        if(this.supportedVersions.get(wfSchemaType)){
          this.supportedVersions.get(wfSchemaType).pushObject(wfSchemaVersionNumber);
        }else{
          this.supportedVersions.set(wfSchemaType, [wfSchemaVersionNumber]);
        }
      }
    }.bind(this));
  },
  parseCoordSchemaConfig() {
    var coordSchemaVersions = this.adminConfig["oozie.service.SchemaService.coord.schemas"].trim().split(",");
    coordSchemaVersions = coordSchemaVersions.map(Function.prototype.call, String.prototype.trim).sort();
    coordSchemaVersions.forEach(function(coordSchemaVersion) {
      var coordSchema = coordSchemaVersion.split("-");
      var coordSchemaType = coordSchema[1];
      var coordSchemaVersionNumber = coordSchema[2].replace(".xsd", "");
      if (coordSchemaType === "coordinator") {
        if(this.supportedVersions.get('coordinator')){
          this.supportedVersions.get('coordinator').pushObject(coordSchemaVersionNumber);
        }else{
          this.supportedVersions.set('coordinator', [coordSchemaVersionNumber]);
        }
      }
    }.bind(this));
  },
  parseBundleSchemaConfig() {
    this.bundleVersions = [];
    var bundleSchemaVersions = this.adminConfig["oozie.service.SchemaService.bundle.schemas"].trim().split(",");
    bundleSchemaVersions = bundleSchemaVersions.map(Function.prototype.call, String.prototype.trim).sort();
    bundleSchemaVersions.forEach(function(bundleSchemaVersion) {
      var bundleSchema = bundleSchemaVersion.split("-");
      var bundleSchemaType = bundleSchema[1];
      var bundleSchemaVersionNumber = bundleSchema[2].replace(".xsd", "");
      if (bundleSchemaType === "bundle") {
        if(this.supportedVersions.get('bundle')){
          this.supportedVersions.get('bundle').pushObject(bundleSchemaVersionNumber);
        }else{
          this.supportedVersions.set('bundle', [bundleSchemaVersionNumber]);
        }
      }
    }.bind(this));
  },
  setDefaultActionVersions(){
    var self=this;
    Object.keys(this.actionSchemaMap).forEach(function(key) {
      if (!self.actionVersions.get(key)){
        self.actionVersions.set(key,self.actionSchemaMap[key]);
        self.currentActionVersion.set(key,self.actionSchemaMap[key][0]);
      }
    });
  }
});
