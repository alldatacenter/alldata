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

import Ember from 'ember';


/**
  Shows alert flash and also creates `alert` objects in store. If creation of
  `alert` objects in store pass `options.flashOnly` as `true`. The options
  required for creating the `alert` objects are:
  ```
    options.message: message field returned by the API server.
    options.status : Status XHR request if the message is a response to XHR request. Defaults to -1.
    options.error: Detailed error to be displayed.
  ```
  Options required for ember-cli-flash can also be passed in the alertOptions to override the
  default behaviour.
*/
export default Ember.Service.extend({
  flashMessages: Ember.inject.service('flash-messages'),
  store: Ember.inject.service('store'),
  alertsChanged: false,

  currentUnreadMessages: function() {
   return this.get('store').peekAll('alert').filter((entry) => {
     return entry.get('read') === false;
   });
  },

  setUnreadMessagesToRead: function() {
    this.currentUnreadMessages().forEach((entry) => {
      entry.set('read', true);
    });
    this.toggleProperty('alertsChanged');
  },

  currentMessagesCount: Ember.computed('alertsChanged', function() {
    return this.currentUnreadMessages().get('length');
  }),

  success: function(message, options = {}, alertOptions = {}) {
    this._processMessage('success', message, options, alertOptions);
  },

  warn: function(message, options = {}, alertOptions = {}) {
    this._processMessage('warn', message, options, alertOptions);
  },

  info: function(message, options = {}, alertOptions = {}) {
    this._processMessage('info', message, options, alertOptions);
  },

  danger: function(message, options = {}, alertOptions = {}) {
    this._processMessage('danger', message, options, alertOptions);
  },

  clearMessages: function() {
    this.get('flashMessages').clearMessages();
  },

  _processMessage: function(type, message, options, alertOptions) {
    this._clearMessagesIfRequired(alertOptions);
    let alertRecord = this._createAlert(message, type, options, alertOptions);
    if(alertRecord) {
      this.toggleProperty('alertsChanged');
      message = this._addDetailsToMessage(message, alertRecord);
    }
    switch (type) {
      case 'success':
        this.get('flashMessages').success(message, this._getOptions(alertOptions));
        break;
      case 'warn':
        this.get('flashMessages').warning(message, this._getOptions(alertOptions));
        break;
      case 'info':
        this.get('flashMessages').info(message, this._getOptions(alertOptions));
        break;
      case 'danger':
        this.get('flashMessages').danger(message, this._getOptions(alertOptions));
    }
  },

  _addDetailsToMessage: function(message, record) {
    let id = record.get('id');
    let suffix = `<a href="#/messages/${id}">(details)</a>`;
    return message + "  " + suffix;
  },

  _createAlert: function(message, type, options, alertOptions) {
    var data = {};
    data.message = message;
    data.responseMessage = options.message || '';
    data.id = this._getNextAlertId();
    data.type = type;
    data.status = options.status || -1;
    data.trace = this._getDetailedError(options.trace);
    delete options.status;
    delete options.error;

    if(alertOptions.flashOnly === true) {
      return;
    }
    return this.get('store').createRecord('alert', data);
  },

  _getDetailedError: function(error) {
    return error || '';
  },

  _getOptions: function(options = {}) {
    var defaultOptions = {
      priority: 100,
      showProgress: true,
      timeout: 6000
    };
    return Ember.merge(defaultOptions, options);
  },

  _getNextAlertId: function() {
    return this.get('store').peekAll('alert').get('length') + 1;
  },

  _clearMessagesIfRequired: function(options = {}) {
    var stackMessages = options.stackMessages || false;
    if(stackMessages !== true) {
      this.clearMessages();
    }
  }
});
