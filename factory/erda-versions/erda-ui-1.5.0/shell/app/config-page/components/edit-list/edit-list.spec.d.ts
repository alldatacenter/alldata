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

declare namespace CP_EDIT_LIST {
  interface Spec {
    type: 'EditList';
    operations: Obj<CP_COMMON.Operation>;
    state: IState;
    props: IProps;
  }

  interface IProps {
    visible?: boolean;
    temp: Temp[];
  }

  interface IState {
    list: Obj[];
  }

  interface Temp {
    title: string;
    key: string;
    width?: number;
    flex?: number;
    render: IRender;
  }

  interface IRender {
    [pro: string]: any;
    type: 'input' | 'text' | 'select' | 'inputSelect';
    required?: boolean;
    uniqueValue?: boolean;
    defaultValue?: string;
  }

  type Props = MakeProps<Spec>;
}
