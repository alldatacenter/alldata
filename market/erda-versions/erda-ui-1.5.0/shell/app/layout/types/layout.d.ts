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

declare namespace LAYOUT {
  interface IMsg {
    id: number;
    title: string;
    content: string;
    module: string;
    status: string;
    createdAt: string;
    readAt: string | null;
    unreadCount: number;
  }

  interface IApp {
    key: string;
    name: string;
    breadcrumbName: string;
    href: string;
    path?: string | Function;
  }

  interface InviteToOrgPayload {
    verifyCode: string;
    userIds: string[];
    orgId: string;
  }

  interface IInitLayout {
    key: string;
    appList: IApp[];
    currentApp: IApp;
    menusMap: Obj<IMenuObj>;
  }

  interface IMenuObj {
    menu: IMenu[];
    detail?: {
      displayName: string;
    };
  }

  interface IMenu {
    icon: string;
    text: string;
    href: string;
  }
}
