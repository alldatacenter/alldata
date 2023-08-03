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
import { Button, Card } from 'antd';
import { PageContainer, Container } from '@/ui/components/PageContainer';
import HighTable from '@/ui/components/HighTable';
import { useRequest, useSelector } from '@/ui/hooks';
import { useTranslation } from 'react-i18next';
import { defaultSize } from '@/configs/pagination';
import DetailModal from './DetailModal';
import { getFilterFormContent, getColumns } from './config';
import { State } from '@/core/stores';

const Comp: React.FC = () => {
  const { t } = useTranslation();

  const tenantList = useSelector<State, State['tenantList']>(state => state.tenantList);

  const [options, setOptions] = useState({
    keyword: '',
    pageSize: defaultSize,
    pageNum: 1,
    tenantList: tenantList,
  });

  const [createModal, setCreateModal] = useState<Record<string, unknown>>({
    open: false,
  });

  const {
    data,
    loading,
    run: getList,
  } = useRequest(
    {
      url: '/role/tenant/list',
      method: 'POST',
      data: options,
    },
    {
      refreshDeps: [options],
    },
  );

  const onEdit = ({ id }) => {
    setCreateModal({
      open: true,
      id,
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

  return (
    <PageContainer useDefaultBreadcrumb={false} useDefaultContainer={false}>
      <Container>
        <Card>
          <HighTable
            suffix={
              <Button type="primary" onClick={() => setCreateModal({ open: true })}>
                {t('pages.TenantRole.New')}
              </Button>
            }
            filterForm={{
              content: getFilterFormContent(),
              onFilter,
            }}
            table={{
              columns: getColumns({ onEdit }),
              rowKey: 'id',
              dataSource: data?.list,
              pagination,
              loading,
              onChange,
            }}
          />
        </Card>
      </Container>

      <DetailModal
        {...createModal}
        open={createModal.open as boolean}
        onOk={async () => {
          await getList();
          setCreateModal({ open: false });
        }}
        onCancel={() => setCreateModal({ open: false })}
      />
    </PageContainer>
  );
};

export default Comp;
