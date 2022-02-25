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
var BundleGenerator= Ember.Object.extend({
  x2js : new X2JS({useDoubleQuotes:true}),
  bundle: null,
  process (){
    var xmlJson={"bundle-app":{}};
    var bundleApp=xmlJson["bundle-app"];
    bundleApp._xmlns = "uri:oozie:bundle:"+this.bundle.schemaVersions.bundleVersion;
    bundleApp._name = this.bundle.name;
    if(!Ember.isEmpty(this.bundle.kickOffTime.value)){
      bundleApp["controls"] = {};
      bundleApp["controls"]["kick-off-time"] = this.bundle.kickOffTime.value;
    }
    this.generateCoordinatorsJson(bundleApp);
    var xmlAsStr = this.get("x2js").json2xml_str(xmlJson);
    return xmlAsStr;
  },
  generateCoordinatorsJson(bundleApp){
    if (this.bundle.coordinators && this.bundle.coordinators.length>0){
      bundleApp["coordinator"] = [];
      this.bundle.coordinators.forEach((coordinator)=>{
        var coordinatorJson = {"_name":coordinator.name};
        coordinatorJson["app-path"] = coordinator.appPath;
        if (coordinator.configuration &&
          coordinator.configuration.property &&
          coordinator.configuration.property.length > 0){
            coordinatorJson["configuration"]={"property":[]};
            var propertiesJson=coordinatorJson.configuration.property;
            coordinator.configuration.property.forEach((prop) =>{
              propertiesJson.push({"name" : prop.name, "value" : prop.value});
            });
          }
          bundleApp["coordinator"].push(coordinatorJson);
      }, this);
    }
  }
});
export {BundleGenerator};
