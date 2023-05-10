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

import React from 'react';
import EmptyHolder from '../empty-holder';
import EmptyListHolder from '../empty-list-holder';

interface IHolder {
  when: boolean | Function;
  page?: boolean;
  children: any;
  [propName: string]: any;
}
export const Holder = ({ page = false, when, children, ...rest }: IHolder) => {
  const showHolder = typeof when === 'function' ? when() : when;
  return showHolder || !children ? page ? <EmptyHolder {...rest} /> : <EmptyListHolder {...rest} /> : children;
};

export default Holder;
