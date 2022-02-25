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

const FsActionValidator = BaseValidator.extend({
  validate(value, options, model, attribute/**/) {
    var isValidated = true,
    msg = "";
    if (!value.length) {
      return false;
    }
    value.forEach(function(item) {
      switch (item.type) {
        case "mkdir":
        case "delete":
        case "touchz":
        if (!item.path) {
          isValidated = false;
          msg = "path is mandatory";
        }
        break;
        case "chmod":
        if (!item.path) {
          isValidated = false;
          msg = "path and permissions are mandatory";
        }
        break;
        case "chgrp":
        if (!item.path || !item.group) {
          isValidated = false;
          msg = "path and group are mandatory";
        }
        break;
        case "move":
        if (!item.source || !item.target) {
          isValidated = false;
          msg = "source and target are mandatory";
        }
        break;
      }
    });
    if (msg.length) {
      return false;
    }
    return true;
  }
});

FsActionValidator.reopenClass({
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

export default FsActionValidator;
