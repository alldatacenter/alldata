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
import ApplicationAdapter from './application';

export default ApplicationAdapter.extend({
  pathForType: function(type) {
    if (type === 'file') {
      return 'listdir';
    }
  },
  parseErrorResponse: function(responseText) {
    var json = this._super(responseText);
    if((typeof json) === 'object') {
      var error = {};
      if (Ember.isPresent(json.success)) {
        // This error is for Invalid Error response (422)
        error.success = json.success;
        error.message = json.message;

        delete json.success;
        delete json.message;

        if(Ember.isArray(json.succeeded)) {
          error.succeeded = json.succeeded;
          delete json.succeeded;
        }
        if (Ember.isArray(json.failed)) {
          error.failed = json.failed;
          delete json.failed;
        }
        if (Ember.isArray(json.unprocessed)) {
          error.unprocessed = json.unprocessed;
          delete json.unprocessed;
        }
      } else {
        // Other errors
        error.message = json.message;
        error.trace = json.trace;
        error.status = json.status;
        delete json.trace;
        delete json.status;
        delete json.message;
      }
      json.errors = [error];
    }

    return json;
  }
});
