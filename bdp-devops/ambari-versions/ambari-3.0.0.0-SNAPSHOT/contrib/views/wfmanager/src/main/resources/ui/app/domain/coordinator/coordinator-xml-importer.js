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
import { Coordinator } from '../coordinator/coordinator';
import SchemaVersions from '../schema-versions';
import CommonUtils from "../../utils/common-utils";
import {SLAMapper} from "../../domain/mapping-utils";
import {SlaInfo} from '../../domain/sla-info';

var CoordinatorXmlImporter= Ember.Object.extend({
  x2js : new X2JS(),
  schemaVersions: SchemaVersions.create({}),
  slaMapper: SLAMapper.create({}),
  importCoordinator (xml){
    var coordinatorJson = this.get("x2js").xml_str2json(xml);
    return this.processCoordinatorXML(coordinatorJson);
  },
  createNewCoordinator(){
    return Coordinator.create({
      parameters : {
        configuration :{
          property : Ember.A([])
        }
      },
      controls : Ember.A([]),
      datasets : Ember.A([]),
      dataInputs : Ember.A([]),
      inputLogic : null,
      dataOutputs : Ember.A([]),
      workflow : {
        appPath : undefined,
        configuration :{
          property : Ember.A([])
        }
      },
      frequency : {
        type : undefined,
        value : undefined
      },
      start : {
        value : undefined,
        displayValue : undefined,
        type : 'date'
      },
      end : {
        value : undefined,
        displayValue : undefined,
        type : 'date'
      },
      timezone : 'UTC',
      dataInputType : 'simple',
      slaInfo : SlaInfo.create({}),
      schemaVersions : {
        coordinatorVersion : this.get('schemaVersions').getDefaultVersion('coordinator')
      }
    });
  },
  processCoordinatorXML(coordinatorJson){
    var errors=Ember.A([]);
    if (!coordinatorJson || !coordinatorJson["coordinator-app"]){
      errors.push({message: "Could not import invalid coordinator",dismissable:true});
      return {coordinator:null, errors: errors};
    }
    var coordinatorApp = coordinatorJson["coordinator-app"];
    var coordinator = this.createNewCoordinator();
    coordinator.name = coordinatorApp._name;
    var coordinatorVersion=CommonUtils.extractSchemaVersion(coordinatorApp._xmlns);
    var maxCoordinatorVersion = Math.max.apply(Math, this.get('schemaVersions').getSupportedVersions('coordinator'));
    if (coordinatorVersion > maxCoordinatorVersion) {
      errors.push({message: "Unsupported coordinator version - " + coordinatorVersion});
    }
    coordinator.schemaVersions.coordinatorVersion = coordinatorVersion;
    var frequency = coordinatorApp._frequency;
    if(CommonUtils.startsWith(frequency,'${coord:')){
      coordinator.frequency.type = frequency.substring(frequency.indexOf(':')+1, frequency.indexOf('('));
      coordinator.frequency.value = frequency.substring(frequency.indexOf('(')+1, frequency.indexOf(')'));
    }else{
      coordinator.frequency.type = 'cron';
      coordinator.frequency.value = frequency;
    }
    coordinator.start = this.extractDateField(coordinatorApp._start);
    coordinator.end = this.extractDateField(coordinatorApp._end);
    coordinator.timezone = coordinatorApp._timezone;
    this.extractDataSets(coordinatorApp, coordinator);
    if(coordinatorApp['input-events'] && coordinatorApp['input-events']['data-in']){
      coordinator.dataInputType = 'simple';
      this.extractInputEvents(coordinatorApp, coordinator);
    }else if(coordinatorApp['input-events']){
      coordinator.dataInputType = 'logical';
      coordinator.supportsConditionalDataInput = true;
      this.extractLogicalInputEvents(coordinatorApp, coordinator);
    }
    if(coordinatorApp['input-logic']){
      this.extractInputLogic(coordinatorApp, coordinator);
    }
    this.extractOutputEvents(coordinatorApp, coordinator);
    this.extractAction(coordinatorApp, coordinator);
    this.extractParameters(coordinatorApp, coordinator);
    this.extractControls(coordinatorApp, coordinator);
    if (coordinatorApp.action.info && coordinatorApp.action.info.__prefix==="sla") {
      coordinator.slaEnabled=true;
      this.get("slaMapper").handleImport(coordinator, coordinatorApp.action.info, "slaInfo");
    }
    return {coordinator: coordinator, errors: errors};
  },
  extractDateField(value){
    var dateField = {};
    var date = new Date(value);
    dateField.value = value;
    if(isNaN(date.getTime())){
      dateField.displayValue = value;
      dateField.type = 'expr';
    }else{
      dateField.type = 'date';
      var utcDate = new Date(date.getTime() + date.getTimezoneOffset()*60*1000);
      dateField.displayValue = moment(utcDate).format("MM/DD/YYYY hh:mm A");
    }
    return dateField;
  },
  extractDataSet(dataset){
    var dataSetJson = {
      name : dataset._name,
      frequency : {},
      initialInstance :this.extractDateField( dataset['_initial-instance']),
      timezone : dataset._timezone
    };
    var frequency = dataset._frequency;
    if(CommonUtils.startsWith(frequency,'${coord:')){
      dataSetJson.frequency.type = frequency.substring(frequency.indexOf(':')+1, frequency.indexOf('('));
      dataSetJson.frequency.value = frequency.substring(frequency.indexOf('(')+1, frequency.indexOf(')'));
    }else{
      dataSetJson.frequency.type = 'cron';
      dataSetJson.frequency.value = frequency;
    }
    dataSetJson["uriTemplate"] = dataset['uri-template'];
    if (dataset.hasOwnProperty('done-flag')){
      dataSetJson.doneFlag = dataset['done-flag'];
      dataSetJson.doneFlagType = "custom";
    } else {
      dataSetJson.doneFlagType = "default";
    }
    return dataSetJson;
  },
  extractDataSets(coordinatorApp, coordinator){
    if (coordinatorApp.datasets && coordinatorApp.datasets.dataset){
      if(Array.isArray(coordinatorApp.datasets.dataset)) {
        coordinatorApp.datasets.dataset.forEach(function(dataset){
          coordinator.datasets.push(this.extractDataSet(dataset));
        }, this);
      }else{
        coordinator.datasets.push(this.extractDataSet(coordinatorApp.datasets.dataset));
      }
    }
  },
  extractDataInput(datain){
    var datainJson = {
      name : datain._name,
      dataset : datain._dataset
    };
    if (datain.instance && datain.instance.length>0){
      datainJson.instances = Ember.A([]);
      if(Array.isArray(datain.instance)) {
        datain.instance.forEach(function(instance){
          datainJson.instances.pushObject(this.extractDateField(instance));
        }, this);
      }else{
        datainJson.instances.pushObject(this.extractDateField(datain.instance));
      }
      datainJson.isList = true;
    }else if (datain["start-instance"] && ["end-instance"]){
      datainJson.start = this.extractDateField(datain["start-instance"]);
      datainJson.end = this.extractDateField(datain["end-instance"]);
      datainJson.isList = false;
    }
    return datainJson;
  },
  extractInputEvents(coordinatorApp, coordinator){
    if(Array.isArray(coordinatorApp['input-events']['data-in'])){
      coordinatorApp['input-events']['data-in'].forEach(function(datain){
        coordinator.dataInputs.push(this.extractDataInput(datain));
      }, this);
    }else{
      coordinator.dataInputs.push(this.extractDataInput(coordinatorApp['input-events']['data-in']));
    }
  },
  extractLogicalInputEvents(coordinatorApp, coordinator){
    var conditionJson = coordinatorApp['input-events'];
    var condition = {};
    coordinator.conditionalDataInput = condition;
    Object.keys(conditionJson).forEach((key)=>{
      condition.operator = key;
      this.parseConditionTree(conditionJson[key], condition);
    }, this);
  },
  extractInputLogic(coordinatorApp, coordinator){
    var conditionJson = coordinatorApp['input-logic'];
    var condition = {};
    coordinator.inputLogic = condition;
    Object.keys(conditionJson).forEach((key)=>{
      condition.operator = key;
      this.parseConditionTree(conditionJson[key], condition);
    }, this);
  },
  extractDataInputOperand(operandJson){
    var operand = {};
    operand.name = operandJson._name;
    operand.type = 'dataInput';
    operand.dataset = operandJson._dataset;
    if(operandJson._min) {
      operand.min = operandJson._min;
    }
    if(operandJson._wait) {
      operand.wait = operandJson._wait;
    }
    return operand;
  },
  parseConditionTree(conditionJson, condition) {
    condition.name = conditionJson._name;
    condition.operands = Ember.A([]);
    Object.keys(conditionJson).forEach( (key) => {
      var operandsJson = conditionJson[key];
      if(key === 'data-in') {
        if(Array.isArray(operandsJson) ) {
          operandsJson.forEach((json) => {
            condition.operands.pushObject(this.extractDataInputOperand(json));
          }, this);
        }else{
          condition.operands.pushObject(this.extractDataInputOperand(operandsJson));
        }
      }else if(key !== '_name') {
        var operand = {};
        operand.operator = key;
        operand.type = 'condition';
        condition.operands.pushObject(operand);
        this.parseConditionTree(operandsJson, operand);
      }
    }, this);
  },
  extractDataOutput(dataOutJson){
    return {
      dataset:dataOutJson._dataset,
      name:dataOutJson._name,
      instance:this.extractDateField(dataOutJson.instance)
    };
  },
  extractOutputEvents(coordinatorApp, coordinator){
    if (coordinatorApp['output-events'] && coordinatorApp['output-events']['data-out']){
      var dataOutputsJson = coordinatorApp["output-events"]["data-out"];
      if(Array.isArray(dataOutputsJson)){
        dataOutputsJson.forEach(function(dataOutJson){
          coordinator.dataOutputs.pushObject(this.extractDataOutput(dataOutJson));
        }, this);
      }else{
        coordinator.dataOutputs.pushObject(this.extractDataOutput(dataOutputsJson));
      }
    }
  },
  extractAction(coordinatorApp, coordinator){
    var actionJson = coordinatorApp['action']['workflow'];
    coordinator.workflow.appPath = actionJson['app-path'];
    if(actionJson.configuration && actionJson.configuration.property){
      if(Array.isArray(actionJson.configuration.property)){
        actionJson.configuration.property.forEach(function(prop){
          coordinator.workflow.configuration.property.push(this.extractConfigProperty(prop));
        }, this);
      }else{
        coordinator.workflow.configuration.property.push(this.extractConfigProperty(actionJson.configuration.property));
      }

    }
  },
  extractConfigProperty(propJson){
    return {"name" : propJson.name, "value" : propJson.value};
  },
  extractParameters(coordinatorApp, coordinator){
    var paramJson = coordinatorApp['parameters'];
    if(!paramJson) {
      return;
    }
    if(paramJson.configuration && paramJson.configuration.property){
      if(Array.isArray(paramJson.configuration.property)){
        paramJson.configuration.property.forEach(function(prop){
          coordinator.parameters.configuration.property.push(this.extractConfigProperty(prop));
        }, this);
      }else{
        coordinator.parameters.configuration.property.push(this.extractConfigProperty(paramJson.configuration.property));
      }
    }
  },
  extractControls(coordinatorApp, coordinator) {
    var controls = coordinatorApp["controls"];
    if(controls && Object.keys(controls).length > 0){
      Object.keys(controls).forEach((controlName)=>{
        coordinator.controls.pushObject({'name':controlName, 'value':controls[controlName]});
      }, this);
    }
  }
});
export { CoordinatorXmlImporter };
