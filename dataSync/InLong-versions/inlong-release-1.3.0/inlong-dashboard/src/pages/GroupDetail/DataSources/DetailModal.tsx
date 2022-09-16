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

import React, { useCallback, useMemo, useState } from 'react';
import { Modal, message } from 'antd';
import { ModalProps } from 'antd/es/modal';
import FormGenerator, { useForm } from '@/components/FormGenerator';
import { useRequest, useUpdateEffect } from '@/hooks';
import { useTranslation } from 'react-i18next';
import { FormItemProps } from '@/components/FormGenerator';
import { sources, SourceType } from '@/metas/sources';
import request from '@/utils/request';

export interface Props extends ModalProps {
  // When editing, use the ID to call the interface for obtaining details
  id?: string;
  inlongGroupId?: string;
}

const sourcesMap: Record<string, SourceType> = sources.reduce(
  (acc, cur) => ({
    ...acc,
    [cur.value]: cur,
  }),
  {},
);

const Comp: React.FC<Props> = ({ id, inlongGroupId, ...modalProps }) => {
  const [form] = useForm();
  const { t } = useTranslation();

  const [currentValues, setCurrentValues] = useState({});

  const [type, setType] = useState(sources[0].value);

  const toFormVals = useCallback(
    v => {
      const mapFunc = sourcesMap[type]?.toFormValues;
      return mapFunc ? mapFunc(v) : v;
    },
    [type],
  );

  const toSubmitVals = useCallback(
    v => {
      const mapFunc = sourcesMap[type]?.toSubmitValues;
      return mapFunc ? mapFunc(v) : v;
    },
    [type],
  );

  const { data, run: getData } = useRequest(
    id => ({
      url: `/source/get/${id}`,
      params: {
        sourceType: type,
      },
    }),
    {
      manual: true,
      formatResult: result => toFormVals(result),
      onSuccess: result => {
        form.setFieldsValue(result);
        setCurrentValues(result);
        setType(result.sourceType);
      },
    },
  );

  const onOk = async () => {
    const values = await form.validateFields();
    const submitData = toSubmitVals(values);
    const isUpdate = Boolean(id);
    if (isUpdate) {
      submitData.id = id;
      submitData.version = data?.version;
    }
    await request({
      url: `/source/${isUpdate ? 'update' : 'save'}`,
      method: 'POST',
      data: {
        ...submitData,
        inlongGroupId,
      },
    });
    modalProps?.onOk(submitData);
    message.success(t('pages.GroupDetail.Sources.SaveSuccessfully'));
  };

  useUpdateEffect(() => {
    if (modalProps.visible) {
      // open
      if (id) {
        getData(id);
      } else {
        form.resetFields(); // Note that it will cause the form to remount to initiate a select request
        setType(sources[0].value);
      }
    } else {
      setCurrentValues({});
    }
  }, [modalProps.visible]);

  const formContent = useMemo(() => {
    const getForm = sourcesMap[type].getForm;
    const config = getForm('form', {
      currentValues,
      form,
    }) as FormItemProps[];
    return [
      {
        type: 'select',
        label: t('pages.GroupDetail.Sources.DataStreams'),
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
        name: 'sourceName',
        type: 'input',
        label: t('meta.Sources.Name'),
        rules: [{ required: true }],
        props: {
          disabled: !!id,
        },
      },
      {
        name: 'sourceType',
        type: 'radio',
        label: t('meta.Sources.Type'),
        rules: [{ required: true }],
        initialValue: sources[0].value,
        props: {
          disabled: !!id,
          options: sources,
          onChange: e => setType(e.target.value),
        },
      } as FormItemProps,
    ].concat(config);
  }, [type, id, currentValues, form, t, inlongGroupId]);

  return (
    <>
      <Modal {...modalProps} title={sourcesMap[type]?.label} width={666} onOk={onOk}>
        <FormGenerator
          content={formContent}
          onValuesChange={vals => setCurrentValues(prev => ({ ...prev, ...vals }))}
          allValues={currentValues}
          form={form}
          useMaxWidth
        />
      </Modal>
    </>
  );
};

export default Comp;
