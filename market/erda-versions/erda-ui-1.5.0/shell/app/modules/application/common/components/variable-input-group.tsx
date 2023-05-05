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
import { Input } from 'antd';
import i18n from 'i18n';
import './variable-input-group.scss';
import { ErdaIcon } from 'common';

interface IVariableInputGroupProps {
  value: any;
  onChange: (options: any) => void;
  onDelete: (key: string) => void;
  // 是否显示锁那个icon
  lock: boolean;
  disabled?: boolean;
}

export default class extends PureComponent<IVariableInputGroupProps, any> {
  state = {
    key: '',
    value: '',
  };

  static getDerivedStateFromProps(nextProps: IVariableInputGroupProps, prevState: any): any {
    const { key, value } = nextProps.value || {
      key: '',
      value: '',
    };
    if (key !== prevState.key || value !== prevState.value) {
      return {
        key,
        value,
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
    const { onDelete, lock, disabled } = this.props;
    const { key, value } = this.state;
    return (
      <div className="variable-input-group">
        <Input
          disabled={disabled}
          className="variable-input"
          value={key}
          onChange={this.changeKey}
          placeholder={i18n.t('dop:please enter the key')}
        />
        <span className="variable-equal">=</span>
        <Input
          disabled={disabled}
          className="variable-input"
          value={value}
          onChange={this.changeValue}
          placeholder={i18n.t('dop:please input the value')}
        />
        {lock !== false ? <ErdaIcon type="lock" className="variable-icon variable-input-lock" /> : null}
        {disabled ? null : (
          <ErdaIcon
            type="delete1"
            className={`align-middle variable-icon cursor-pointer ${lock === false && 'ml-3'}`}
            onClick={() => onDelete(key)}
          />
        )}
      </div>
    );
  }

  private changeKey = (e: any) => {
    const state = {
      key: e.target.value,
      value: this.state.value,
    };
    this.setState(state);

    this.triggerChange(state);
  };

  private changeValue = (e: any) => {
    const state = {
      key: this.state.key,
      value: e.target.value,
    };
    this.setState(state);
    this.triggerChange(state);
  };
}
