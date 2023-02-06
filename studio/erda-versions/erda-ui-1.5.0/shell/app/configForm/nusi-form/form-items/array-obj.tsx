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
import { isEmpty, map, get, isString } from 'lodash';
import { Input, Form, Select, InputNumber, Switch } from 'antd';
import { getLabel, noop, createCombiner } from './common';
import { commonFields, checkWhen } from './common/config';
import i18n from 'i18n';
import './array-obj.scss';

const { Option } = Select;

interface IArrayObjItemProps {
  keys: string[] | Array<{ key: string; name: string }>;
  data: Obj;
  className?: string;
  operation?: any;
  updateItem: (arg: any) => void;
  itemRender?: (data: Obj, updateItem: (arg: Obj) => void) => any;
}
const defaultKeys = ['key', 'value'];
const ArrayObjItem = ({
  updateItem,
  data,
  className = '',
  operation = null,
  itemRender,
  keys = defaultKeys,
}: IArrayObjItemProps) => {
  return (
    <div className={`dice-form-array-obj ${className}`}>
      {itemRender
        ? itemRender(data, updateItem)
        : map(keys, (item) => {
            const [key] = isString(item) ? [item, item] : [get(item, 'key') || '', get(item, 'name') || ''];
            return key ? (
              <Input
                key={key}
                value={data[key]}
                placeholder={i18n.t('please enter')}
                onChange={(e) => updateItem({ [key]: e.target.value })}
              />
            ) : null;
          })}
      {operation}
    </div>
  );
};

const changeValue = (obj: Obj[]) => obj;

export const ArrayObj = createCombiner<Obj, Obj>({
  valueFixIn: changeValue,
  valueFixOut: changeValue,
  CombinerItem: ArrayObjItem,
  defaultItem: ({ keys = defaultKeys, defaultItem: _defaultItem }) => {
    if (_defaultItem) return _defaultItem;
    const dItem = {};
    map(keys, (item) => {
      const key = isString(item) ? item : get(item, 'key') || '';
      key && (dItem[key] = undefined);
    });
    return dItem;
  },
});

