/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * A select that can automatically initiate asynchronous (cooperating with useRequest) to obtain drop-down list data
 */
import React, { useMemo, useState, useEffect } from 'react';
import { Select, Space, Input, Spin } from 'antd';
import type { SelectProps, OptionProps } from 'antd/es/select';
import { useRequest } from '@/hooks';
import debounce from 'lodash/debounce';
import i18n from '@/i18n';

// example options: {
//   requestService: '/group/listAll',
//   requestParams: {
//     formatResult: result =>
//       result.map(item => ({
//         label: item.name,
//         value: item.value,
//       })),
//   },
//   requestAuto: false // Whether to automatically initiate a request when the component is mounted, by default when the drop-down box is expanded
// },
export interface HighSelectProps extends Omit<SelectProps<any>, 'options'> {
  options?:
    | OptionProps
    | {
        requestService: unknown;
        requestParams?: unknown;
        requestAuto?: boolean;
        requestTrigger?: ('onOpen' | 'onSearch')[];
      };
  asyncValueLabel?: string;
  useInput?: boolean;
  useInputProps?: Record<string, unknown>;
  addonAfter?: React.ReactNode;
}

const HighSelect: React.FC<HighSelectProps> = ({
  options,
  asyncValueLabel,
  useInput = false,
  useInputProps,
  addonAfter,
  ...rest
}) => {
  const [diyWatcher, setDiyWatcher] = useState(true);
  const [diyState, setDiyState] = useState(false);

  const {
    data: list = [],
    loading,
    run: getList,
  } = useRequest(options?.requestService, {
    manual: !options?.requestAuto,
    ready: !!options?.requestService && (options?.requestParams?.ready ?? true),
    ...options?.requestParams,
  });

  const debounceGetList = debounce(value => {
    getList(value);
  }, 300);

  const optionList = useMemo(() => {
    const output = Array.isArray(options) ? options : list;

    return useInput
      ? output.concat({
          label: i18n.t('components.HighSelect.Customize'),
          value: '__DIYState',
        })
      : output;
  }, [list, options, useInput]);

  useEffect(() => {
    if (diyWatcher && optionList.every(item => item.value !== rest.value) && !diyState) {
      setDiyState(true);
    }
  }, [diyWatcher, rest.value, optionList, diyState]);

  if (rest.mode === 'tags') {
    return <Select {...rest} />;
  }

  const onSearch = value => {
    if (options?.requestService && options?.requestTrigger?.includes('onSearch')) {
      debounceGetList(value);
    }
    if (rest.onSearch) {
      rest.onSearch(value);
    }
  };

  const onDropdownVisibleChange = (open: boolean) => {
    if (
      open &&
      options?.requestService &&
      (!options?.requestTrigger || options?.requestTrigger?.includes('onOpen'))
    ) {
      getList();
    }
    if (rest.onDropdownVisibleChange) {
      rest.onDropdownVisibleChange(open);
    }
  };

  const onValueChange = value => {
    const optionItem = Array.isArray(value)
      ? optionList.filter(item => value.includes(item.value))
      : optionList.find(item => item.value === value);
    if (typeof rest.onChange === 'function') {
      rest.onChange(value, optionItem);
    }
  };

  const onSelectChange = value => {
    const newDiyState = value === '__DIYState';
    if (diyState !== newDiyState) setDiyState(newDiyState);
    if (newDiyState) {
      setDiyWatcher(false);
      return;
    }

    onValueChange(value);
  };

  const onInputChange = e => {
    onValueChange(e.target.value);
  };

  const showSearch =
    rest.showSearch || options?.requestTrigger?.includes('onSearch')
      ? rest.showSearch
      : optionList.length > 5;

  const SelectComponent = (
    <Select
      {...rest}
      showSearch={showSearch}
      placeholder={
        showSearch
          ? rest.placeholder || i18n.t('components.HighSelect.SearchPlaceholder')
          : rest.placeholder
      }
      dropdownRender={
        rest.dropdownRender ||
        (menu => (loading ? <Spin size="small" style={{ margin: '0 15px' }} /> : menu))
      }
      notFoundContent={rest.notFoundContent || (loading ? <span /> : undefined)}
      onSearch={options?.requestTrigger?.includes('onSearch') ? onSearch : rest.onSearch}
      onDropdownVisibleChange={onDropdownVisibleChange}
      onChange={onSelectChange}
      value={
        useInput && diyState
          ? '__DIYState'
          : (!optionList.length && rest.value && asyncValueLabel) || rest.value
      }
      options={optionList.map(item => ({
        label: item.label,
        value: item.value,
        title: item.title,
        disabled: item.disabled,
      }))}
    />
  );

  return useInput || addonAfter ? (
    <Space>
      {SelectComponent}
      {useInput && diyState && (
        <Input {...useInputProps} value={rest.value} onChange={onInputChange} />
      )}
      {addonAfter}
    </Space>
  ) : (
    SelectComponent
  );
};

export default HighSelect;
