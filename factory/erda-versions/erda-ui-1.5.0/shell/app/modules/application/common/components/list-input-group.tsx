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
import { Input, InputNumber } from 'antd';
import { cloneDeep, map } from 'lodash';
import { ErdaIcon } from 'common';
import './variable-input-group.scss';
import { uuid } from 'common/utils';

interface IVariableInputGroupProps {
  value: any;
  placeholder?: string;
  label: string;
  disabled?: boolean;
  required: boolean;
  isProperty?: boolean;
  type?: 'number' | 'string';
  onChange: (options: any[]) => void;
}

export default class extends PureComponent<IVariableInputGroupProps, any> {
  constructor(props: IVariableInputGroupProps) {
    super(props);
    const ids: string[] = [];
    const _values = map(props.value || [], (item) => {
      let id: string = uuid();
      while (ids.includes(id)) {
        id = uuid();
      }
      ids.push(id);

      return {
        value: item,
        id,
      };
    });
    this.state = {
      value: _values,
      ids,
    };
  }

  triggerChange = (changedValue: any) => {
    const { onChange } = this.props;
    if (onChange) {
      onChange(changedValue);
    }
    this.setState({
      value: changedValue,
    });
  };

  render() {
    const { placeholder, label, required, isProperty, disabled, type } = this.props;
    const { value } = this.state;

    const inputs = value.map((item: any, index: number) => {
      const { value: input, id } = item;
      let inputField = (
        <Input
          disabled={disabled}
          className="list-full-input"
          defaultValue={input}
          onChange={(e: any) => this.changeValue(e.target.value, index)}
          placeholder={placeholder}
        />
      );
      if (type === 'number') {
        inputField = (
          <InputNumber
            disabled={disabled}
            className="list-full-input"
            defaultValue={input}
            onChange={(e: any) => this.changeValue(e, index)}
            placeholder={placeholder}
          />
        );
      }
      return (
        <div key={id} className="list-full-input-group">
          {inputField}
          {disabled ? null : (
            <ErdaIcon type="delete1" className="align-middle variable-icon ml-3 cursor-pointer" onClick={() => this.onDelete(index)} />
          )}
        </div>
      );
    });
    return (
      <div>
        <span className={isProperty === true ? 'edit-service-label' : 'global-input-form-title'}>
          {required ? <span className="ant-form-item-required" /> : null}
          {label ? <span>{label}:</span> : null}
          {disabled ? null : (
            <ErdaIcon
              type="plus"
              className="align-middle edit-service-label-icon cursor-pointer"
              onClick={this.addPort}
            />
          )}
        </span>
        {inputs}
      </div>
    );
  }

  private changeValue = (v: any, index: number) => {
    const { value } = this.state;
    const reValue = cloneDeep(value);
    // @ts-ignore
    reValue[index].value = v;
    const state = {
      value: reValue,
    };

    this.triggerChange(state.value);
  };

  private addPort = () => {
    const { value, ids } = this.state;
    let reValue = cloneDeep(value);
    if (!reValue) {
      reValue = [];
    }

    let id = uuid();
    while (ids.includes(id)) {
      id = uuid();
    }

    // @ts-ignore
    reValue.push({ value: '', id });

    this.setState({
      value: reValue,
      ids: [...ids, id],
    });
  };

  private onDelete = (index: number) => {
    const { value, ids } = this.state;
    const reValue = cloneDeep(value);
    const reIds = cloneDeep(ids);
    reValue.splice(index, 1);
    reIds.splice(index, 1);

    this.setState({
      ids: reIds,
    });

    this.triggerChange(reValue);
  };
}
