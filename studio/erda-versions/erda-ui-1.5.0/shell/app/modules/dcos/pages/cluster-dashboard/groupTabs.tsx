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

import React, { useEffect, useState } from 'react';
import { forEach, map } from 'lodash';
import { Tabs } from 'antd';
import { MachineList } from 'dcos/pages/cluster-dashboard/machine-list';
import { ResourceSummary, ResourceTable } from './resources-summary';
import { ResourcesUsagePie } from './resources-usage-trend';
import AlarmRecord from './alarm-record';
import i18n from 'i18n';

const { TabPane } = Tabs;

interface IProps {
  machineList: any[];
  activedGroup: any;
  onActiveMachine: (payload: object, key?: string) => void;
}

interface ITempCluster {
  clusterName: string;
  hostIPs: string[];
}
const GroupTabs = ({ machineList, onActiveMachine, activedGroup }: IProps) => {
  const [clusters, setClusters] = useState<ITempCluster[]>([]);
  const [activeKey, setActiveKey] = useState('machine');
  const clusterNameStr = clusters.map((c) => c.clusterName).join(',');

  useEffect(() => {
    const clusterHostMap = {};
    forEach(machineList, ({ clusterName, ip }) => {
      clusterHostMap[clusterName] = clusterHostMap[clusterName] ? [...clusterHostMap[clusterName], ip] : [ip];
    });
    setClusters(
      map(clusterHostMap, (hostIPs, clusterName) => ({
        clusterName,
        hostIPs,
      })),
    );
    if (activeKey === 'state') {
      setActiveKey('machine');
    }
  }, [machineList, activeKey]);

  return (
    <Tabs activeKey={activeKey} onChange={setActiveKey}>
      <TabPane tab={`${i18n.t('cmp:machines')}`} key="machine">
        <MachineList
          machineList={machineList}
          onClickMachine={onActiveMachine}
          onClickInstance={(record: any) => {
            onActiveMachine(record, 'instance');
          }}
        />
      </TabPane>
      <TabPane tab={`${i18n.t('cmp:machine alarm')}`} key="alarm">
        <AlarmRecord clusters={clusters} />
      </TabPane>
      {/* TODO: keep it until completely done. */}
      {/* <TabPane tab={`${i18n.t('cmp:resource overview')}`} key="resource-overview">
        <ResourceSummary clusterNameStr={clusterNameStr} />
      </TabPane>
      <TabPane tab={`${i18n.t('cmp:resource distribute')}`} key="resource-distribute">
        <ResourcesUsagePie clusterNameStr={clusterNameStr} />
      </TabPane> */}
    </Tabs>
  );
};

export default GroupTabs;
