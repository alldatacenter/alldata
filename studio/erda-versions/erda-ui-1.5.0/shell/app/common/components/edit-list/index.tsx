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
import { Button, Input, Select, Tooltip } from 'antd';
import { useUpdate } from 'common/use-hooks';
import { InputSelect, Icon as CustomIcon, ErdaIcon } from 'common';
import { produce } from 'immer';
import i18n from 'i18n';
import { useUpdateEffect } from 'react-use';
import { getCheckListFromRule } from 'configForm/form/form';
import { get, isEmpty, map, isEqual, isArray, isPlainObject, set, compact, includes, filter, debounce } from 'lodash';
import './index.scss';

interface IData {
  [k: string]: any;
}

interface ITemp {
  title: string;
  key: string;
  width?: number;
  flex?: number;
  titleTip?: string;
  render: {
    type: 'input' | 'text' | 'select' | 'inputSelect';
    props?: Obj;
    required?: boolean;
    uniqueValue?: boolean;
    defaultValue?: any;
    rules?: Array<{ msg: string; [k: string]: string }>;
    [pro: string]: any;
  };
}

const defaultRender = {
  type: 'input',
  required: false,
};

interface IELProps {
  value: IData[];
  onChange: (val: IData[]) => void;
  onSave?: (val: IData[]) => void;
  onBlurSave?: (val: IData[]) => void;
  dataTemp: ITemp[];
  disabled?: boolean;
  showTitle?: boolean; // 显示表头
}

const noop = () => {};

const getTempData = (dataTemp: ITemp[], withTitle = false) => {
  const data = {};
  map(dataTemp, (temp) => {
    data[temp.key] = withTitle ? temp.title : (temp.render || defaultRender).defaultValue;
  });
  return data;
};

const getDuplicateValue = (arr: string[]) =>
  filter(arr, (value, index, iteratee) => includes(iteratee, value, index + 1));

export const validateValue = (dataTemp: ITemp[], value: IData[]) => {
  const requiredKeys = [] as ITemp[];
  const uniqValueKeys = [] as ITemp[];
  dataTemp.forEach((item) => {
    item.render?.required && requiredKeys.push(item);
    item.render?.uniqueValue && uniqValueKeys.push(item);
  });

  let validTip = '';
  if (!isEmpty(uniqValueKeys)) {
    // 唯一值的key
    uniqValueKeys.forEach((uk) => {
      if (!validTip) {
        const duplicateValues = getDuplicateValue(compact(map(value, uk.key)));
        const sameKey = i18n.t('the same {key} exists', { key: duplicateValues.join(',') });
        duplicateValues.length && (validTip = `${uk.title || uk.key} ${sameKey}`);
      }
    });
  }
  if (!validTip) {
    requiredKeys.forEach((rk) => {
      value.forEach((v) => {
        if (!validTip) {
          const unValid = notEmptyValue(get(v, rk.key));
          !unValid && (validTip = i18n.t('{name} can not empty', { name: rk.title || rk.key }));
        }
      });
    });
  }

  if (!validTip) {
    dataTemp.forEach((item) => {
      const rules = item.render?.rules;
      if (!validTip && rules) {
        value.forEach((v) => {
          if (!validTip) validTip = validRulesValue(rules, get(v, item.key));
        });
      }
    });
  }
  return validTip;
};

