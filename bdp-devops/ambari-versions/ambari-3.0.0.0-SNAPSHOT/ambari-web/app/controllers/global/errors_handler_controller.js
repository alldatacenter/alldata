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

App.ErrorsHandlerController = Em.Controller.extend(App.Persist, {

  name: 'errorsHandlerController',

  /**
  * @const
  */
  ERROR_STORAGE_SIZE: 500000,

  /**
  * @const
  */
  MAX_TRACE_LENGTH: 1000,

  init: function () {
    var oldError = window.onerror || Em.K;
    var self = this;
    window.onerror = function (err, url, lineNumber, colNumber, Err) {
      oldError.call(this, err, url, lineNumber, colNumber, Err);
      self.saveErrorLogs(err, url, lineNumber, colNumber, Err);
    };
    return this._super();
  },

  /**
   * load logs from server
   */
  loadErrorLogs: function() {
    this.getUserPref('errors');
  },

  /**
  * @method getUserPrefSuccessCallback
  * @param {object|null} data
  */
  getUserPrefSuccessCallback: function(data) {
    if (data) {
      localStorage.setObject('errors', data);
    }
  },

  /**
   * save error logs to localStorage and server
   * @param {string} err
   * @param {string} url
   * @param {number} lineNumber
   * @param {number} colNumber
   * @param {Error} Err
   */
  saveErrorLogs: function(err, url, lineNumber, colNumber, Err) {
    var ls = localStorage.getObject('errors') || {};
    var key = new Date().getTime();
    var stackTrace = Em.get(Err || {}, 'stack');

    if (stackTrace) {
      var origin = location.origin || (location.protocol + '//' + location.host),
        path = origin + location.pathname + 'javascripts',
        pattern = new RegExp(path, 'g');
      stackTrace = stackTrace.replace(pattern, '').substr(0, this.MAX_TRACE_LENGTH);
    }

    var val = {
      file: url,
      line: lineNumber,
      col: colNumber,
      error: err,
      stackTrace: stackTrace
    };

    //overwrite errors if storage full
    if (JSON.stringify(ls).length > this.ERROR_STORAGE_SIZE) {
      delete ls[Object.keys(ls).sort()[0]];
    }

    ls[key] = val;
    localStorage.setObject('errors', ls);
    this.postUserPref('errors', ls);
  }
});
