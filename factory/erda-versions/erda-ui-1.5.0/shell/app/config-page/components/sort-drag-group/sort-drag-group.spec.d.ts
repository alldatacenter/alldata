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

declare namespace CP_SORT_GROUP {
  interface Spec {
    type: 'SortGroup';
    state: Obj;
    operations: Obj;
    props?: IProps;
    data: IData;
  }

  interface IProps {
    draggable?: boolean;
    groupDraggable?: boolean;
  }

  interface IData {
    type: string;
    value: Item[];
  }

  interface Operation {
    icon: string;
    hoverShow?: boolean;
    hoverTip?: string;
    key: string;
    reload: boolean;
    text?: string;
    disabled?: boolean;
    disabledTip?: string;
    confirm?: string | { title: string; subTitle: string };
    meta?: Obj;
    show?: boolean;
    prefixIcon?: string;
    fillMeta?: string;
  }

  interface Item {
    id: number;
    groupId: number;
    title: string;
    operations?: Obj<Operation>;
  }

  type Props = MakeProps<Spec>;
}
