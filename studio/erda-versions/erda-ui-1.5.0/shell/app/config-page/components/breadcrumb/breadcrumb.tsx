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
import { Breadcrumb, Menu } from 'antd';
import { Ellipsis, ErdaIcon } from 'common';
import { map } from 'lodash';

const noop = () => {};
const CP_Breadcrumb = (props: CP_BREADCRUMB.Props) => {
  const { data, operations, execOperation, props: configProps } = props;
  const { list = [] } = data || {};
  const { visible = true } = configProps || {};

  const onClickItem = (key: string) => {
    operations?.click && execOperation(operations.click, key);
  };

  if (!visible) return null;
  return (
    <Breadcrumb separator={<ErdaIcon className="align-middle text-xs" type="right" size="14px" />}>
      {map(list, (item, idx) => {
        if (item.menus) {
          const menu = (
            <Menu>
              {item.menus.map((menuItem) => (
                <Menu.Item>
                  <span className={cls} onClick={() => onClickItem(menuItem.key)}>
                    <Ellipsis title={menuItem.item}>{menuItem.item}</Ellipsis>
                  </span>
                </Menu.Item>
              ))}
            </Menu>
          );
          return (
            <Breadcrumb.Item key={item.activeKey} overlay={menu}>
              <span className="inline-block align-bottom" style={{ maxWidth: 140 }}>
                <Ellipsis title={item.item}>{item.item}</Ellipsis>
              </span>
            </Breadcrumb.Item>
          );
        }
        const [cls, onClick] = idx !== list.length - 1 ? ['cursor-pointer', () => onClickItem(item.key)] : ['', noop];
        return (
          <Breadcrumb.Item key={item.key}>
            <span className={`${cls} inline-block align-bottom`} onClick={onClick} style={{ maxWidth: 140 }}>
              <Ellipsis title={item.item}>{item.item}</Ellipsis>
            </span>
          </Breadcrumb.Item>
        );
      })}
    </Breadcrumb>
  );
};

export default CP_Breadcrumb;
