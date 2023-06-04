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
import type { DefaultOptionType } from 'antd/es/select';
import { State } from '@/core/stores';
import { useSelector } from '@/ui/hooks';
import HighSelect, { HighSelectProps } from '@/ui/components/HighSelect';

export interface UserSelectProps extends HighSelectProps {
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

function setCache(data: DefaultOptionType[]) {
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

const UserSelect: React.FC<UserSelectProps> = ({
  value,
  onChange,
  currentUserClosable = true,
  ...rest
}) => {
  const { userName } = useSelector<State, State>(state => state);

  const [currentValue, setCurrentValue] = useState<string | string[]>(value);

  useEffect(() => {
    if (value && value !== currentValue) {
      setCurrentValue(value);
    }
    // eslint-disable-next-line
  }, [value]);

  const onValueChange = (
    newValue: string | string[],
    option: DefaultOptionType | DefaultOptionType[],
  ) => {
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

  return (
    <HighSelect
      showSearch
      allowClear
      {...rest}
      options={{
        ...rest.options,
        requestTrigger: ['onSearch'],
        requestService: name => ({
          url: '/user/listAll',
          method: 'POST',
          data: {
            name,
          },
        }),
        requestParams: {
          initialData: getCache(),
          formatResult: result =>
            result?.list?.map(item => ({
              ...item,
              label: item.name,
              value: item.name,
              disabled: item.value === userName,
            })),
        },
      }}
      optionLabelProp="value"
      filterOption={false}
      value={currentValue}
      onChange={onValueChange}
    />
  );
};

export default UserSelect;
