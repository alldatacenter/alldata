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

import { Button, Dropdown, Menu } from 'antd';
import { ClickParam } from 'core/common/interface';
import { ErdaIcon } from 'common';
import React from 'react';
import { map } from 'lodash';

const { SubMenu } = Menu;

interface IProps {
  [propName: string]: any;
  buttonText?: string;
  btnProps?: object;
  overlay?: any;
  menuList?: IMenuItem[];
  loading?: boolean;
  trigger?: Array<'click' | 'hover' | 'contextMenu'>;
  onClickMenu?: (item: ClickParam) => void;
}

interface IMenuItem {
  key: string;
  name: string;
  disabled?: boolean;
  children?: IMenuItem[];
}

const DropdownSelect = (props: IProps) => {
  const {
    menuList,
    onClickMenu,
    overlay,
    trigger,
    buttonText,
    loading = false,
    children,
    btnProps,
    ...restProps
  } = props;
  let _overlay = overlay;
  if (menuList) {
    _overlay = (
      <Menu onClick={onClickMenu}>
        {map(menuList, (item: IMenuItem, key: string) => {
          if (item.children) {
            return (
              <SubMenu key={key} title={item.name}>
                {map(item.children, (subItem: IMenuItem) => (
                  <Menu.Item key={subItem.key} disabled={subItem.disabled}>
                    {subItem.name}
                  </Menu.Item>
                ))}
              </SubMenu>
            );
          } else {
            return (
              <Menu.Item key={item.key} disabled={item.disabled}>
                {item.name}
              </Menu.Item>
            );
          }
        })}
      </Menu>
    );
  }
  return (
    <Dropdown overlay={_overlay} trigger={trigger || ['click']} {...restProps}>
      {children || (
        <Button type="default" loading={loading} {...btnProps}>
          {buttonText}
          <ErdaIcon type="caret-down" className="align-middle ml-0.5" size="16" />
        </Button>
      )}
    </Dropdown>
  );
};

export default DropdownSelect;
