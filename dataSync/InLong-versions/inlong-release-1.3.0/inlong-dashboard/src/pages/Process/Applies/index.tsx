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

import React, { useState } from 'react';
import { State } from '@/models';
import HighTable from '@/components/HighTable';
import { useRequest, useSelector } from '@/hooks';
import { defaultSize } from '@/configs/pagination';
import { getFilterFormContent, getColumns } from './config';

export const activedName = 'applies';

const Comp: React.FC = () => {
  const userName = useSelector<State, State['userName']>(state => state.userName);

  const [options, setOptions] = useState({
    pageSize: defaultSize,
    pageNum: 1,
  });

  const { data, loading } = useRequest(
    {
      url: '/workflow/listProcess',
      params: {
        ...options,
        applicant: userName,
        includeCurrentTask: true,
      },
    },
    {
      refreshDeps: [options],
    },
  );

  const onChange = ({ current: pageNum, pageSize }) => {
    setOptions(prev => ({
      ...prev,
      pageNum,
      pageSize,
    }));
  };

  const onFilter = allValues => {
    setOptions(prev => ({
      ...prev,
      ...allValues,
      pageNum: 1,
    }));
  };

  const pagination = {
    pageSize: options.pageSize,
    current: options.pageNum,
    total: data?.total,
  };

  return (
    <HighTable
      filterForm={{
        content: getFilterFormContent(options),
        onFilter,
      }}
      table={{
        columns: getColumns(activedName),
        rowKey: 'id',
        dataSource: data?.list,
        pagination,
        loading,
        onChange,
      }}
    />
  );
};

export default Comp;
