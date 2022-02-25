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

const DuplicateDataNodeName = BaseValidator.extend({
  validate(value, options, model, attribute) {
    if (model.get('dataNodes')) {
      var killNodes = model.get('workflow').killNodes || [];
      var nodeNames = new Map(killNodes.map((killNode) => [killNode.name, killNode.name]));
      for(let i=0;i<model.get('dataNodes').length;i++){
        let item = model.get('dataNodes').objectAt(i);
        if (item.data.node) {
          if(Ember.isBlank(item.data.node.name)){
            return "Node name should not be blank";
          }
          Ember.set(item.data.node, "errors", false);
          if(nodeNames.get(item.data.node.name) && item.data.node.type !== 'kill'){
            Ember.set(item.data.node, "errors", true);
            return `${item.data.node.name} : Node name should be unique`;
          }else{
            nodeNames.set(item.data.node.name, item.data);
            Ember.set(item.data.node, "errors", false);
          }
        }
      }
      return true;
    }
    return true;
  }
});

DuplicateDataNodeName.reopenClass({
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

export default DuplicateDataNodeName;
