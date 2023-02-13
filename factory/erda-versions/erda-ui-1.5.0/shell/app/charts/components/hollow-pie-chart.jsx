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
import { remove, size, find, reduce, floor } from 'lodash';
import ChartRender from './chart-render';
import i18n from 'i18n';

const HollowPieChart = ({ data }) => {
  const { result = [] } = data;
  remove(result, { value: 0 });
  const colorsMap = {
    succeed: '#46CC93',
    // running: '#2D9EF1',
    failed: '#F56B6B',
    canceled: '#F4C254',
  };
  const useColor = result.map((item) => colorsMap[item.type]);
  const success = (find(result, { type: 'succeed' }) || { value: 0 }).value;
  const total = reduce(result, (sum, item) => sum + item.value, 0) || 1;

  const getOption = () => {
    const option = {
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)',
      },
      title: {
        text: `${i18n.t('charts:success rate')}: ${floor((success / total) * 100, 2)}%`,
        x: 'center',
        y: 'center',
        textStyle: {
          color: '#46CC93',
          fontSize: 14,
          fontWeight: 'bold',
        },
      },
      series: [
        {
          name: i18n.t('status'),
          type: 'pie',
          radius: ['50%', '70%'],
          hoverOffset: 2,
          label: {
            normal: {
              formatter: '{b}:{c}',
            },
            emphasis: {
              show: true,
              textStyle: {
                fontWeight: 'bold',
              },
            },
          },
          labelLine: {
            normal: {
              length: 5,
              length2: 5,
              smooth: true,
            },
          },
          data: result,
        },
      ],
      color: useColor,
    };
    return option;
  };
  return <ChartRender data={data} hasData={size(result) > 0} getOption={getOption} />;
};
export default HollowPieChart;
