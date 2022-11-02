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

App.AjaxDefaultErrorPopupBodyView = Em.View.extend({

  classNames: ['api-error'],
  templateName: require('templates/utils/ajax'),

  /**
   * HTTP request URL
   * @type {string}
   */
  url: '',

  /**
   * HTTP request type
   * @type {string}
   */
  type: '',

  /**
   * HTTP response status code
   * @type {number}
   */
  status: 0,

  /**
   * Received error message
   * @type {string}
   */
  message: '',

  /**
   * Status code string
   * @type {string}
   */
  statusCode: Em.computed.i18nFormat('utils.ajax.defaultErrorPopupBody.statusCode', 'status'),

  /**
   * Indicates if error message should be displayed
   * @type {boolean}
   */
  showMessage: Em.computed.bool('message'),

  /**
   * HTTP response error description
   * @type {string}
   */
  api: Em.computed.i18nFormat('utils.ajax.defaultErrorPopupBody.message', 'type', 'url')

});
