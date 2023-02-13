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

declare namespace COMMON_ALARM_REPORT {
  interface TargetInfo {
    type: string;
    groupId: number;
    groupType: string;
    notifyGroup?: COMMON_NOTIFY.INotifyGroup;
  }

  interface ReportTaskQuery {
    name: string;
    dashboardId: string;
    type: string;
    notifyTarget: TargetInfo;
  }

  interface ReportTask {
    id: number;
    name: string;
    type: string;
    enable: boolean;
    dashboardId: string;
    createdAt: number;
    notifyTarget: TargetInfo;
  }

  interface DashboardBlock {
    id: string;
    name: string;
    viewConfig?: any;
  }

  interface ReportType {
    value: string;
    name: string;
  }

  interface ReportRecord {
    id: number;
    start: number;
    end?: number;
    dashboardBlock: DashboardBlock;
  }

  interface GetAlarmReport {
    type: string;
    query: {
      filter_host_ip: string;
      timestamp: string;
    };
  }

  interface AlarmReportQuerys {
    points: number;
    start: string;
    end: string;
    filter_cluster_name: string;
    avg?: string[] | string;
    max?: string;
    group?: string;
  }

  interface QueryCmdLine {
    timestamp: number;
    filter_cluster_name: string;
    filter_pid: string;
    process_name: string;
    start: string;
    end: string;
    group: string;
  }

  interface AlarmReports {
    timer: number;
    results: Array<{
      data: Array<{
        tag: string;
      }>;
    }>;
  }
}
