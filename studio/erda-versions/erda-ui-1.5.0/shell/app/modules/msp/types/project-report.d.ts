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

declare namespace PROJECT_REPORT {
  interface QueryConfig {
    projectId: string;
    workspace: string;
  }

  interface Config {
    id: number;
    projectId: number;
    projectName: string;
    workspace: string;
    created: string;
    weeklyReportConfig: string;
    dailyReportConfig: string;
    weeklyReportEnable: boolean;
    dailyReportEnable: boolean;
  }

  interface WeeklySetting {
    weeklyReportConfig: string;
    weeklyReportEnable: boolean;
  }

  interface DailySetting {
    dailyReportEnable: boolean;
    dailyReportConfig: string;
  }

  interface SetConfig extends QueryConfig {
    setting: DailySetting | WeeklySetting;
  }

  interface QueryReport extends QueryConfig {
    type: 'weekly' | 'daily';
    pageSize: number;
    pageNo: number;
    start: number;
    end: number;
  }

  interface IReport {
    id: number;
    key: string;
    start: string;
    end: string;
    version: string;
    created: string;
  }

  interface GetReportDetail extends QueryConfig {
    type: 'weekly' | 'daily';
    key: string;
  }
}
