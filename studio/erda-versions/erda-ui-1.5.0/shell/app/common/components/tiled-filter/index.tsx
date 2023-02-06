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
import { ErdaIcon } from 'common';
import i18n from 'i18n';
import { debounce, map, max } from 'lodash';
import { useMount, useUpdateEffect } from 'react-use';
import './index.scss';

export interface IOption {
  value: string;
  label: string;
}

export interface IField {
  type: 'input' | 'select' | 'dropdown-select';
  multiple?: boolean;
  label: string;
  key: string;
  showSearch?: boolean;
  placeholder?: string;
  options: IOption[];
}

export interface IProps {
  fields: IField[];
  value: Object;
  delay?: number;
  expand?: boolean;
  labelWidth?: number;
  onChange: (fullValue: Object, value?: Object) => void;
}

const MAX_LABEL_WIDTH = 180;

const TiledFilter = (props: IProps) => {
  const { fields, delay = 1000, value: propsValue, onChange, expand: propsExpand = true } = props;
  const [expand, setExpand] = React.useState(propsExpand);
  const [value, setValue] = React.useState(propsValue || {});
  const [labelWidth, setLabelWidth] = React.useState<string | number>('auto');

  useMount(() => {
    const labelEles = document.querySelectorAll('.tiled-fields-item-label');
    const maxWidth: number = max(map(labelEles, (ele: HTMLSpanElement) => ele.offsetWidth));
    setLabelWidth(Math.min(maxWidth, MAX_LABEL_WIDTH));
  });

  useUpdateEffect(() => {
    setValue(propsValue || {});
  }, [propsValue]);

  React.useEffect(() => {
    setExpand(propsExpand);
  }, [propsExpand]);

  const debouncedChange = React.useRef(debounce(onChange, delay));
  const debouncedInputChange = React.useRef(debounce(onChange, 1000));

  const inputFields: IField[] = [];
  const selectFields: IField[] = [];
  fields.forEach((field) => {
    if (field.type === 'input') {
      inputFields.push(field);
    } else if (field.type.includes('select')) {
      selectFields.push(field);
    }
  });

  const onChangeItem = (val: string, field: IField, forceUpdate?: boolean) => {
    if (forceUpdate) {
      setValue((prevV) => {
        const newVal = { ...prevV, [field.key]: val };
        debouncedChange.current(newVal, { [field.key]: val });
        return { ...prevV, [field.key]: val };
      });
      return;
    }
    const curValue = value[field.key];
    if (field.multiple) {
      setValue((prevV) => {
        const curChangeVal = curValue?.includes(val)
          ? curValue.filter((vItem: string) => vItem !== val)
          : (curValue || []).concat(val);
        const newVal = { ...prevV, [field.key]: curChangeVal };
        debouncedChange.current(newVal, { [field.key]: curChangeVal });
        return newVal;
      });
    } else {
      setValue((prevV) => {
        const curChangeVal = curValue === val ? undefined : val;
        const newVal = { ...prevV, [field.key]: curChangeVal };
        debouncedChange.current(newVal, { [field.key]: curChangeVal });
        return newVal;
      });
    }
  };

  const onChangeInputItem = (val: string, field: IField) => {
    setValue((prevV) => {
      const newVal = { ...prevV, [field.key]: val };
      debouncedInputChange.current(newVal, { [field.key]: val });
      return newVal;
    });
  };

  const clearSelect = () => {
    const newVal = {};
    selectFields.forEach((sItem) => {
      newVal[sItem.key] = undefined;
    });
    setValue((prev) => {
      debouncedChange.current({ ...prev, ...newVal }, { ...newVal });
      return { ...prev, ...newVal };
    });
  };

  const getValueLength = () => {
    let valLength = 0;
    selectFields.forEach((sItem) => {
      const curVal = value[sItem.key];
      if (Array.isArray(curVal)) {
        valLength += curVal.length;
      } else if (curVal !== undefined && curVal !== '') {
        valLength += 1;
      }
    });
    return valLength;
  };

  const curValLength = getValueLength();
  return (
    <div className="tiled-filter">
      <div className={`tiled-fields ${expand ? '' : 'no-expand'}`}>
        {selectFields.map((item) => {
          return (
            <SelectFieldItem
              key={item.key}
              field={item}
              value={value?.[item.key]}
              onChangeItem={onChangeItem}
              labelWidth={labelWidth}
            />
          );
        })}
      </div>
      <div className="flex justify-between items-center">
        <div className="flex items-center">
          {curValLength ? (
            <>
              <span>{`${i18n.t('selected {xx}', { xx: `${curValLength}${i18n.t('common:items')}` })}`}</span>
              <span className="fake-link ml-2 mr-4" onClick={clearSelect}>
                {i18n.t('common:clear selected')}
              </span>
            </>
          ) : null}
          <div className="tiled-input">
            {inputFields.map((inputItem) => {
              return (
                <Input
                  className={'tiled-input-item'}
                  key={inputItem.key}
                  value={value[inputItem.key]}
                  size="small"
                  allowClear
                  prefix={<ErdaIcon type="search1" size="16" />}
                  placeholder={inputItem.placeholder || i18n.t('press enter to search')}
                  onChange={(e) => onChangeInputItem(e.target.value, inputItem)}
                />
              );
            })}
          </div>
        </div>
        <div className={`flex items-center expand-area`} onClick={() => setExpand(!expand)}>
          <span className="mr-2">{expand ? i18n.t('fold') : i18n.t('expand')}</span>
          <ErdaIcon
            type="down"
            className={`expand-icon flex items-center ${expand ? 'expand' : ''}`}
            size="16"
          />
        </div>
      </div>
    </div>
  );
};

interface IFieldItemProps {
  field: IField;
  labelWidth: number | string;
  value: string[] | string;
  onChangeItem: (val: string, field: IField, forceUpdate?: boolean) => void;
}

const SelectFieldItem = (props: IFieldItemProps) => {
  const { field, labelWidth, value, onChangeItem } = props;
  const { type, label, options, showSearch, multiple } = field;

  let Comp: React.ReactNode = null;
  const _value: string[] = multiple ? value : [value];
  switch (type) {
    case 'select':
      Comp = (
        <div className="tiled-fields-item flex">
          <div className="tiled-fields-item-label text-right mr-2 pt-1" style={{ width: labelWidth }}>
            {label}
          </div>
          <div className="tiled-fields-item-option flex-1">
            {options.map((option) => (
              <span
                key={option.value}
                className={`tiled-fields-option-item ${_value?.includes(option.value) ? 'chosen-item' : ''}`}
                onClick={() => onChangeItem(option.value, field)}
              >
                {option.label}
              </span>
            ))}
          </div>
        </div>
      );
      break;
    case 'dropdown-select':
      Comp = (
        <div className="tiled-fields-item flex">
          <div className="tiled-fields-item-label text-right mr-2 pt-1" style={{ width: labelWidth }}>
            {label}
          </div>
          <div className="tiled-fields-item-option flex-1">
            <Select
              showSearch={showSearch}
              size={'small'}
              value={value}
              allowClear
              optionFilterProp={'children'}
              mode={multiple ? 'multiple' : undefined}
              getPopupContainer={() => document.body}
              style={{ minWidth: 240, maxWidth: '100%' }}
              onChange={(v) => {
                onChangeItem(v, field, true);
              }}
            >
              {options.map((option) => {
                return (
                  <Select.Option key={option.value} value={option.value}>
                    {option.label}
                  </Select.Option>
                );
              })}
            </Select>
          </div>
        </div>
      );
      break;
    default:
      break;
  }

  return Comp;
};

export default TiledFilter;
