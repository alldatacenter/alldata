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
import Constants from '../utils/constants';

export default Ember.Service.extend({
  simpleProperty : /^[A-Za-z_][0-9A-Za-z_]+$/,
  dynamicProperty :  /^\${[A-Za-z_][0-9A-Za-z_]*}$/,
  dynamicPropertyWithElMethod : /^\${[A-Za-z_][0-9A-Za-z_]*\((([A-Za-z_][0-9A-Za-z_]*)(,([A-Za-z_][0-9A-Za-z_]*))*)*\)}$/,
  dynamicPropertyWithWfMethod : /^\${wf:[A-Za-z_][0-9A-Za-z_]*\((([A-Za-z_][0-9A-Za-z_]*)(,([A-Za-z_][0-9A-Za-z_]*))*)*\)}$/i,
  hadoopEL : /^\${hadoop:.*}$/,
  extractor : /\${.+?\}/g,
  containsParameters(path) {
    var matches = path.match(this.get('extractor'));
    return matches !== null;
  },
  extract : function(property) {
    var matches = property.match(this.get('extractor'));
    var dynamicProperties = [];
    if(null == matches) {
      return dynamicProperties;
    }
    matches.forEach((match)=>{
      if(this.get('dynamicProperty').test(match) && Constants.elConstants.indexOf(match) < 0){
        dynamicProperties.push(match);
      }
    }.bind(this));
    return dynamicProperties;
  },
  getProperties : function(currentValue, dynamicProperties) {
    if( typeof currentValue === "object" ) {
      Object.keys(currentValue).forEach((value, key)=> {
        this.getProperties(currentValue[value], dynamicProperties);
      }.bind(this));
    } else {
      if (typeof currentValue==="string"){
        var extractedProperties = this.extract(currentValue);
        extractedProperties.forEach((value)=>{
          dynamicProperties.set(value,value);
        });
      }
    }
    return dynamicProperties;
  },
  getDynamicProperties : function(xml){
    var x2js = new X2JS();
    var workflowJSON = x2js.xml_str2json(xml);
    var props= this.getProperties(workflowJSON, new Map());
    return props;
  }
});
