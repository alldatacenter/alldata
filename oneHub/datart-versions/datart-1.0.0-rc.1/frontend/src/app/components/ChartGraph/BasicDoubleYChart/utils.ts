/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { ChartDataSectionField } from 'app/types/ChartConfig';
import { IChartDataSet } from 'app/types/ChartDataSet';
import { getMinAndMaxNumber, round } from 'app/utils/chartHelper';
import { calcNiceExtent } from 'app/utils/internalChartHelper';
import { IntervalConfig } from './types';

export function getYAxisIntervalConfig(
  leftConfigs: ChartDataSectionField[],
  rightConfigs: ChartDataSectionField[],
  chartDataset: IChartDataSet<string>,
): IntervalConfig {
  const [lMin, lMax] = getMinAndMaxNumber(leftConfigs, chartDataset);
  const [rMin, rMax] = getMinAndMaxNumber(rightConfigs, chartDataset);

  const { extent: lExtent, interval: lInterval } = calcNiceExtent([lMin, lMax]);
  const { extent: rExtent, interval: rInterval } = calcNiceExtent([rMin, rMax]);
  if (!rInterval || !lInterval) {
    return {
      leftMin: undefined,
      leftMax: undefined,
      leftInterval: undefined,
      rightMin: undefined,
      rightMax: undefined,
      rightInterval: undefined,
    };
  }

  const intervalConfig: IntervalConfig = {
    leftMin: lExtent[0],
    leftMax: lExtent[1],
    leftInterval: lInterval,
    rightMin: rExtent[0],
    rightMax: rExtent[1],
    rightInterval: rInterval,
  };

  const lSub = [Math.floor(lMin / lInterval), Math.ceil(lMax / lInterval)];
  const rSub = [Math.floor(rMin / rInterval), Math.ceil(rMax / rInterval)];
  const minSub = Math.abs(lSub[0]) > Math.abs(rSub[0]) ? lSub[0] : rSub[0];
  const maxSub = Math.abs(lSub[1]) > Math.abs(rSub[1]) ? lSub[1] : rSub[1];
  intervalConfig.leftMax = round(maxSub * lInterval);
  intervalConfig.leftMin = round(minSub * lInterval);
  intervalConfig.rightMax = round(maxSub * rInterval);
  intervalConfig.rightMin = round(minSub * rInterval);
  return intervalConfig;
}
