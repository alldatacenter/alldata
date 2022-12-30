/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Select } from 'antd';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo, useMemo } from 'react';
import { isEmpty, isFunc } from 'utils/object';
import { ItemLayoutProps } from '../types';
import { itemLayoutComparer } from '../utils';
import { BW } from './components/BasicWrapper';

const BasicSelector: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({
    ancestors,
    translate: t = title => title,
    data: row,
    dataConfigs,
    onChange,
  }) => {
    const { comType, options, ...rest } = row;
    const { translateItemLabel, hideLabel, ...restOptions } = options || {};

    const handleSelectorValueChange = value => {
      onChange?.(ancestors, value, options?.needRefresh);
    };

    const cachedDataConfigs = useMemo(
      () => dataConfigs?.map(col => ({ ...col })),
      [dataConfigs],
    );

    const safeInvokeAction = () => {
      let results: any[] = [];
      try {
        results = isFunc(row?.options?.getItems)
          ? row?.options?.getItems?.call(
              Object.create(null),
              cachedDataConfigs,
            ) || []
          : row?.options?.items || [];
      } catch (error) {
        console.error(`BasicSelector | invoke action error ---> `, error);
      } finally {
        return results;
      }
    };

    return (
      <BW label={!hideLabel ? t(row.label, true) : ''}>
        <Select
          className="datart-ant-select"
          dropdownMatchSelectWidth
          {...rest}
          {...restOptions}
          defaultValue={rest.default}
          placeholder={t('select')}
          options={safeInvokeAction()?.map(o => {
            const label = isEmpty(o['label']) ? o : o.label;
            const value = isEmpty(o['value']) ? o : o.value;
            return {
              label: !!translateItemLabel ? t(label, true) : label,
              value,
            };
          })}
          onChange={handleSelectorValueChange}
        />
      </BW>
    );
  },
  itemLayoutComparer,
);

export default BasicSelector;