const EditList = (props: IELProps) => {
  const {
    value: propsVal,
    onChange,
    onBlurSave: pOnBlurSave,
    dataTemp,
    onSave = noop,
    showTitle = true,
    disabled = false,
  } = props;
  const [{ value, changed, forceUpdate }, updater, update] = useUpdate({
    value: propsVal,
    changed: false,
    forceUpdate: false,
  });

  const valueRef = React.useRef(value as any);
  const debounceChange = React.useRef(debounce(onChange, 1000));

  useUpdateEffect(() => {
    if (!isEqual(value, propsVal)) {
      updater.value(propsVal);
    }
  }, [propsVal]);

  const tempData = getTempData(dataTemp);
  useUpdateEffect(() => {
    // onSave(value);
    if (!isEqual(value, propsVal)) {
      if (forceUpdate) {
        onChange(value);
      } else {
        debounceChange.current(value);
      }
      update({
        changed: true,
        forceUpdate: false,
      });
      valueRef.current = value;
    }
  }, [value]);

  const changeData = (val: any, forceChange = false) => {
    update({
      value: val,
      forceUpdate: forceChange,
    });
  };

  const onBlurSave = (v?: IData[]) => {
    setTimeout(() => {
      // 延时确保值正确，不同控件修改值的时机不一样，select触发update同时会触发保存
      if (pOnBlurSave && !validateValue(dataTemp, v || valueRef.current || [])) {
        pOnBlurSave(v || valueRef.current);
      }
    }, 100);
  };

  const updateItem = (index: number) => (d: any) => {
    const newVal = map(value, (val, idx) => {
      if (isPlainObject(val)) {
        if (index === idx) {
          const curVal = produce(val, (draft: Obj) => {
            const curKey = Object.keys(d)[0];
            set(draft, curKey, d[curKey]);
          });
          return curVal;
        }
        return val;
      } else {
        return index === idx ? d : val;
      }
    });
    changeData(newVal);
  };

  const addItem = () => {
    const v = [...(value || []), tempData];
    changeData(v);
    onBlurSave();
  };

  const deleteItem = (index: number) => {
    const v = filter(value, (_, idx) => index !== idx);
    changeData(v, true);
    onBlurSave();
  };

  if (isEmpty(dataTemp)) return null;

  const validTip = validateValue(dataTemp, value || []);

  return (
    <div className="edit-list">
      <div className="edit-list-box">
        {showTitle ? (
          <ListItem
            dataTemp={dataTemp}
            isTitle
            operation={
              <ErdaIcon type="reduce-one" className="edit-list-item-operation not-allowed" />
            }
          />
        ) : null}
        {map(value, (item, idx) => (
          <ListItem
            key={idx}
            dataTemp={dataTemp}
            value={item}
            updateItem={updateItem(idx)}
            onBlurSave={onBlurSave}
            disabled={disabled}
            operation={
              <div className="table-operations">
                <span
                  className={`table-operations-btn ${disabled ? 'not-allowed' : ''}`}
                  onClick={() => !disabled && deleteItem(idx)}
                >
                  {i18n.t('delete')}
                </span>
              </div>
            }
          />
        ))}
      </div>
      {validTip ? <div className="p-2 text-red">{validTip}</div> : null}
      <div className="edit-list-bottom mt-1">
        {disabled ? (
          <Button className="not-allowed" size="small">
            {i18n.t('common:add')}
          </Button>
        ) : (
          <Button className="" size="small" onClick={addItem}>
            {i18n.t('common:add')}
          </Button>
        )}
        {
          // autoSave ? null : (
          //   changed ? (
          //     <>
          //       <Button size='small' className='ml-2' onClick={cancelEdit}>取消</Button>
          //       <Button size='small' type='primary' className='ml-2' onClick={onSave} disabled={!!validTip}>保存</Button>
          //     </>
          //   ) : null
          // )
        }
      </div>
    </div>
  );
};

export default EditList;

interface IListItemProps {
  dataTemp: ITemp[];
  value?: IData;
  updateItem?: (val: IData) => void;
  onBlurSave?: (val: IData) => void;
  operation?: React.ReactElement;
  disabled?: boolean;
  isTitle?: boolean;
}

const oprationKey = '__op__';
const operationTemp = {
  key: oprationKey,
  title: '操作',
  width: 60,
  render: { type: 'custom' },
};

const ListItem = (props: IListItemProps) => {
  const { dataTemp, value = {}, updateItem: pUpdateItem = noop, operation, ...rest } = props;
  const updateItem = (k: string) => (v: any) => {
    pUpdateItem({ [k]: v });
  };

  const [temp, useValue] = operation
    ? [[...dataTemp, operationTemp], { ...value, [oprationKey]: operation }]
    : [dataTemp, value];

  return (
    <div className="edit-list-item-box">
      {map(temp, (item, idx) => (
        <RenderItem
          key={idx}
          operation={operation}
          temp={item}
          value={get(useValue, item.key)}
          updateItem={updateItem(item.key)}
          {...rest}
        />
      ))}
    </div>
  );
};

