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
import { Modal, message, Button } from 'antd';
import { ModalProps } from 'antd/es/modal';
import FormGenerator, { useForm } from '@/ui/components/FormGenerator';
import { useRequest, useSelector, useUpdateEffect } from '@/ui/hooks';
import i18n from '@/i18n';
import request from '@/core/utils/request';
import { State } from '@/core/stores';
import TenantModal from './TenantModal';

export interface Props extends ModalProps {
  id?: number;
  record?: Record<string, any>;
}

const Comp: React.FC<Props> = ({ id, ...modalProps }) => {
  const [form] = useForm();
  const tenant = useSelector<State, State['tenant']>(state => state.tenant);
  const roles = useSelector<State, State['roles']>(state => state.roles);
  const [createModal, setCreateModal] = useState<Record<string, unknown>>({
    open: false,
  });

  const onClick = () => {
    setCreateModal({ open: true });
  };

  const formContent = useMemo(() => {
    return [
      {
        type: 'select',
        label: i18n.t('pages.Tenant.config.Name'),
        name: 'tenant',
        rules: [{ required: true }],
        props: {
          showSearch: true,
          allowClear: true,
          filterOption: false,
          options: {
            requestTrigger: ['onOpen', 'onSearch'],
            requestService: keyword => ({
              url: '/tenant/list',
              method: 'POST',
              data: {
                keyword,
                pageNum: 1,
                pageSize: 10,
                listByLoginUser: true,
              },
            }),
            requestParams: {
              formatResult: result =>
                result?.list?.map(item => ({
                  ...item,
                  label: item.name,
                  value: item.name,
                })),
            },
          },
          dropdownRender: menu => (
            <>
              {roles.includes('INLONG_ADMIN') || roles.includes('INLONG_OPERATOR') ? (
                <Button type="link" onClick={onClick} style={{ marginLeft: 0 }}>
                  {i18n.t('pages.Tenant.New')}
                </Button>
              ) : (
                ''
              )}
              {menu}
            </>
          ),
        },
      },
      {
        type: 'select',
        label: i18n.t('pages.Tenant.config.Username'),
        name: 'username',
        rules: [{ required: true }],
        props: values => ({
          showSearch: true,
          allowClear: true,
          filterOption: false,
          options: {
            requestTrigger: ['onOpen', 'onSearch'],
            requestService: keyword => ({
              url: '/user/listAll',
              method: 'POST',
              data: {
                keyword,
                pageNum: 1,
                pageSize: 10,
              },
            }),
            requestParams: {
              formatResult: result =>
                result?.list?.map(item => ({
                  ...item,
                  label: item.name,
                  value: item.name,
                })),
            },
          },
        }),
      },
      {
        type: 'select',
        label: i18n.t('pages.Tenant.config.TenantRole'),
        name: 'roleCode',
        rules: [{ required: true }],
        props: {
          options: [
            {
              label: i18n.t('pages.Tenant.config.Admin'),
              value: 'TENANT_ADMIN',
            },
            {
              label: i18n.t('pages.Tenant.config.GeneralUser'),
              value: 'TENANT_OPERATOR',
            },
          ],
        },
      },
    ];
  }, []);

  const { data, run: getData } = useRequest(
    id => ({
      url: `/role/tenant/get/${id}`,
    }),
    {
      manual: true,
      onSuccess: result => {
        form.setFieldsValue(result);
      },
    },
  );

  const onOk = async () => {
    const values = await form.validateFields();
    const submitData = {
      tenant,
      ...values,
    };
    const isUpdate = Boolean(id);
    if (isUpdate) {
      submitData.id = id;
      submitData.version = data?.version;
    }
    await request({
      url: isUpdate ? '/role/tenant/update' : '/role/tenant/save',
      method: 'POST',
      data: { ...submitData },
    });
    await modalProps?.onOk(submitData);
    message.success(i18n.t('basic.OperatingSuccess'));
  };

  useUpdateEffect(() => {
    if (modalProps.open) {
      if (id) {
        getData(id);
      }
    } else {
      form.resetFields();
    }
  }, [modalProps.open]);

  return (
    <>
      <Modal
        {...modalProps}
        title={id ? i18n.t('basic.Edit') : i18n.t('pages.TenantRole.New')}
        width={600}
        onOk={onOk}
      >
        <FormGenerator
          labelCol={{ span: 5 }}
          wrapperCol={{ span: 20 }}
          content={formContent}
          form={form}
          initialValues={id ? data : ''}
          useMaxWidth
        />
      </Modal>
      <TenantModal
        {...createModal}
        onOk={async () => {
          setCreateModal(prev => ({ ...prev, open: false }));
        }}
        onCancel={() => setCreateModal(prev => ({ ...prev, open: false }))}
      />
    </>
  );
};

export default Comp;
