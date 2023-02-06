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

declare namespace CP_CONTAINER {
  interface Spec {
    type: 'Container' | 'LRContainer' | 'RowContainer';
    left?: Obj;
    right?: Obj;
    props?: IProps;
    contentSetting?: string;
  }

  interface IProps {
    visible?: boolean;
    direction?: 'column' | 'row'; // 对应flex-direction
    contentSetting?: 'between' | 'center' | 'start' | 'end'; // 对应justify-content
    isTopHead?: boolean;
    spaceSize?: 'none' | 'small' | 'middle' | 'big' | 'large' | 'huge';
    whiteBg?: boolean;
    border?: boolean;
    fullHeight?: boolean;
    flexHeight?: boolean;
    startAlign?: boolean;
    scrollAuto?: boolean;
    overflowHidden?: boolean;
    className?: string;
  }

  type Props = MakeProps<Spec> & {
    children?: any;
    props: {
      className: string;
      onClick?: () => void;
      leftProportion?: number;
      rightProportion?: number;
    };
  };
}
