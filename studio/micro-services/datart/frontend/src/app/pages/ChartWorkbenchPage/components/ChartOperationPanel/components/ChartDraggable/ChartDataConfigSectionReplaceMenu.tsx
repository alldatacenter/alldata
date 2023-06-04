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

import { Menu } from 'antd';
import { ChartDataSectionField } from 'app/types/ChartConfig';
import { ChartDataViewMeta } from 'app/types/ChartDataViewMeta';
import { FC } from 'react';
import { getDefaultAggregate, updateDataConfigByField } from './utils';

export const ChartDataConfigSectionReplaceMenu: FC<{
  uid;
  type;
  viewFields: ChartDataViewMeta[];
  config;
  ancestors;
  columnConfig: ChartDataSectionField;
  onConfigChanged;
}> = ({
  uid,
  type,
  viewFields,
  ancestors,
  config,
  columnConfig,
  onConfigChanged,
}) => {
  const handleFieldConfigChanged = (item: ChartDataViewMeta) => {
    const newFieldConfig: ChartDataSectionField = {
      ...item,
      aggregate: undefined,
      category: item.category as any,
      colName: item.name,
      type: item.type!,
      uid: columnConfig.uid,
    };
    newFieldConfig.aggregate = getDefaultAggregate(newFieldConfig, config);
    const newConfig = updateDataConfigByField(
      columnConfig.uid!,
      config,
      newFieldConfig,
      columnConfig,
    );

    onConfigChanged?.(ancestors, newConfig, true);
  };

  const renderMenuItem = (item: ChartDataViewMeta) => {
    if (item.children && item.children.length) {
      return (
        <Menu.SubMenu key={item.name} title={item.name}>
          {item.children.map(item => renderMenuItem(item))}
        </Menu.SubMenu>
      );
    } else {
      return (
        <Menu.Item
          key={item.name}
          onClick={() => handleFieldConfigChanged(item)}
        >
          {item.name}
        </Menu.Item>
      );
    }
  };
  return <Menu>{viewFields.map(item => renderMenuItem(item))}</Menu>;
};
