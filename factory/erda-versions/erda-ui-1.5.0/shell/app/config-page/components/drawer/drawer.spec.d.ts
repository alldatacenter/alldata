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

declare namespace CP_DRAWER {
  interface Spec {
    type: 'Drawer';
    operations?: Obj<CP_COMMON.Operation>;
    state: IState;
    props: IProps;
  }

  interface IState {
    visible: boolean;
  }

  interface IProps {
    title?: string;
    closable?: boolean;
    maskClosable?: boolean;
    placement?: 'top' | 'right' | 'bottom' | 'left';
    size?: 's' | 'm' | 'l' | 'xl'; // s:256, m: 560, l: 800, xl: 1100
  }

  type Props = MakeProps<Spec> & {
    content: Obj;
    footer?: React.ReactNode;
  };
}
