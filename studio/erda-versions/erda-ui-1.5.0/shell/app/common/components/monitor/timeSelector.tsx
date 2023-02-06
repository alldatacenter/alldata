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
import TimeSelector from './components/timeSelector';
import TimeRangeSelector from './components/timeRangeSelector';
import monitorCommonStore from 'common/stores/monitorCommon';
import routeInfoStore from 'core/stores/route';
import { Moment } from 'moment';

interface IProps {
  rangeMode?: boolean;
  className?: string;
  defaultTime?: number | Moment[] | number[];
  inline?: boolean;
  disabledDate?: (arg: any) => boolean;
}
const TimeSelectorContainer = (props: IProps) => {
  const { changeTimeSpan: onChangeTime } = monitorCommonStore.reducers;
  const timeSpan = monitorCommonStore.useStore((s) => s.timeSpan);
  const query = routeInfoStore.useStore((s) => s.query);
  const { rangeMode = true, ...rest } = props;
  const fullProps = { ...rest, timeSpan, query, onChangeTime };
  return rangeMode ? <TimeRangeSelector {...fullProps} /> : <TimeSelector {...fullProps} />;
};
export default TimeSelectorContainer;
