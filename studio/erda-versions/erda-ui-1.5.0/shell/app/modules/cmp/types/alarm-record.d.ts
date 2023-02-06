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

declare namespace ALARM_REPORT {
  interface RecordListQuery {
    alertState?: string;
    alertType?: string;
    handleState?: string;
    handleId?: number;
    clusters?: any;
    tenantGroup?: string;
  }
  interface RecordListItem {
    id: number;
    groupId: string;
    title: string;
    alertState: string;
    alertType: string;
    handleState: string;
    handlerId: string;
    updateTime: number;
    handleTime: number;
    alertName: string;
    projectId?: number;
    issueId?: number;
    expressionId: number;
  }
  interface AlarmTimesQuery {
    start: number;
    end: number;
    filter_alert_group_id: string;
    filter_dice_org_id?: string;
    filter_tenant_group?: string;
    filter_terminus_key?: string;
    count: string;
  }
  interface AlarmHistoriesQuery {
    start: number;
    end: number;
    count?: number;
    groupId: string;
    tenantGroup?: string;
  }
  interface AlarmHistory {
    groupId: string;
    alertState: string;
    content: string;
    timestamp: number;
    title: string;
    displayUrl: string;
    isSilence: boolean;
  }
}
