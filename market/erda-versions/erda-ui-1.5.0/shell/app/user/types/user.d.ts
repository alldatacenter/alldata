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

interface IPermResponseData {
  access: boolean;
  exist: boolean;
  roles: string[];
  contactInfo?: string;
  contactsWhenNoPermission: null | string[];
  permissionList: IPerm[];
  resourceRoleList?: IPerm[];
  scopeInfo?: IScopeInfo;
}

interface IScopeInfo {
  projectName?: string;
  appName?: string;
}

interface IPerm {
  resource: string;
  action: string;
  resourceRole?: string;
}
interface IAddPerm {
  scope: string;
  permissionList: IPerm[];
}

interface IHasPermQuery {
  scope: string;
  resource: string;
  action: string;
}

interface IGetScopePermQuery {
  scope: string;
  scopeID: string;
}

interface ILoginUser {
  id: string;
  email: string;
  nick: string;
  name: string;
  avatar: string;
  phone: string;
  token: string;
  userType?: string;
  isSysAdmin?: boolean;
  isNewUser?: boolean;
  adminRoles: string[];
}

interface IMember {
  id?: string;
  userId: string;
  email: string;
  mobile: string;
  name: string;
  nick: string;
  avatar: string;
  status: string;
  scope: {
    type: string;
    id: string;
  };
  roles: string[];
  removed: boolean;
  labels: string[] | null;
}
