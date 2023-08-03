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
import { Button, Spin, Modal, message } from 'antd';
import { ModalProps } from 'antd/es/modal';
import { useRequest, useUpdateEffect } from '@/ui/hooks';
import { useTranslation } from 'react-i18next';
import EditableTable from '@/ui/components/EditableTable';
import FormGenerator, { useForm } from '@/ui/components/FormGenerator';
import { useLoadMeta, SinkMetaType } from '@/plugins';
import request from '@/core/utils/request';

export interface DetailModalProps extends ModalProps {
  inlongGroupId: string;
  inlongStreamId: string;
  defaultType?: string;
  // (True operation, save and adjust interface) Need to upload when editing
  id?: string;
  // others
  onOk?: (values) => void;
}

const Comp: React.FC<DetailModalProps> = ({
  inlongGroupId,
  inlongStreamId,
  defaultType,
  id,
  ...modalProps
}) => {
  const [form] = useForm();

  const { t } = useTranslation();

  const [sinkType, setSinkType] = useState('');

  const { loading: pluginLoading, Entity } = useLoadMeta<SinkMetaType>('sink', sinkType);

  const { data: groupData, run: getGroupData } = useRequest(`/group/get/${inlongGroupId}`, {
    manual: true,
    ready: Boolean(inlongGroupId),
    refreshDeps: [inlongGroupId],
  });

  const {
    data,
    loading,
    run: getData,
  } = useRequest(
    id => ({
      url: `/sink/get/${id}`,
    }),
    {
      manual: true,
      formatResult: result => new Entity()?.parse(result) || result,
      onSuccess: result => {
        setSinkType(result.sinkType);
        form.setFieldsValue(result);
      },
    },
  );

  useUpdateEffect(() => {
    if (modalProps.open) {
      // open
      if (id) {
        getGroupData();
        getData(id);
      } else {
        form.setFieldsValue({ inlongGroupId, sinkType: defaultType });
        setSinkType(defaultType);
      }
    } else {
      form.resetFields();
      setSinkType('');
    }
  }, [modalProps.open]);

  const formContent = useMemo(() => {
    if (Entity) {
      const row = new Entity().renderSyncRow();
      return row.map(item => ({
        ...item,
        col: item.name === 'sinkType' || item.type === EditableTable ? 24 : 12,
      }));
    }

    return [];
  }, [Entity]);

  const onOk = async (startProcess = false) => {
    const values = await form.validateFields();
    const submitData = new Entity()?.stringify(values) || values;
    const isUpdate = Boolean(id);
    if (startProcess) {
      submitData.startProcess = true;
    }
    if (isUpdate) {
      submitData.id = id;
      submitData.version = data?.version;
    }
    await request({
      url: isUpdate ? '/sink/update' : '/sink/save',
      method: 'POST',
      data: {
        ...submitData,
        inlongGroupId,
        inlongStreamId,
      },
    });
    modalProps?.onOk(submitData);
    message.success(t('basic.OperatingSuccess'));
  };

  return (
    <Modal
      title={id ? t('pages.GroupDetail.Sink.Edit') : t('pages.GroupDetail.Sink.New')}
      width={1200}
      {...modalProps}
      footer={[
        <Button key="cancel" onClick={e => modalProps.onCancel(e)}>
          {t('pages.GroupDetail.Sink.Cancel')}
        </Button>,
        <Button key="save" type="primary" onClick={() => onOk(false)}>
          {t('pages.GroupDetail.Sink.Save')}
        </Button>,
        groupData?.status === 130 && id && (
          <Button key="run" type="primary" onClick={() => onOk(true)}>
            {t('pages.GroupDetail.Sink.SaveAndRefresh')}
          </Button>
        ),
      ]}
    >
      <Spin spinning={loading || pluginLoading}>
        <FormGenerator
          labelCol={{ flex: '0 0 200px' }}
          wrapperCol={{ flex: '1' }}
          col={12}
          content={formContent}
          form={form}
          initialValues={id ? data : { inlongGroupId }}
          onValuesChange={(c, values) => {
            setSinkType(values.sinkType);
          }}
        />
      </Spin>
    </Modal>
  );
};

export default Comp;
