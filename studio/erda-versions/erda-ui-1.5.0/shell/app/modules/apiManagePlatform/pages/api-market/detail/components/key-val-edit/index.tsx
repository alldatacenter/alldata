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

import React, { useState, useEffect, Fragment, memo } from 'react';
import { isEmpty, find, cloneDeep } from 'lodash';
import { Input } from 'antd';
import { Icon as CustomIcon } from 'common';
import './index.scss';
import i18n from 'i18n';

interface IKeyValProps {
  data: Array<{
    [key: string]: any;
    required: boolean;
    error?: boolean;
  }>;
  type: string;
  dataModel?: object;
  itemMap: any[];
  opList?: any[];
  onChange: (...args: any) => any;
}

const KeyValueEdit = (props: IKeyValProps) => {
  const { data, type, dataModel, itemMap, onChange } = props;
  const [values, setValues] = useState(data || []);
  useEffect(() => {
    let newVal: any = [];
    if (isEmpty(data)) {
      newVal = dataModel ? [{ ...dataModel }] : [];
    } else if (find(data, dataModel)) {
      newVal = data;
    } else {
      newVal = dataModel ? [...data, { ...dataModel }] : data;
    }
    setValues(newVal);
  }, [data, dataModel]);
  const updateValue = (index: number, key: string, value: string, autoSave = false) => {
    const newValues = cloneDeep(values);
    const { required } = newValues[index];
    newValues[index][key] = value.trim();
    if (required && value.trim() === '') {
      newValues[index].error = true;
    } else {
      newValues[index].error = false;
    }
    setValues(newValues);
    onChange(
      type,
      newValues.filter((item: any) => item.key),
      autoSave,
      () => {},
    );
  };
  if (values.length === 0) {
    return (
      <div className="key-value-edit">
        <p className="no-params">{i18n.t('the current request has no {name}', { name: type })}</p>
      </div>
    );
  }
  const handleDelete = (index: number) => {
    const newValues = cloneDeep(values).filter((_item: any, i) => i !== index);
    setValues(newValues);
    onChange(
      type,
      newValues.filter((item: any) => item.key),
      false,
      () => {},
    );
  };
  return (
    <div className="key-value-edit">
      {values.map((item, index) => {
        return (
          <div className="key-value-edit-item" key={String(index)}>
            {(itemMap || []).map((t, i) => {
              const { type: compType, props: compProps, render, getProps } = t;
              const comprops = {
                ...compProps,
                value: item[compType],
                onChange: (val: string, autoSave: boolean) => {
                  updateValue(index, compType, val, autoSave);
                },
              };
              const { className = '', ...extraProps } = getProps ? getProps(item) : {};
              return (
                <Fragment key={String(i)}>
                  {render ? (
                    render(comprops)
                  ) : (
                    <Input
                      className={`flex-1 ${className}`}
                      {...comprops}
                      onChange={(e) => {
                        updateValue(index, compType, e.target.value);
                      }}
                      {...extraProps}
                    />
                  )}
                  <div className="key-value-edit-item-separate" />
                </Fragment>
              );
            })}
            <div className="key-value-edit-item-operation">
              {item.editKey && index + 1 !== values.length ? (
                <CustomIcon
                  type="sc1"
                  onClick={() => {
                    handleDelete(index);
                  }}
                />
              ) : null}
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default memo(KeyValueEdit);
