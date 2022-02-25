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
import {SLAMapper} from "../../domain/mapping-utils";

var CoordinatorGenerator= Ember.Object.extend({
  x2js : new X2JS({useDoubleQuotes:true}),
  coordinator: null,
  slaMapper: SLAMapper.create({}),
  init(){
  },
  process(){
    var xmlJson={"coordinator-app":{}};
    var coordinatorApp=xmlJson["coordinator-app"];
    coordinatorApp._name = this.coordinator.name;
    if(this.coordinator.frequency.type !== 'cron'){
      coordinatorApp._frequency = "${coord:"+this.coordinator.frequency.type+"("+this.coordinator.frequency.value+")}";
    }else{
      coordinatorApp._frequency = this.coordinator.frequency.value;
    }
    coordinatorApp._start = this.coordinator.start.value;
    coordinatorApp._end = this.coordinator.end.value;
    coordinatorApp._timezone = this.coordinator.timezone;
    coordinatorApp._xmlns = "uri:oozie:coordinator:"+this.coordinator.schemaVersions.coordinatorVersion;
    this.generateParameters(coordinatorApp);
    this.generateControls(coordinatorApp);
    this.generateDataSets(coordinatorApp);
    if(this.coordinator.dataInputType === 'simple'){
      this.generateInputEvents(coordinatorApp);
    }else{
      this.generateConditionalInputEvents(coordinatorApp);
    }
    if(this.coordinator.inputLogic){
      this.generateInputLogic(coordinatorApp);
    }
    this.generateOutputEvents(coordinatorApp);
    this.generateAction(coordinatorApp);
    if(this.coordinator.slaEnabled){
      coordinatorApp["_xmlns:sla"] = "uri:oozie:sla:0.2";
      this.get("slaMapper").hanldeGeneration(this.coordinator.slaInfo, coordinatorApp.action);
    }
    var xmlAsStr = this.get("x2js").json2xml_str(xmlJson);
    return xmlAsStr;
  },
  generateDataSets(coordinatorApp){
    if (this.coordinator.datasets && this.coordinator.datasets.length>0){
      var datasets=[];
      coordinatorApp["datasets"]={"dataset":datasets};
      this.coordinator.datasets.forEach(function(dataset){
        var dataSetJson={_name:dataset.name,
          "_initial-instance":dataset.initialInstance.value,
          _timezone:dataset.timezone
        };
        if(dataset.frequency.type !== 'cron'){
          dataSetJson._frequency = "${coord:"+ dataset.frequency.type + "("+dataset.frequency.value + ")}";
        }else{
          dataSetJson._frequency = dataset.frequency.value;
        }
        dataSetJson["uri-template"]=dataset.uriTemplate;
        if (dataset.doneFlagType === 'custom'){
          dataSetJson["done-flag"]=dataset.doneFlag;
        }
        datasets.push(dataSetJson);
      });
    }
  },
  generateInputEvents(coordinatorApp){
    if (this.coordinator.dataInputs && this.coordinator.dataInputs.length>0){
      coordinatorApp["input-events"]={"data-in":[]};
      var dataInListJson=coordinatorApp["input-events"]["data-in"];
      this.coordinator.dataInputs.forEach(function(datain){
        var datainJson={_name:datain.name,_dataset:datain.dataset};
        if (datain.instances && datain.instances.length>0){
          var instancesJson=[];
          datain.instances.forEach(function(instance){
            if (instance&& instance.value){
              instancesJson.push(instance.value);
            }
          });
          datainJson["instance"]=instancesJson;
        }else if (datain.start && datain.end){
          datainJson["start-instance"]=datain.start.value;
          datainJson["end-instance"]=datain.end.value;
        }
        dataInListJson.push(datainJson);
      });
    }
  },
  generateConditionalInputEvents(coordinatorApp){
    if(this.coordinator.conditionalDataInput){
      var condition = this.coordinator.conditionalDataInput;
      coordinatorApp["input-events"] = {};
      var inputEventJson = coordinatorApp["input-events"];
      inputEventJson[condition.operator] = {};
      var conditionJson = inputEventJson[condition.operator];
      this.parseConditionTree(conditionJson, condition);
    }
  },
  generateInputLogic(coordinatorApp){
    var condition = this.coordinator.inputLogic;
    coordinatorApp["input-logic"] = {};
    var inputLogicJson = coordinatorApp["input-logic"];
    inputLogicJson[condition.operator] = {};
    var conditionJson = inputLogicJson[condition.operator];
    this.parseConditionTree(conditionJson, condition);
  },
  parseConditionTree(conditionJson, condition){
    if(!condition) {
      return;
    }
    if(condition.min){
      conditionJson._min = condition.min;
    }
    if(condition.wait){
      conditionJson._wait = condition.wait;
    }
    if(condition.name){
      conditionJson._name = condition.name;
    }
    if(!condition.operands){
      return;
    }
    condition.operands.forEach((operand)=>{
      if(operand.type === 'dataInput'){
        if(!conditionJson["data-in"]){
          conditionJson["data-in"] = [];
        }
        var dataInJson = {_dataset:operand.dataset};
        if(operand.min){
          dataInJson._min = operand.min;
        }
        if(operand.wait){
          dataInJson._wait = operand.wait;
        }
        if(operand.name){
          dataInJson._name = operand.name;
        }
        conditionJson["data-in"].push(dataInJson);
      }else if(operand.type === 'condition'){
        conditionJson[operand.operator] = {};
        this.parseConditionTree(conditionJson[operand.operator], operand);
      }
    }, this);
  },
  generateOutputEvents(coordinatorApp){
    if (this.coordinator.dataOutputs && this.coordinator.dataOutputs.length>0){
      coordinatorApp["output-events"]={"data-out":[]};
      var dataOutputsJson=  coordinatorApp["output-events"]["data-out"];
      this.coordinator.dataOutputs.forEach(function(dataOut){
        var dataOutJson={_dataset:dataOut.dataset,_name:dataOut.name,instance:dataOut.instance.value};
        dataOutputsJson.push(dataOutJson);
      });
    }
  },
  generateAction(coordinatorApp){
    var actionJson={"workflow":{"app-path":this.coordinator.workflow.appPath}};
    coordinatorApp.action=actionJson;
    if (this.coordinator.workflow.configuration &&
      this.coordinator.workflow.configuration.property &&
      this.coordinator.workflow.configuration.property.length>0){
        actionJson.workflow["configuration"]={"property":[]};
        var propertiesJson=actionJson.workflow.configuration.property;
        this.coordinator.workflow.configuration.property.forEach(function(prop){
          propertiesJson.push({"name":prop.name,"value":prop.value});
        });
      }
    },
    generateParameters(coordinatorApp){
      if (this.coordinator.parameters.configuration &&
        this.coordinator.parameters.configuration.property &&
        this.coordinator.parameters.configuration.property.length>0){
          coordinatorApp["parameters"] = {};
          var paramJson = coordinatorApp["parameters"];
          paramJson["configuration"]={"property":[]};
          var propertiesJson=paramJson.configuration.property;
          this.coordinator.parameters.configuration.property.forEach(function(prop){
            propertiesJson.push({"name":prop.name,"value":prop.value});
          });
        }
    },
    generateControls(coordinatorApp) {
      if(this.coordinator.controls && this.coordinator.controls.length > 0){
          coordinatorApp["controls"] = {};
          this.coordinator.controls.forEach((control)=>{
            if(control.value){
              coordinatorApp["controls"][control.name] = control.value;
            }
          }, this);
          if(Object.keys(coordinatorApp["controls"]).length === 0){
            delete coordinatorApp["controls"];
          }
      }
    }
  });
  export {CoordinatorGenerator};
