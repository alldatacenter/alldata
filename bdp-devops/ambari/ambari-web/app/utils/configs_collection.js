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
 * This is util for storing static config info
 * Such way of managing config is much faster rather then Ember data
 * since it's using plain objects
 * This util has simple method to manipulate config objects
 */

/**
 * these variables are used to store configs in array and in object to get config quickly
 * these properties shouldn't be managed in other way rather then method of configCollection
 * @type {Array}
 */
var configsCollection = [],
  /**
   * this should be object with key - config id and value - config object
   * @type {Object}
   */
  configsCollectionMap = {};

App.configsCollection = Em.Object.create({

  /**
   * adds config property to configs array and map
   * should assert error if config has no id
   * @param c
   * @method add
   */
  add: function (c) {
    Em.assert('id should be defined', c && c.id);
    if (!configsCollectionMap[c.id]) {
      configsCollection.push(c);
    }
    configsCollectionMap[c.id] = c;
  },

  /**
   * get config by id from map
   * @param id
   * @returns {Object}
   * @method getConfig
   */
  getConfig: function(id) {
    Em.assert('id should be defined', id);
    return configsCollectionMap[id];
  },

  /**
   * get config from map by name and fileName
   * @param name
   * @param fileName
   * @returns {*|Object}
   * @method getConfigByName
   */
  getConfigByName: function(name, fileName) {
    Em.assert('name and filename should be defined', name && fileName);
    return this.getConfig(App.config.configId(name, fileName));
  },

  /**
   * get all configs
   * @returns {Array}
   * @method getAll
   */
  getAll: function() {
    return configsCollection;
  },

  /**
   * clear all configs
   * @method clearAll
   */
  clearAll: function() {
    configsCollection = [];
    configsCollectionMap = {};
  }

});
