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
import dopStore from 'dop/stores';
import { useMount, useUpdateEffect } from 'react-use';
import { Select } from 'antd';
import i18n from 'i18n';

const { Option } = Select;
const dataSourceTypeMap = {
  mysql: 'MySQL',
  redis: 'Redis',
};

export const DataSourceSelector = (props: any) => {
  const [options, setOptions] = React.useState([]);
  const { projectId, dataSourceType, onChange, value, ...rest } = props;

  useMount(() => {
    getData();
  });

  const getData = () => {
    if (projectId && dataSourceType) {
      // 暂时只取测试环境的addon, Custom/AliCloud-Rds/AliCloud-Redis 为自定义类型，自定义类型暂为redis和mysql共享
      dopStore
        .getDataSourceAddons({
          projectId,
          displayName: [dataSourceTypeMap[dataSourceType], 'Custom', 'AliCloud-Rds', 'AliCloud-Redis'],
        })
        .then((res: any) => {
          setOptions(
            (res || []).map((item: any) => ({
              value: item.instanceId,
              label: `${item.name}${item.tag ? `(${item.tag})` : ''}`,
            })),
          );
        });
    }
  };

  useUpdateEffect(() => {
    onChange(undefined);
    getData();
  }, [projectId, dataSourceType]);

  const dataSourceOptionRender = (item: any) => {
    return (
      <Option key={item.value} value={item.value}>
        <span className="ml-2" title={item.label}>
          {item.label}
        </span>
      </Option>
    );
  };

  return (
    <Select
      className="w-full"
      notFoundContent={i18n.t('common:no data')}
      showArrow={false}
      showSearch
      optionFilterProp="children"
      onChange={onChange}
      filterOption={(input: any, option: any) => {
        return option.props.children.props.children.toLowerCase().indexOf(input.toLowerCase()) >= 0;
      }}
      value={value}
      placeholder={i18n.t('please select')}
      {...rest}
    >
      {options.map(dataSourceOptionRender)}
    </Select>
  );
};
