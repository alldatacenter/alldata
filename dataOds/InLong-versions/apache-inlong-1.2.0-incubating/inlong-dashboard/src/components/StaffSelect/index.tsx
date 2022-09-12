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

import React, { useState, useEffect } from 'react';
import { Select, Spin, Tag } from 'antd';
import type { SelectProps, OptionProps } from 'antd/es/select';
import { useTranslation } from 'react-i18next';
import { State } from '@/models';
import { useRequest, useSelector } from '@/hooks';
import debounce from 'lodash/debounce';

export interface StaffSelectProps extends SelectProps<any> {
  onChange?: (value: string | string[]) => void;
  // Whether to delete the currently logged in user
  currentUserClosable?: boolean;
}

const cacheKey = 'STAFFS_CACHE';

function getCache() {
  try {
    const data = JSON.parse(window.localStorage.getItem(cacheKey) || '[]');
    return Array.isArray(data) ? data : [];
  } catch {
    return [];
  }
}

function setCache(data: OptionProps[]) {
  const oldData = getCache();
  const oldDataMap = oldData.reduce(
    (acc, cur) => ({
      ...acc,
      [cur.value]: 1,
    }),
    {},
  );

  const newData = data
    .filter(item => !oldDataMap[item.value] && item.value)
    .map(item => ({
      label: item.label,
      value: item.value,
    }))
    .concat(oldData)
    .slice(0, 50);

  window.localStorage.setItem(cacheKey, JSON.stringify(newData));
}

const StaffSelect: React.FC<StaffSelectProps> = ({
  value,
  onChange,
  currentUserClosable = true,
  ...rest
}) => {
  const { t } = useTranslation();

  const { userName } = useSelector<State, State>(state => state);

  const [currentValue, setCurrentValue] = useState<string | string[]>(value);

  useEffect(() => {
    if (value && value !== currentValue) {
      setCurrentValue(value);
    }
    // eslint-disable-next-line
  }, [value]);

  const onValueChange = (newValue: string | string[], option: OptionProps | OptionProps[]) => {
    if (!currentUserClosable) {
      if (typeof newValue === 'string') {
        newValue = userName;
      } else {
        !newValue.includes(userName) && newValue.unshift(userName);
      }
    }

    setCurrentValue(newValue);

    if (option) {
      const data = Array.isArray(option) ? option : [option];
      setCache(data);
    }

    if (onChange && newValue !== value) {
      onChange(newValue);
    }
  };

  const { data: staffList, loading, run: getStaffList } = useRequest(
    (userName = '') => ({
      url: '/user/listAllUsers',
      params: {
        userName,
      },
    }),
    {
      manual: true,
      initialData: getCache(),
      formatResult: result =>
        result.list?.map(item => ({
          label: item.name,
          value: item.name,
        })),
    },
  );

  const onSearch = debounce((value: string) => {
    if (value) {
      getStaffList(value);
    }
  }, 300);

  const dropdownRender = menu =>
    loading ? <Spin size="small" style={{ margin: '0 15px' }} /> : menu;

  return (
    <Select
      placeholder={t('components.StaffSelect.Placeholder')}
      showSearch
      allowClear
      {...rest}
      optionLabelProp="value"
      filterOption={false}
      value={currentValue}
      dropdownRender={dropdownRender}
      notFoundContent={loading ? <span /> : undefined}
      onSearch={onSearch}
      onChange={onValueChange}
      options={staffList}
      tagRender={props => (
        <Tag
          closable={props.value === userName ? currentUserClosable : true}
          onClose={props.onClose}
          style={{ fontSize: '14px', background: '#f5f5f5', border: 'none' }}
        >
          {props.label}
        </Tag>
      )}
    />
  );
};

export default StaffSelect;
