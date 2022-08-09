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

App.ServiceUpCheckView = Em.View.extend({

  template: Em.Handlebars.compile([
    '<button {{bindAttr disabled="view.startServicesDisabled"}} class="pull-right btn" {{action startServices target="view"}}>',
      '{{ t popup.clusterCheck.Upgrade.fail.services_up.action_btn }}',
    '</button>',
    '{{ t popup.clusterCheck.Upgrade.fail.services_up }} {{view.servicesList}}'
  ].join('')),

  classNames: ['custom-cluster-check'],

  startServicesDisabled: false,

  servicesList: function () {
    return this.get('check.failed_on').join(', ');
  }.property('check.failed_on'),

  startServices: function () {
    this.set('startServicesDisabled', true);
    const data = {
      ServiceInfo: {
        state: 'STARTED'
      },
      context: "Start required services",
      urlParams: "ServiceInfo/service_name.in(" + this.get('servicesList') + ")"
    };

    return App.ajax.send({
      name: 'common.services.update',
      sender: this,
      data: data,
      success: 'requestSuccess'
    });
  },

  requestSuccess: function () {
    this.set('startServicesDisabled', false);
    App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }
    });
  }

});
