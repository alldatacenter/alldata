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

declare namespace CP_ICON {
  interface Spec {
    type: 'Icon';
    props: IProps;
  }

  interface IProps {
    hoverActive?: boolean;
    iconType: string;
    visible?: boolean;
    width?: string; // with of svg, and it's more priority than size
    height?: string; // height of svg, and it's more priority than size
    spin?: boolean; // use infinite rotate animation like loading icon, the default value is false
    size?: string | number; // size of svg with default value of 1rem. Use width and height if width-to-height ratio is not 1
    fill?: string; // color of svg fill area, and it's more priority than color
    stroke?: string; // color of svg stroke, and it's more priority than color
    color?: string; // color of svg
    rtl?: boolean; // acoustic image, the default value is from left to right
  }

  type Props = MakeProps<Spec>;
}
