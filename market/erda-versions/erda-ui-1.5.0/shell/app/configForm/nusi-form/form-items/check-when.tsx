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
import { map, isEmpty, filter } from 'lodash';
import { Select, Input, Tooltip, Form } from 'antd';
import { getLabel, noop } from './common';
import { ErdaIcon } from 'common';
import i18n from 'i18n';
import './check-when.scss';

const { Option } = Select;

type Operator = '=' | '!=' | '>' | '>=' | '<' | '<=' | 'includes' | 'contains' | 'not_contains' | 'empty' | 'not_empty';
const OperatorList = [
  { key: '=', name: '等于' },
  { key: '!=', name: '不等于' },
  { key: '>', name: '大于' },
  { key: '>=', name: '大等于' },
  { key: '<', name: '小于' },
  { key: '<=', name: '小等于' },
  { key: 'includes', name: '包含值(includes)' },
  { key: 'contains', name: '值包含(contains)' },
  { key: 'not_contains', name: '值不包含(not_contains)' },
  { key: 'empty', name: '为空' },
  { key: 'not_empty', name: '不为空' },
];
type Mode = 'create' | 'edit';
type CheckItem =
  | {
      field: string;
      operator: Operator;
      value: string | number | boolean | any[];
      valueType?: 'string' | 'number' | 'boolean';
    }
  | {
      mode: Mode;
    };

type CheckItemData =
  | {
      field: string;
      operator: Operator;
      value: string | number | boolean;
      type: string;
    }
  | {
      mode: Mode;
      type: string;
    };
export interface IProps {
  value: CheckItem[][];
  allField: any[];
  form: FormInstance;
  onChange: (arg: CheckItem[][]) => void;
}

// 二维组合组件 :  value=[[{field,operator,value},{mode}]]
const typeMap = {
  formItemCondition: {
    key: 'formItemCondition',
    name: '表单项条件',
  },
  formMode: {
    key: 'formMode',
    name: '表单状态',
  },
};

const valueToData = (value: CheckItem[][]) => {
  return map(value, (item) => {
    return map(item, (subItem) => {
      return { ...subItem, type: typeMap[subItem.mode ? 'formMode' : 'formItemCondition'].key };
    });
  });
};
const dataToValue = (data: CheckItemData[][]) => {
  const value = [] as CheckItem[][];
  map(data, (item) => {
    const subVal = [] as CheckItem[];
    map(item, (subItem) => {
      const { type, ...rest } = subItem;
      !isEmpty(rest) && subVal.push(rest);
    });
    !isEmpty(subVal) && value.push(subVal);
  });
  return value;
};

interface IItemProps {
  data: CheckItemData[];
  fieldOption: any[];
  updateItem: (arg: any, idx: number) => void;
  deleteSubItem: (idx: number) => void;
  addSubItem: () => void;
}
const Item = (props: IItemProps) => {
  const { data, updateItem, deleteSubItem, addSubItem, fieldOption } = props;
  return (
    <div className="combiner-item">
      {map(data, (item, idx) => (
        <SubItem
          key={`${idx}`}
          data={item}
          fieldOption={fieldOption}
          updateItem={(v) => updateItem(v, idx)}
          operation={
            <Tooltip title="删除条件项">
              <ErdaIcon
                type="reduce-one"
                size="20"
                className="combiner-operation sub"
                onClick={() => deleteSubItem(idx)}
              />
            </Tooltip>
          }
        />
      ))}
      <Tooltip title="添加条件项（统一条件组内的项之间为逻辑与关系）">
        <ErdaIcon type="add-one" size="20" className="combiner-operation sub" onClick={() => addSubItem()} />
      </Tooltip>
    </div>
  );
};

interface ISubItemProps {
  data: CheckItemData;
  updateItem: (arg: any) => void;
  operation: any;
  fieldOption: any[];
}

const SubItem = (props: ISubItemProps) => {
  const { data, updateItem, operation, fieldOption } = props;
  return (
    <div className="combiner-sub-item">
      <Select
        value={data.type}
        onChange={(val) =>
          updateItem({
            type: val,
            operator: undefined,
            field: undefined,
            value: undefined,
            mode: undefined,
          })
        }
      >
        {map(typeMap, (typeItem) => (
          <Option key={typeItem.key} value={typeItem.key}>
            {typeItem.name}
          </Option>
        ))}
      </Select>
      {data.type === typeMap.formMode.key ? (
        <Select value={data.mode} onChange={(val) => updateItem({ mode: val })}>
          <Option key="create" value="create">
            创建
          </Option>
          <Option key="edit" value="edit">
            编辑
          </Option>
        </Select>
      ) : (
        <>
          <Tooltip title="受控表单项">
            <Select
              value={data.field}
              onChange={(val) =>
                updateItem({
                  field: val,
                })
              }
              placeholder="受控表单项key"
            >
              {map(fieldOption, (f) => (
                <Option key={f.key} value={f.key}>
                  {f.key}
                </Option>
              ))}
            </Select>
          </Tooltip>
          <Tooltip title="逻辑关系">
            <Select
              placeholder="逻辑关系"
              dropdownMatchSelectWidth={false}
              value={data.operator}
              onChange={(val) => updateItem({ operator: val })}
            >
              {map(OperatorList, (opt) => (
                <Option key={opt.key} value={opt.key}>
                  {opt.name}
                </Option>
              ))}
            </Select>
          </Tooltip>
          <Input
            value={data.operator === 'includes' ? data.value.join(',') : data.value}
            onChange={(e) =>
              updateItem({ value: data.operator === 'includes' ? e.target.value.split(',') : e.target.value })
            }
            placeholder="逻辑比较值"
          />
        </>
      )}
      {operation}
    </div>
  );
};

