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

import React, { useEffect, useMemo, useState } from 'react';
import { Modal, Spin, message, Form, Input, Select } from 'antd';
import { ModalProps } from 'antd/es/modal';
import FormGenerator, { useForm } from '@/ui/components/FormGenerator';
import { useRequest, useUpdateEffect } from '@/ui/hooks';
import { useTranslation } from 'react-i18next';
import { useDefaultMeta, useLoadMeta, TransformMetaType } from '@/plugins';
import request from '@/core/utils/request';
import EditableTable, { ColumnsItemProps } from '../EditableTable';
import i18n from '@/i18n';
import { fieldAllTypes } from './FieldTypeConf';

export interface Props extends ModalProps {
  // When editing, use the ID to call the interface for obtaining details
  id?: string;
  inlongGroupId: string;
  inlongStreamId: string;
  defaultType?: string;
  isSource: boolean;
}

const Comp: React.FC<Props> = ({
  id,
  inlongGroupId,
  inlongStreamId,
  isSource,
  defaultType,
  ...modalProps
}) => {
  const [form] = useForm();
  const { t } = useTranslation();

  const { defaultValue } = useDefaultMeta('transform');

  const [type, setType] = useState(defaultValue);

  const [sinkType, setSinkType] = useState('');

  const { loading, Entity } = useLoadMeta<TransformMetaType>('transform', type);

  const { data, run: getData } = useRequest(
    streamId => ({
      url: `/stream/getBrief`,
      params: {
        groupId: inlongGroupId,
        streamId,
      },
    }),
    {
      manual: true,
      formatResult: result => new Entity()?.parse(result) || result,
    },
  );

  const { data: sinkData, run: getSinkData } = useRequest(
    {
      url: '/sink/list',
      method: 'POST',
      data: {
        inlongGroupId,
        inlongStreamId,
      },
    },
    {
      manual: true,
      onSuccess: result => {
        setSinkType(result.list[0]?.sinkType);
      },
    },
  );

  const fieldTypes = ['int', 'long', 'float', 'double', 'string', 'date', 'timestamp'].map(
    item => ({
      label: item,
      value: item,
    }),
  );

  const sinkFieldList: ColumnsItemProps[] = [
    {
      title: i18n.t('meta.Sinks.SourceFieldName'),
      dataIndex: 'sourceFieldName',
      type: 'input',
      rules: [
        { required: true },
        {
          pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/,
          message: i18n.t('meta.Stream.FieldNameRule'),
        },
      ],
      props: {
        disabled: true,
      },
    },
    {
      title: i18n.t('meta.Sinks.SourceFieldType'),
      dataIndex: 'sourceFieldType',
      type: 'select',
      initialValue: fieldTypes[0].label,
      rules: [{ required: true }],
      props: {
        disabled: true,
        options: fieldTypes,
      },
    },
    {
      title: i18n.t('meta.Stream.FieldName'),
      dataIndex: 'fieldName',
      type: 'input',
      rules: [
        { required: true },
        {
          pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/,
          message: i18n.t('meta.Stream.FieldNameRule'),
        },
      ],
      props: (text, record) => ({
        disabled: record.id,
      }),
    },
    {
      title: i18n.t('meta.Stream.FieldType'),
      dataIndex: 'fieldType',
      type: 'select',
      initialValue: '',
      props: (text, record) => ({
        disabled: record.id,
        options: isSource === true ? fieldTypes : fieldAllTypes[sinkType],
      }),
      rules: [{ required: true }],
    },
    {
      title: i18n.t('meta.Stream.FieldComment'),
      dataIndex: 'fieldComment',
    },
  ];

  const fieldList: ColumnsItemProps[] = [
    {
      title: i18n.t('meta.Stream.FieldName'),
      dataIndex: 'fieldName',
      type: 'input',
      rules: [
        { required: true },
        {
          pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/,
          message: i18n.t('meta.Stream.FieldNameRule'),
        },
      ],
      props: (text, record) => ({
        disabled: record.id,
      }),
    },
    {
      title: i18n.t('meta.Stream.FieldType'),
      dataIndex: 'fieldType',
      type: 'select',
      initialValue: '',
      props: (text, record) => ({
        disabled: record.id,
        options: isSource === true ? fieldTypes : fieldAllTypes[sinkType],
      }),
      rules: [{ required: true }],
    },
    {
      title: i18n.t('meta.Stream.FieldComment'),
      dataIndex: 'fieldComment',
    },
  ];

  const onOk = async () => {
    const values = await form.validateFields();
    const isUpdate = Boolean(inlongStreamId);
    const submitData = {
      ...values,
      inlongGroupId,
      inlongStreamId,
    };
    if (isUpdate) {
      submitData.id = id;
      submitData.version = data?.version;
    }
    if (isSource === true) {
      await request({
        url: `/stream/update`,
        method: 'POST',
        data: {
          ...submitData,
        },
      });
    } else {
      sinkData.list[0].sinkFieldList = values.sinkFieldList;
      const sinkSubmitData = {
        ...sinkData.list[0],
        inlongGroupId,
        inlongStreamId,
      };
      await request({
        url: `/sink/update`,
        method: 'POST',
        data: {
          ...sinkSubmitData,
        },
      });
    }
    modalProps?.onOk(submitData);
    message.success(t('pages.GroupDetail.Sources.SaveSuccessfully'));
  };

  useEffect(() => {
    if (inlongStreamId) {
      getData(inlongStreamId);
      getSinkData();
      setSinkType(sinkData?.list[0]?.sinkType);
    }
  }, [getData, getSinkData, inlongStreamId]);

  useEffect(() => {
    if (!id && isSource === false) {
      form.setFieldsValue({
        sinkFieldList: data?.fieldList.map(item => ({
          sourceFieldName: item.fieldName,
          sourceFieldType: item.fieldType,
          fieldName: item.fieldName,
          fieldType: '',
        })),
      });
    }
  }, [data?.fieldList, form, getData, getSinkData, id, inlongStreamId, isSource]);

  return (
    <>
      <Modal
        {...modalProps}
        title={
          isSource === true
            ? t('components.FieldList.CreateSource')
            : t('components.FieldList.CreateSink')
        }
        width={888}
        onOk={onOk}
      >
        <Spin spinning={loading}>
          <Form form={form} initialValues={isSource === true ? data : sinkData?.list[0]}>
            <Form.Item name={isSource === true ? 'fieldList' : 'sinkFieldList'}>
              <EditableTable
                columns={isSource === true ? fieldList : sinkFieldList}
                dataSource={isSource === true ? data?.fieldList : sinkData?.sinkFieldList}
              ></EditableTable>
            </Form.Item>
          </Form>
        </Spin>
      </Modal>
    </>
  );
};

export default Comp;
