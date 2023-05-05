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

declare namespace CP_BUTTON {
  interface Spec {
    type: 'Button';
    operations?: Obj<CP_COMMON.Operation>;
    props?: IProps;
  }

  interface IProps {
    text: string;
    type?: any;
    disabled?: boolean;
    disabledTip?: string;
    ghost?: boolean;
    menu?: MenuItem[];
    prefixIcon?: string;
    style?: Obj;
    suffixIcon?: string;
    tooltip?: string;
    tipProps?: Obj;
    visible?: boolean;
  }
  interface MenuItem {
    key: string;
    text: string;
    disabled?: boolean;
    disabledTip?: string;
    prefixIcon?: string;
    confirm?: string;
    operations: Obj<CP_COMMON.Operation>;
  }

  type Props = MakeProps<Spec>;
}
