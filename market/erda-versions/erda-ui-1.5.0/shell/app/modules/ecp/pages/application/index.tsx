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
import { isEmpty } from 'lodash';
import { useUpdate } from 'common/use-hooks';
import { MonitorDrawer } from '../components/monitor-drawer';

const applicationList = () => {
  const [{ monitorVisible, chosenSite }, updater] = useUpdate({
    monitorVisible: false,
    chosenSite: {} as MACHINE_MANAGE.IMonitorInfo,
  });

  return (
    <div>
      <DiceConfigPage showLoading scenarioKey="edge-application" scenarioType="edge-application" />
      {!isEmpty(chosenSite) ? (
        <MonitorDrawer data={chosenSite} visible={monitorVisible} onClose={() => updater.monitorVisible(false)} />
      ) : null}
    </div>
  );
};

export default applicationList;
