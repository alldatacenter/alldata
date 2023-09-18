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

import TimeSelector from 'app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartTimeSelector';
import { FC, memo } from 'react';
import { PresentControllerFilterProps } from '.';

const RecommendTimeFilter: FC<PresentControllerFilterProps> = memo(
  ({ condition, onConditionChange }) => {
    const i18NPrefix = 'viz.common.filter.date';
    return (
      <TimeSelector.RecommendRangeTimeSelector
        i18nPrefix={i18NPrefix}
        condition={condition}
        onConditionChange={onConditionChange}
      />
    );
  },
);

export default RecommendTimeFilter;
