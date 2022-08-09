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
var LZString = require('utils/lz-string');

/**
 * Small mixin for processing user preferences
 * Provide methods to save/load some values in <code>persist</code> storage
 * Save available only for admin users!
 * When using this mixin you should redeclare methods:
 * <ul>
 *   <li>getUserPrefSuccessCallback</li>
 *   <li>getUserPrefErrorCallback</li>
 *   <li>postUserPrefSuccessCallback</li>
 *   <li>postUserPrefErrorCallback</li>
 * </ul>
 * @type {Em.Mixin}
 */
App.Persist = Em.Mixin.create({

  /**
   * Additional to request data
   * @type {object}
   */
  additionalData: {},

  /**
   * Get persist value from server with persistKey
   * @param {String} key
   */
  getUserPref: function(key) {
    return App.ajax.send({
      name: 'persist.get',
      sender: this,
      data: {
        key: key,
        data: this.get('additionalData')
      },
      success: 'getUserPrefSuccessCallback',
      error: 'getUserPrefErrorCallback'
    });
  },

  /**
   *
   * @param {string} key
   * @returns {$.Deferred}
   */
  getDecompressedData: function(key) {
    var dfd = $.Deferred();
    App.ajax.send({
      name: 'persist.get.text',
      sender: this,
      data: {
        key: key
      }
    }).always(function(data, textStatus, error) {
      if (data && typeof data === 'string') {
        dfd.resolve(JSON.parse(LZString.decompressFromBase64(data)));
      } else {
        dfd.reject({error: error});
      }
    });
    return dfd.promise();
  },

  /**
   * Should be redefined in objects that use this mixin
   * @param {*} response
   * @param {Object} request
   * @param {Object} data
   * @returns {*}
   */
  getUserPrefSuccessCallback: function (response, request, data) {},

  /**
   * Should be redefined in objects that use this mixin
   * @param {Object} request
   * @param {Object} ajaxOptions
   * @param {String} error
   */
  getUserPrefErrorCallback: function (request, ajaxOptions, error) {},

  /**
   * Post persist key/value to server, value is object
   * Only for admin users!
   * @param {String} key
   * @param {Object} value
   */
  postUserPref: function (key, value) {
    if (!App.isAuthorized('CLUSTER.MANAGE_USER_PERSISTED_DATA')) {
      return $.Deferred().reject().promise();
    }
    var keyValuePair = {};
    keyValuePair[key] = JSON.stringify(value);
    return this.post(keyValuePair);
  },

  /**
   *
   * @param {string} key
   * @param {Object} value
   * @returns {$.ajax}
   */
  postCompressedData: function (key, value) {
    var keyValuePair = {};
    keyValuePair[key] = !Em.isEmpty(value) ? LZString.compressToBase64(JSON.stringify(value)) : '';
    return this.post(keyValuePair);
  },

  post: function(keyValuePair) {
    return App.ajax.send({
      'name': 'persist.post',
      'sender': this,
      'beforeSend': 'postUserPrefBeforeSend',
      'data': {
        'keyValuePair': keyValuePair
      },
      'success': 'postUserPrefSuccessCallback',
      'error': 'postUserPrefErrorCallback'
    });
  },

  /**
   * Should be redefined in objects that use this mixin
   * @param {*} response
   * @param {Object} request
   * @param {Object} data
   * @returns {*}
   */
  postUserPrefSuccessCallback: function (response, request, data) {},

  /**
   * Should be redefined in objects that use this mixin
   * @param {Object} request
   * @param {Object} ajaxOptions
   * @param {String} error
   */
  postUserPrefErrorCallback: function(request, ajaxOptions, error) {},

  /**
   * Little log before post request
   * @param {Object} request
   * @param {Object} ajaxOptions
   * @param {Object} data
   */
  postUserPrefBeforeSend: function(request, ajaxOptions, data){
  }

});
