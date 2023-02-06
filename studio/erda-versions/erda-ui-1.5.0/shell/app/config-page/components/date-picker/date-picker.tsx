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
import { DatePicker as PureDatePicker } from 'antd';
import moment, { Moment } from 'moment';
import { useUpdateEffect } from 'react-use';
import { map } from 'lodash';

const { RangePicker } = PureDatePicker;

const valueConvert = (val: number | number[] | Moment | Moment[]) => {
  const convertItem = (v: number | Moment) => {
    if (moment.isMoment(v)) {
      return moment(v).valueOf();
    } else {
      return v && moment(v);
    }
  };
  return Array.isArray(val) ? val.map((vItem) => convertItem(vItem)) : convertItem(val);
};

const DatePicker = (props: CP_DATE_PICKER.Props) => {
  const { cId, props: configProps, state, extraFooter, operations, execOperation, customOp } = props;
  const { visible = true, type, borderTime, ranges, ...rest } = configProps || {};
  const [value, setValue] = React.useState<Moment | Moment[]>(valueConvert(state?.value) as Moment | Moment[]);

  useUpdateEffect(() => {
    setValue(valueConvert(state?.value) as Moment | Moment[]);
  }, [state?.value]);

  React.useEffect(() => {
    customOp?.onChange?.(state);
  }, [state]);

  useUpdateEffect(() => {
    if (operations?.onChange) {
      const val =
        borderTime && Array.isArray(value)
          ? value.map((vItem, idx) => {
              if (idx === 0 && vItem) {
                return vItem.startOf('dates');
              } else if (idx === 1 && vItem) {
                return vItem.endOf('dates');
              }
              return vItem;
            })
          : value;
      execOperation(operations.onChange, { value: valueConvert(val) });
    }
  }, [value]);

  if (!visible) return null;

  const onChange = (val: Moment | Moment[]) => {
    setValue(val);
  };

  // const disabledDate = () => {};

  if (type === 'dateRange') {
    const rangeConvert = (_ranges?: Obj<number[]>) => {
      const reRanges = {};
      map(_ranges, (v, k) => {
        reRanges[k] = valueConvert(v);
      });
      return reRanges;
    };
    return (
      <RangePicker
        value={value}
        ranges={rangeConvert(ranges)}
        onChange={onChange}
        renderExtraFooter={() => extraFooter}
        {...rest}
      />
    );
  }

  return <PureDatePicker value={value} onChange={onChange} extraFooter={() => extraFooter} {...rest} />;
};

export default DatePicker;
