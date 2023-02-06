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

import * as history from 'history';
import 'jest-enzyme';
import { IFormItem } from 'common';

export interface Location extends history.Location {
  query: any;
}

declare global {
  interface Window {
    sysNotify(): any;
  }
}

// export {GlobalNavigationProps} from '@terminus/nusi/es/global-navigation/interface'
// export type History = history.History;

export interface IUseFilterProps<T = any> {
  onSubmit: (value: Record<string, any>) => void;
  onReset: () => void;
  onPageChange: (pNo: number) => void;
  fetchDataWithQuery: (pNo?: number) => void;
  queryCondition: any;
  pageNo: number;
  autoPagination: (paging: IPaging) => Obj;
  onTableChange: (pagination: any, _filters: any, sorter: any) => void;
  sizeChangePagination: (paging: IPaging) => JSX.Element;
}

export interface IUseMultiFilterProps extends Omit<IUseFilterProps, 'onTableChange' | 'sizeChangePagination'> {
  activeType: string;
  onChangeType: (t: string | number) => void;
}

export type FormModalList = IFormItem[] | ((form: any, isEdit: boolean) => IFormItem[]);

// TODO: remove these type, import from antd directly
export type { FormInstance } from 'antd/es/form/Form';
export type { SelectValue, SelectProps } from 'antd/es/select';
export type { DrawerProps } from 'antd/es/drawer';
export type { CheckboxChangeEvent } from 'antd/es/checkbox/Checkbox';
export type {
  AntTreeNodeSelectedEvent,
  TreeProps,
  TreeNodeNormal,
  AntTreeNode,
  AntTreeNodeProps,
  AntTreeNodeDropEvent,
} from 'antd/lib/tree/Tree';
export type { UploadProps } from 'antd/es/upload';
export type { InputProps } from 'antd/es/input';
export type { MenuProps } from 'antd/es/menu';
// export type { ColumnProps, IActions } from '../nusi/wrapped-table'; // TODO: replace with antd-overwrite table
export type { ModalProps } from 'antd/es/modal';
export type { FormProps, FormItemProps } from 'antd/es/form';
export type { AbstractTooltipProps } from 'antd/es/tooltip';
export type { BreadcrumbProps, Route } from 'antd/es/breadcrumb/Breadcrumb';
export type { PaginationProps } from 'antd/es/pagination';
export type { RadioChangeEvent } from 'antd/es/radio/interface';
export type { TransferItem } from 'antd/es/transfer/index';
export type { ColumnProps } from 'antd/es/table';
