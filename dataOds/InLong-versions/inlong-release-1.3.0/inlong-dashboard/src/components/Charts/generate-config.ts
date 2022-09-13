/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import { getStrByteLen } from '@/utils';

type ObjectType = Record<string, any>;

export const mergeLegend = (option: ObjectType) => {
  if (!option.legend) return option.legend;

  return {
    padding: 0,
    bottom: 0,
    ...option.legend,
  };
};

export const mergeTitle = (option: ObjectType) => {
  if (!option.title) return option.title;
  const merge = (titleObjConf: ObjectType) => ({
    ...titleObjConf,
    textStyle: {
      fontSize: 14,
      ...titleObjConf.textStyle,
    },
  });

  return Array.isArray(option.title) ? option.title.map(merge) : merge(option.title);
};

export const mergeGrid = (option: ObjectType) => {
  const hasYAxisName = Array.isArray(option.yAxis)
    ? option.yAxis.some(item => item.name)
    : option.yAxis && option.yAxis.name;

  // legend position
  const hasLegend = !!option.legend;
  const legendPos = hasLegend && option.legend.top === 0 ? 'top' : 'bottom';

  const hasTitle = Array.isArray(option.title) ? true : option.title && option.title.show !== false;

  // get left and right space
  let left = 0;
  let right = 0;
  if (hasYAxisName) {
    if (Array.isArray(option.yAxis)) {
      const byteLenL = getStrByteLen((option.yAxis[0] && option.yAxis[0].name) || '');
      const byteLenR = getStrByteLen((option.yAxis[1] && option.yAxis[1].name) || '');
      // 1 byte = 6px
      left += (byteLenL / 2) * 6;
      right += (byteLenR / 2) * 6;
    } else {
      const byteLenL = getStrByteLen(option.yAxis.name || '');
      left += (byteLenL / 2) * 6;
    }
  }

  const grid = {
    containLabel: true,
    left: left || 5,
    right: right || 5,
    top: hasYAxisName || (hasLegend && legendPos === 'top') ? 30 : 15,
    bottom: hasLegend && legendPos === 'bottom' ? 30 : 0,
    ...option.grid,
  };
  if (hasTitle) delete grid.top;

  return grid;
};

export const mergeSeries = (option: ObjectType) => {
  if (!option.series) return option.series;

  const conf = {
    line: {
      showSymbol: false,
      // smooth: true,
    },
    bar: {
      barGap: 0,
      barMaxWidth: 20,
    },
  };

  const merge = (seriesObjConf: ObjectType) => {
    const deafultConf = conf[seriesObjConf.type as keyof typeof conf];
    if (deafultConf) {
      return {
        ...seriesObjConf,
        ...deafultConf,
      };
    }
    return seriesObjConf;
  };

  return Array.isArray(option.series) ? option.series.map(merge) : merge(option.series);
};

// merge all
// return { legend, grid }
export const merge = (option: ObjectType) => {
  const legend = mergeLegend(option);
  const title = mergeTitle(option);
  const grid = mergeGrid({ ...option, legend });
  const series = mergeSeries(option);

  return {
    ...option,
    title,
    legend,
    grid,
    series,
  };
};
