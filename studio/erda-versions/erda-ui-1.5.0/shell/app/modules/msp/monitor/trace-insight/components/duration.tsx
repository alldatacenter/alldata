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
import { Input, Select } from 'antd';
import i18n from 'i18n';

const timeUnit = [
  {
    label: 's',
    value: 's',
  },
  {
    label: 'ms',
    value: 'ms',
  },
];

interface IValue {
  timer: number | string;
  unit: 'ms' | 's';
}

interface ICompProps {
  defaultUnit?: IValue['unit'];
  value?: Partial<IValue>;

  onChange(data: IValue): void;
}

const InputWithUnit = ({ onChange, defaultUnit = 'ms', value }: ICompProps) => {
  const [unit, setUnit] = React.useState(defaultUnit);
  const [timer, setTimer] = React.useState<IValue['timer']>('');
  const triggerChange = (changedValue: Partial<IValue>) => {
    onChange?.({ timer, unit, ...value, ...changedValue });
  };
  const handleChangeUnit = (v: IValue['unit']) => {
    if (value && !(unit in value)) {
      setUnit(v);
    }
    triggerChange({ unit: v });
  };
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newTimer = !isNaN(+e.target.value) ? e.target.value : '';
    if (value && !(timer in value)) {
      setTimer(newTimer);
    }
    triggerChange({ timer: newTimer });
  };
  const addonAfter = (
    <Select
      size="small"
      onChange={handleChangeUnit}
      defaultValue={defaultUnit}
      value={value?.unit || unit}
      bordered={false}
      dropdownMatchSelectWidth={false}
    >
      {timeUnit.map((item) => (
        <Select.Option key={item.value} value={item.value}>
          {item.label}
        </Select.Option>
      ))}
    </Select>
  );

  return <Input size="small" value={value?.timer || timer} onChange={handleChange} addonAfter={addonAfter} />;
};

interface IProps {
  onChange?(data: IValue[]): void;

  value: IValue[];
}

export const transformDuration = (duration?: IValue) => {
  const proportion = {
    ms: 1000000,
    s: 1000000000,
  };
  if (duration?.timer) {
    const { timer, unit } = duration;
    return Number(timer) * proportion[unit];
  } else {
    return undefined;
  }
};

const Duration = ({ value, onChange }: IProps) => {
  const [timers, setTimes] = React.useState<IValue[]>([]);
  const handleChange = (data: IValue, index: number) => {
    const newTimer: IValue[] = value?.length ? [...value] : [...timers];
    newTimer[index] = data;
    if (!value?.[index]) {
      setTimes(newTimer);
    }
    onChange?.(newTimer);
  };
  return (
    <Input.Group compact className="trace-duration flex items-center w-64">
      <InputWithUnit
        value={value?.[0] || timers[0]}
        onChange={(data) => {
          handleChange(data, 0);
        }}
      />
      <span className="mx-1">~</span>
      <InputWithUnit
        value={value?.[1] || timers[1]}
        onChange={(data) => {
          handleChange(data, 1);
        }}
      />
    </Input.Group>
  );
};

export default Duration;
