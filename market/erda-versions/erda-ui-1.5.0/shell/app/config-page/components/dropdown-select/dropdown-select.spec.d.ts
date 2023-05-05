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

declare namespace CP_DROPDOWN_SELECT {
  interface Spec {
    type: 'DropdownSelect';
    props: IProps;
    state: IState;
  }

  interface IProps {
    quickSelect?: IQuickSelect[];
    visible?: boolean;
    showLimit: number;
    overlay?: any;
    options?: IOptionItem[];
    trigger?: Array<'click' | 'hover' | 'contextMenu'>;
  }

  interface IState {
    value: string;
    label: string;
  }

  interface IOptionItem {
    label: string;
    value: string;
    operations: Obj<CP_COMMON.Operation>;
    prefixImgSrc?: string;
    prefixIcon?: string;
    suffixIcon?: string;
    disabled?: boolean;
  }

  interface IQuickSelect {
    label: string;
    value: string;
    operations?: Obj<CP_COMMON.Operation>;
  }

  type Props = MakeProps<Spec>;
}
