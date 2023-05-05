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

import { Form, Input, Col } from 'antd';
import { getLabel, noop, createCombiner } from 'app/configForm/nusi-form/form-items/common';
import React from 'react';
import { commonFields, checkWhen } from 'app/configForm/nusi-form/form-items/common/config';
import i18n from 'i18n';
import { isEmpty, map, isString, get } from 'lodash';

const FormItem = Form.Item;
interface IMapItemProps {
  data: Obj;
  className?: string;
  operation?: any;
  updateItem: (arg: any) => void;
  itemRender?: (data: Obj, updateItem: (arg: Obj) => void) => void;
}
const changeValue = (obj: Obj[]) => obj;

const defaultItem = [
  { key: 'key', type: 'input' },
  { key: 'value', type: 'input' },
];
const MapObjComp = (props: any) => {
  const { value, onChange, disabled, componentProps } = props;

  const { objItems } = componentProps || {};
  const {
    key: keyPlaceholder = i18n.t('please enter {name}', { name: 'key' }),
    value: valuePlaceholder = i18n.t('please enter {name}', { name: 'value' }),
  } = componentProps?.placeholder || {};

  const Comp: any = React.useMemo(() => {
    const _objItems = isEmpty(objItems) ? defaultItem : objItems;
    return createCombiner({
      valueFixIn: changeValue,
      valueFixOut: changeValue,
      CombinerItem: ({ updateItem, data, className = '', operation = null }: IMapItemProps) => {
        return (
          <div className={`${className}`}>
            <Input.Group>
              <Col span={8}>
                <Input
                  {...componentProps}
                  placeholder={keyPlaceholder}
                  disabled={disabled}
                  value={data.key}
                  onChange={(e) => {
                    updateItem({ key: e.target.value });
                  }}
                />
              </Col>
              <Col span={1}>
                <div style={{ lineHeight: '28px', textAlign: 'center' }}>=</div>
              </Col>
              <Col span={8}>
                <Input
                  {...componentProps}
                  placeholder={valuePlaceholder}
                  disabled={disabled}
                  value={data.value}
                  onChange={(e) => {
                    updateItem({ value: e.target.value });
                  }}
                />
              </Col>
            </Input.Group>
            {operation}
          </div>
        );
      },
      defaultItem: () => {
        const dItem = {};
        map(_objItems, (item) => {
          const key = isString(item) ? item : get(item, 'key') || '';
          key && (dItem[key] = undefined);
        });
        return dItem;
      },
    });
  }, [componentProps, disabled, keyPlaceholder, objItems, valuePlaceholder]);

  return <Comp value={value} onChange={onChange} disabled={disabled} {...componentProps} />;
};

let orderMap = {};

export const FormMap = ({ fixOut = noop, fixIn = noop, extensionFix, requiredCheck, trigger = 'onChange' }) =>
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
      defaultValue,
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
        <MapObjComp value={fixIn(value)} onChange={handleChange} disabled={disabled} componentProps={componentProps} />
      </FormItem>
    );
  });

export const config = {
  name: 'map',
  Component: FormMap, // 某React组件，props中必须有value、onChange
  requiredCheck: (value: any) => {
    if (isEmpty(value)) {
      return [false, i18n.t('can not be empty')];
    } else {
      let valid = true;
      Object.keys(value).forEach((k) => {
        if (k?.endsWith('&repeat')) {
          valid = false;
        }
      });
      return [valid, i18n.t('{name} already exists', { name: 'key' })];
    }
  },
  fixOut: (itemData: any) => {
    const data = {};
    let emptyKeyCount = 0;
    orderMap = {};
    let isEmptyItem = false;
    map(itemData, ({ key: k, value: v }, index: number) => {
      if (orderMap[k] === undefined) {
        orderMap[k] = !isEmptyItem ? index : index - 1;
      } else {
        orderMap[`${k}&repeat`] = !isEmptyItem ? index : index - 1;
      }

      if (!isEmpty(k) || !isEmpty(v) || index === itemData?.length - 1) {
        if (k !== undefined) {
          if (orderMap[`${k}&repeat`]) {
            data[`${k}&repeat`] = v;
          } else {
            data[k] = v;
          }
        } else if (emptyKeyCount === 0) {
          data[''] = v;
          emptyKeyCount += 1;
        }
      } else {
        isEmptyItem = true;
      }
    });
    // 在获取表单数据时，将React组件的value格式化成需要的格式
    return data;
  },
  fixIn: (itemData: any) => {
    // 从schema到React组件映射时，修正传入React组件的value；
    const mapParams: Array<{ key: any; value: any }> = [];
    map(Object.keys(itemData || {}), (k) => {
      const index = orderMap[k || 'undefined'] !== undefined ? orderMap[k || 'undefined'] : orderMap[''];
      if (k.endsWith('&repeat')) {
        mapParams[index] = { key: k.split('&repeat')[0], value: itemData[k] };
      } else {
        mapParams[index] = { key: k, value: itemData[k] };
      }
    });

    const list = isEmpty(itemData) ? [] : mapParams;
    return list.filter((item) => item);
  },
  extensionFix: (data: any, options: any) => {
    // 从schema到React组件映射时，修正传入React组件的配置项
    return data;
  },
};

export const formConfig = {
  map: {
    name: 'map对象',
    value: 'map',
    fieldConfig: {
      basic: {
        key: 'basic',
        name: '基本配置',
        fields: [...commonFields.filter((item) => item.key !== 'defaultValue'), ...checkWhen],
      },
      componentProps: {
        key: 'componentProps',
        name: '组件配置',
        fields: [
          {
            label: 'Key placeholder',
            key: 'componentProps.placeholder.key',
            type: 'input',
            component: 'input',
          },
          {
            label: 'Value placeholder',
            key: 'componentProps.placeholder.value',
            type: 'input',
            component: 'input',
          },
        ],
      },
    },
  },
};
