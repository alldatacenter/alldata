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

import DS from 'ember-data';
import Ember from 'ember';
import ENV from 'files-view/config/environment';

export default DS.RESTAdapter.extend({
  init: function () {
    Ember.$.ajaxSetup({
      cache: false
    });
  },

  namespace: Ember.computed(function() {
    var parts = window.location.pathname.split('/').filter(function(i) {
      return i !== "";
    });
    var view = parts[parts.length - 3];
    var version = '/versions/' + parts[parts.length - 2];
    var instance = parts[parts.length - 1];

    if (!/^(\d+\.){2,3}\d+$/.test(parts[parts.length - 2])) { // version is not present
      instance = parts[parts.length - 2];
      version = '';
    }

    if (ENV.environment === 'development') {
      return '/resources/files/fileops';
    }

    return 'api/v1/views/' + view + version + '/instances/' + instance + '/resources/files/fileops';
  }),

 headers: Ember.computed(function () {
    let headers = {
      'X-Requested-By': 'ambari',
      'Content-Type': 'application/json'
    };

    if (ENV.environment === 'development') {
      // In development mode when the UI is served using ember serve the xhr requests are proxied to ambari server
      // by setting the proxyurl parameter in ember serve and for ambari to authenticate the requests, it needs this
      // basic authorization. This is for default admin/admin username/password combination.
      headers['Authorization'] = 'Basic YWRtaW46YWRtaW4=';
    }
    return headers;
  })
});
