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

/**
 * Error that should be used when
 * not null type required
 *
 * @param message
 * @constructor
 */
App.NotNullTypeError = function(message) {
  this.name = "NotNullTypeError";
  this.message = "Not null expected. " + (message || "");
};

App.NotNullTypeError.prototype = new TypeError();

/**
 * Error that should be used when
 * object required
 *
 * @param message
 * @constructor
 */
App.ObjectTypeError = function(message) {
  this.name = "ObjectTypeError";
  this.message = "Object expected. " + (message || "");
};

App.ObjectTypeError.prototype = new App.NotNullTypeError();

/**
 * Error that should be used when
 * array required
 *
 * @param message
 * @constructor
 */
App.ArrayTypeError = function(message) {
  this.name = "ArrayTypeError";
  this.message = "Array expected. " + (message || "");
};

App.ArrayTypeError.prototype = new App.NotNullTypeError();
/**
 * Error that should be used when
 * function required
 *
 * @param message
 * @constructor
 */
App.FunctionTypeError = function(message) {
  this.name = "FunctionTypeError";
  this.message = "Function expected. " + (message || "");
};

App.FunctionTypeError.prototype = new App.NotNullTypeError();
/**
 * Error that should be used when
 * ember object required
 *
 * @param message
 * @constructor
 */
App.EmberObjectTypeError = function(message) {
  this.name = "EmberObjectTypeError";
  this.message = "Ember object expected. " + (message || "");
};

App.EmberObjectTypeError.prototype = new App.ObjectTypeError();