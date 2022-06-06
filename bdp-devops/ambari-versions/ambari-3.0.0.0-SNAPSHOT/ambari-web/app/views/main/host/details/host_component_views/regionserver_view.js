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

App.RegionServerComponentView = App.HostComponentView.extend(App.Decommissionable, {

  componentForCheckDecommission: 'HBASE_MASTER',

  setDesiredAdminState: function (desiredAdminState) {
    this.getRSDecommissionStatus(desiredAdminState);
  },

  getRSDecommissionStatus: function (desiredAdminState) {
    const hostName = App.HBaseService.find('HBASE').get('master.hostName');
    App.ajax.send({
      name: 'host.host_component.decommission_status_regionserver',
      sender: this,
      data: {
        hostName,
        desiredAdminState
      },
      success: 'getRSDecommissionStatusSuccessCallback',
      error: 'getRSDecommissionStatusErrorCallback'
    });
  },

  getRSDecommissionStatusSuccessCallback: function (data, opt, params) {
    const {desiredAdminState} = params,
      hostName = this.get('content.hostName');
    if (data) {
      const liveRSHostsMetrics = Em.get(data, 'items.0.metrics.hbase.master.liveRegionServersHosts'),
        deadRSHostsMetrics = Em.get(data, 'items.0.metrics.hbase.master.deadRegionServersHosts'),
        liveRSHosts = this.parseRegionServersHosts(liveRSHostsMetrics),
        deadRSHosts = this.parseRegionServersHosts(deadRSHostsMetrics),
        isLiveRS = liveRSHosts.contains(hostName),
        isDeadRS = deadRSHosts.contains(hostName),
        isInServiceDesired = desiredAdminState === 'INSERVICE',
        isDecommissionedDesired = desiredAdminState === 'DECOMMISSIONED';
      if ((liveRSHosts.length + deadRSHosts.length === 0) || (isInServiceDesired && isLiveRS) || (isDecommissionedDesired && isDeadRS)) {
        this.setDesiredAdminStateDefault(desiredAdminState);
      } else if (isInServiceDesired) {
        this.setStatusAs('RS_DECOMMISSIONED');
      } else if (isDecommissionedDesired) {
        this.setStatusAs('INSERVICE');
      }
    } else {
      this.setDesiredAdminStateDefault(desiredAdminState);
    }
  },

  getRSDecommissionStatusErrorCallback: function (request, ajaxOptions, error, opt, params) {
    this.setDesiredAdminStateDefault(params.desiredAdminState);
  },

  parseRegionServersHosts: function (str) {
    const items = str ? str.split(';') : [],
      hosts = items.map(item => item.split(',')[0]);
    return hosts;
  },

  setDesiredAdminStateDefault: function (desiredAdminState) {
    switch (desiredAdminState) {
      case 'INSERVICE':
        this.setStatusAs(desiredAdminState);
        break;
      case 'DECOMMISSIONED':
        this.setStatusAs('RS_DECOMMISSIONED');
        break;
    }
  }
});
