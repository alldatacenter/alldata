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

// Application bootstrapper
module.exports = Em.Application.create({
  //LOG_TRANSITIONS: true, 
  //LOG_TRANSITIONS_INTERNAL: true
  smokeTests: false,

  errorLog: "",

  getNamespaceUrl: function() {
    var parts = window.location.pathname.split('/').filter(function (i) {
      return i !== "";
    });
    var view = parts[parts.length - 3];
    var version = '/versions/' + parts[parts.length - 2];
    var instance = parts[parts.length - 1];

    if (!/^(\d+\.){2,3}\d+$/.test(parts[parts.length - 2])) { // version is not present
      instance = parts[parts.length - 2];
      version = '';
    }
    var namespaceUrl = 'api/v1/views/' + view + version + '/instances/' + instance;
    return namespaceUrl.replace(/^\/|\/$/g, ''); //remove starting slash if proxied through knox
  },

  Resolver: Ember.DefaultResolver.extend({
    resolveTemplate: function(parsedName) {
      var resolvedTemplate = this._super(parsedName);
      var templateName = 'templates/' + parsedName.fullNameWithoutType.replace(/\./g, '/');
      if (resolvedTemplate) {
        return resolvedTemplate;
      } else {
        return Ember.TEMPLATES[templateName];
      }
    }
  })
});