interface IRenderItem {
  temp: ITemp;
  value: any;
  updateItem: (v: any) => void;
  disabled?: boolean;
  operation?: React.ReactElement;
  isTitle?: boolean;
  onBlurSave: (val?: IData) => void;
}

const notEmptyValue = (v: any) => {
  if (isPlainObject(v) || isArray(v)) {
    return !isEmpty(v);
  }
  return !(v === '' || v === null || v === undefined);
};

const validRulesValue = (rules: any[] = [], value: any) => {
  let validTip = '';
  (rules || []).forEach((item) => {
    const checkList = getCheckListFromRule(item) || [];
    checkList.forEach((checkFun: any) => {
      if (!validTip) {
        const result = checkFun(value, item); // 返回 [status: 'success' | 'error', msg: '']
        if (result[0] === false) validTip = result[1];
      }
    });
  });
  return validTip;
};

const RenderItem = (props: IRenderItem) => {
  const { temp, value, updateItem, disabled, isTitle, onBlurSave = noop } = props;
  const ref = React.useRef<HTMLTextAreaElement>(null as any as HTMLTextAreaElement);
  let Comp = '' as React.ReactElement | string;
  const { flex = 1, width, render, titleTip, ...rest } = temp;
  const { type = 'input', required, rules, props: rProps } = render || defaultRender;
  const [curType, curVal] = isTitle ? ['text', temp.title] : [type, value];
  let cls = isTitle ? 'edit-list-item-title' : '';
  let style = {};
  if (width) {
    style = { width };
  } else {
    cls += ` flex-${flex} min-width`;
  }
  if (required) {
    cls += ' required-item';
  }

  const handleInput = () => {
    ref.current.style.height = '30px';
    ref.current.style.height = `${ref.current.scrollHeight}px`;
  };

  const handleFocus = () => {
    ref.current.style.height = `${ref.current.scrollHeight}px`;
  };

  const handleBlur = () => {
    if (curType === 'textarea') {
      ref.current.style.height = '30px';
    }
    onBlurSave();
  };

  const isValidValue = required ? notEmptyValue(curVal) : true;
  let itemCls = '';
  let validTip = '';
  if (!isValidValue) {
    itemCls = 'has-error';
  } else {
    validTip = validRulesValue(rules, value);
    if (validTip) itemCls = 'has-error';
  }

  switch (curType) {
    case 'text':
    case 'custom':
      Comp =
        isTitle && titleTip ? (
          <div className="flex items-center flex-wrap justify-start">
            {curVal}
            <Tooltip title={titleTip}>
              <CustomIcon type="help" className="ml-1 text-sm" />
            </Tooltip>
          </div>
        ) : (
          curVal
        );
      break;
    case 'select':
      Comp = (
        <Select
          value={curVal}
          className={'edit-list-select w-full'}
          getPopupContainer={() => document.body}
          onChange={(v: any) => {
            updateItem(v);
            onBlurSave();
          }}
          {...rProps}
        />
      );
      break;
    case 'inputSelect':
      Comp = (
        <InputSelect
          {...rProps}
          disabled={disabled}
          value={curVal}
          onChange={(val: string) => updateItem(val)}
          onBlur={() => onBlurSave()}
        />
      );
      break;
    case 'input':
      Comp = (
        <Input
          className="nowrap"
          required={required}
          disabled={disabled}
          value={curVal as string}
          onChange={(e: any) => updateItem(e.target.value)}
          onBlur={() => onBlurSave()}
          {...rProps}
        />
      );
      break;
    case 'textarea':
      Comp = (
        <textarea
          className="w-full ant-input edit-list-item-textarea"
          ref={ref}
          required={required}
          disabled={disabled}
          value={curVal as string}
          onChange={(e: any) => updateItem(e.target.value)}
          onInput={handleInput}
          onBlur={handleBlur}
          onFocus={handleFocus}
          {...rProps}
        />
      );
      break;
    default:
      break;
  }
  return (
    <div style={style} className={`${cls} ${itemCls} edit-list-item`}>
      {Comp}
      {/* {validTip ? <div className='p-2 text-red'>{validTip}</div> : null} */}
    </div>
  );
};
