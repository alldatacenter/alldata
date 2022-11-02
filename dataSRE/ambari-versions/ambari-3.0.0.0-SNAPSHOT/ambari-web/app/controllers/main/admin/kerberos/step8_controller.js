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

App.KerberosWizardStep8Controller = App.KerberosProgressPageController.extend({
  name: 'kerberosWizardStep8Controller',
  clusterDeployState: 'KERBEROS_DEPLOY',
  commands: ['startServices'],

  startServices: function () {
    var skipServiceCheck = App.router.get('clusterController.ambariProperties')['skip.service.checks'] === "true";
    App.ajax.send({
      name: 'common.services.update',
      sender: this,
      data: {
        "context": "Start services",
        "ServiceInfo": {
          "state": "STARTED"
        },
        urlParams: "params/run_smoke_test=" + !skipServiceCheck
      },
      success: 'startPolling',
      error: 'startServicesErrorCallback'
    });
  },

  isSubmitDisabled: Em.computed.notExistsIn('status', ['COMPLETED', 'FAILED'])

});
