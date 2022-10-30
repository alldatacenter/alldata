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

import React, { useMemo, useState, useCallback } from 'react';
import { Modal, message } from 'antd';
import { ModalProps } from 'antd/es/modal';
import { useRequest, useUpdateEffect } from '@/hooks';
import { useTranslation } from 'react-i18next';
import FormGenerator, { useForm, FormItemProps } from '@/components/FormGenerator';
import { Sinks, SinkType } from '@/metas/sinks';
import request from '@/utils/request';

export interface DetailModalProps extends ModalProps {
  inlongGroupId: string;
  // (True operation, save and adjust interface) Need to upload when editing
  id?: string;
  // others
  onOk?: (values) => void;
}

const SinksMap: Record<string, SinkType> = Sinks.reduce(
  (acc, cur) => ({
    ...acc,
    [cur.value]: cur,
  }),
  {},
);

const Comp: React.FC<DetailModalProps> = ({ inlongGroupId, id, ...modalProps }) => {
  const [form] = useForm();

  const { t } = useTranslation();

  const [currentValues, setCurrentValues] = useState({});

  const [sinkType, setSinkType] = useState(Sinks[0].value);

  const toFormVals = useCallback(
    v => {
      const mapFunc = SinksMap[sinkType]?.toFormValues;
      return mapFunc ? mapFunc(v) : v;
    },
    [sinkType],
  );

  const toSubmitVals = useCallback(
    v => {
      const mapFunc = SinksMap[sinkType]?.toSubmitValues;
      return mapFunc ? mapFunc(v) : v;
    },
    [sinkType],
  );

  const { data, run: getData } = useRequest(
    id => ({
      url: `/sink/get/${id}`,
      params: {
        sinkType,
      },
    }),
    {
      manual: true,
      formatResult: result => toFormVals(result),
      onSuccess: result => {
        form.setFieldsValue(result);
        setCurrentValues(result);
        setSinkType(result.sinkType);
      },
    },
  );

  useUpdateEffect(() => {
    if (modalProps.visible) {
      // open
      if (id) {
        getData(id);
      } else {
        form.resetFields(); // Note that it will cause the form to remount to initiate a select request
        setSinkType(Sinks[0].value);
      }
    } else {
      setCurrentValues({});
    }
  }, [modalProps.visible]);

  const formContent = useMemo(() => {
    const getForm = SinksMap[sinkType].getForm;
    const config = getForm('form', {
      currentValues,
      inlongGroupId,
      isEdit: !!id,
      form,
    }) as FormItemProps[];
    return [
      {
        type: 'select',
        label: t('pages.GroupDetail.Sink.DataStreams'),
        name: 'inlongStreamId',
        props: {
          disabled: !!id,
          options: {
            requestService: {
              url: '/stream/list',
              method: 'POST',
              data: {
                pageNum: 1,
                pageSize: 1000,
                inlongGroupId,
              },
            },
            requestParams: {
              formatResult: result =>
                result?.list.map(item => ({
                  label: item.inlongStreamId,
                  value: item.inlongStreamId,
                })) || [],
            },
          },
        },
        rules: [{ required: true }],
      },
      {
        name: 'sinkName',
        type: 'input',
        label: t('meta.Sinks.SinkName'),
        rules: [
          { required: true },
          {
            pattern: /^[a-zA-Z][a-zA-Z0-9_-]*$/,
            message: t('meta.Sinks.SinkNameRule'),
          },
        ],
        props: {
          disabled: !!id,
        },
      },
      {
        name: 'sinkType',
        type: 'select',
        label: t('meta.Sinks.SinkType'),
        rules: [{ required: true }],
        initialValue: Sinks[0].value,
        props: {
          disabled: !!id,
          options: Sinks,
          onChange: value => setSinkType(value),
        },
      },
      {
        name: 'description',
        type: 'textarea',
        label: t('meta.Sinks.Description'),
        props: {
          showCount: true,
          maxLength: 300,
        },
      } as FormItemProps,
    ].concat(config);
  }, [sinkType, inlongGroupId, id, currentValues, form, t]);

  const onOk = async () => {
    const values = await form.validateFields();
    delete values._showHigher; // delete front-end key
    const submitData = toSubmitVals(values);
    const isUpdate = Boolean(id);
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
      },
    });
    modalProps?.onOk(submitData);
    message.success(t('basic.OperatingSuccess'));
  };

  const onValuesChangeHandler = (...rest) => {
    setCurrentValues(prev => ({ ...prev, ...rest[1] }));
  };

  return (
    <Modal title={SinksMap[sinkType]?.label} width={1200} {...modalProps} onOk={onOk}>
      <FormGenerator
        labelCol={{ span: 4 }}
        wrapperCol={{ span: 20 }}
        content={formContent}
        form={form}
        allValues={data}
        onValuesChange={onValuesChangeHandler}
      />
    </Modal>
  );
};

export default Comp;
