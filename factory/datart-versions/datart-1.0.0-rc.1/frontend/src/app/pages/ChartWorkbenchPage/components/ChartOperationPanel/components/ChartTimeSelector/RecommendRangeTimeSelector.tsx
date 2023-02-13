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

import { Radio, Space } from 'antd';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import ChartFilterCondition, {
  ConditionBuilder,
} from 'app/models/ChartFilterCondition';
import { FilterCondition } from 'app/types/ChartConfig';
import { recommendTimeRangeConverter } from 'app/utils/time';
import { RECOMMEND_TIME } from 'globalConstants';
import { FC, memo, useMemo, useState } from 'react';
import CurrentRangeTime from './CurrentRangeTime';

const RecommendRangeTimeSelector: FC<
  {
    condition?: FilterCondition;
    onConditionChange: (condition: ChartFilterCondition) => void;
  } & I18NComponentProps
> = memo(({ i18nPrefix, condition, onConditionChange }) => {
  const t = useI18NPrefix(i18nPrefix);
  const [recommend, setRecommend] = useState<string | undefined>(
    condition?.value as string,
  );

  const handleChange = recommendTime => {
    setRecommend(recommendTime);
    const filter = new ConditionBuilder(condition)
      .setValue(recommendTime)
      .asRecommendTime();
    onConditionChange?.(filter);
  };

  const rangeTimes = useMemo(() => {
    return recommendTimeRangeConverter(recommend);
  }, [recommend]);

  return (
    <>
      <Space direction="vertical">
        <CurrentRangeTime times={rangeTimes} />
        <Radio.Group
          value={recommend}
          onChange={e => handleChange(e.target?.value)}
        >
          <Space direction="vertical" key={'smallRange'}>
            {[
              RECOMMEND_TIME.TODAY,
              RECOMMEND_TIME.YESTERDAY,
              RECOMMEND_TIME.THIS_WEEK,
            ].map(time => (
              <Radio key={time} value={time}>
                {t(time)}
              </Radio>
            ))}
          </Space>
          <Space direction="vertical" key={'wideRange'}>
            {[
              RECOMMEND_TIME.LAST_7_DAYS,
              RECOMMEND_TIME.LAST_30_DAYS,
              RECOMMEND_TIME.LAST_90_DAYS,
              RECOMMEND_TIME.LAST_1_MONTH,
              RECOMMEND_TIME.LAST_1_YEAR,
            ].map(time => (
              <Radio key={time} value={time}>
                {t(time)}
              </Radio>
            ))}
          </Space>
        </Radio.Group>
      </Space>
    </>
  );
});

export default RecommendRangeTimeSelector;
