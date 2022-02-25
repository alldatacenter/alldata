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

App.AutostartDisabledCheckView = Em.View.extend({
  template: Em.Handlebars.compile([
    '<button {{bindAttr disabled="view.autostartButtonDisabled"}} class="pull-right btn" {{action disableAutostart target="view"}}>',
      '{{ t popup.clusterCheck.Upgrade.fail.auto_start_disabled.action_btn }}',
    '</button>',
    '{{ t popup.clusterCheck.Upgrade.fail.auto_start_disabled }}'
  ].join('')),

  classNames: ['custom-cluster-check'],

  autostartButtonDisabled: false,

  disableAutostart: function () {
    const mainAdminServiceAutoStartController = App.router.get('mainAdminServiceAutoStartController');
    this.set('autostartButtonDisabled', true);

    mainAdminServiceAutoStartController.load().done(() => {
      mainAdminServiceAutoStartController.set('isGeneralRecoveryEnabled', false);
      mainAdminServiceAutoStartController.saveClusterConfigs(mainAdminServiceAutoStartController.get('clusterConfigs'), false).always((data) => {
        this.set('autostartButtonDisabled', false);
      })
    }).fail( (data) => {
      this.set('autostartButtonDisabled', false);
    });
  }
});
