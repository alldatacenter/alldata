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
import { ChartDataViewMeta } from 'app/types/ChartDataViewMeta';
import { FC } from 'react';
import DateLevelMenuItems from './DateLevelMenuItems';

const DateLevelAction: FC<{
  config: ChartDataSectionField;
  availableSourceFunctions?: string[];
  metas?: ChartDataViewMeta[];
  onConfigChange: (
    config: ChartDataSectionField,
    needRefresh?: boolean,
    replacedConfig?: ChartDataSectionField,
  ) => void;
  mode?: 'menu';
}> = ({ config, metas, onConfigChange, availableSourceFunctions }) => {
  const actionNeedNewRequest = true;
  const onChange = newConfig => {
    onConfigChange?.(newConfig, actionNeedNewRequest, config);
  };

  return (
    <DateLevelMenuItems
      availableSourceFunctions={availableSourceFunctions}
      config={config}
      metas={metas}
      onChange={onChange}
    />
  );
};

export default DateLevelAction;
