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
import i18n from 'i18n';
import './statistics-selector.scss';

interface IProps {
  onChange: (arg: any) => void;
}

const { Option } = Select;

export const STATISTICS = {
  avg: { name: i18n.t('msp:average value'), value: 'avg' },
  max: { name: i18n.t('msp:peak'), value: 'max' },
};

const StatisticsSelector = (props: IProps) => {
  const { onChange } = props;
  const changeValue = (val: string) => {
    onChange(val);
  };
  return (
    <Select className="gi-statistics-selector" defaultValue={STATISTICS.avg.value} onChange={changeValue}>
      {map(STATISTICS, ({ name, value }) => (
        <Option value={value} key={value}>
          {name}
        </Option>
      ))}
    </Select>
  );
};

export default StatisticsSelector;
