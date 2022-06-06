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


const App = require('app');

App.MasterMaintenanceDisabledCheckView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_checks_maintenance'),

  maintananceOff: function (e) {
    const host = e.context;
    Ember.set(host, 'processing', true);

    App.ajax.send({
      name: 'bulk_request.hosts.passive_state',
      sender: this,
      data: {
        hostNames: host.name,
        passive_state: 'OFF',
        requestInfo: Em.I18n.t('hosts.host.details.for.postfix').format(this.t('passiveState.turn' + ' off'))
      }
    }).always(function () {
      Ember.set(host, 'processing', false);
    })
  },

  hosts: function () {
    return this.get('check.failed_on').map(host => ({name: host, processing: false}));
  }.property('check', 'check.failed_on')
});