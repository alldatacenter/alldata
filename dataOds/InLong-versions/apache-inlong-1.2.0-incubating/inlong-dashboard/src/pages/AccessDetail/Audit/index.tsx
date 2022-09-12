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

import React, { useMemo, useState } from 'react';
import FormGenerator, { useForm } from '@/components/FormGenerator';
import HighTable from '@/components/HighTable';
import { useRequest } from '@/hooks';
import { timestampFormat } from '@/utils';
import Charts from '@/components/Charts';
import { CommonInterface } from '../common';
import { getFormContent, toChartData, toTableData, getTableColumns, auditList } from './config';

type Props = CommonInterface;

const Comp: React.FC<Props> = ({ inlongGroupId }) => {
  const [form] = useForm();

  const [query, setQuery] = useState({
    inlongStreamId: '',
    auditIds: auditList.map(item => item.value),
    dt: +new Date(),
  });

  const { data: sourceData = [], run } = useRequest(
    {
      url: '/audit/list',
      params: {
        ...query,
        auditIds: query.auditIds?.join(','),
        dt: timestampFormat(query.dt, 'yyyy-MM-dd'),
        inlongGroupId,
      },
    },
    {
      ready: Boolean(query.inlongStreamId && query.auditIds?.length),
    },
  );

  const sourceDataMap = useMemo(() => {
    const flatArr = sourceData
      .reduce(
        (acc, cur) =>
          acc.concat(
            cur.auditSet.map(item => ({
              ...item,
              auditId: cur.auditId,
            })),
          ),
        [],
      )
      .sort((a, b) => +new Date(a.logTs) - +new Date(b.logTs));
    const output = flatArr.reduce((acc, cur) => {
      if (!acc[cur.logTs]) {
        acc[cur.logTs] = {};
      }
      acc[cur.logTs] = {
        ...acc[cur.logTs],
        [cur.auditId]: cur.count,
      };
      return acc;
    }, {});
    return output;
  }, [sourceData]);

  const onSearch = async () => {
    await form.validateFields();
    run();
  };

  const onDataStreamSuccess = data => {
    const defaultDataStream = data[0]?.value;
    if (defaultDataStream) {
      form.setFieldsValue({ inlongStreamId: defaultDataStream });
      setQuery(prev => ({ ...prev, inlongStreamId: defaultDataStream }));
      run();
    }
  };

  return (
    <>
      <div style={{ marginBottom: 40 }}>
        <FormGenerator
          form={form}
          layout="inline"
          content={getFormContent(inlongGroupId, query, onSearch, onDataStreamSuccess)}
          style={{ marginBottom: 30 }}
          onFilter={allValues =>
            setQuery({
              ...allValues,
              dt: +allValues.dt.$d,
            })
          }
        />
        <Charts height={400} option={toChartData(sourceData, sourceDataMap)} />
      </div>

      <HighTable
        table={{
          columns: getTableColumns(sourceData),
          dataSource: toTableData(sourceData, sourceDataMap),
          rowKey: 'logTs',
        }}
      />
    </>
  );
};

export default Comp;
