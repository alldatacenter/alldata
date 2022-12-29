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
import { ChartDataSectionField } from 'app/types/ChartConfig';
import { updateBy } from 'app/utils/mutation';
import { FC, useState } from 'react';

const SizeAction: FC<{
  config: ChartDataSectionField;
  onConfigChange: (
    config: ChartDataSectionField,
    needRefresh?: boolean,
  ) => void;
}> = ({ config, onConfigChange }) => {
  const actionNeedNewRequest = true;
  const [size, setSize] = useState(config?.size);

  const handleChange = selectedValue => {
    const newConfig = updateBy(config, draft => {
      draft.size = selectedValue;
    });
    setSize(selectedValue);
    onConfigChange?.(newConfig, actionNeedNewRequest);
  };

  return (
    <Slider
      dots
      tooltipVisible
      value={size}
      onChange={handleChange}
      min={1}
      max={10}
      step={1}
    />
  );
};

export default SizeAction;
