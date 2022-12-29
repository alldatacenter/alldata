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

import {
  CrossFilteringSetting,
  DrillThroughSetting,
  ViewDetailSetting,
} from 'app/components/FormGenerator/Customize/Interaction/types';
import { IChartDrillOption } from 'app/types/ChartDrillOption';
import { createContext } from 'react';

const ChartDrillContext = createContext<{
  drillOption?: IChartDrillOption;
  availableSourceFunctions?: string[];
  crossFilteringSetting?: CrossFilteringSetting;
  viewDetailSetting?: ViewDetailSetting;
  drillThroughSetting?: DrillThroughSetting;
  onDrillOptionChange?: (option: IChartDrillOption) => void;
  onDateLevelChange?: (type: string, option: any) => void;
  onViewDataChange?: () => void;
  onCrossFilteringChange?: () => void;
  onDrillThroughChange?: (ruleId?: string) => void;
}>({});

export default ChartDrillContext;
