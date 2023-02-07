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

import React, { useState, useMemo } from 'react';
import { Card } from 'antd';
import { PageContainer, Container } from '@/components/PageContainer';
import { DashTotalRevert, DashPending, DashRejected, DashCancelled } from '@/components/Icons';
import { DashboardCardList } from '@/components/DashboardCard';
import { useRequest, useHistory, useParams } from '@/hooks';
import i18n from '@/i18n';
import Applies, { activedName as AppliesActivedName } from './Applies';
import Approvals, { activedName as ApprovalsActivedName } from './Approvals';

const tabList = [
  {
    tab: i18n.t('pages.Approvals.MyApproval'),
    key: ApprovalsActivedName,
    content: <Approvals />,
  },
  {
    tab: i18n.t('pages.Approvals.MyApplication'),
    key: AppliesActivedName,
    content: <Applies />,
  },
];

const tabListMap = tabList.reduce(
  (acc, item) => ({
    ...acc,
    [item.key]: item.content,
  }),
  {},
);

const dashCardList = [
  {
    desc: i18n.t('pages.Approvals.status.Ok'),
    dataIndex: 'totalApproveCount',
    icon: <DashTotalRevert />,
  },
  {
    desc: i18n.t('pages.Approvals.status.Rejected'),
    dataIndex: 'totalRejectCount',
    icon: <DashRejected />,
  },
  {
    desc: i18n.t('pages.Approvals.status.Processing'),
    dataIndex: 'totalProcessingCount',
    icon: <DashPending />,
  },
  {
    desc: i18n.t('pages.Approvals.status.Canceled'),
    dataIndex: 'totalCancelCount',
    icon: <DashCancelled />,
  },
];

const Comp: React.FC = () => {
  const history = useHistory();
  const { type } = useParams<Record<string, string>>();

  const [actived, setActived] = useState(type || tabList[0].key);

  const { data: summary = {} } = useRequest({
    url: '/workflow/processSummary',
  });

  const onTabsChange = value => {
    setActived(value);
    history.push({
      pathname: `/process/${value}`,
    });
  };

  const dashboardList = useMemo(
    () =>
      dashCardList.map(item => ({
        ...item,
        title: summary[item.dataIndex] || 0,
      })),
    [summary],
  );

  return (
    <PageContainer useDefaultBreadcrumb={false} useDefaultContainer={false}>
      <Container>
        <DashboardCardList dataSource={dashboardList} />
      </Container>

      <Container>
        <Card
          tabList={tabList}
          activeTabKey={actived}
          onTabChange={key => {
            onTabsChange(key);
          }}
          headStyle={{ border: 'none' }}
          tabProps={{ size: 'middle' }}
        >
          {tabListMap[actived]}
        </Card>
      </Container>
    </PageContainer>
  );
};

export default Comp;
