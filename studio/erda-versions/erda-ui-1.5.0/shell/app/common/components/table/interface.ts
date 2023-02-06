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

import { SorterResult, TablePaginationConfig, TableRowSelection } from 'antd/es/table/interface';
import { ColumnProps as AntdColumnProps, TableProps } from 'antd/es/table';

export { SorterResult, TablePaginationConfig, TableProps };

export interface IActions<T> {
  width?: number | string;
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

export interface ColumnProps<T> extends AntdColumnProps<T> {
  width?: number | string;
  subTitle?: ((text: string, record: T, index: number) => React.ReactNode) | React.ReactNode;
  icon?: ((text: string, record: T, index: number) => React.ReactNode) | React.ReactNode;
  show?: boolean;
  hidden?: boolean;
  sortTitle?: React.ReactNode;
}

export interface IRowSelection<T extends object = any> extends TableRowSelection<T> {
  actions?: IRowActions[];
}

export interface IRowActions {
  key: string;
  name: string;
  disabled?: boolean;
  onClick: () => void;
}

interface IAction {
  title: string;
  onClick: () => void;
  show?: boolean;
}

export interface TableConfigProps<T> {
  slot?: React.ReactNode;
  columns: Array<ColumnProps<T>>;
  setColumns: (val: Array<ColumnProps<T>>) => void;
  onReload: ([key]: any) => void;
  sortColumn: SorterResult<T>;
}

export declare type TableAction = 'paginate' | 'sort' | 'filter';
