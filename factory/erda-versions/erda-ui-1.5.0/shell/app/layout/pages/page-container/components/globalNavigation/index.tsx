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
import UserMenu from './userMenu';
import { UserMenuProps } from './interface';

import './index.scss';

interface IProps {
  layout?: 'vertical' | 'horizontal';
  verticalBrandIcon?: React.ReactNode;
  operations?: RightOperationItemProps[];
  userMenu?: UserMenuProps;
  slot?: {
    title?: React.ReactNode;
    element?: React.ReactNode;
    onClick?: (param: React.MouseEventHandler) => void;
  };
}

export interface RightOperationItemProps {
  title?: React.ReactNode;
  icon?: React.ReactNode;
  onClick?: () => void;
}

const PageContainer = ({ verticalBrandIcon, operations, userMenu, slot }: IProps) => {
  const renderRightOperation = () => {
    return operations!.map((item, index) => {
      const { onClick, icon } = item;
      return (
        <a
          key={index}
          className={`erda-global-nav-operation-item flex items-center justify-center cursor-pointer`}
          onClick={onClick}
        >
          {icon}
        </a>
      );
    });
  };

  return (
    <div className={`erda-global-nav flex flex-col items-center relative`}>
      {slot?.element && (
        <div
          className={`erda-global-nav-slot-container flex items-center justify-center text-center cursor-pointer align-top`}
        >
          {slot.element}
        </div>
      )}
      <div className={`erda-global-nav-logo-container flex items-center justify-center`}>{verticalBrandIcon}</div>
      <div className={`erda-global-nav-right-container flex flex-col items-center justify-end`}>
        <div className={`erda-global-nav-operation-container flex flex-col`}>{renderRightOperation()}</div>
        {userMenu && (
          <div className={`erda-global-nav-avatar-item flex items-center justify-center cursor-pointer`}>
            <UserMenu {...userMenu} />
          </div>
        )}
      </div>
    </div>
  );
};

export default PageContainer;
