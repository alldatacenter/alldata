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

import { isEqual } from 'lodash';
import React from 'react';
import { DatePicker } from 'antd';
import { getTimeSpan, getTimeRanges } from 'common/utils';
import moment, { Moment } from 'moment';
import i18n from 'i18n';

import './timeSelector.scss';

interface IProps {
  timeSpan: ITimeSpan;
  defaultTime?: number | Moment[] | number[];
  inline?: boolean;
  query?: any;
  className?: string;
  onChangeTime: (args: any) => void;
  disabledDate?: (arg: any) => boolean;
}
interface IState {
  value: [Moment, Moment];
  prevProps: IProps;
}

const { RangePicker } = DatePicker;

class TimeSelector extends React.Component<IProps, IState> {
  private timeLimit: number;

  constructor(props: IProps) {
    super(props);
    const { timeSpan, defaultTime, query } = props;
    let initTime = defaultTime;
    let { startTimeMs, endTimeMs } = timeSpan;
    if (query && query.start && query.end) {
      initTime = [+query.start, +query.end];
    }

    this.timeLimit = 7; // 7天内时间
    if (initTime) {
      const times = getTimeSpan(initTime);
      startTimeMs = times.startTimeMs;
      endTimeMs = times.endTimeMs as number;
      this.props.onChangeTime(initTime);
    }
    this.state = {
      value: [moment(startTimeMs), moment(endTimeMs)],
      prevProps: props,
    };
  }

  static getDerivedStateFromProps(nextProps: IProps, prevState: IState) {
    if (!isEqual(nextProps.timeSpan, prevState.prevProps.timeSpan)) {
      const { startTimeMs, endTimeMs } = nextProps.timeSpan;
      return { prevProps: nextProps, value: [moment(startTimeMs), moment(endTimeMs)] };
    }
    return null;
  }

  onChangeTime = (value: any) => {
    // After updating to Antd4.x, click OK to trigger the onChange
    this.setState({ value }, () => {
      this.props.onChangeTime(value);
    });
  };

  onOpenChange = (status: boolean) => {
    if (!status) {
      const { startTimeMs, endTimeMs } = this.props.timeSpan;
      this.setState({
        value: [moment(startTimeMs), moment(endTimeMs)],
      });
    }
  };

  disabledDate = (current: Moment | undefined) => {
    // 不可选时间，7天内
    const { disabledDate } = this.props;
    if (disabledDate) {
      return disabledDate(current);
    }
    const endEdge = moment();
    const startEdge = moment().subtract(this.timeLimit + 1, 'days');
    return !!current && (current > endEdge || current < startEdge);
  };

  render() {
    const { inline, className } = this.props;
    const styleName = inline ? 'monitor-time-selector-inline' : 'monitor-time-selector';
    const { value } = this.state;
    return (
      <div className={`${className} ${styleName}`}>
        <RangePicker
          showTime
          style={{ width: 370 }}
          format="YYYY-MM-DD HH:mm:ss"
          allowClear={false}
          placeholder={[i18n.t('common:start at'), i18n.t('common:end at')]}
          onChange={this.onChangeTime}
          value={value}
          disabledDate={this.disabledDate}
          onOpenChange={this.onOpenChange}
          ranges={getTimeRanges()}
        />
      </div>
    );
  }
}

export default TimeSelector;
