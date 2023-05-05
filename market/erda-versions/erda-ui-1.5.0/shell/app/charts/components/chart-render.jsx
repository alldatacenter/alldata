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
import { EmptyHolder } from 'common';
import Echarts from './echarts';
import { theme } from '../theme';

const ChartRender = ({ data, hasData, getOption, ...rest }) => {
  if (!hasData) {
    // loading = undefined 表示第一次加载
    return data && (data.loading === undefined || data.loading) ? (
      <Echarts key="chart" showLoading option={{}} notMerge {...rest} />
    ) : (
      <EmptyHolder />
    );
  }
  return <Echarts key="chart" showLoading={data.loading} option={getOption()} notMerge {...rest} />;
};

export default ChartRender;
