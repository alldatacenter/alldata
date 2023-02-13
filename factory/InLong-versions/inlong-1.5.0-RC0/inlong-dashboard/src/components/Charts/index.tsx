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

import React, { useState, useEffect, useRef } from 'react';
import * as echarts from 'echarts/core';
import throttle from 'lodash/throttle';
import { CanvasRenderer } from 'echarts/renderers';
import { BarChart, LineChart } from 'echarts/charts';
import {
  TooltipComponent,
  TitleComponent,
  GridComponent,
  LegendComponent,
} from 'echarts/components';
import { merge } from './generate-config';

echarts.use([
  BarChart,
  LineChart,
  TooltipComponent,
  TitleComponent,
  GridComponent,
  LegendComponent,
  CanvasRenderer,
]);

export interface Props {
  option: any;
  height?: number;
  isEmpty?: boolean;
  forceUpdate?: boolean;
}

const Charts: React.FC<Props> = ({
  option,
  isEmpty = false,
  height = 300,
  forceUpdate = false,
}) => {
  const [chartIns, setChartIns] = useState<any>();
  const domRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const { current: dom } = domRef;
    if (!dom) return undefined;

    // mount
    let chart = null as any;
    const resize = throttle(() => {
      if (chart) chart.resize();
    }, 1000);
    chart = echarts.init(dom);
    if (Object.keys(option).length) {
      // init option
      chart.setOption(merge(option));
    }
    setChartIns(chart);
    window.addEventListener('resize', resize);

    return () => {
      // unmount
      if (chart) {
        window.removeEventListener('resize', resize);
        chart.dispose();
      }
    };
    // eslint-disable-next-line
  }, []);

  useEffect(() => {
    if (!chartIns) return;
    if (isEmpty) {
      chartIns.clear();
    } else if (Object.keys(option).length) {
      // update
      forceUpdate && chartIns.clear();
      chartIns.setOption(merge(option));
    }
  }, [chartIns, option, isEmpty, forceUpdate]);

  return <div ref={domRef} style={{ height: `${height}px` }} />;
};

export default Charts;
