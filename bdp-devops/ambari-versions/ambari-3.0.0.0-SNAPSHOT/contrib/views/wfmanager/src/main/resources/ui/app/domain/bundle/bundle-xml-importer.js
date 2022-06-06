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
import { Bundle } from '../bundle/bundle';
import SchemaVersions from '../schema-versions';
import CommonUtils from "../../utils/common-utils";

var BundleXmlImporter= Ember.Object.extend({
  x2js : new X2JS(),
  schemaVersions : SchemaVersions.create({}),
  importBundle (xml){
    var bundleJson = this.get("x2js").xml_str2json(xml);
    return this.processBundleXML(bundleJson);
  },
  processBundleXML(bundleJson){
    var errors=Ember.A([]);
    var bundle = Bundle.create({
      name : '',
      kickOffTime : {
        value : '',
        displayValue : '',
        type : 'date'
      },
      coordinators : Ember.A([]),
      schemaVersions : {
        bundleVersion : this.get("schemaVersions").getDefaultVersion('bundle')
      }
    });
    if (!bundleJson || !bundleJson["bundle-app"]){
      errors.push({message: "Could not import invalid bundle",dismissable:true});
      return {bundle:null, errors: errors};
    }
    var bundleApp=bundleJson["bundle-app"];
    bundle.name = bundleApp._name;
    var bundleVersion=CommonUtils.extractSchemaVersion(bundleApp._xmlns);
    var maxBundleVersion = Math.max.apply(Math, this.get('schemaVersions').getSupportedVersions('bundle'));
    if (bundleVersion > maxBundleVersion) {
      errors.push({message: "Unsupported bundle version - " + bundleVersion});
    } else {
      bundle.schemaVersions.bundleVersion = bundleVersion;
    }
    if(bundleApp.controls && bundleApp.controls["kick-off-time"]) {
      bundle.kickOffTime = this.extractDateField(bundleApp["controls"]["kick-off-time"]);
    }
    this.processCoordinatorsJson(bundleApp, bundle);
    return {bundle: bundle, errors: errors};
  },
  processCoordinatorsJson(bundleApp, bundle){
    if (bundleApp.coordinator){
      bundle.coordinators = Ember.A([]);
      if(Array.isArray(bundleApp.coordinator)){
        bundleApp.coordinator.forEach((coordinator)=>{
          bundle.coordinators.push(this.extractCoordinator(coordinator));
        }, this);
      }else{
        bundle.coordinators.push(this.extractCoordinator(bundleApp.coordinator));
      }
    }
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
  extractCoordinator(coordinator) {
    var coordinatorJson = {"name":coordinator._name};
    coordinatorJson.appPath =  coordinator["app-path"];
    if (coordinator.configuration &&
    coordinator.configuration.property &&
    coordinator.configuration.property.length > 0){
      coordinatorJson.configuration = {};
      coordinatorJson.configuration.property = Ember.A([]);
      coordinator.configuration.property.forEach(function(prop){
        coordinatorJson.configuration.property.push({"name" : prop.name, "value" : prop.value});
      });
    }
    return coordinatorJson;
  }
});
export {BundleXmlImporter};
