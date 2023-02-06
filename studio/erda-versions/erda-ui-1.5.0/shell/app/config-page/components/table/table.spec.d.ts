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

declare namespace CP_TABLE {
  interface Spec {
    type: 'Table';
    state?: IState;
    operations?: Obj<CP_COMMON.Operation>;
    batchOperations?: Obj<CP_COMMON.Operation>;
    data?: IData;
    props: IProps;
  }

  interface IData {
    list: Obj[];
  }

  interface IProps {
    pageSizeOptions?: string[];
    columns: Column[];
    rowKey: string;
    styleNames?: Obj;
    title?: string;
    visible?: boolean;
    rowSelection?: Obj;
    selectable?: boolean;
    showHeader?: boolean;
    pagination?: boolean;
    batchOperations?: string[];
    expandedProps?: {
      columns: Column[];
      rowKey: string;
    };
  }

  interface IState {
    total: number;
    pageNo: number;
    pageSize: number;
    selectedRowKeys?: string[];
    sorter?: { field: string; order: string };
  }

  type Props = MakeProps<Spec> & {
    slot?: React.ReactNode;
  };

  interface Column {
    title: string;
    dataIndex: string;
    titleRenderType?: string;
    width?: number;
    titleTip?: string | string[];
    data?: any;
  }

  type RenderType =
    | 'textWithTags'
    | 'operationsDropdownMenu'
    | 'progress'
    | 'tableOperation'
    | 'string-list'
    | 'userAvatar'
    | 'memberSelector'
    | 'gantt'
    | 'textWithBadge'
    | 'textWithLevel'
    | 'datePicker'
    | 'linkText'
    | 'bgProgress'
    | 'tagsRow';

  interface Row_Obj {
    [k: string]: any;
    renderType: RenderType;
    value?: any;
    prefixIcon?: string;
    operations?: Obj<CP_COMMON.Operation>;
  }
}
