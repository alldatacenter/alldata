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
import { Select } from 'antd';
import { map } from 'lodash';
import { ErdaIcon } from 'common';
import './scaleSelector.scss';

const { Option } = Select;

interface IProps {
  scale: number;
  onChange: (arg: number) => void;
}

export const ScaleSelector = ({ scale, onChange }: IProps) => {
  const [curScale, setCurScale] = React.useState(1);
  React.useEffect(() => {
    if (curScale !== scale) setCurScale(scale);
  }, [curScale, scale]);
  const scaleOpt = [
    { name: '20%', value: 0.2 },
    { name: '40%', value: 0.4 },
    { name: '60%', value: 0.6 },
    { name: '80%', value: 0.8 },
    { name: '100%', value: 1 },
    { name: '120%', value: 1.2 },
    { name: '140%', value: 1.4 },
    { name: '160%', value: 1.6 },
    { name: '180%', value: 1.8 },
  ];
  const changeScale = (val: number) => {
    const validVal = Math.max(Math.min(1.8, val), 0.2);
    setCurScale(validVal);
    onChange && onChange(validVal);
  };

  return (
    <div className="scale-selector">
      <ErdaIcon
        type="minus"
        size="16"
        className="mr-1 scale-minus scale-op"
        onClick={() => changeScale(Number((curScale - 0.2).toFixed(1)))}
      />
      <Select
        value={`${(curScale * 100).toFixed(0)}%`}
        onChange={changeScale}
        showArrow={false}
        dropdownMatchSelectWidth={false}
      >
        {map(scaleOpt, ({ name, value }) => (
          <Option key={value} value={value}>
            {name}
          </Option>
        ))}
      </Select>
      <ErdaIcon
        type="plus"
        size="16"
        className="scale-plus scale-op"
        onClick={() => changeScale(Number((curScale + 0.2).toFixed(1)))}
      />
    </div>
  );
};
