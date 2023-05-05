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

import React from 'react';
import { DatePicker } from 'antd';
import moment, { Moment } from 'moment';
import { useUpdate } from 'common/use-hooks';
import { getTimeRanges, getTimeSpan } from 'common/utils';
import i18n from 'i18n';
import { RangePickerProps } from 'core/common/interface';

const { RangePicker } = DatePicker;

interface IProps extends RangePickerProps {
  defaultTime?: number | Moment[] | number[];
  disabledDate?: (currentDate?: Moment) => boolean;
  onOk: (v: any) => void;
}

const CommonRangePicker = ({ defaultTime, disabledDate, onOk, ...rest }: IProps) => {
  const [{ value }, updater] = useUpdate({ value: undefined as any });

  React.useEffect(() => {
    const { startTimeMs, endTimeMs } = getTimeSpan(defaultTime);
    updater.value([moment(startTimeMs), moment(endTimeMs)]);
  }, [defaultTime, updater]);

  const defaultDisabledDate = (current: Moment | undefined) => {
    const endEdge = moment();
    const startEdge = moment().subtract(8, 'days');
    return !!current && (current > endEdge || current < startEdge);
  };

  return (
    <RangePicker
      showTime
      style={{ width: 370 }}
      format="YYYY-MM-DD HH:mm:ss"
      allowClear={false}
      placeholder={[i18n.t('common:start at'), i18n.t('common:end at')]}
      onChange={(v: any) => {
        updater.value(v);
        onOk(getTimeSpan(v));
      }}
      value={value as any}
      disabledDate={disabledDate || defaultDisabledDate}
      ranges={getTimeRanges()}
      {...rest}
    />
  );
};

export default CommonRangePicker;
