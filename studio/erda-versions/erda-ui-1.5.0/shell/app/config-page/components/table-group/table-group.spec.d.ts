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

declare namespace CP_TABLE_GROUP {
  interface Spec {
    type: 'TableGroup';
    state?: IState;
    operations?: Obj<CP_COMMON.Operation>;
    data?: IData;
    props: IProps;
  }

  interface IData {
    list: IItem[];
  }

  interface IItem {
    title: CP_TEXT.Props;
    subtitle: CP_TITLE.IProps;
    description: CP_TEXT.IProps;
    table: CP_TABLE.Props;
    extraInfo: CP_TEXT.Props;
  }

  interface IProps {
    visible: boolean;
  }

  interface IState {
    total: number;
    pageNo: number;
    pageSize: number;
  }

  interface ITableBoardProps extends CONFIG_PAGE.ICommonProps {
    props: IItem;
  }

  type Props = MakeProps<Spec>;
}
