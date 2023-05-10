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

import React, { useEffect } from 'react';
import { map, find, isEmpty } from 'lodash';
import { useUpdate } from 'common/use-hooks';
import MachineTable from '../machine-manager/machine-table';
import { useLoading } from 'core/stores/loading';
import clusterDashboardStore from 'dcos/stores/dashboard';
import orgMachineStore from '../../stores/machine';

interface IProps {
  machineList: ORG_MACHINE.IMachine[];
  onClickMachine: (payload: object) => void;
  onClickInstance: (payload: object) => void;
}
export const PureMachineManager = ({ machineList, onClickMachine, onClickInstance }: IProps) => {
  const { getMachineStatus } = orgMachineStore.effects;
  const [loading] = useLoading(clusterDashboardStore, ['getGroupInfos']);
  const [state, updater] = useUpdate({
    logType: '',
    machineInfo: {},
    modalVisible: false,
    slideVisible: false,
    machineListWithStatus: machineList,
  });

  useEffect(() => {
    if (isEmpty(machineList)) {
      updater.machineListWithStatus([]);
      return;
    }

    updater.machineListWithStatus(machineList);

    getMachineStatus(map(machineList, ({ ip }) => ip)).then((hostsStatus) => {
      const machineListWithStatus = map(machineList, (item) => {
        const target = find(hostsStatus, { host_ip: item.ip });
        return target
          ? {
              ...item,
              status: target.status_level,
              abnormalMsg: target.abnormal_msg,
            }
          : {};
      }) as ORG_MACHINE.IMachine[];
      updater.machineListWithStatus(machineListWithStatus);
    });
  }, [getMachineStatus, machineList, updater]);

  return (
    <div className="cluster-summary">
      <MachineTable
        list={state.machineListWithStatus}
        gotoMachineMonitor={onClickMachine}
        gotoMachineTasks={onClickInstance}
        isFetching={loading}
      />
    </div>
  );
};

export const MachineList = PureMachineManager;
