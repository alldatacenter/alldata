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

import { DatePicker } from 'antd';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import { TimeFilterConditionValue } from 'app/types/ChartConfig';
import { formatTime } from 'app/utils/time';
import { TIME_FORMATTER } from 'globalConstants';
import moment from 'moment';
import { FC, memo } from 'react';

const ExactTimeSelector: FC<
  {
    time?: TimeFilterConditionValue;
    onChange: (time) => void;
  } & I18NComponentProps
> = memo(({ time, i18nPrefix, onChange }) => {
  const t = useI18NPrefix(i18nPrefix);

  const handleMomentTimeChange = momentTime => {
    const timeStr = formatTime(momentTime, TIME_FORMATTER);
    onChange?.(timeStr);
  };

  return (
    <DatePicker
      showTime
      value={moment(time as string)}
      onChange={handleMomentTimeChange}
      placeholder={t('select')}
    />
  );
});

export default ExactTimeSelector;
