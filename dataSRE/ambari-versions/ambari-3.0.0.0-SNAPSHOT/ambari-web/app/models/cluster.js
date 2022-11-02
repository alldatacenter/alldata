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
var credentialUtils = require('utils/credentials');

App.Cluster = DS.Model.extend({
  clusterName: DS.attr('string'),
  stackName: DS.attr('string'),
  version: DS.attr('string'),
  totalHosts:DS.attr('number'),
  securityType: DS.attr('string'),
  credentialStoreProperties: DS.attr('object', {defaultValue: {}}),
  /**
   * Array containing desired configs. New array
   * should be set by instances of class.
   */
  desiredConfigs: null,

  isKerberosEnabled: Em.computed.equal('securityType', 'KERBEROS'),

  isCredentialStorePersistent: function () {
    return this.get('credentialStoreProperties')[credentialUtils.STORE_TYPES.PERSISTENT_PATH] === "true";
  }.property('credentialStoreProperties')
});

App.Cluster.FIXTURES = [];
