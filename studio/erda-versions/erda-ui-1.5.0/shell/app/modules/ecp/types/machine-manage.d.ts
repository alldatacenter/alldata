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

declare namespace MACHINE_MANAGE {
  interface IMonitorInfo {
    extraQuery?: object;
    instance: {
      id?: string;
      containerId?: string;
      clusterName?: string;
      hostIP?: string;
    };
    type: 'monitor' | 'log' | 'terminal';
    timeSwitch?: boolean;
    api?: string;
  }

  interface IGroupInfoQuery {
    orgName: string;
    clusters: Array<{ clusterName: string; hostIPs?: string[] }>;
    filters: IFilterQuery[];
    groups: string[];
  }

  interface IGroupInfo {
    name: string | null;
    metric: {
      machines: number;
      abnormalMachines: number;
      cpuUsage: number;
      cpuRequest: number;
      cpuLimit: number;
      cpuOrigin: number;
      cpuTotal: number;
      cpuAllocatable: number;
      memUsage: number;
      memRequest: number;
      memLimit: number;
      memOrigin: number;
      memTotal: number;
      memAllocatable: number;
      diskUsage: number;
      diskTotal: number;
    };
    machines: ORG_MACHINE.IMachine[] | null;
    groups: IGroupInfo[] | null;
  }

  interface IFilterQuery {
    key: string;
    values: string[];
  }

  interface IOfflineMachine {
    id: number;
    siteIP: string;
  }
}
