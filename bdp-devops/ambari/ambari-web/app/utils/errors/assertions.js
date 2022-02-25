/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');
require('utils/errors/definitions');

/**
 * Method to check custom statement and
 * throws custom error if statement is false
 *
 * @param desc
 * @param test
 * @param ErrorObject
 */
App.assert = function(desc, test, ErrorObject) {
  if (!test) {
    ErrorObject = ErrorObject || Error;
    throw new ErrorObject(desc ? ' Info:  ' + desc : '');
  }
};

/**
 * Check if passed variable should be not null
 *
 * @param object
 * @param desc
 */
App.assertExists = function(object, desc) {
  App.assert(desc, !Em.isNone(object), App.NotNullTypeError);
};

/**
 * Check if passed variable is object
 *
 * @param object
 * @param desc
 */
App.assertObject = function(object, desc) {
  App.assert(desc, (typeof object === 'object') && object, App.ObjectTypeError);
};

/**
 * Check if variable is instance of ember object
 *
 * @param object
 * @param desc
 */
App.assertEmberObject = function(object, desc) {
  App.assert(desc, Em.typeOf(object) === 'instance', App.EmberObjectTypeError);
};

/**
 * Check if variable is array
 *
 * @param object
 * @param desc
 */
App.assertArray = function(object, desc) {
  App.assert(desc, Em.isArray(object), App.ArrayTypeError);
};

/**
 * Check if variable is function
 *
 * @param object
 * @param desc
 */
App.assertFunction = function(object, desc) {
  App.assert(desc, Em.typeOf(object) === 'function', App.FunctionTypeError);
};

