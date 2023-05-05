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

import { Input, InputNumber } from 'antd';
import i18n from 'i18n';
import { Icon as CustomIcon } from 'common';
import React from 'react';
import './time-input.scss';

interface ITextProps {
  [k: string]: any;
  value?: string;
  originalValue: string;
  displayName: string;
  disabled?: boolean;
  showErrTip?: boolean;
  passAndTrigger?: boolean;
  rule?: any;
  triggerChangeOnButton?: boolean;
  onChange?: (v: number | string) => void;
}

export const TextFieldInput = React.forwardRef(
  (
    {
      value,
      originalValue,
      displayName,
      onChange = () => {},
      showErrTip = false,
      passAndTrigger = false,
      triggerChangeOnButton = false,
      rule = {},
      ...rest
    }: ITextProps,
    ref,
  ) => {
    const [showTip, setShowTip] = React.useState(false);
    const [showBtn, setShowBtn] = React.useState(false);
    const clickBtn = React.useRef(false);
    const [_value, setValue] = React.useState(value);

    const checkReg = rule?.pattern;
    const checkMsg = rule?.message;

    React.useEffect(() => {
      setValue(value);
      const pass = value && value.length ? checkReg?.test(value) : true;
      showErrTip && setShowTip(!pass);
    }, [checkReg, showErrTip, value]);

    const triggerSave = (v: string | number, pass: boolean) => {
      showErrTip && setShowTip(!pass);
      if (passAndTrigger) {
        pass && onChange(v);
      } else {
        onChange(v);
      }
    };

    const onInputChange = (_v: string) => {
      const pass = _v && _v.length ? checkReg?.test(_v) : true;
      showErrTip && setShowTip(!pass);
      setValue(_v);
      if (!triggerChangeOnButton) {
        triggerSave(_v, pass);
      }
    };

    const onBlur = () => {
      const pass = _value !== '' ? checkReg?.test(_value) : true;
      if (!clickBtn.current) {
        triggerSave(_value, pass);
        setShowBtn(false);
      }
      clickBtn.current = false;
    };

    const onSave = () => {
      clickBtn.current = true;
      const pass = _value !== '' ? checkReg?.test(_value) : true;
      triggerSave(_value, pass);
      setShowBtn(false);
    };

    const onCancel = () => {
      clickBtn.current = true;
      setShowBtn(false);
      setValue(originalValue); // 原始值
    };

    return (
      <>
        <Input
          allowClear
          className={showTip ? 'with-error' : ''}
          placeholder={i18n.t('please enter {name}', { name: displayName })}
          onFocus={() => setShowBtn(true)}
          {...rest}
          ref={ref}
          value={_value}
          onChange={(e) => onInputChange(e.target.value)}
          onBlur={onBlur}
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
      </>
    );
  },
);

interface INumberProps {
  [k: string]: any;
  value?: number;
  onChange?: (v: number) => void;
}

export const NumberFieldInput = React.forwardRef(({ value, onChange = () => {}, ...rest }: INumberProps, ref) => {
  const [_value, setValue] = React.useState(value);
  React.useEffect(() => {
    setValue(value);
  }, [value]);
  const onInputChange = (_v: number) => {
    setValue(_v);
  };

  const onSave = (v: number) => {
    onChange(v);
  };

  return (
    <>
      <InputNumber
        {...rest}
        value={_value}
        ref={ref}
        onChange={(v) => onInputChange(v)}
        onBlur={(e) => onSave(e.target.value)}
      />
    </>
  );
});
