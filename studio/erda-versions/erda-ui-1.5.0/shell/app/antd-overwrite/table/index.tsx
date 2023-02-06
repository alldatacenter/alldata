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
import { Dropdown, Menu, Divider } from 'antd';
import Table, { ColumnProps as AntdColumnProps, TableProps } from 'antd/es/table';
import i18n from 'i18n';

import './index.scss';

const { Column, ColumnGroup, Summary } = Table;
export interface ColumnProps<recordType> extends AntdColumnProps<recordType> {
  /**
   * id\number - 72
   *
   * user\status\type\cpu\memory - 120
   *
   * email\phone\roles\ip - 160
   *
   * time - 200
   *
   * operations - 80 * n, according to the number of buttons and the number of words
   *
   * detail\content\description - No need to increase the width of the adaptive, and add the scroll.x of a certain number to the table
   *
   * All width should be at least larger than the Title in English
   */
  width?: IWidth;
}

type IWidth = 64 | 72 | 80 | 96 | 120 | 160 | 176 | 200 | 240 | 280 | 320;

interface IProps<T extends object = any> extends TableProps<T> {
  columns: Array<ColumnProps<T>>;
  actions?: IActions<T>;
}

export interface IActions<T> {
  width: IWidth;
  /**
   * (record: T) => IAction[]
   *
   * interface IAction {
   *   title: string;
   *   onClick: () => void;
   * }
   */
  render: (record: T) => IAction[];
  /**
   * Limit the number of displays
   */
  limitNum?: number;
}

interface IAction {
  title: string;
  onClick: () => void;
}

function WrappedTable<T extends object = any>({ columns, rowClassName, actions, className, ...props }: IProps<T>) {
  const newColumns = columns?.map(({ ...args }: ColumnProps<T>) => ({
    ellipsis: true,
    ...args,
  }));

  return (
    <Table
      className={`wrapped-table ${className || ''}`}
      scroll={{ x: '100%' }}
      columns={[...newColumns, ...renderActions(actions)]}
      rowClassName={props.onRow ? `cursor-pointer ${rowClassName || ''}` : rowClassName}
      size="small"
      {...props}
    />
  );
}

function renderActions<T extends object = any>(actions?: IActions<T>): Array<ColumnProps<T>> {
  if (actions) {
    const { width, render, limitNum } = actions;
    return [
      {
        title: i18n.t('operation'),
        width,
        dataIndex: 'operation',
        ellipsis: true,
        fixed: 'right',
        render: (_: any, record: T) => {
          const list = render(record);

          const menu = (limitNum || limitNum === 0) && (
            <Menu>
              {list.slice(limitNum).map((item) => (
                <Menu.Item key={item.title} onClick={item.onClick}>
                  <span className="fake-link mr-1">{item.title}</span>
                </Menu.Item>
              ))}
            </Menu>
          );

          return (
            <span className="operate-list">
              {list.slice(0, limitNum).map((item, index: number) => (
                <>
                  {index !== 0 && <Divider type="vertical" />}
                  <span className="fake-link mr-1 align-middle" key={item.title} onClick={item.onClick}>
                    {item.title}
                  </span>
                </>
              ))}
              {menu && (
                <Dropdown overlay={menu} align={{ offset: [0, 5] }}>
                  <Icon />
                </Dropdown>
              )}
            </span>
          );
        },
      },
    ];
  } else {
    return [];
  }
}

const Icon = ({ className, ...rest }: { className?: string }) => {
  return (
    // @ts-ignore iconpark component
    <iconpark-icon
      name={'more'}
      fill={'#106,84,158'}
      class={`cursor-pointer align-middle table-more-ops ${className || ''}`}
      {...rest}
    />
  );
};

WrappedTable.Column = Column;
WrappedTable.ColumnGroup = ColumnGroup;
WrappedTable.Summary = Summary;

export default WrappedTable;
