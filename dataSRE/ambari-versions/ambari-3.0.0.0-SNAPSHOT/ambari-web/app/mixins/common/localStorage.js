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
 * Fast save/load value to Local Storage
 * Object that implements it should have property <code>name</code> defined
 * @type {Ember.Mixin}
 */
App.LocalStorage = Em.Mixin.create({

  /**
   * <code>App.db.data</code> key
   * @returns {String}
   */
  dbNamespace: function() {
    var name = this.get('name');
    if (Em.isNone(name)) {
      name = this.get('controller.name');
    }
    return name.capitalize().replace('Controller', '');
  }.property('name'),

  /**
   * get property from local storage
   * @param {String} key
   * @return {*}
   */
  getDBProperty: function(key){
    return App.db.get(this.get('dbNamespace'), key);
  },

  /**
   * get properties from local storage
   * @param {String[]} listOfProperties
   * @return {*}
   */
  getDBProperties: function(listOfProperties){
    return App.db.getProperties(this.get('dbNamespace'), listOfProperties);
  },

  /**
   * set property to local storage
   * @param {String} key
   * @param {*} value
   */
  setDBProperty: function(key, value) {
    App.db.set(this.get('dbNamespace'), key, value);
  },

  /**
   * set properties to local storage
   * @param {object} hash
   */
  setDBProperties: function(hash) {
    App.db.setProperties(this.get('dbNamespace'), hash);
  },

  /**
   * Delete the dbNamespace from the localStorage
   * Usually this function is called on quiting/completing the wizard
   */
  resetDbNamespace: function() {
    App.db.data[this.get('dbNamespace')] =  {};
    localStorage.setObject('ambari', App.db.data);
  }

});
