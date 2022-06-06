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

/*
  Base Mixin to be used by different Services to get the common behaviors mixed
  in to the service.
*/
export default Ember.Mixin.create({
  store: Ember.inject.service('store'),

  getBaseDirPath: function(path) {
    return path.substring(0, path.lastIndexOf('/') + 1);
  },

  _getBaseURLFragments: function() {
    var adapter = this.get('store').adapterFor('file');
    var baseURL = adapter.buildURL('file');
    return baseURL.split('/');
  },

  extractError: function(error) {
    if (Ember.isArray(error.errors) && (error.errors.length >= 0)) {
      return error.errors[0];
    }
    return {};
  },

  isInvalidError: function(error) {
    // This seems to a slight hack. But from backend the response of 422 is
    // always a hash which has success param set and value is false
    return Ember.isPresent(error.success) && error.success === false;
  }
});
