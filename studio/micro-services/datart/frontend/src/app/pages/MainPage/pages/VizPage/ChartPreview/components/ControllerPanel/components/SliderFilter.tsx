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

import { Slider } from 'antd';
import { updateBy } from 'app/utils/mutation';
import { FC, memo, useState } from 'react';
import { PresentControllerFilterProps } from '.';

const SliderFilter: FC<PresentControllerFilterProps> = memo(
  ({ condition, options, onConditionChange }) => {
    const [valueRange, setValueRange] = useState(() => {
      if (Array.isArray(condition?.value)) {
        return condition?.value as [number, number];
      }
    });

    const handleValueChange = (values: [number, number]) => {
      const newCondition = updateBy(condition!, draft => {
        draft.value = values;
      });
      onConditionChange(newCondition);
      setValueRange(newCondition.value as [number, number]);
    };

    return (
      <Slider
        range
        value={valueRange}
        onChange={handleValueChange}
        min={options?.min}
        max={options?.max}
      />
    );
  },
);

export default SliderFilter;
