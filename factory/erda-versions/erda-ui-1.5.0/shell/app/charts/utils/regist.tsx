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
import { registDataConvertor, registChartOptionFn, registControl } from '@erda-ui/dashboard-configurator';
import { getLineOption } from './line-option';
import { monitorDataConvertor } from './convertor';
import { TimeSelector } from 'common';
import i18n from 'i18n';

const { Option } = Select;

registDataConvertor('monitor', monitorDataConvertor);
registChartOptionFn('monitorLine', getLineOption);

export const registChartControl = () => {
  registControl('groupSelect', ({ selectedGroup, groups, handleChange, title, width }: any) => {
    return (
      <div className="chart-selector">
        <span>{title}</span>
        <Select
          className="my-3"
          value={selectedGroup || groups[0]}
          style={{ width: width || 200 }}
          onChange={handleChange}
        >
          {map(groups, (item) => (
            <Option value={item} key={item}>
              {item}
            </Option>
          ))}
        </Select>
      </div>
    );
  });

  registControl('timeRangeSelect', () => {
    return (
      <div className="chart-time-selector">
        <span>{i18n.t('charts:select time range')}：</span>
        <TimeSelector inline defaultTime={24} />
      </div>
    );
  });
};
