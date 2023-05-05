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

import React from 'react';
import { useUserMap } from 'core/stores/userMap';
import { get } from 'lodash';

interface IProps {
  id: string | number;
  render?: (data: ADMIN_USER.IPlatformUser, id?: string | number) => React.ReactNode;
}
const defaultRender = (data: ADMIN_USER.IPlatformUser, id: string | number) => {
  return data.nick || data.name || id;
};

const UserInfo = ({ id, render = defaultRender }: IProps) => {
  const userMap = useUserMap();
  const userInfo = get(userMap, id, {});
  return <>{render(userInfo, id)}</>;
};

export default UserInfo;
