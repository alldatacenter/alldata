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

declare namespace CP_DROPDOWN {
  interface Spec {
    type: 'Dropdown';
    operations?: Obj<CP_COMMON.Operation>;
    props: IProps;
  }

  interface IMenu {
    key: string;
    disabled?: boolean;
    danger?: boolean;
    title?: string;
    label: string;
  }

  interface IProps {
    visible?: boolean;
    text?: string;
    menuProps?: Obj;
    menus: IMenu[];

    arrow?: boolean;
    disabled?: boolean;
    destroyPopupOnHide?: boolean;
    overlayStyle?: Obj;
    placement?: 'bottomLeft' | 'bottomCenter' | 'bottomRight' | 'topLeft' | 'topCenter' | 'topRight';
    trigger?: 'click' | 'hover' | 'contextMenu' | Array<'click' | 'hover' | 'contextMenu'>;
  }

  type Props = MakeProps<Spec> & {
    children?: React.ReactElement;
  };
}
