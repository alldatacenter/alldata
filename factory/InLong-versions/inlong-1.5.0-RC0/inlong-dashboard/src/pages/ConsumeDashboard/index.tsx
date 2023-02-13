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

import React, { useCallback, useState } from 'react';
import { Button, Card, Modal, message } from 'antd';
import { PageContainer, Container } from '@/components/PageContainer';
import HighTable from '@/components/HighTable';
import { DashboardCardList } from '@/components/DashboardCard';
import { useRequest, useHistory } from '@/hooks';
import request from '@/utils/request';
import { defaultSize } from '@/configs/pagination';
import { dashCardList, useColumns } from './config';
import { statusList, lastConsumerStatusList } from '@/metas/consumes/common/status';
import i18n from '@/i18n';
import { useDefaultMeta } from '@/metas';

const Comp: React.FC = () => {
  const { options: consumes } = useDefaultMeta('consume');

  const history = useHistory();
  const [options, setOptions] = useState({
    // keyword: '',
    // status: '',
    // mqType: '',
    pageSize: defaultSize,
    pageNum: 1,
  });

  const { data: summary = {} } = useRequest({
    url: '/consume/countStatus',
  });

  const {
    data,
    loading,
    run: getList,
  } = useRequest(
    {
      url: '/consume/list',
      params: options,
    },
    {
      refreshDeps: [options],
    },
  );

  const onDelete = async ({ id }) => {
    Modal.confirm({
      title: i18n.t('basic.DeleteConfirm'),
      onOk: async () => {
        await request({
          url: `/consume/delete/${id}`,
          method: 'DELETE',
        });
        await getList();
        message.success(i18n.t('basic.DeleteSuccess'));
      },
    });
  };

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

  const dashboardList = dashCardList.map(item => ({
    ...item,
    title: summary[item.dataIndex] || 0,
  }));

  const columns = useColumns({ onDelete });

  const getFilterFormContent = useCallback(
    defaultValues => [
      {
        type: 'inputsearch',
        name: 'keyword',
        initialValue: defaultValues.keyword,
        props: {
          allowClear: true,
        },
      },
      {
        type: 'select',
        name: 'status',
        label: i18n.t('basic.Status'),
        initialValue: defaultValues.status,
        props: {
          allowClear: true,
          options: statusList,
          dropdownMatchSelectWidth: false,
        },
      },
      {
        type: 'select',
        name: 'lastConsumeStatus',
        label: i18n.t('pages.ConsumeDashboard.config.OperatingStatus'),
        initialValue: defaultValues.lastConsumeStatus,
        props: {
          allowClear: true,
          options: lastConsumerStatusList,
          dropdownMatchSelectWidth: false,
        },
      },
      {
        type: 'select',
        name: 'mqType',
        label: i18n.t('meta.Consume.MQType'),
        initialValue: defaultValues.mqType,
        props: {
          allowClear: true,
          options: consumes.filter(x => x.value),
          dropdownMatchSelectWidth: false,
        },
      },
    ],
    [consumes],
  );

  return (
    <PageContainer useDefaultBreadcrumb={false} useDefaultContainer={false}>
      <Container>
        <DashboardCardList dataSource={dashboardList} />
      </Container>

      <Container>
        <Card>
          <HighTable
            suffix={
              <Button type="primary" onClick={() => history.push('/consume/create')}>
                {i18n.t('pages.ConsumeCreate.NewSubscribe')}
              </Button>
            }
            filterForm={{
              content: getFilterFormContent(options),
              onFilter,
            }}
            table={{
              columns,
              rowKey: 'id',
              dataSource: data?.list,
              pagination,
              loading,
              onChange,
            }}
          />
        </Card>
      </Container>
    </PageContainer>
  );
};

export default Comp;
