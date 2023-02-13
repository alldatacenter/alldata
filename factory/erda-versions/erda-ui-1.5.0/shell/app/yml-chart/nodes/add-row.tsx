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
import { Tooltip } from 'antd';
import i18n from 'i18n';
import './add-row.scss';

export interface IProps {
  data: Obj;
  onClickNode: (data: any, arg?: any) => void;
}

const noop = () => {};
export const AddRow = (props: IProps) => {
  const { data, onClickNode = noop } = props;

  const onClick = () => {
    onClickNode(data);
  };
  return (
    <div className="yml-chart-node add-line" onClick={onClick}>
      <Tooltip title={i18n.t('insert node')}>+</Tooltip>
    </div>
  );
};
