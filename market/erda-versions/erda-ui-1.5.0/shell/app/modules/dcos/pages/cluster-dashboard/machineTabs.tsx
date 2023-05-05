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

import React, { useState } from 'react';
import i18n from 'i18n';
import { Tabs } from 'antd';
import MachineDetail from './machine-detail';
import InstanceList from './instance-list';
import AlarmRecord from './alarm-record';

const { TabPane } = Tabs;

interface IProps {
  activeMachine: ORG_MACHINE.IMachine;
  activeMachineTab?: string;
}

const MachineTabs = ({ activeMachine, activeMachineTab }: IProps) => {
  const [activeKey, setActiveKey] = useState(activeMachineTab || 'overview');
  const { clusterName, ip } = activeMachine;

  const clusters = React.useRef([{ clusterName, hostIPs: [ip] }]);

  return (
    <Tabs activeKey={activeKey} onChange={setActiveKey}>
      <TabPane tab={i18n.t('machine overview')} key="overview">
        <MachineDetail type="insight" machineDetail={activeMachine} />
      </TabPane>
      <TabPane tab={i18n.t('cmp:machine alarm')} key="alarm">
        <AlarmRecord clusters={clusters.current} />
      </TabPane>
      <TabPane tab={i18n.t('cmp:instance list')} key="instance">
        <InstanceList instanceType="all" clusters={clusters.current} />
      </TabPane>
      <TabPane tab={i18n.t('services')} key="service">
        <InstanceList instanceType="service" clusters={clusters.current} />
      </TabPane>
      <TabPane tab={i18n.t('task list')} key="job">
        <InstanceList instanceType="job" clusters={clusters.current} />
      </TabPane>
      <TabPane tab={i18n.t('cmp:machine detail')} key="info">
        <MachineDetail type="info" machineDetail={activeMachine} />
      </TabPane>
    </Tabs>
  );
};

export default MachineTabs;
