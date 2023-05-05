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
import { Popover, Menu, Avatar } from 'antd';
import { UserMenuProps } from './interface';
import { getAvatarChars } from 'app/common/utils';

const UserMenu = ({ avatar, name, operations }: UserMenuProps) => {
  const nick = getAvatarChars(avatar?.chars || '');
  return (
    <Popover
      placement={'rightBottom'}
      overlayClassName="erda-global-nav-user-menu"
      content={
        <div className="container flex flex-col">
          <div className="user-info flex">
            <div className="avatar">
              <Avatar src={avatar?.src} size={48}>
                {nick}
              </Avatar>
            </div>
            <div className="desc-container flex items-baseline justify-center flex-col truncate">
              <div className="name">{name}</div>
            </div>
          </div>
          <div className="operation-group">
            <Menu>
              {operations?.map(({ onClick, icon, title }) => {
                return (
                  <Menu.Item key={title} onClick={onClick}>
                    <div className="flex items-center">
                      {icon}
                      {title}
                    </div>
                  </Menu.Item>
                );
              })}
            </Menu>
          </div>
        </div>
      }
    >
      <Avatar src={avatar?.src}>{nick}</Avatar>
    </Popover>
  );
};

export default UserMenu;
