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
require('controllers/main/admin/kerberos/progress_controller');

App.KerberosDisableController = App.KerberosProgressPageController.extend(App.WizardEnableDone, {

  name: 'kerberosDisableController',
  clusterDeployState: 'DEFAULT',
  commands: ['startZookeeper', 'stopAllButZookeeper', 'unkerberize', 'deleteKerberos', 'startAllServices'],

  tasksMessagesPrefix: 'admin.kerberos.disable.step',

  loadStep: function() {
    this.set('content.controllerName', 'kerberosDisableController');
    this.loadTasksStatuses();
    this.loadTasksRequestIds();
    this.loadRequestIds();
    this._super();
  },

  startZookeeper: function () {
    this.startServices(false, ["ZOOKEEPER"], true);
  },

  stopAllButZookeeper: function () {
    this.stopServices(["ZOOKEEPER"], false);
  },

  unkerberize: function () {
    return App.ajax.send({
      name: 'admin.unkerberize.cluster',
      sender: this,
      success: 'startPolling',
      error: 'onTaskErrorWithSkip'
    });
  },

  skipTask: function () {
    return App.ajax.send({
      name: 'admin.unkerberize.cluster.skip',
      sender: this,
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  deleteKerberos: function () {
    return App.ajax.send({
      name: 'common.delete.service',
      sender: this,
      data: {
        serviceName: 'KERBEROS'
      },
      success: 'onTaskCompleted',
      error: 'onTaskCompleted'
    });
  },

  startAllServices: function () {
    this.startServices(true);
  }

});
