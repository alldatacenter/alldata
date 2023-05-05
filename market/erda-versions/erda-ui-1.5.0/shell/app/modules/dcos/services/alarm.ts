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

import agent from 'agent';

export interface IMachineAlarmQuery {
  startTime: number;
  endTime: number;
  orgID: number;
  type: string | string[];
  targetType: string;
  metricID: string[];
  pageSize: number;
  pageNo: number;
}
export const getAlarmList = (query: IMachineAlarmQuery): IPagingResp<ORG_ALARM.Ticket> => {
  return agent
    .get('/api/tickets')
    .query(query)
    .then((response: any) => response.body);
};
