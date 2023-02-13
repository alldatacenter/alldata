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

import React, { PureComponent } from 'react';
import { InputNumber } from 'antd';
import i18n from 'i18n';
import './variable-input-group.scss';

interface IVariableInputGroupProps {
  value: any;
  placeholder?: string;
  onChange: (options: any[]) => void;
  disabled: boolean;
}

const defaultValue = {
  mem: 0,
  cpu: 0,
  disk: 0,
  network: 'overlay',
};

export default class extends PureComponent<IVariableInputGroupProps, any> {
  state = {
    value: defaultValue,
  };

  static getDerivedStateFromProps(nextProps: any) {
    return {
      value: nextProps.value || defaultValue,
    };
  }

  triggerChange = (changedValue: any) => {
    const { onChange } = this.props;
    if (onChange) {
      onChange(changedValue);
    }
  };

  render() {
    const { disabled } = this.props;
    const { value } = this.state;
    return (
      <div>
        <div>
          <span className="edit-service-label">cpu {i18n.t('dop:cores')}: </span>
          <span>
            <InputNumber
              disabled={disabled}
              className="w-full"
              value={value.cpu || 0}
              onChange={(e: any) => this.changeValue(e, 'cpu')}
            />
          </span>
        </div>
        <div>
          <span className="edit-service-label">{i18n.t('dop:memory size')}(M): </span>
          <span>
            <InputNumber
              disabled={disabled}
              className="w-full"
              value={value.mem || 0}
              onChange={(e: any) => this.changeValue(e, 'mem')}
            />
          </span>
        </div>
        <div>
          <span className="edit-service-label">{i18n.t('dop:disk size')}(M): </span>
          <span>
            <InputNumber
              disabled={disabled}
              className="w-full"
              value={value.disk || 0}
              onChange={(e: any) => this.changeValue(e, 'disk')}
            />
          </span>
        </div>
        {/** 2021.3  前后端的network不一致，经确认，是个无效配置（后端暂未支持），先去除 */}
        {/* <div>
          <span className="edit-service-label">{i18n.t('dop:network configuration')}: </span>
          <span>
            <Select
              disabled={disabled}
              value={value.network || 'overlay'}
              onChange={(e: any) => this.changeValue(e, 'network')}
            >
              <Option value="overlay">overlay</Option>
              <Option value="host">host</Option>
            </Select>
          </span>
        </div> */}
      </div>
    );
  }

  private changeValue = (v: any, key: string) => {
    const { value } = this.state;
    value[key] = v;
    // @ts-ignore
    if (key !== 'network' && v === '') {
      value[key] = 0;
    }
    const state = {
      value: { ...value },
    };
    this.triggerChange(state.value);
  };
}
