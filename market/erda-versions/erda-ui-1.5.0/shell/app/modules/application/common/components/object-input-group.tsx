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
import { Input, message } from 'antd';
import { cloneDeep, forEach, isEqual } from 'lodash';
import i18n from 'i18n';
import { ErdaIcon } from 'common';

interface IEditGlobalVariableProps {
  value: any;
  errorMessage?: string;
  required: boolean;
  disabled?: boolean;
  isProperty?: boolean;
  label?: string;
  onChange: (options: any) => void;
}

const convertGlobalVariableList = (props: any) => {
  const list: any[] = [];

  if (props.value instanceof Array) {
    forEach(props.value, (value: any) => {
      forEach(value, (v: any, k: string) => {
        list.push({
          key: k,
          value: v,
        });
      });
    });
  } else {
    forEach(props.value, (value: any, key: string) => {
      list.push({
        key,
        value,
      });
    });
  }

  return {
    props: props.value,
    globalVariableList: list,
  };
};

class ObjectInputGroup extends PureComponent<IEditGlobalVariableProps, any> {
  state = {
    globalVariableList: [],
    props: null,
  };

  static getDerivedStateFromProps(nextProps: any, prevState: any) {
    if (!isEqual(nextProps.value, prevState.props)) {
      return convertGlobalVariableList(nextProps);
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
    const { globalVariableList } = this.state;
    const { label, required, isProperty, disabled } = this.props;

    const content = globalVariableList.map((item: any, index: number) => {
      return (
        <div key={String(index)} className="variable-input-group">
          <Input
            disabled={disabled}
            className="variable-input"
            value={item.key}
            onChange={(e: any) => this.changeKey(e, index)}
            placeholder={`${i18n.t('please enter')}Key`}
          />
          <span className="variable-equal">=</span>
          <Input
            disabled={disabled}
            className="variable-input"
            value={item.value}
            onChange={(e: any) => this.changeValue(e, index)}
            placeholder={`${i18n.t('please enter')}Value`}
          />
          {disabled ? null : (
            <ErdaIcon
              type="delete1"
              className="variable-icon ml-3 cursor-pointer"
              onClick={() => this.deleteVariable(index)}
            />
          )}
        </div>
      );
    });

    return (
      <div>
        <div className={isProperty === true ? 'edit-service-label' : 'global-input-form-title'}>
          {required ? <span className="ant-form-item-required" /> : null}
          {label}：
          {disabled ? null : (
            <ErdaIcon
              type="plus"
              className="variable-icon cursor-pointer"
              onClick={this.addNewVariable}
            />
          )}
        </div>
        {content}
      </div>
    );
  }

  private changeKey = (e: any, index: number) => {
    const curVal = e.target.value;
    if (/^\d/.test(curVal)) {
      return message.error(i18n.t('can not start with number'));
    }
    const { value } = this.props;
    const { globalVariableList } = this.state;
    // @ts-ignore
    globalVariableList[index].key = e.target.value;

    let result: any = {};

    if (value instanceof Array) {
      result = [];
      globalVariableList.forEach((i: any) => {
        result.push({
          [i.key]: i.value,
        });
      });
    } else {
      globalVariableList.forEach((item: any) => {
        result[item.key] = item.value;
      });
    }

    this.triggerChange(result);
  };

  private changeValue = (e: any, index: number) => {
    const { value } = this.props;
    const { globalVariableList } = this.state;
    // @ts-ignore
    globalVariableList[index].value = e.target.value;

    let result: any = {};

    if (value instanceof Array) {
      result = [];
      globalVariableList.forEach((i: any) => {
        result.push({
          [i.key]: i.value,
        });
      });
    } else {
      globalVariableList.forEach((item: any) => {
        result[item.key] = item.value;
      });
    }

    this.triggerChange(result);
  };

  private deleteVariable = (index: number) => {
    const { value } = this.props;
    const { globalVariableList } = this.state;

    globalVariableList.splice(index, 1);

    let result: any = {};

    if (value instanceof Array) {
      result = [];
      globalVariableList.forEach((i: any) => {
        result.push({
          [i.key]: i.value,
        });
      });
    } else {
      globalVariableList.forEach((item: any) => {
        result[item.key] = item.value;
      });
    }

    this.triggerChange(result);
  };

  private addNewVariable = () => {
    const { globalVariableList } = this.state;

    // @ts-ignore
    globalVariableList.push({
      key: '',
      value: '',
    });
    this.setState({
      globalVariableList: cloneDeep(globalVariableList),
    });
  };
}

export default ObjectInputGroup;
