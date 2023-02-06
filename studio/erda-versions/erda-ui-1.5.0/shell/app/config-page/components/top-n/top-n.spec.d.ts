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
declare namespace CP_DATA_RANK {
  interface IItem {
    id: string;
    name: string;
    value: number;
    percent: number;
    unit?: string;

    [key: string]: any;
  }

  interface IListITem {
    title: string;
    span: number;
    color?: string;
    titleIcon?: string;
    backgroundIcon?: string;
    items: IItem[];
  }

  interface Spec {
    type: 'TopN';
    props: {
      rowsProps?: import('antd/es/row').RowProps;
      theme: { color: string; titleIcon: string; backgroundIcon: string }[];
    };
    data: {
      list: IListITem[];
    };
  }

  type Props = MakeProps<Spec>;
}
