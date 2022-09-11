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
import { Modal } from 'antd';
import { ModalProps } from 'antd/es/modal';
import { useRequest, useUpdateEffect } from '@/hooks';
import { useTranslation } from 'react-i18next';
import FormGenerator, {
  useForm,
  FormItemProps,
  FormGeneratorProps,
} from '@/components/FormGenerator';
import { Storages, StoragesType } from '@/components/MetaData';

export interface DetailModalProps extends ModalProps {
  inlongGroupId: string;
  name?: string;
  content?: FormItemProps[];
  // (True operation, save and adjust interface) Need to upload when editing
  id?: string;
  // (False operation) Need to pass when editing, row data
  record?: Record<string, any>;
  sinkType: 'HIVE' | 'TEST';
  dataType?: string;
  // defaultRowTypeFields, which can be used to auto-fill form default values
  defaultRowTypeFields?: Record<string, unknown>[];
  // others
  onOk?: (values) => void;
  onValuesChange?: FormGeneratorProps['onValuesChange'];
}

const StoragesMap: Record<string, StoragesType> = Storages.reduce(
  (acc, cur) => ({
    ...acc,
    [cur.value]: cur,
  }),
  {},
);

const Comp: React.FC<DetailModalProps> = ({
  inlongGroupId,
  id,
  record,
  sinkType,
  name,
  content = [],
  dataType,
  defaultRowTypeFields,
  onValuesChange,
  ...modalProps
}) => {
  const [form] = useForm();

  const { t } = useTranslation();

  const [currentValues, setCurrentValues] = useState({});

  const fieldListKey = useMemo(() => {
    return {
      HIVE: {
        // Field name of the field array form
        columnsKey: 'sinkFieldList',
        // In addition to the defaultRowTypeFields field that is populated by default, additional fields that need to be populated
        // The left is the defaultRowTypeFields field, and the right is the newly filled field
        restMapping: {
          fieldName: 'fieldName',
        },
      },
      CLICKHOUSE: {
        columnsKey: 'sinkFieldList',
        restMapping: {
          fieldName: 'fieldName',
        },
      },
    }[sinkType];
  }, [sinkType]);

  const toFormVals = useCallback(
    v => {
      const mapFunc = StoragesMap[sinkType]?.toFormValues;
      return mapFunc ? mapFunc(v) : v;
    },
    [sinkType],
  );

  const toSubmitVals = useCallback(
    v => {
      const mapFunc = StoragesMap[sinkType]?.toSubmitValues;
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
      onSuccess: result => {
        form.setFieldsValue(toFormVals(result));
        setCurrentValues(toFormVals(result));
      },
    },
  );

  useUpdateEffect(() => {
    if (modalProps.visible) {
      // open
      form.resetFields(); // Note that it will cause the form to remount to initiate a select request
      if (id) {
        getData(id);
        return;
      }
      if (Object.keys(record || {})?.length) {
        form.setFieldsValue(toFormVals(record));
        setCurrentValues(toFormVals(record));
      } else {
        const usefulDefaultRowTypeFields = defaultRowTypeFields?.filter(
          item => item.fieldName && item.fieldType,
        );
        if (fieldListKey && usefulDefaultRowTypeFields?.length) {
          const getFieldListColumns = Storages.find(item => item.value === sinkType)
            ?.getFieldListColumns;
          form.setFieldsValue({
            [fieldListKey.columnsKey]: usefulDefaultRowTypeFields?.map(item => ({
              // The default value defined by cloumns
              ...getFieldListColumns(dataType).reduce(
                (acc, cur) => ({
                  ...acc,
                  [cur.dataIndex]: cur.initialValue,
                }),
                {},
              ),
              // Extra fill
              ...Object.keys(fieldListKey.restMapping).reduce(
                (acc, key) => ({
                  ...acc,
                  [fieldListKey.restMapping[key]]: item[key],
                }),
                {},
              ),
              // Default fill
              sourceFieldName: item.fieldName,
              sourceFieldType: item.fieldType,
              fieldComment: item.fieldComment,
            })),
          });
        }
      }
    } else {
      setCurrentValues({});
    }
  }, [modalProps.visible]);

  const formContent = useMemo(() => {
    const getForm = StoragesMap[sinkType].getForm;
    const config = getForm('form', {
      currentValues,
      inlongGroupId,
      isEdit: !!id,
      dataType,
      form,
    }) as FormItemProps[];
    return [
      {
        name: 'sinkName',
        type: 'input',
        label: t('components.AccessHelper.StorageMetaData.SinkName'),
        rules: [
          { required: true },
          {
            pattern: /^[a-zA-Z][a-zA-Z0-9_-]*$/,
            message: t('components.AccessHelper.StorageMetaData.SinkNameRule'),
          },
        ],
        props: {
          disabled: !!id,
        },
      },
      {
        name: 'description',
        type: 'textarea',
        label: t('components.AccessHelper.StorageMetaData.Description'),
        props: {
          showCount: true,
          maxLength: 300,
        },
      } as FormItemProps,
    ].concat(config);
  }, [sinkType, dataType, inlongGroupId, id, currentValues, form, t]);

  const onOk = async () => {
    const values = await form.validateFields();
    delete values._showHigher; // delete front-end key
    modalProps.onOk && modalProps.onOk(toSubmitVals(values));
  };

  const onValuesChangeHandler = (...rest) => {
    setCurrentValues(prev => ({ ...prev, ...rest[1] }));

    if (onValuesChange) {
      (onValuesChange as any)(...rest);
    }
  };

  return (
    <Modal title={sinkType} width={1200} {...modalProps} onOk={onOk}>
      <FormGenerator
        name={name}
        labelCol={{ span: 4 }}
        wrapperCol={{ span: 20 }}
        content={content.concat(formContent)}
        form={form}
        allValues={data}
        onValuesChange={onValuesChangeHandler}
      />
    </Modal>
  );
};

export default Comp;
