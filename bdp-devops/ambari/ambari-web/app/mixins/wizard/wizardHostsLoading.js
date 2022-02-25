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

/**
 * Mixin for loading hosts info in wizards
 * @type {Ember.Mixin}
 */
App.WizardHostsLoading = Em.Mixin.create({


  /**
   * @type {boolean}
   * @default false
   */
  isLoaded: false,

  willInsertElement: function() {
    this.set('isLoaded', false);
    this.loadHosts();
  },

  /**
   * load hosts from server
   */
  loadHosts: function () {
    return App.ajax.send({
      name: 'hosts.high_availability.wizard',
      data: {},
      sender: this,
      success: 'loadHostsSuccessCallback',
      error: 'loadHostsErrorCallback'
    });
  },

  loadHostsSuccessCallback: function (data, opt, params) {
    var hosts = {};

    data.items.forEach(function (item) {
      hosts[item.Hosts.host_name] = {
        name: item.Hosts.host_name,
        bootStatus: "REGISTERED",
        isInstalled: true
      };
    });
    this.get('controller').setDBProperty('hosts', hosts);
    this.set('controller.content.hosts', hosts);
    this.set('isLoaded', true);
  },

  loadHostsErrorCallback: function(){
    this.set('isLoaded', true);
  }

});


