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

import { expect } from '@jest/globals';
import { IChart } from 'app/types/Chart';
import { isFunc } from 'utils/object';

const isChartModelImpl = (chart: IChart) => {
  return (
    Boolean(chart) &&
    Boolean(chart.config) &&
    Boolean(chart?.meta?.name) &&
    isFunc(chart.onMount) &&
    isFunc(chart.onUpdated) &&
    isFunc(chart.onResize) &&
    isFunc(chart.onUnMount) &&
    chart.isISOContainer !== undefined &&
    chart?.meta?.id !== undefined
  );
};

expect.extend({
  toBeDatartChartModel(chart) {
    if (isChartModelImpl(chart)) {
      return {
        message: () => `expected ${chart} to be Datart Chart Model`,
        pass: true,
      };
    } else {
      return {
        message: () => `expected ${chart} not to be Datart Chart Model`,
        pass: false,
      };
    }
  },
});
