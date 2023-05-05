// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import { map } from 'lodash';
import React from 'react';
import { Select } from 'antd';
import { useEffectOnce } from 'react-use';
import i18n from 'i18n';
import { Moment } from 'moment';

import './timeSelector.scss';

interface IProps {
  defaultTime?: number | Moment[] | number[];
  timeSpan: ITimeSpan;
  inline?: boolean;
  className?: string;
  onChangeTime: (args: any) => void;
}

const timeSelectorPlan = {
  1: `1${i18n.t('common:changes in the hour')}`,
  3: `3${i18n.t('common:changes in the hour')}`,
  24: `1${i18n.t('common:changes in the day')}`,
  72: `3${i18n.t('common:changes in the day')}`,
  168: `7${i18n.t('common:changes in the day')}`,
};

const TimeSelector = (props: IProps) => {
  const { defaultTime, onChangeTime, timeSpan, className, inline } = props;
  const { hours } = timeSpan;
  const styleName = inline ? 'monitor-time-selector-inline' : 'monitor-time-selector';
  useEffectOnce(() => {
    if (defaultTime) {
      onChangeTime(defaultTime);
    } else {
      onChangeTime(timeSpan.hours);
    }
  });

  const handleChangeTime = (val: number) => {
    props.onChangeTime(val);
  };

  return (
    <div className={`${className} ${styleName}`}>
      <Select
        key="select"
        className="time-range-selector"
        defaultValue={timeSelectorPlan[defaultTime || hours]}
        onChange={handleChangeTime}
      >
        {map(timeSelectorPlan, (value, key) => {
          return (
            <Select.Option key={value} value={key}>
              {value}
            </Select.Option>
          );
        })}
      </Select>
    </div>
  );
};

export default TimeSelector;
