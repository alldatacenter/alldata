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

import { map, last, cloneDeep } from 'lodash';
import { Input } from 'antd';
import React from 'react';
import { ErdaIcon } from 'common';

export interface IKVRecord {
  Key: JSX.Element;
  Desc: JSX.Element | null;
  Value: JSX.Element;
  KeyDescComp: JSX.Element | null;
  Op: JSX.Element;
}

interface IKVProp {
  value?: Array<{
    [prop: string]: any;
    key: string;
    value: string;
  }>;
  children: Function;
  keyName?: string;
  keyDesc?: string;
  descName?: string;
  valueName?: string;
  KeyComp?: any;
  DescComp?: any;
  ValueComp?: any;
  OpComp?: any;
  compProps?: object; // 其他传给所有Comp的prop，例如disabled
  emptyHolder?: boolean; // 没有数据时显示一个空行
  autoAppend?: boolean; // 自动追加空行
  onChange: (value: object) => any;
}

const DefaultKey = ({ record, keyName, update, ...rest }: any) => (
  <Input maxLength={100} value={record[keyName]} onChange={(e) => update(e.target.value)} {...rest} />
);
const DefaultValue = ({ record, valueName, update, ...rest }: any) => (
  <Input value={record[valueName]} onChange={(e) => update(e.target.value)} {...rest} />
);
const DefaultOp = ({ index, className = '', deleteIndex, ...rest }: any) => {
  return rest.disabled ? (
    <ErdaIcon type="delete1" className={`not-allowed ${className} mt-2.5`} {...rest} />
  ) : (
    <ErdaIcon
      type="delete1"
      className={`${className} mt-2.5`}
      onClick={() => deleteIndex(index)}
      {...rest}
    />
  );
};
const getEmpty = (keyName: string, valueName: string, descName: string, keyDesc: string) => ({
  [keyName]: '',
  [valueName]: '',
  [descName]: '',
  [keyDesc]: '',
});

const lastIsEmpty = (list: any[], keyName: string, valueName: string, descName: string, keyDesc: string) => {
  const lastItem = last(list);
  return lastItem && !lastItem[keyName] && !lastItem[valueName] && !lastItem[descName] && !lastItem[keyDesc];
};

/**
 * key-value基础组件
 * @param value [{ key, value }]
 * @param onChange 参数同value格式
 * @param keyName 自定义key使用的字段名
 * @param valueName 自定义value使用的字段名
 * @param keyDesc 自定义keyDesc使用的字段名
 * @param KeyComp 自定义Key组件，默认为Input
 * @param DescComp 自定义 Desc 组件，无默认
 * @param ValueComp 自定义Value组件，默认为Input
 * @param OpComp 自定义操作组件，默认为一个删除图标
 * @param compProps 其他传给所有Comp的prop，例如disabled
 * @param autoAppend 最后一个不为空时自动追加一行
 * @param emptyHolder 整体为空时自动追加一行
 * @param children 传入一个方法，参数包含：
  ```
    * CompList, // 组件列表 [{ Key, Value, Op }]
    * fullData, // 数据 [{ key, value }], 和传入value的区别是可能包含一个空的行
  ```
 */
const KVPair = ({
  children,
  value,
  onChange,
  keyName = 'key',
  descName = 'desc',
  valueName = 'value',
  keyDesc = 'keyDesc',
  KeyComp,
  DescComp,
  ValueComp,
  KeyDescComp,
  OpComp,
  compProps = {},
  autoAppend = false,
  emptyHolder = false,
}: IKVProp) => {
  const initValue: any[] = value || [];
  const [fullData, setData] = React.useState(initValue);

  React.useEffect(() => {
    const emptyItem = getEmpty(keyName, valueName, descName, keyDesc);

    if (emptyHolder && !initValue.length) {
      initValue.push(emptyItem);
    } else if (autoAppend && !lastIsEmpty(initValue, keyName, valueName, descName, keyDesc)) {
      initValue.push(emptyItem);
    }
    setData([...initValue]);
  }, [autoAppend, descName, emptyHolder, initValue, keyName, value, valueName, keyDesc]);

  const updateValue = (idx: number, key: string, val: string) => {
    const newData = cloneDeep(fullData);
    newData[idx][key] = val;
    if (autoAppend && !lastIsEmpty(newData, keyName, valueName, descName, keyDesc)) {
      newData.push(getEmpty(keyName, valueName, descName, keyDesc));
    }
    setData([...newData]);
    onChange(newData);
  };

  const deleteIndex = (index: number) => {
    const newList = fullData.filter((_, i) => i !== index);
    setData(newList);
    onChange(newList);
  };

  const CompList: IKVRecord[] = [];

  const Key = KeyComp || DefaultKey;
  const Value = ValueComp || DefaultValue;
  const Op = OpComp || DefaultOp;

  map(fullData, (item: any, i: number) => {
    CompList.push({
      Key: (
        <Key
          key={i}
          index={i}
          record={item}
          keyName={keyName}
          update={(val: string) => updateValue(i, keyName, val.trim())}
          {...compProps}
        />
      ),
      Value: (
        <Value
          key={i}
          index={i}
          record={item}
          valueName={valueName}
          update={(val: string) => updateValue(i, valueName, val.trim())}
          {...compProps}
        />
      ),
      Desc: DescComp ? (
        <DescComp
          key={i}
          index={i}
          record={item}
          descName={descName}
          update={(val: string) => updateValue(i, descName, val.trim())}
          {...compProps}
        />
      ) : null,
      KeyDescComp: KeyDescComp ? (
        <KeyDescComp
          key={i}
          index={i}
          record={item}
          keyDesc={keyDesc}
          update={(val: string) => updateValue(i, keyDesc, val)}
          {...compProps}
        />
      ) : null,
      Op: (
        <Op
          key={i}
          index={i}
          record={item}
          deleteIndex={deleteIndex}
          className="text-base hover-active"
          {...compProps}
        />
      ),
    });
  });

  return children({ fullData, CompList });
};

export default KVPair;
