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

declare namespace CP_POPOVER {
  interface IProps {
    size: 's' | 'm' | 'l';
    visible?: boolean;
    title?: string;
    placement?:
      | 'left'
      | 'right'
      | 'top'
      | 'bottom'
      | 'topLeft'
      | 'topRight'
      | 'bottomLeft'
      | 'bottomRight'
      | 'leftTop'
      | 'leftBottom'
      | 'rightTop'
      | 'rightBottom';
  }

  interface Spec {
    type: 'Popover';
    props: IProps;
  }

  type Props = MakeProps<Spec> & {
    children?: React.ReactNode | string;
    content?: React.ReactNode;
  };
}
