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
import { InputNumber, Select } from 'antd';
import { isEqual } from 'lodash';
import i18n from 'i18n';
import './variable-input-group.scss';

const { Option } = Select;

interface IVariableInputGroupProps {
  value: any;
  placeholder?: string;
  onChange: (options: any[]) => void;
  disabled?: boolean;
}

const defaultValue = {
  replicas: 1,
};

export default class extends PureComponent<IVariableInputGroupProps, any> {
  state = {
    value: defaultValue,
  };

  static getDerivedStateFromProps(nextProps: any, prevState: any) {
    if (!isEqual(nextProps.value, prevState.value)) {
      return {
        value: nextProps.value || defaultValue,
      };
    }
    return null;
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
    const replicas = (
      <div>
        <span className="edit-service-label">{i18n.t('number of instance')}: </span>
        <span>
          <InputNumber
            disabled={disabled}
            className="w-full"
            value={value.replicas || 1}
            onChange={(e: any) => this.changeValue(e, 'replicas')}
            placeholder={i18n.t('dop:please enter the number of instance')}
          />
        </span>
      </div>
    );
    return (
      <div>
        <div>
          <span className="edit-service-label">{i18n.t('dop:deployment mode')}: </span>
          <span>
            <Select
              disabled={disabled}
              defaultValue={'replicated'}
              onChange={(e: any) => this.changeValue(e, 'mode')}
              placeholder={i18n.t('dop:please enter the network configuration')}
            >
              <Option value="replicated">replicated</Option>
            </Select>
          </span>
        </div>
        {replicas}
      </div>
    );
  }

  private changeValue = (v: any, key: string) => {
    const { value } = this.state;
    // @ts-ignore
    value[key] = v;
    const state = {
      value: { ...value },
    };
    this.setState(state);
    this.triggerChange(state.value);
  };
}
