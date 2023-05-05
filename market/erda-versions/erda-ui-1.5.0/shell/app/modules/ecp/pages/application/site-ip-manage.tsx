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

import React from 'react';
import DiceConfigPage from 'app/config-page';
import routeInfoStore from 'core/stores/route';
import { useUpdate } from 'common/use-hooks';
import { MonitorDrawer } from '../components/monitor-drawer';

const appSiteIpManage = () => {
  const [{ monitorVisible, chosenSite }, updater, update] = useUpdate({
    monitorVisible: false,
    chosenSite: {} as MACHINE_MANAGE.IMonitorInfo,
  });

  const { id, siteName } = routeInfoStore.useStore((s) => s.params);
  const { appName } = routeInfoStore.useStore((s) => s.query);
  const inParams = {
    id: +id,
    appName,
    siteName,
  };

  return (
    <div>
      <DiceConfigPage
        showLoading
        scenarioKey="edge-app-site-ip"
        scenarioType="edge-app-site-ip"
        inParams={inParams}
        customProps={{
          siteIpList: {
            op: {
              operations: {
                viewMonitor: (site: { meta: MACHINE_MANAGE.IMonitorInfo }) => {
                  update({
                    monitorVisible: true,
                    chosenSite: site.meta,
                  });
                },
                viewLog: (site: { meta: MACHINE_MANAGE.IMonitorInfo }) => {
                  update({
                    monitorVisible: true,
                    chosenSite: site.meta,
                  });
                },
                viewTerminal: (site: { meta: MACHINE_MANAGE.IMonitorInfo }) => {
                  update({
                    monitorVisible: true,
                    chosenSite: site.meta,
                  });
                },
              },
            },
          },
        }}
      />
      <MonitorDrawer data={chosenSite} visible={monitorVisible} onClose={() => updater.monitorVisible(false)} />
    </div>
  );
};

export default appSiteIpManage;
