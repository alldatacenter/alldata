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

import React, { useState } from 'react';
import { Select, Cascader } from 'antd';
import { useUpdateEffect } from 'react-use';
import { get } from 'lodash';
import './index.scss';

// const mockData = {
//   key: 'foo',
//   label: '标题',
//   emptyText: '全部',
//   type: 'multipleSelect',
//   data: [
//     {
//       type: 'select',
//       placeholder: '请选择类型',
//       optionLabelProp: 'label',
//       showSearchByValueAndLabel: true,
//       options: [
//         {
//           value: 'key1',
//           label: '前置场景出参',
//           optionLabel: '选择了前置场景出参',
//         },
//         {
//           value: 'key2',
//           label: 'Mock',
//           optionLabel: '选择了Mock',
//         },
//         {
//           value: 'key3',
//           label: '固定值',
//           optionLabel: '选择了固定值',
//         },
//       ],
//     },
//     {
//       type: 'select',
//       placeholder: '可快速过滤路径',
//       options: [
//         { value: 'key3', label: '接口A' },
//         { value: 'key4', label: '接口B' },
//       ],
//     },
//     {
//       type: 'cascader',
//       showSearchByValue: true,
//       placeholder: '级联',
//       options: [
//         {
//           value: 'zhejiang',
//           label: 'ZheJiang',
//           children: [
//             {
//               value: 'hangzhou',
//               label: 'Hangzhou',
//               children: [
//                 {
//                   value: 'xihu',
//                   label: 'West Lake',
//                 },
//                 {
//                   value: 'xiasha',
//                   label: 'Xia Sha',
//                   disabled: true,
//                 },
//               ],
//             },
//           ],
//         },
//         {
//           value: 'jiangsu',
//           label: 'Jiangsu',
//           children: [
//             {
//               value: 'nanjing',
//               label: 'Nanjing',
//               children: [
//                 {
//                   value: 'zhonghuamen',
//                   label: 'Zhong Hua men',
//                 },
//               ],
//             },
//           ],
//         },
//       ],
//     },
//   ],
// };

// <MultiSelect data={mockData} />

const { Option } = Select;

interface ISingProps {
  type: 'select' | 'cascader';
  showSearchByValueAndLabel?: boolean;
  showSearchByValue?: boolean;
  allowInput?: boolean;
  value: string;
  onChange: (data: string) => void;
  options: Array<{
    value: string;
    label: string;
    text: string;
  }>;
}

function SingleSelect(props: ISingProps) {
  const {
    options,
    showSearchByValueAndLabel,
    type,
    showSearchByValue,
    allowInput,
    value: propsVal,
    onChange,
    ...rest
  } = props;
  const [filteredOptions, setFilteredOptions] = useState(options);
  const [value, setValue] = useState(propsVal) as any;

  function searchByValueAndLabel(inputValue: any) {
    const opts = options.filter((x: any) => {
      const { label = '', value: v = '', text = '' } = x;
      return (
        label.toLowerCase().includes(inputValue) ||
        v.toLowerCase().includes(inputValue) ||
        text.toLowerCase().includes(inputValue)
      );
    });
    setFilteredOptions(opts);
  }

  function filter(inputValue: any, path: any, names: any) {
    return path.some((option: any) => option.label.toLowerCase().indexOf(inputValue.toLowerCase()) > -1);
  }

  useUpdateEffect(() => {
    onChange(value);
  }, [value]);

  function displayRender(label: any) {
    return label[label.length - 1];
  }
  if (type === 'select') {
    return (
      <Select
        value={value}
        showSearch={showSearchByValueAndLabel}
        filterOption={!showSearchByValueAndLabel}
        onSelect={(selected) => {
          setValue(selected);
          setFilteredOptions(options);
        }}
        style={{ width: 240 }}
        onBlur={() => {
          // blur 的时候重置options，清空过滤条件
          setFilteredOptions(options);
        }}
        onSearch={(v) => {
          // 避免onBlur的时候将value回填成 ''
          if (allowInput && v !== '') {
            setValue(v);
            setFilteredOptions(options);
          }

          if (showSearchByValueAndLabel) {
            searchByValueAndLabel(v.toLowerCase());
          }
        }}
        {...rest}
      >
        {filteredOptions.map((item: any) => (
          <Option key={item.value} value={item.value}>
            <div>{item.label}</div>
            <div>{item.text}</div>
          </Option>
        ))}
      </Select>
    );
  }

  if (type === 'cascader') {
    return (
      <Cascader
        options={options}
        showSearch={showSearchByValue ? { filter } : false}
        displayRender={displayRender}
        {...rest}
      />
    );
  }

  return null;
}

interface IProps {
  data: Array<
    {
      key: string;
    } & Omit<ISingProps, 'onChange' | 'value'>
  >;
  value: {
    [k: string]: string;
  };
  onChangeMap: {
    [k: string]: (value: string) => void;
  };
  onChange: (data: { [k: string]: string }) => void;
}

export default function MultiSelect(props: IProps) {
  const { data = [], value, onChangeMap, onChange: propsOnChange } = props;
  const onChange = (k: string) => (v: any) => {
    propsOnChange({ ...value, [k]: v });
    if (onChangeMap[k]) {
      onChangeMap[k](v);
    }
  };
  return (
    <div className="flex justify-between items-center">
      {data.map((item: any, index: any) => {
        return <SingleSelect {...item} key={`${index}`} onChange={onChange(item.key)} value={get(value, item.key)} />;
      })}
    </div>
  );
}
