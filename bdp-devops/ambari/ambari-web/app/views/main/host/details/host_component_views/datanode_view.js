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

App.DataNodeComponentView = App.HostComponentView.extend(App.Decommissionable, {

  componentForCheckDecommission: 'NAMENODE',

  /**
   * Get component decommission status from server
   * @returns {$.ajax}
   */
  getDNDecommissionStatus: function () {
    // always get datanode decommission status from active namenodes (if NN HA or federation enabled)
    const hdfs = App.HDFSService.find().objectAt(0),
      activeNNHostNames = (!hdfs.get('snameNode') && hdfs.get('activeNameNodes.length')) ?
        hdfs.get('activeNameNodes').mapProperty('hostName').join(',') : hdfs.get('nameNode.hostName');
    return App.ajax.send({
      name: 'host.host_component.decommission_status_datanode',
      sender: this,
      data: {
        hostNames: activeNNHostNames,
        componentName: this.get('componentForCheckDecommission')
      },
      success: 'getDNDecommissionStatusSuccessCallback',
      error: 'getDNDecommissionStatusErrorCallback'
    });
  },

  /**
   * Set received value or null to <code>decommissionedStatusObject</code>
   * @param {Object} response
   * @returns {Object|null}
   */
  getDNDecommissionStatusSuccessCallback: function (response) {
    if (response && response.items) {
      const statusObjects = response.items.mapProperty('metrics.dfs.namenode');
      this.computeStatus(statusObjects);
    }
  },

  /**
   * Set null to <code>decommissionedStatusObject</code> if server returns error
   * @returns {null}
   */
  getDNDecommissionStatusErrorCallback: function () {
    this.set('decommissionedStatusObject', null);
    return null;
  },

  /**
   * load Recommission/Decommission status from adminState of each live node
   */
  loadComponentDecommissionStatus: function () {
    return this.getDNDecommissionStatus();
  },

  setDesiredAdminState: function (desired_admin_state) {
    this.setStatusAs(desired_admin_state);
  },

  /**
   * compute and set decommission state by namenode metrics
   * @param metricObjects
   */
  computeStatus: function (metricObjects) {
    const hostName = this.get('content.hostName');
    let inServiceCount = 0,
      decommissioningCount = 0,
      decommissionedCount = 0;
    metricObjects.forEach(curObj => {
      if (curObj) {
        const liveNodesJson = App.parseJSON(curObj.LiveNodes);
        for (let hostPort in liveNodesJson) {
          if (hostPort.indexOf(hostName) === 0) {
            switch (liveNodesJson[hostPort].adminState) {
              case 'In Service':
                inServiceCount++;
                break;
              case 'Decommission In Progress':
                decommissioningCount++;
                break;
              case 'Decommissioned':
                decommissionedCount++;
                break;
            }
            return;
          }
        }
      }
    });
    if (decommissioningCount) {
      this.setStatusAs('DECOMMISSIONING');
    } else if (inServiceCount && !decommissionedCount) {
      this.setStatusAs('INSERVICE');
    } else if (!inServiceCount && decommissionedCount) {
      this.setStatusAs('DECOMMISSIONED');
    } else {
      // if namenodes are down, get desired_admin_state to decide if the user had issued a decommission
      this.getDesiredAdminState();
    }
  }
});
