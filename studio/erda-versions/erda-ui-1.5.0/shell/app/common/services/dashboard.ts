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

type CustomDashBoard = Merge<Custom_Dashboard.DashboardItem, { viewConfig: DC.Layout }>;

export const getCustomDashboard = ({ id, isSystem = false }: { id: string; isSystem?: boolean }): CustomDashBoard => {
  return agent
    .get(isSystem ? `/api/dashboard/system/blocks/${id}` : `/api/dashboard/blocks/${id}`)
    .then((response: any) => response.body);
};
