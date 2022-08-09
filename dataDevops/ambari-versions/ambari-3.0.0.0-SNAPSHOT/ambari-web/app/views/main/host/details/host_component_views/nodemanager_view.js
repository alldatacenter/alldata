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

App.NodeManagerComponentView = App.HostComponentView.extend(App.Decommissionable, {

  componentForCheckDecommission: 'RESOURCEMANAGER',

  setDesiredAdminState: function (desiredAdminState) {
    var self = this;
    switch (desiredAdminState) {
      case "INSERVICE":
        // can be decommissioned if already started
        this.setStatusAs(desiredAdminState);
        break;
      case "DECOMMISSIONED":
        this.getDecommissionStatus();
        break;
    }
  },

  /**
   * set decommission status according to component status and resource manager metrics
   */
  setDecommissionStatus: function (curObj) {
    var rmComponent = this.get('content.service.hostComponents').findProperty('componentName', this.get('componentForCheckDecommission')),
        hostName = this.get('content.hostName');

    rmComponent.set('workStatus', curObj.component_state);
    if (curObj.rm_metrics) {
      // Update RESOURCEMANAGER status
      var nodeManagersArray = App.parseJSON(curObj.rm_metrics.cluster.nodeManagers);
      if (nodeManagersArray.findProperty('HostName', hostName)) {
        // decommisioning ..
        this.setStatusAs('DECOMMISSIONING');
      } else {
        // decommissioned ..
        this.setStatusAs('DECOMMISSIONED');
      }
    } else {
      // in this case ResourceManager not started. Set status to Decommissioned
      this.setStatusAs('DECOMMISSIONED');
    }
  }
});
