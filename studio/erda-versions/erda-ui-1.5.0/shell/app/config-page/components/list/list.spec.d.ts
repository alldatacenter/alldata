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

declare namespace CP_LIST {
  interface Spec {
    type: 'List';
    operations?: Obj<CP_COMMON.Operation>;
    props?: IProps;
    data: IData;
    state?: IState;
  }

  interface IState {
    pageNo?: number;
    pageSize?: number;
    total?: number;
  }

  interface IProps {
    rowKey?: string;
    visible?: boolean;
    size?: ISize;
    isLoadMore?: boolean;
    alignCenter?: boolean;
    noBorder?: boolean;
    pageSizeOptions?: string[];
  }

  type ISize = 'middle' | 'large' | 'small';

  interface IData {
    list: IListData[];
  }

  interface IListData {
    [pro: string]: any;
    id?: string | number;
    title: string;
    description?: string;
    prefixImg?: string | React.ReactNode;
    extraInfos?: IIconInfo[];
    operations?: Obj<CP_COMMON.Operation>;
  }

  interface IIconInfo {
    icon?: string;
    text: string;
    type?: 'success' | 'normal' | 'warning' | 'error';
    tooltip?: string;
    operations?: Obj<CP_COMMON.Operation>;
  }

  type Props = MakeProps<Spec>;
}