const defaultItem = [
  { key: 'key', type: 'input' },
  { key: 'value', type: 'input' },
];
const ArrayObjComp = (props: any) => {
  const { value, onChange, disabled, componentProps } = props;

  const { objItems, direction = 'column' } = componentProps || {};

  const objItemsLength = objItems?.length;
  const Comp: any = React.useMemo(() => {
    const _objItems = isEmpty(objItems) ? defaultItem : objItems;
    return createCombiner({
      valueFixIn: changeValue,
      valueFixOut: changeValue,
      CombinerItem: ({
        updateItem,
        data,
        className = '',
        operation = null,
        itemRender,
        keys = _objItems,
      }: IArrayObjItemProps) => {
        return (
          <div className={`dice-form-array-obj ${className}`}>
            <div
              className={`dice-form-array-obj-item flex-1 ${
                direction === 'row' ? 'flex justify-between items-center direction-row' : ''
              }`}
            >
              {itemRender
                ? itemRender(data, updateItem)
                : map(keys, (item, i) => {
                    const {
                      key,
                      label,
                      required = false,
                      labelTip = '',
                      component = 'input',
                      options = [],
                      getComp,
                      componentProps: itemComponentProps = {},
                    } = isString(item) ? { key: item, label: item } : (item as Obj);

                    let valid: any =
                      required && (data[key] === undefined || data[key] === '')
                        ? ['error', i18n.t('{name} can not empty')]
                        : ['success'];

                    if (typeof getComp === 'function') {
                      return <FormItem key={key || i}>{getComp(data)}</FormItem>;
                    }
                    let CompItem = null;
                    switch (component) {
                      case 'input':
                        CompItem = (
                          <Input
                            key={key}
                            value={data[key]}
                            placeholder={i18n.t('please enter {name}', { name: label || key })}
                            onChange={(e) => updateItem({ [key]: e.target.value })}
                            {...itemComponentProps}
                          />
                        );
                        break;
                      case 'select':
                        {
                          const _options = isString(options)
                            ? map(options.split(';'), (oStr) => {
                                const [k = '', v = ''] = oStr.split(':');
                                return { name: v, value: k };
                              })
                            : options;
                          CompItem = (
                            <Select
                              key={key}
                              value={data[key]}
                              placeholder={i18n.t('please select {name}', { name: label || key })}
                              onChange={(v: any) => updateItem({ [key]: v })}
                              {...itemComponentProps}
                            >
                              {map(_options, (oItem) => (
                                <Option key={oItem.value} value={oItem.value}>
                                  {oItem.name}
                                </Option>
                              ))}
                            </Select>
                          );
                        }
                        break;
                      case 'inputNumber':
                        CompItem = (
                          <InputNumber
                            key={key}
                            value={data[key]}
                            placeholder={i18n.t('please enter {name}', { name: label || key })}
                            onChange={(v) => updateItem({ [key]: v })}
                            {...itemComponentProps}
                          />
                        );
                        break;
                      case 'switch':
                        CompItem = (
                          <Switch
                            key={key}
                            checked={!!data[key]}
                            onChange={(v) => updateItem({ [key]: v })}
                            {...itemComponentProps}
                          />
                        );
                        valid = ['success'];
                        break;
                      case 'object':
                        {
                          //
                          const attrs = [] as any[];
                          if (isString(options)) {
                            const opts = options.split(';');
                            map(opts, (opt) => {
                              const [aK, aT] = opt.split(':');
                              if (aT && ['string', 'number', 'boolean'].includes(aT)) {
                                attrs.push({ key: aK, type: aT });
                              }
                            });
                          }
                          CompItem = (
                            <div key={key}>
                              <div className="font-bold mt-2">{getLabel(label || key, labelTip)}</div>
                              {map(attrs, (attr: any) => {
                                const { type: _type, key: _k } = attr;
                                const attrKey = `${key}.${_k}`;
                                const curVal = get(data, attrKey);
                                if (_type === 'string') {
                                  return (
                                    <FormItem colon key={_k} label={_k} required={false}>
                                      <Input
                                        key={_k}
                                        value={curVal}
                                        placeholder={i18n.t('please enter {name}', { name: _k })}
                                        onChange={(e) => updateItem({ [attrKey]: e.target.value })}
                                      />
                                    </FormItem>
                                  );
                                } else if (_type === 'number') {
                                  return (
                                    <FormItem colon key={_k} label={_k} required={false}>
                                      <InputNumber
                                        key={_k}
                                        value={curVal}
                                        placeholder={i18n.t('please enter {name}', { name: _k })}
                                        onChange={(v) => updateItem({ [attrKey]: v })}
                                      />
                                    </FormItem>
                                  );
                                } else if (_type === 'boolean') {
                                  return (
                                    <FormItem colon key={_k} label={_k} required={false}>
                                      <Switch checked={!!curVal} onChange={(v) => updateItem({ [attrKey]: v })} />
                                    </FormItem>
                                  );
                                }
                                return null;
                              })}
                            </div>
                          );
                        }
                        break;
                      default:
                        break;
                    }
                    if (component === 'object') return CompItem;

                    return key && CompItem ? (
                      <FormItem
                        key={key}
                        colon
                        label={label ? getLabel(label || key, labelTip) : undefined}
                        validateStatus={valid[0]}
                        help={valid[1]}
                        required={required}
                        style={label ? undefined : { width: '100%' }}
                      >
                        {CompItem}
                      </FormItem>
                    ) : null;
                  })}
            </div>
            <div className="operations">{operation}</div>
          </div>
        );
      },
      defaultItem: () => {
        const dItem = {};
        map(_objItems, (item) => {
          const key = isString(item) ? item : get(item, 'key') || '';
          if (get(item, 'component') === 'object') {
            dItem[key] = {};
          } else {
            key && (dItem[key] = undefined);
          }
        });
        return dItem;
      },
    });
  }, [direction, objItemsLength]);

  return <Comp value={value} onChange={onChange} disabled={disabled} {...componentProps} />;
};

const FormItem = Form.Item;

export const FormArrayObj = ({ fixOut = noop, fixIn = noop, extensionFix, requiredCheck, trigger = 'onChange' }) =>
  React.memo(({ fieldConfig, form }: any) => {
    const {
      key,
      value,
      label,
      visible,
      valid,
      disabled,
      registerRequiredCheck,
      componentProps = {},
      required,
      wrapperProps,
      labelTip,
      requiredCheck: _requiredCheck,
    } = fieldConfig;
    registerRequiredCheck(_requiredCheck || requiredCheck);
    const handleChange = (val: any) => {
      form.setFieldValue(key, fixOut(val));
      (componentProps.onChange || noop)(val);
    };

    return (
      <FormItem
        colon
        label={getLabel(label, labelTip)}
        className={visible ? '' : 'hidden'}
        validateStatus={valid[0]}
        help={valid[1]}
        required={required}
        {...wrapperProps}
      >
        <ArrayObjComp
          value={fixIn(value)}
          onChange={handleChange}
          disabled={disabled}
          componentProps={componentProps}
        />
      </FormItem>
    );
  });

