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

declare namespace CP_RADIO {
  interface Spec {
    type: 'Radio';
    props: IProps;
    state: IState;
    data: IData;
    operations?: Obj<CP_COMMON.Operation>;
  }

  interface IState {
    childrenValue?: Obj;
    value: string;
  }

  interface IData {
    options?: Option[];
  }

  interface IProps {
    buttonStyle?: 'solid' | 'outline';
    disabled?: boolean;
    disabledTip?: string;
    options?: Option[];
    radioType?: string;
    size?: 'small' | 'middle' | 'large';
    visible: boolean; // TODO: remove after support options: { visible }
  }

  interface Option {
    key: string;
    text: string;
    children?: IOptionChildren[]; // 作为下拉选项
    operations?: Obj<CP_COMMON.Operation>;
    prefixIcon?: string;
    suffixIcon?: string;
    tooltip?: string;
    width?: string;
  }

  interface IOptionChildren {
    key: string;
    text: string;
  }

  type Props = MakeProps<Spec>;
}