export const CheckWhenCombiner = (props: IProps) => {
  const { value, onChange, allField, form } = props;
  const [data, setData] = React.useState(valueToData(value) as CheckItemData[][]);

  const fieldOption = filter(allField, (item: any) => item.key !== form.getData().key);

  React.useEffect(() => {
    onChange(dataToValue(data));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data]);

  const updateItem = (d: any, indexArr: number[]) => {
    const [rowIdx, colIdx] = indexArr;
    setData(
      map(data, (item, idx) => {
        return map(item, (subItem, subIdx) => {
          return rowIdx === idx && colIdx === subIdx ? { ...subItem, ...d } : { ...subItem };
        });
      }),
    );
  };

  const addItem = () => {
    setData([...data, [{ type: 'formItemCondition' }]] as any);
  };
  const addSubItem = (index: number) => {
    const newData = [] as CheckItemData[][];
    map(data, (item, idx) => {
      newData.push(index === idx ? [...item, { type: 'formItemCondition' }] : (item as any));
    });
    setData(newData);
  };

  const deleteItem = (index: number) => {
    const newData = [] as any;
    map(data, (item, idx) => {
      index !== idx && newData.push([...item]);
    });
    setData(newData);
  };

  const deleteSubItem = (rowIdx: number, colIdx: number) => {
    const newData = [] as CheckItemData[][];
    map(data, (item, idx) => {
      const colData = [] as CheckItemData[];
      map(item, (subItem, subIdx) => {
        if (!(rowIdx === idx && colIdx === subIdx)) {
          colData.push({ ...subItem });
        }
      });
      colData.length && newData.push([...colData]);
    });
    setData(newData);
  };

  return (
    <div className="dice-form-nusi-check-when-combiner">
      {map(data, (item, rowIdx) => {
        return (
          <div className="combiner-item-container">
            <div className="combiner-item-box" key={`${rowIdx}`}>
              <Item
                data={item}
                updateItem={(d, colIdx) => updateItem(d, [rowIdx, colIdx])}
                deleteSubItem={(colIdx: number) => deleteSubItem(rowIdx, colIdx)}
                addSubItem={() => addSubItem(rowIdx)}
                fieldOption={fieldOption}
              />
              <Tooltip title="删除条件组">
                <ErdaIcon
                  type="reduce-one"
                  size="20"
                  className="combiner-operation"
                  onClick={() => deleteItem(rowIdx)}
                />
              </Tooltip>
            </div>
            {rowIdx === data.length - 1 ? null : <div className="split">或</div>}
          </div>
        );
      })}
      <Tooltip title="添加条件组（不同条件组之间为逻辑或关系）">
        <ErdaIcon type="add-one" size="20" className="combiner-operation" onClick={addItem} />
      </Tooltip>
    </div>
  );
};

const FormItem = Form.Item;

export const FormCheckWhen = ({ fixOut = noop, fixIn = noop, extensionFix, requiredCheck, trigger = 'onChange' }) =>
  React.memo(({ fieldConfig, form, ...rest }: any) => {
    const {
      key,
      value,
      label,
      visible,
      valid,
      registerRequiredCheck,
      componentProps,
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
        <CheckWhenCombiner {...rest} value={fixIn(value)} onChange={handleChange} form={form} />
      </FormItem>
    );
  });

export const config = {
  name: 'checkWhen',
  Component: FormCheckWhen, // 某React组件，props中必须有value、onChange
  requiredCheck: (value) => {
    // 必填校验时，特殊的校验规则
    return [!isEmpty(value), i18n.t('can not be empty')];
  },
  fixOut: (value, options) => {
    // 在获取表单数据时，将React组件的value格式化成需要的格式
    return value;
  },
  fixIn: (value = [], options) => {
    // 从schema到React组件映射时，修正传入React组件的value
    return value;
  },
  extensionFix: (data, options) => {
    // 从schema到React组件映射时，修正传入React组件的配置项
    return data;
  },
};
