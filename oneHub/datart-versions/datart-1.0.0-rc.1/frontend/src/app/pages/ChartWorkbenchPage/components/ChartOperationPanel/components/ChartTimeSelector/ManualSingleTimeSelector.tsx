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

import { Select, Space } from 'antd';
import { TimeFilterValueCategory } from 'app/constants';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import { TimeFilterConditionValue } from 'app/types/ChartConfig';
import { formatTime } from 'app/utils/time';
import { TIME_FORMATTER } from 'globalConstants';
import moment from 'moment';
import { FC, memo, useState } from 'react';
import styled from 'styled-components/macro';
import ExactTimeSelector from './ExactTimeSelector';
import RelativeTimeSelector from './RelativeTimeSelector';

const ManualSingleTimeSelector: FC<
  {
    time?: TimeFilterConditionValue;
    isStart: boolean;
    onTimeChange: (time) => void;
  } & I18NComponentProps
> = memo(({ time, isStart, i18nPrefix, onTimeChange }) => {
  const t = useI18NPrefix(i18nPrefix);
  const [type, setType] = useState(() => {
    return typeof time === 'string'
      ? TimeFilterValueCategory.Exact
      : TimeFilterValueCategory.Relative;
  });

  const handleTimeCategoryChange = type => {
    setType(type);
    if (type === TimeFilterValueCategory.Exact) {
      onTimeChange?.(formatTime(moment(), TIME_FORMATTER));
    } else if (type === TimeFilterValueCategory.Relative) {
      onTimeChange?.({ unit: 'd', amount: 1, direction: '-' });
    } else {
      onTimeChange?.(null);
    }
  };

  const renderTimeSelector = type => {
    switch (type) {
      case TimeFilterValueCategory.Exact:
        return (
          <ExactTimeSelector
            time={time}
            i18nPrefix={i18nPrefix}
            onChange={onTimeChange}
          />
        );
      case TimeFilterValueCategory.Relative:
        return (
          <RelativeTimeSelector
            time={time}
            i18nPrefix={i18nPrefix}
            onChange={onTimeChange}
          />
        );
    }
  };

  return (
    <StyledManualSingleTimeSelector>
      <Select value={type} onChange={handleTimeCategoryChange}>
        <Select.Option value={TimeFilterValueCategory.Exact}>
          {t(TimeFilterValueCategory.Exact)}
        </Select.Option>
        <Select.Option value={TimeFilterValueCategory.Relative}>
          {t(TimeFilterValueCategory.Relative)}
        </Select.Option>
      </Select>
      {isStart ? `${t('startTime')} : ` : `${t('endTime')} : `}
      {renderTimeSelector(type)}
    </StyledManualSingleTimeSelector>
  );
});

export default ManualSingleTimeSelector;

const StyledManualSingleTimeSelector = styled(Space)`
  & > .ant-select {
    width: 80px;
  }
`;
