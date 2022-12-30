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

import { DrillMode } from 'app/models/ChartDrillOption';
import { ChartDataSectionField, FilterCondition } from 'app/types/ChartConfig';
export interface IChartDrillOption {
  mode: DrillMode;
  isSelectedDrill: boolean;
  isBottomLevel: boolean;
  isDrillable: boolean;
  canSelect: boolean;
  toggleSelectedDrill(enable?: boolean): void;
  getAllFields(): ChartDataSectionField[];
  getDrilledFields(): ChartDataSectionField[];
  getAllDrillDownFields(): Array<{
    field: ChartDataSectionField;
    condition?: FilterCondition;
  }>;
  getCurrentFields(): ChartDataSectionField[] | undefined;
  getCurrentDrillLevel(): number;
  drillDown(filterData?: { [key in string]: any }): void;
  expandDown(): void;
  drillUp(field?: ChartDataSectionField): void;
  expandUp(field?: ChartDataSectionField): void;
  rollUp(): void;
  clearAll();
}
