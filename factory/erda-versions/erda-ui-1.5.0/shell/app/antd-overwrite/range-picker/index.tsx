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
import { RangePickerProps } from 'antd/es/date-picker';

const { RangePicker: AntdRangePicker } = DatePicker;

type IProps = RangePickerProps & {
  borderTime?: boolean; //
};

/**
 * @param {boolean} [props.borderTime] - set time to [00:00:00, 23:59:59]
 */
const RangePicker = (props: IProps) => {
  const { borderTime, onChange, value, ...rest } = props;
  const [_value, setValue] = React.useState(value);

  React.useEffect(() => {
    setValue(value);
  }, [value]);

  if (!borderTime) {
    return <AntdRangePicker {...props} />;
  }

  const handleChange = (dates: any, dateStrings: [string, string]) => {
    const moments = dates;
    if (dates?.length) {
      moments[0] = dates[0]?.startOf('date');
      moments[1] = dates[1]?.endOf('date');
    }

    setValue(moments);
    onChange && onChange(moments, dateStrings);
  };

  return <AntdRangePicker {...rest} value={_value} onChange={handleChange} />;
};

export default RangePicker;
