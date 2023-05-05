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

declare namespace CP_IMAGE {
  interface Spec {
    type: 'Image';
    props?: IProps;
  }

  interface IProps {
    alt?: string;
    src?: string;
    isCircle?: boolean;
    size?: 'small' | 'normal' | 'big' | 'large';
    visible?: boolean;
    type?: string;
    display?: 'inline-block' | 'block';
  }

  type Props = MakeProps<Spec>;
}
