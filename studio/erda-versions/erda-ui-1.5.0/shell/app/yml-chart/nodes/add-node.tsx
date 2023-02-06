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
import { Icon as CustomIcon } from 'common';
import i18n from 'i18n';
import './add-node.scss';

export interface IProps {
  data: Obj;
  onClickNode: (data: any, arg?: any) => void;
}
const noop = () => {};
export const AddNode = (props: IProps) => {
  const { data, onClickNode = noop } = props;

  const onClick = () => {
    onClickNode(data);
  };
  return (
    <div className="yml-chart-node add-node" onClick={onClick}>
      <CustomIcon type="tj1" className="add-icon mb-3" />
      <span className="add-node-txt">{i18n.t('add {name}', { name: i18n.t('node') })}</span>
    </div>
  );
};
