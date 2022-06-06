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

App.ManageJournalNodeWizardStep3Controller = App.HighAvailabilityWizardStep4Controller.extend({
  name: 'manageJournalNodeWizardStep3Controller',

  isActiveNameNodesStarted: true,

  isHDFSNameSpacesLoaded: Em.computed.alias('App.router.clusterController.isHDFSNameSpacesLoaded'),

  isDataLoadedAndNextEnabled: Em.computed.and('isNextEnabled', 'isHDFSNameSpacesLoaded'),

  pullCheckPointsStatuses: function () {
    if (this.get('isHDFSNameSpacesLoaded')) {
      this.removeObserver('isHDFSNameSpacesLoaded', this, 'pullCheckPointsStatuses');
      const hdfsModel = App.HDFSService.find('HDFS'),
        nameSpaces = hdfsModel.get('masterComponentGroups'),
        nameSpacesCount = nameSpaces.length;
      if (nameSpacesCount > 1) {
        let hostNames = hdfsModel.get('activeNameNodes').mapProperty('hostName');
        if (hostNames.length < nameSpacesCount) {
          nameSpaces.forEach(nameSpace => {
            const {hosts} = nameSpace,
              hasActiveNameNode = hosts.some(hostName => hostNames.contains(hostName));
            if (!hasActiveNameNode) {
              const hostForNameSpace = hosts.find(hostName => {
                  return App.HostComponent.find(`NAMENODE_${hostName}`).get('workStatus') === 'STARTED';
                }) || hosts[0];
              hostNames.push(hostForNameSpace);
            }
          });
        }
        App.ajax.send({
          name: 'admin.high_availability.getNnCheckPointsStatuses',
          sender: this,
          data: {
            hostNames
          },
          success: 'checkNnCheckPointsStatuses'
        });
      } else {
        this.pullCheckPointStatus();
      }
    } else {
      this.addObserver('isHDFSNameSpacesLoaded', this, 'pullCheckPointsStatuses');
    }
  },

  checkNnCheckPointsStatuses: function (data) {
    const items = Em.getWithDefault(data, 'items', []),
      isNextEnabled = items.length && items.every(this.getNnCheckPointStatus);
    this.setProperties({
      isActiveNameNodesStarted: items.length && items.everyProperty('HostRoles.desired_state', 'STARTED'),
      isNextEnabled
    });
    if (!isNextEnabled) {
      window.setTimeout(() => {
        this.pullCheckPointsStatuses();
      }, this.POLL_INTERVAL);
    }
  }
});