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

import { ChartDetailConfigDTO } from 'app/types/ChartConfigDTO';
import { pipe } from 'utils/object';
import { alpha3 } from './alpha3';

/**
 * @summary A function used for auto merge/fix chart config data by different version
 * @description
 *  transforms:
 *      0. alpha3 - 2021.11.18, issue #228
 *      1. .....
 * @param {ChartDetailConfigDTO} config, which is going to merge
 * @returns {ChartDetailConfigDTO} merged results and mark the version to latest
 *  */
export function migrateChartConfig(
  config?: ChartDetailConfigDTO,
): ChartDetailConfigDTO | undefined {
  if (!config) {
    return config;
  }
  return pipe(alpha3)(config);
}
