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

import React, { useMemo, useRef } from 'react';
import { Button, Card, Descriptions, Modal, message, Space } from 'antd';
import { parse } from 'qs';
import { PageContainer, Container, FooterToolbar } from '@/components/PageContainer';
import { useParams, useRequest, useLocation, useHistory } from '@/hooks';
import i18n from '@/i18n';
import { timestampFormat } from '@/utils';
import request from '@/utils/request';
import Steps from './Steps';
import Access from './Access';
import Consume from './Consume';

const workflowFormat = (applicant, startEvent, taskHistory = []) => {
  const taskHistoryMap = new Map(taskHistory.map(item => [item.name, item]));
  let data = [
    {
      title: i18n.t('pages.ApprovalDetail.SubmitApplication'),
      name: '',
      desc: applicant,
      status: 'COMPLETED',
    },
  ];
  const nextList = [startEvent.next];
  while (nextList.length) {
    const next = nextList.shift();
    data = data.concat(
      next.map(item => {
        if (item.next && item.next.length) {
          nextList.push(item.next);
        }
        return {
          title: nextList.length ? item.displayName : i18n.t('pages.ApprovalDetail.Done'),
          desc: item.approvers?.join(', '),
          name: item.name,
          status: item.status,
          remark: taskHistoryMap.get(item.name)?.remark,
        };
      }),
    );
  }
  return data;
};

const titleNameMap = {
  applies: i18n.t('pages.ApprovalDetail.Requisition'),
  approvals: i18n.t('pages.ApprovalDetail.WaitingForApproval'),
};

const Comp: React.FC = () => {
  const history = useHistory();
  const location = useLocation();

  const { id, type } = useParams<Record<string, string>>();

  const taskId = useMemo(() => parse(location.search.slice(1))?.taskId, [location.search]);

  const formRef = useRef(null);

  const { data = {} } = useRequest({
    url: `/workflow/detail/${id}`,
    params: {
      taskId: taskId,
    },
  });

  const { currentTask, processInfo, workflow, taskHistory } = (data || {}) as any;

  const onApprove = async () => {
    const { remark, form: formData } = await formRef?.current?.onOk();
    const submitData = {
      remark,
    } as any;
    if (formData && Object.keys(formData).length) {
      submitData.form = {
        ...formData,
        formName: currentTask?.formData?.formName,
      };
    }

    await request({
      url: `/workflow/approve/${taskId}`,
      method: 'POST',
      data: submitData,
    });
    history.push('/process/approvals');
    message.success(i18n.t('basic.OperatingSuccess'));
  };

  const onReject = async () => {
    Modal.confirm({
      title: i18n.t('pages.ApprovalDetail.ConfirmReject'),
      onOk: async () => {
        const { remark } = await formRef?.current?.onOk(false);
        await request({
          url: `/workflow/reject/${taskId}`,
          method: 'POST',
          data: {
            remark,
          },
        });
        history.push('/process/approvals');
        message.success(i18n.t('pages.ApprovalDetail.RejectSuccess'));
      },
    });
  };

  const onCancel = async () => {
    Modal.confirm({
      title: i18n.t('pages.ApprovalDetail.ConfirmWithdrawal'),
      onOk: async () => {
        // const { remark } = await formRef?.current?.onOk(false);
        await request({
          url: `/workflow/cancel/` + id,
          method: 'POST',
          data: {
            remark: '',
          },
        });
        history.push('/process/applies');
        message.success(i18n.t('pages.ApprovalDetail.RevokeSuccess'));
      },
    });
  };

  const stepsData = useMemo(() => {
    if (workflow?.startEvent) {
      return workflowFormat(processInfo?.applicant, workflow.startEvent, taskHistory);
    }
    return [];
  }, [workflow, processInfo, taskHistory]);

  const Footer = () => (
    <>
      {type === 'approvals' && currentTask?.status === 'PENDING' && (
        <Space style={{ display: 'flex', justifyContent: 'center' }}>
          <Button type="primary" onClick={onApprove}>
            {i18n.t('pages.ApprovalDetail.Ok')}
          </Button>
          <Button onClick={onReject}>{i18n.t('pages.ApprovalDetail.Reject')}</Button>
          <Button onClick={() => history.push('/process/approvals')}>
            {i18n.t('pages.ApprovalDetail.Back')}
          </Button>
        </Space>
      )}
      {type === 'applies' && processInfo?.status === 'PROCESSING' && (
        <Space style={{ display: 'flex', justifyContent: 'center' }}>
          <Button onClick={onCancel}>{i18n.t('pages.ApprovalDetail.Withdraw')}</Button>
          <Button onClick={() => history.push('/process/applies')}>
            {i18n.t('pages.ApprovalDetail.Back')}
          </Button>
        </Space>
      )}
    </>
  );

  const Form = useMemo(() => {
    return {
      APPLY_GROUP_PROCESS: Access,
      APPLY_CONSUMPTION_PROCESS: Consume,
    }[processInfo?.name];
  }, [processInfo]);

  // Approval completed
  const isFinished = currentTask?.status === 'APPROVED';
  // Do not display redundant approval information, such as approval cancellation/rejection
  const noExtraForm = currentTask?.status === 'REJECTED' || currentTask?.status === 'CANCELED';

  const suffixContent = [
    {
      type: 'textarea',
      label: i18n.t('pages.ApprovalDetail.ApprovalComments'),
      name: 'remark',
      initialValue: currentTask?.remark,
      props: {
        showCount: true,
        maxLength: 100,
        disabled: isFinished || noExtraForm,
      },
    },
  ];

  const formProps = {
    defaultData: data,
    isViwer: type !== 'approvals',
    isAdminStep: currentTask?.name === 'ut_admin',
    isFinished,
    noExtraForm,
    suffixContent,
  };

  return (
    <PageContainer
      breadcrumb={[
        { name: `${titleNameMap[type] || i18n.t('pages.ApprovalDetail.Process')}${id}` },
      ]}
      useDefaultContainer={false}
    >
      <div style={{ display: 'flex' }}>
        <Container style={{ flex: 1, marginRight: 20 }}>
          <Card title={workflow?.displayName}>
            <Descriptions>
              <Descriptions.Item label={i18n.t('pages.ApprovalDetail.Applicant')}>
                {processInfo?.applicant}
              </Descriptions.Item>
              <Descriptions.Item label={i18n.t('pages.ApprovalDetail.ApplicationTime')}>
                {processInfo?.startTime ? timestampFormat(processInfo?.startTime) : ''}
              </Descriptions.Item>
            </Descriptions>

            {Form && <Form ref={formRef} {...formProps} />}
          </Card>
        </Container>
        <Container style={{ flex: '0 0 200px' }}>
          <Card title={i18n.t('pages.ApprovalDetail.ApprovalProcess')} style={{ height: '100%' }}>
            <Steps data={stepsData} />
          </Card>
        </Container>
      </div>
      <FooterToolbar extra={<Footer />} />
    </PageContainer>
  );
};

export default Comp;