export const config = {
  name: 'arrayObj',
  Component: FormArrayObj, // 某React组件，props中必须有value、onChange
  requiredCheck: (value: any) => {
    // 必填校验时，特殊的校验规则
    return [!isEmpty(value), i18n.t('can not be empty')];
  },
  fixOut: (value: any) => {
    // 在获取表单数据时，将React组件的value格式化成需要的格式
    return value;
  },
  fixIn: (value = [], options: any) => {
    // 从schema到React组件映射时，修正传入React组件的value
    return value;
  },
  extensionFix: (data: any, options: any) => {
    // 从schema到React组件映射时，修正传入React组件的配置项
    return data;
  },
};

export const formConfig = {
  arrayObj: {
    name: '数组对象',
    value: 'arrayObj',
    fieldConfig: {
      basic: {
        key: 'basic',
        name: '基本配置',
        fields: [...commonFields, ...checkWhen],
      },
      componentProps: {
        key: 'componentProps',
        name: '组件配置',
        fields: [
          {
            label: '排列方向',
            key: 'componentProps.direction',
            type: 'radio',
            component: 'radio',
            defaultValue: 'column',
            dataSource: {
              static: [
                { name: '纵向', value: 'column' },
                { name: '横向', value: 'row' },
              ],
            },
          },
          {
            label: '属性',
            key: 'componentProps.objItems',
            type: 'arrayObj',
            component: 'arrayObj',
            componentProps: {
              itemRender: (_data: Obj = {}, updateItem: Function) => {
                const isSelect = _data.component === 'select';
                const isObject = _data.component === 'object';
                return (
                  <div key={_data.key}>
                    <FormItem colon label={'属性key'} required>
                      <Input
                        key={'key'}
                        value={_data.key}
                        placeholder={i18n.t('please enter {name}', { name: '属性key' })}
                        onChange={(e) => updateItem({ key: e.target.value })}
                      />
                    </FormItem>
                    <FormItem colon label={'属性标签'} required={false}>
                      <Input
                        key={'label'}
                        value={_data.label}
                        placeholder={i18n.t('please enter {name}', { name: '属性标签' })}
                        onChange={(e) => updateItem({ label: e.target.value })}
                      />
                    </FormItem>
                    <FormItem colon label={'属性标签提示'} required={false}>
                      <Input
                        key={'labelTip'}
                        value={_data.labelTip}
                        placeholder={i18n.t('please enter {name}', { name: '属性标签提示' })}
                        onChange={(e) => updateItem({ labelTip: e.target.value })}
                      />
                    </FormItem>
                    <FormItem colon label={'组件'} required={false}>
                      <Select
                        key={'component'}
                        value={_data.component || 'input'}
                        placeholder={i18n.t('please select {name}', { name: '组件' })}
                        onChange={(v) => updateItem({ component: v })}
                      >
                        {map(subComponent, (oItem) => (
                          <Option key={oItem.value} value={oItem.value}>
                            {oItem.name}
                          </Option>
                        ))}
                      </Select>
                    </FormItem>
                    {isSelect ? (
                      <FormItem colon label={'选择项'} required={false}>
                        <Input
                          key={'options'}
                          value={_data.options}
                          placeholder={'使用k1:v1;k2:v2方式填写'}
                          onChange={(e) => updateItem({ options: e.target.value })}
                        />
                      </FormItem>
                    ) : null}
                    {isObject ? (
                      <FormItem colon label={'子项'} required={false}>
                        <Input
                          key={'options'}
                          value={_data.options}
                          placeholder={'使用k1:string;k2:number方式填写'}
                          onChange={(e) => updateItem({ options: e.target.value })}
                        />
                      </FormItem>
                    ) : null}
                    <FormItem colon label={'是否必填'} required={false}>
                      <Switch
                        key={'required'}
                        checked={!!_data.required}
                        onChange={(v) => updateItem({ required: v })}
                      />
                    </FormItem>
                  </div>
                );
              },
              // objItems: [
              //   { key: 'key', label: '属性key' },
              //   { key: 'label', label: '属性描述' },
              //   {
              //     key: 'component',
              //     label: '组件',
              //     component: 'select',
              //     options: [
              //       { value: 'input', name: '输入框(input)' },
              //       { value: 'select', name: '选择框(select)' },
              //       { value: 'switch', name: '开关(switch)' },
              //       { value: 'inputNumber', name: '数字框(inputNumber)' },
              //       // { value: 'inputNumber', name: '数字框(inputNumber)' },
              //     ],
              //   },
              //   {
              //     key: 'required',
              //     label: '是否必填',
              //     component: 'switch',
              //   },
              // ],
            },
          },
        ],
      },
    },
  },
};

const subComponent = [
  { value: 'input', name: '输入框(input)' },
  { value: 'select', name: '选择框(select)' },
  { value: 'switch', name: '开关(switch)' },
  { value: 'inputNumber', name: '数字框(inputNumber)' },
  { value: 'object', name: '对象(Object)' },
];
