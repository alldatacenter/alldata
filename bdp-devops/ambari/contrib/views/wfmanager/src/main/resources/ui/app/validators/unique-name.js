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

import BaseValidator from 'ember-cp-validations/validators/base';

const UniqueName = BaseValidator.extend({
  validate(value, options, model, attribute) {
    var nameMap = [], errorMsg = undefined, i = 0;
    if(value){
      value.forEach(function(item, index){
        if(!item.name){
          errorMsg = "Name cannot be blank";
          i = index;
        } else if(nameMap.indexOf(item.name) > -1){
          i = index;
          errorMsg = "Name cannot be duplicate";
        } else{
          nameMap.push(item.name);
        }
      });
      if(errorMsg){
        if(i){
          value.splice(i, 1);
        }
        return errorMsg;
      }
      return true;
    }
  }
});

UniqueName.reopenClass({
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

export default UniqueName;
