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

import React, { useEffect, useState } from 'react';
import { Divider, Dropdown, Input, MenuProps, message, Space, theme } from 'antd';
import { useDispatch, useRequest, useSelector } from '@/ui/hooks';
import { State } from '@/core/stores';
import { useTranslation } from 'react-i18next';
import { useLocalStorage } from '@/core/utils/localStorage';

const { useToken } = theme;

const Comp: React.FC = () => {
  const { t } = useTranslation();
  const tenant = useSelector<State, State['tenant']>(state => state.tenant);
  const userName = useSelector<State, State['userName']>(state => state.userName);
  const roles = useSelector<State, State['roles']>(state => state.roles);
  const [getLocalStorage, setLocalStorage, removeLocalStorage] = useLocalStorage('tenant');

  const { token } = useToken();
  const contentStyle = {
    backgroundColor: token.colorBgElevated,
    borderRadius: token.borderRadiusLG,
    boxShadow: token.boxShadowSecondary,
  };
  const [filter, setFilter] = useState(true);
  const [filterData, setFilterData] = useState([]);
  const [data, setData] = useState([]);

  const defaultOptions = {
    keyword: '',
    pageSize: 10,
    pageNum: 1,
  };
  const dispatch = useDispatch();

  const [options, setOptions] = useState(defaultOptions);

  const { run: getStreamData } = useRequest(
    {
      url: '/tenant/list',
      method: 'POST',
      data: {
        listByLoginUser: true,
        ...options,
      },
    },
    {
      refreshDeps: [options],
      manual: userName !== undefined ? false : true,
      onSuccess: result => {
        const tenant = [];
        const tenantList = [];
        result.list.map(item => {
          tenantList.push(item.name);
          tenant.push({
            label: item.name,
            key: item.name,
          });
        });
        dispatch({
          type: 'setTenantInfo',
          payload: {
            tenantList: tenantList,
          },
        });
        setData(tenant);
      },
    },
  );

  const { run: getData } = useRequest(
    name => ({
      url: `/user/currentUser`,
      method: 'post',
      headers: {
        tenant: name,
      },
    }),
    {
      manual: true,
    },
  );

  const onClick: MenuProps['onClick'] = ({ key }) => {
    getData(key);
    setLocalStorage({ name: key });
    message.success(t('components.Layout.Tenant.Success'));
    window.location.reload();
  };

  const onFilter = allValues => {
    setFilterData(data.filter(item => item.key === allValues));
    setFilter(false);
  };

  useEffect(() => {
    if (userName !== undefined) {
      getStreamData();
    }
  }, [userName]);

  return (
    <>
      <Dropdown
        menu={{ items: filter ? data : filterData, onClick }}
        placement="bottomLeft"
        dropdownRender={menu => (
          <div style={contentStyle}>
            {React.cloneElement(menu as React.ReactElement, { style: { boxShadow: 'none' } })}
            <Divider style={{ margin: 0 }} />
            <Space style={{ padding: 8 }}>
              <Input.Search allowClear onSearch={onFilter} style={{ width: 130 }} />
            </Space>
          </div>
        )}
      >
        <span style={{ fontSize: 14 }}>{tenant}</span>
      </Dropdown>
    </>
  );
};

export default Comp;
