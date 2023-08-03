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

import React, { useCallback, useEffect, useState } from 'react';
import i18n from '@/i18n';
import { Button, Card, Modal, message } from 'antd';
import { PageContainer, Container } from '@/ui/components/PageContainer';
import HighTable from '@/ui/components/HighTable';
import { DashboardCardList } from '@/ui/components/DashboardCard';
import request from '@/core/utils/request';
import { useRequest, useHistory } from '@/ui/hooks';
import { defaultSize } from '@/configs/pagination';
import { GroupLogs } from '@/ui/components/GroupLogs';
import { dashCardList, useColumns } from './config';
import { statusList } from '@/plugins/groups/common/status';
import { useDefaultMeta } from '@/plugins';

const Comp: React.FC = () => {
  const { options: groups } = useDefaultMeta('group');

  const history = useHistory();
  const [options, setOptions] = useState({
    pageSize: defaultSize,
    pageNum: 1,
    inlongGroupMode: 1,
  });

  const [groupLogs, setGroupLogs] = useState({
    open: false,
    inlongGroupId: '',
    inlongGroupMode: false,
  });

  const { data: summary = {} } = useRequest({
    url: '/group/countByStatus',
    params: {
      inlongGroupMode: 1,
    },
  });

  const {
    data,
    loading,
    run: getList,
  } = useRequest(
    {
      url: '/group/list',
      method: 'POST',
      data: options,
    },
    {
      refreshDeps: [options],
    },
  );

  const onDelete = ({ inlongGroupId }) => {
    Modal.confirm({
      title: i18n.t('pages.GroupDashboard.ConfirmDelete'),
      onOk: async () => {
        await request({
          url: `/group/delete/${inlongGroupId}`,
          method: 'DELETE',
        });
        await getList();
        message.success(i18n.t('pages.GroupDashboard.SuccessfullyDeleted'));
      },
    });
  };

  const openModal = ({ inlongGroupId }) => {
    setGroupLogs({ open: true, inlongGroupId: inlongGroupId, inlongGroupMode: false });
  };

  const onRestart = ({ inlongGroupId }) => {
    Modal.confirm({
      title: i18n.t('pages.GroupDashboard.ConfirmRestart'),
      onOk: async () => {
        await request({
          url: `/group/restartProcess/${inlongGroupId}`,
          method: 'POST',
          data: {
            groupId: inlongGroupId,
          },
        });
        await getList();
        message.success(i18n.t('pages.GroupDashboard.SuccessfullyRestart'));
      },
    });
  };

  const onStop = ({ inlongGroupId }) => {
    Modal.confirm({
      title: i18n.t('pages.GroupDashboard.ConfirmStop'),
      onOk: async () => {
        await request({
          url: `/group/suspendProcess/${inlongGroupId}`,
          method: 'POST',
          data: {
            groupId: inlongGroupId,
          },
        });
        await getList();
        message.success(i18n.t('pages.GroupDashboard.SuccessfullyStop'));
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

  const columns = useColumns({ onDelete, openModal, onRestart, onStop });

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
    ],
    [groups],
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
              <Button type="primary" onClick={() => history.push('/sync/create')}>
                {i18n.t('pages.SynchronizeDashboard.Create')}
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

      <GroupLogs
        {...groupLogs}
        onOk={() => setGroupLogs({ open: false, inlongGroupId: '', inlongGroupMode: false })}
        onCancel={() => setGroupLogs({ open: false, inlongGroupId: '', inlongGroupMode: false })}
      />
    </PageContainer>
  );
};

export default Comp;
