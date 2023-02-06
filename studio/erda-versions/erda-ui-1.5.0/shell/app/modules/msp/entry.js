// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import routers from './router';
import configCenterStore from './stores/config-center';
import dubboStore from './stores/dubbo';
import gatewayStore from './stores/gateway';
import httpStore from './stores/http';
import infoStore from './stores/info';
import logStore from './stores/log-analytics';
import mspStore from './stores/micro-service';
import projectReportStore from './stores/project-report';
import topologyStore from './stores/topology-service-analyze';
import traceStore from './stores/trace';
import zkproxyStore from './stores/zkproxy';
import logAnalyzeStore from './stores/log-analyze';

export default (registerModule) => {
  return registerModule({
    key: 'msp',
    stores: [
      configCenterStore,
      dubboStore,
      gatewayStore,
      httpStore,
      infoStore,
      mspStore,
      projectReportStore,
      topologyStore,
      traceStore,
      zkproxyStore,
      logStore,
      logAnalyzeStore,
    ],
    routers,
  });
};
