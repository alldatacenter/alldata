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

App.ManageJournalNodeProgressPageController = App.ManageJournalNodeWizardController.extend(App.wizardProgressPageControllerMixin, {
  name: 'manageJournalNodeProgressPageController',
  clusterDeployState: 'JOURNALNODE_MANAGEMENT',
  tasksMessagesPrefix: 'admin.manageJournalNode.wizard.step',
  isRollback: false,
  
  /**
   * Prepare object to send to the server to save configs
   * Split all configs by site names and note
   * @param siteNames Array
   * @param data Object
   * @param note String
   */
  reconfigureSites: function(siteNames, data, note) {

    return siteNames.map(function(_siteName) {
      var config = data.items.findProperty('type', _siteName);
      var configToSave = {
        type: _siteName,
        properties: config && config.properties,
        service_config_version_note: note || ''
      };
      if (config && config.properties_attributes) {
        configToSave.properties_attributes = config.properties_attributes;
      }
      return configToSave;
    });
  }
});
