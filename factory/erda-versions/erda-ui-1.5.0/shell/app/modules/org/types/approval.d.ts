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

declare namespace APPROVAL {
  type ApprovalType = 'done' | 'undone';
  type ApprovalStatus = 'pending' | 'denied' | 'approved';
  type ApprovalItemType = 'lib-reference' | 'certificate';

  interface Item {
    id: string;
    type: ApprovalItemType;
    title: string;
    desc: string;
    priority: string;
    status: ApprovalStatus;
    orgId: string;
    submitter: string;
    approver: string;
    entityId: string;
    targetId: string;
    approvalTime: string;
    createTime: string;
    updateTime: string;
    extra: {
      ios: string;
      android: string;
    };
  }

  interface IApprovalQuery {
    pageNo: number;
    pageSize: number;
    status?: string | string[];
  }

  interface UpdateBody {
    id: string;
    status: ApprovalStatus;
  }
}
