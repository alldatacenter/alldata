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
import { remove, size } from 'lodash';
import { ChartRender } from 'charts';
import i18n from 'i18n';

interface IProps {
  data: IData;
}
interface IData {
  result: IResult[];
}
interface IResult {
  value: number;
  type: string;
}
const TestPieChart = ({ data }: IProps): JSX.Element => {
  const { result = [] } = data;
  remove(result, { value: 0 });
  const colorsMap = {
    failed: '#DF3409',
    error: '#e63871',
    skipped: '#FEAB00',
    passed: '#2DC083',
  };
  const useColor = result.map((item) => colorsMap[item.type]);
  const getOption = () => {
    const option = {
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)',
      },
      legend: {
        orient: 'vertical',
        left: '0',
        bottom: '0',
        data: ['failed', 'error', 'skipped', 'passed'],
      },
      calculable: true,
      series: [
        {
          name: i18n.t('status'),
          type: 'pie',
          radius: '60%',
          center: ['50%', '50%'],
          label: {
            normal: { formatter: '{b}:{c}' },
            emphasis: {
              show: true,
              textStyle: {
                fontSize: '16',
                fontWeight: 'bold',
              },
            },
            itemStyle: {
              emphasis: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)',
              },
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
export default TestPieChart;
