/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

/** @module utils.credentials **/

/**
 * Credential Resource format.
 * @typedef {object} credentialResourceObject
 * @property {string} principal user principal name
 * @property {string} key user password
 * @property {string} type type of credential store e.g. <b>persistent</b> or <b>temporary</b>
 */
module.exports = {

  STORE_TYPES: {
    TEMPORARY: 'temporary',
    PERSISTENT: 'persisted',
    PERSISTENT_KEY: 'persistent',
    TEMPORARY_KEY: 'temporary',
    PERSISTENT_PATH: 'storage.persistent',
    TEMPORARY_PATH: 'storage.temporary'
  },

  ALIAS: {
    KDC_CREDENTIALS: 'kdc.admin.credential'
  },

  /**
   * Store credentials to server
   *
   * @member utils.credentials
   * @param {string} clusterName cluster name
   * @param {string} alias credential alias name e.g. "kdc.admin.credentials"
   * @param {credentialResourceObject} resource resource info to set e.g.
   * <code>
   * {
   *   principal: "USERNAME",
   *   key: "SecretKey",
   *   type: "persisted"
   * }
   * </code>
   *
   * Where:
   * <ul>
   *   <li>principal: the principal (or username) part of the credential to store</li>
   *   <li>key: the secret key part of the credential to store</li>
   *   <li>type: declares the storage facility type: "persisted" or "temporary"</li>
   * </ul>
   * @returns {$.Deferred} promise object
   */
  createCredentials: function(clusterName, alias, resource) {
    return App.ajax.send({
      sender: this,
      name: 'credentials.create',
      data: {
        clusterName: clusterName,
        resource: resource,
        alias: alias
      },
      error: 'createCredentialsErrorCallback'
    });
  },

  credentialsSuccessCallback: function(data, opt, params) {
    params.callback(data.items.length ? data.items.mapProperty('Credential') : []);
  },

  createCredentialsErrorCallback: function(req, ajaxOpts, error) {
  },

  /**
   * @see createCredentials
   * @member utils.credentials
   * @param {string} clusterName
   * @param {string} alias
   * @param {credentialResourceObject} resource
   * @returns {$.Deferred} promise object
   */
  createOrUpdateCredentials: function(clusterName, alias, resource) {
    var self = this;
    var dfd = $.Deferred();
    this.getCredential(clusterName, alias).then(function() {
      // update previously stored credentials
      self.updateCredentials(clusterName, alias, resource).always(function() {
        var status = arguments[1];
        var result = arguments[2];
        dfd.resolve(status === "success", result);
      });
    }, function() {
      // create credentials if they not exist
      self.createCredentials(clusterName, alias, resource).always(function() {
        var status = arguments[1];
        var result = arguments[2];
        dfd.resolve(status === "success", result);
      });
    });
    return dfd.promise();
  },

  /**
   * Retrieve single credential from cluster by specified alias name
   *
   * @member utils.credentials
   * @param {string} clusterName cluster name
   * @param {string} alias credential alias name e.g. "kdc.admin.credentials"
   * @param {function} [callback] success callback to invoke, credential will be passed to first argument
   * @returns {$.Deferred} promise object
   */
  getCredential: function(clusterName, alias, callback) {
    return App.ajax.send({
      sender: this,
      name: 'credentials.get',
      data: {
        clusterName: clusterName,
        alias: alias,
        callback: callback
      },
      success: 'getCredentialSuccessCallback',
      error: 'getCredentialErrorCallback'
    });
  },

  getCredentialSuccessCallback: function(data, opt, params) {
    if (params.callback) {
      params.callback(Em.getWithDefault(data, 'Credential', null));
    }
  },

  getCredentialErrorCallback: function() {},

  /**
   * Update credential by alias and cluster name
   *
   * @see createCredentials
   * @param {string} clusterName
   * @param {string} alias
   * @param {object} resource
   * @returns {$.Deferred} promise object
   */
  updateCredentials: function(clusterName, alias, resource) {
    return App.ajax.send({
      sender: this,
      name: 'credentials.update',
      data: {
        clusterName: clusterName,
        alias: alias,
        resource: resource
      }
    });
  },

  /**
   * Get credenial list from server by specified cluster name
   *
   * @param {string} clusterName cluster name
   * @param {function} callback
   * @returns {$.Deferred} promise object
   */
  credentials: function(clusterName, callback) {
    return App.ajax.send({
      sender: this,
      name: 'credentials.list',
      data: {
        clusterName: clusterName,
        callback: callback
      },
      success: 'credentialsSuccessCallback'
    });
  },

  /**
   * Remove credential from server by specified cluster name and alias
   *
   * @param {string} clusterName cluster name
   * @param {string} alias credential alias name e.g. "kdc.admin.credentials"
   */
  removeCredentials: function(clusterName, alias) {
    return App.ajax.send({
      sender: this,
      name: 'credentials.delete',
      data: {
        clusterName: clusterName,
        alias: alias
      }
    });
  },

  /**
   * Get info regarding credential storage type like <code>persistent</code> and <code>temporary</code>
   *
   * @param {string} clusterName cluster name
   * @param {function} callback
   * @returns {$.Deferred} promise object
   */
  storageInfo: function(clusterName, callback) {
    return App.ajax.send({
      sender: this,
      name: 'credentials.store.info',
      data: {
        clusterName: clusterName,
        callback: callback
      },
      success: 'storageInfoSuccessCallback'
    });
  },

  storageInfoSuccessCallback: function(json, opt, params, request) {
    if (json.Clusters) {
      var storage = Em.getWithDefault(json, 'Clusters.credential_store_properties', {});
      var storeTypesObject = {};

      storeTypesObject[this.STORE_TYPES.PERSISTENT_KEY] = storage[this.STORE_TYPES.PERSISTENT_PATH] === "true";
      storeTypesObject[this.STORE_TYPES.TEMPORARY_KEY] = storage[this.STORE_TYPES.TEMPORARY_PATH] === "true";
      params.callback(storeTypesObject);
    } else {
      params.callback(null);
    }
  },

  /**
   * Resolves promise with <code>true</code> value if secure store is persistent
   *
   * @param {string} clusterName
   * @returns {$.Deferred} promise object
   */
  isStorePersisted: function(clusterName) {
    return this.storeTypeStatus(clusterName, this.STORE_TYPES.PERSISTENT_KEY);
  },

  /**
   * Resolves promise with <code>true</code> value if secure store is temporary
   *
   * @param {string} clusterName
   * @returns {$.Deferred} promise object
   */
  isStoreTemporary: function(clusterName) {
    return this.storeTypeStatus(clusterName, this.STORE_TYPES.TEMPORARY_KEY);
  },

  /**
   * Get store type value for specified cluster and store type e.g. <b>persistent</b> or <b>temporary</b>
   *
   * @member utils.credentials
   * @param {string} clusterName
   * @param {string} type store type e.g. <b>persistent</b> or <b>temporary</b>
   * @returns {$.Deferred} promise object
   */
  storeTypeStatus: function(clusterName, type) {
    var dfd = $.Deferred();
    this.storageInfo(clusterName, function(storage) {
      dfd.resolve(Em.get(storage, type));
    }).fail(function(error) {
      dfd.reject(error);
    });
    return dfd.promise();
  },

  /**
   * Generate payload for storing credential.
   *
   * @member utils.credentials
   * @param {string} principal principal name
   * @param {string} key secret key
   * @param {string} type storage type e.g. <b>persisted</b>, <b>temporary</b>
   * @returns {credentialResourceObject} resource template
   */
  createCredentialResource: function(principal, key, type) {
    return {
      principal: principal,
      key: key,
      type: type
    };
  },

  /**
   * Check that KDC credentials stored as <b>persisted</b> and not <b>temporary</b> from specified credentials list.
   *
   * @member utils.credentials
   * @param {object[]} credentials credentials list retrieved from API @see credentials
   * @returns {boolean} <code>true</code> if credentials are persisted
   */
  isKDCCredentialsPersisted: function(credentials) {
    var kdcCredentials = credentials.findProperty('alias', this.ALIAS.KDC_CREDENTIALS);
    if (kdcCredentials) {
      return Em.getWithDefault(kdcCredentials, 'type', this.STORE_TYPES.TEMPORARY) === this.STORE_TYPES.PERSISTENT;
    }
    return false;
  }
};
