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

import { Input, Tooltip } from 'antd';
import i18n from 'i18n';
import { Icon as CustomIcon } from 'common';
import React from 'react';
import './time-input.scss';

export const checkReg = /^(\d+w\s)?(\d+d\s)?(\d+h\s)?(\d+m\s)?$/;
export const checkMsg = i18n.t('dop:Format must be 2w 3d 4h 5m');
const hourMin = 60;
const dayMin = 8 * hourMin; // 1d = 8h
const weekMin = 5 * dayMin; // 1w = 5d
// 解析 1w 3d 3h 4m 字符串为分钟
export const transToNum = (v?: string | number) => {
  if (typeof v === 'undefined') {
    return 0;
  }
  if (!checkReg.test(`${v} `) || typeof v === 'number') {
    return v;
  }
  let all = 0;
  v.trim()
    .split(' ')
    .forEach((p: string) => {
      const number = p.slice(0, -1);
      const unit = p.slice(-1);
      const toMin = {
        w: (num: number) => num * weekMin,
        d: (num: number) => num * dayMin,
        h: (num: number) => num * hourMin,
        m: (num: number) => num,
      };
      all += toMin[unit](+number);
    });
  return all;
};

export const transToStr = (v?: string | number) => {
  const num = Number(v);
  if (isNaN(num) || typeof v === 'string') {
    return v;
  }
  const min = num % 60;
  const hour = Math.floor((num / hourMin) % 8); // 1d = 8h
  const day = Math.floor((num / dayMin) % 5); // 1w = 5d
  const week = Math.floor(num / weekMin);
  const weekText = week > 0 ? `${week}w` : '';
  const dayText = day > 0 ? `${day}d` : '';
  const hourText = hour > 0 ? `${hour}h` : '';
  const minText = min > 0 ? `${min}m` : '';
  return [weekText, dayText, hourText, minText].filter((a) => !!a).join(' ');
};

interface IProps {
  [k: string]: any;
  value?: string;
  originalValue: string;
  disabled?: boolean;
  showErrTip?: boolean;
  passAndTrigger?: boolean;
  triggerChangeOnButton?: boolean;
  tooltip?: React.ReactElement;
  onChange?: (v: number | string) => void;
}
export const TimeInput = React.forwardRef(
  (
    {
      value,
      originalValue,
      onChange = () => {},
      showErrTip = false,
      passAndTrigger = false,
      triggerChangeOnButton = false,
      tooltip,
      ...rest
    }: IProps,
    ref,
  ) => {
    const [showTip, setShowTip] = React.useState(false);
    const [showBtn, setShowBtn] = React.useState(false);
    const clickBtn = React.useRef(false);
    const [_value, setValue] = React.useState(transToNum(value));

    React.useEffect(() => {
      setValue(transToNum(value));
      const pass = value && value.length ? checkReg.test(`${value} `) : true;
      showErrTip && setShowTip(!pass);
    }, [showErrTip, value]);

    const triggerSave = (v: string | number, pass: boolean) => {
      showErrTip && setShowTip(!pass);
      if (passAndTrigger) {
        pass && onChange(transToNum(v));
      } else {
        onChange(transToNum(v));
      }
    };

    const onInputChange = (_v: string) => {
      const pass = _v && _v.length ? checkReg.test(`${_v} `) : true;
      showErrTip && setShowTip(!pass);
      setValue(_v);
      if (!triggerChangeOnButton) {
        triggerSave(_v, pass);
      }
    };

    const onBlur = () => {
      const pass = typeof _value !== 'number' && _value !== '' ? checkReg.test(`${_value} `) : true; // 为number时表示没有改动，不用校验
      if (!clickBtn.current) {
        triggerSave(_value, pass);
        setShowBtn(false);
      }
      clickBtn.current = false;
    };

    const onSave = () => {
      clickBtn.current = true;
      const pass = typeof _value !== 'number' && _value !== '' ? checkReg.test(`${_value} `) : true;
      triggerSave(_value, pass);
      setShowBtn(false);
    };

    const onCancel = () => {
      clickBtn.current = true;
      setShowBtn(false);
      setValue(originalValue); // 原始值
    };

    return (
      <Tooltip placement="topLeft" title={tooltip}>
        <Input
          allowClear
          className={showTip ? 'with-error' : ''}
          placeholder={i18n.t('dop:please input time')}
          onFocus={() => setShowBtn(true)}
          {...rest}
          ref={ref}
          value={transToStr(_value)}
          onChange={(e) => onInputChange(e.target.value)}
          onBlur={() => setTimeout(onBlur, 200)}
        />
        {showTip ? <span className="text-xs text-red">{checkMsg}</span> : null}
        {triggerChangeOnButton && showBtn ? (
          <div className="issue-part-save-group">
            <span className="issue-part-save" onClick={onSave}>
              <CustomIcon className="mr-0" type="duigou" />
            </span>
            <span className="issue-part-cancel" onClick={onCancel}>
              <CustomIcon className="mr-0" type="gb" />
            </span>
          </div>
        ) : null}
      </Tooltip>
    );
  },
);
