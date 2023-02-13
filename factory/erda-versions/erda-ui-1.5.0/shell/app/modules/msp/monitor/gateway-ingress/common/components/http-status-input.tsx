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
import { InputNumber } from 'antd';
import { isNumber } from 'lodash';
import i18n from 'i18n';
import './http-status-input.scss';

interface IProps {
  onChange: (val: any) => void;
}

const HttpStatusInput = (props: IProps) => {
  const { onChange } = props;
  const changeVal = (val: any) => {
    if (isNumber(val) && val >= 100 && val <= 600) {
      onChange(val);
    }
  };
  return (
    <InputNumber
      className="gi-http-status-input"
      placeholder={i18n.t('msp:please enter http status code ')}
      max={600}
      min={100}
      onChange={changeVal}
    />
  );
};

export default HttpStatusInput;
