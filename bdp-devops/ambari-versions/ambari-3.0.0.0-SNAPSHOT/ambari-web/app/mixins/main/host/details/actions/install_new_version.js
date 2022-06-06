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

App.InstallNewVersion = Em.Mixin.create({

  /**
   * show popup confirmation of version installation
   * @param event
   */
  installVersionConfirmation: function (event) {
    var self = this;

    return App.showConfirmationPopup(function () {
        self.installVersion(event);
      },
      Em.I18n.t('hosts.host.stackVersions.install.confirmation').format(event.context.get('displayName'))
    );
  },

  /**
   * install HostStackVersion on host
   * @param {object} event
   */
  installVersion: function (event) {
    App.ajax.send({
      name: 'host.stack_versions.install',
      sender: this,
      data: {
        hostName: this.get('content.hostName'),
        version: event.context
      },
      success: 'installVersionSuccessCallback'
    });
  },

  /**
   * success callback of <code>installVersion</code>
   * on success set version status to INSTALLING
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   */
  installVersionSuccessCallback: function (data, opt, params) {
    App.HostStackVersion.find(params.version.get('id')).set('status', 'INSTALLING');
    App.db.set('repoVersionInstall', 'id', [data.Requests.id]);
    App.clusterStatus.setClusterStatus({
      wizardControllerName: this.get('name'),
      localdb: App.db.data
    });
  },

  /**
   * Call callback after loading service metrics
   * @param callback
   */
  isServiceMetricsLoaded: function(callback) {
    App.router.get('mainController').isLoading.call(App.router.get('clusterController'), 'isServiceContentFullyLoaded').done(callback);
  }

});