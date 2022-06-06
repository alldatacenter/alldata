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
import BaseValidator from 'ember-cp-validations/validators/base';

const DuplicateFlattenedNodeName = BaseValidator.extend({
  validate(value, options, model, attribute) {
    var nodeNames = new Map();
    model.get("validationErrors").clear();
    model.get('flattenedNodes').forEach((item)=>{
      Ember.set(item, "errors", false);
      if(nodeNames.get(item.name)){
        Ember.set(item, "errors", true);
        model.get("validationErrors").pushObject({node:item,message:"Node name should be unique"});
      }else{
        nodeNames.set(item.name, item);
        Ember.set(item, "errors", false);
      }
      if(model.get("supportedActionTypes").indexOf(item.actionType) === -1 && item.type === "action"){
        model.get('validationErrors').pushObject({node : item ,message : item.actionType+" is unsupported"});
      }
      var nodeErrors=item.validateCustom();
      if (nodeErrors.length>0){
        Ember.set(item, "errors", true);
        nodeErrors.forEach(function(errMsg){
          model.get("errors").pushObject({node:item,message:errMsg });
        });
      }
    });

    if(model.get('flattenedNodes').length !== nodeNames.size || (model.get("errors") && model.get("errors").length>0)){
      return false;
    }
    return true;
  }
});

DuplicateFlattenedNodeName.reopenClass({
  /**
   * Define attribute specific dependent keys for your validator
   *
   * @param {String}  attribute   The attribute being evaluated
   * @param {Unknown} options     Options passed into your validator
   * @return {Array}
   */
  getDependentsFor(/* attribute, options */) {
    return [];
  }
});

export default DuplicateFlattenedNodeName;
