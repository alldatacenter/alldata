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
import { Dropdown, Menu } from 'antd';
import { OperationAction } from 'config-page/utils';

const CP_DROPDOWN = (props: CP_DROPDOWN.Props) => {
  const { execOperation, operations, children, props: configProps, customOp } = props;
  const { visible, menus, menuProps, ...rest } = configProps || {};
  if (visible === false) return null;

  const onClick = (op?: CP_COMMON.Operation) => {
    if (op) {
      execOperation(op);
      customOp?.[op.key] && customOp[op.key](op);
    }
  };

  const menuOverlay = (
    <Menu {...menuProps}>
      {menus?.map((menu) => {
        return (
          <Menu.Item {...menu} key={menu.key}>
            <OperationAction operation={operations?.[menu.key]} onClick={() => onClick(operations?.[menu.key])}>
              <div>{menu.label}</div>
            </OperationAction>
          </Menu.Item>
        );
      })}
    </Menu>
  );
  return (
    <Dropdown overlay={menuOverlay} zIndex={1000} {...rest}>
      <span>{children}</span>
    </Dropdown>
  );
};

export default CP_DROPDOWN;
