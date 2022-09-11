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
import { Button, Table, Modal, message } from 'antd';
import { useTranslation } from 'react-i18next';
import request from '@/utils/request';
import { useUpdateEffect, usePrevious } from '@/hooks';
import { dataSourcesBinLogColumns as binLogColumns } from '@/components/MetaData/DataSourcesBinLog';
import { dataSourcesFileColumns as fileColumns } from '@/components/MetaData/DataSourcesFile';
import CreateModal from './CreateModal';

export interface DataSourcesEditorProps {
  value?: Record<string, any>[];
  onChange?: Function;
  readonly?: boolean;
  type?: 'BINLOG' | 'FILE';
  // Whether to use real operations (for example, to call the background interface when deleting/newing, etc.)
  useActionRequest?: boolean;
  inlongGroupId?: string;
  // Data stream ID, required for real operation
  inlongStreamId?: string;
}

const removeIdFromValues = values =>
  values.map(item => {
    const obj = { ...item };
    delete obj._etid;
    return obj;
  });

const addIdToValues = values =>
  values?.map(item => {
    const obj = { ...item };
    obj._etid = Math.random().toString();
    return obj;
  });

const cache: Record<string, any> = {};

const Comp = ({
  value,
  onChange,
  readonly = false,
  type = 'FILE',
  useActionRequest,
  inlongGroupId,
  inlongStreamId,
}: DataSourcesEditorProps) => {
  const { t } = useTranslation();

  const [data, setData] = useState(addIdToValues(value) || []);
  const previousType = usePrevious(type);

  const [createModal, setCreateModal] = useState({
    visible: false,
    _etid: '',
    id: '',
    record: {},
  }) as any;

  const triggerChange = newData => {
    if (onChange) {
      onChange(removeIdFromValues(newData));
    }
  };

  useUpdateEffect(() => {
    cache[previousType] = data;
    const cacheData = cache[type] || [];
    setData(cacheData);
    triggerChange(cacheData);
  }, [type]);

  const onSaveRequest = async values => {
    const isUpdate = createModal.id;
    const submitData = {
      ...values,
      inlongGroupId,
      inlongStreamId,
      sourceType: type,
    };
    if (isUpdate) submitData.id = createModal.id;
    const newId = await request({
      url: `/source/${isUpdate ? 'update' : 'save'}`,
      method: 'POST',
      data: submitData,
    });
    return isUpdate ? createModal.id : newId;
  };

  const onAddRow = async rowValues => {
    const newData = data.concat(addIdToValues([rowValues]));
    setData(newData);
    triggerChange(newData);
  };

  const onDeleteRequest = id => {
    return new Promise(resolve => {
      Modal.confirm({
        title: t('basic.DeleteConfirm'),
        onOk: async () => {
          await request({
            url: `/source/delete/${id}`,
            method: 'DELETE',
            params: {
              sourceType: type,
            },
          });
          resolve(true);
          message.success(t('DeleteSuccess'));
        },
      });
    });
  };

  const onDeleteRow = async record => {
    const { _etid, id } = record;
    if (useActionRequest) {
      await onDeleteRequest(id);
    }
    const newData = [...data];
    const index = newData.findIndex(item => item._etid === _etid);
    newData.splice(index, 1);
    setData(newData);
    triggerChange(newData);
  };

  const onEditRow = record => {
    setCreateModal({
      visible: true,
      id: useActionRequest ? record?.id : true,
      _etid: record?._etid,
      record,
    });
  };

  const onUpdateRow = (_etid, record) => {
    const newData = data.map(item => {
      if (item._etid === _etid) {
        return record;
      }
      return item;
    });

    setData(newData);
  };

  const columns = (type === 'BINLOG' ? binLogColumns : fileColumns).concat(
    readonly
      ? []
      : [
          {
            title: t('basic.Operating'),
            dataIndex: 'actions',
            width: 120,
            render: (text, record) => (
              <>
                <Button type="link" onClick={() => onEditRow(record)}>
                  {t('basic.Edit')}
                </Button>
                <Button type="link" onClick={() => onDeleteRow(record)}>
                  {t('basic.Delete')}
                </Button>
              </>
            ),
          },
        ],
  );

  return (
    <>
      <Table
        pagination={false}
        dataSource={data}
        columns={columns}
        rowKey="_etid"
        size="small"
        scroll={{
          y: 520,
        }}
        footer={
          readonly
            ? null
            : () => (
                <>
                  <Button type="link" onClick={() => setCreateModal({ visible: true })}>
                    {t('components.AccessHelper.DataSourcesEditor.NewDataSource')}
                  </Button>
                </>
              )
        }
      />

      <CreateModal
        {...createModal}
        type={type}
        id={createModal.id !== true && createModal.id}
        visible={createModal.visible}
        onOk={async values => {
          const isUpdate = createModal.id;
          const id = useActionRequest ? await onSaveRequest(values) : '';
          const result = id ? { id, ...values } : { ...createModal.record, ...values };
          isUpdate ? onUpdateRow(createModal._etid, result) : onAddRow(result);
          setCreateModal({ visible: false });
        }}
        onCancel={() => setCreateModal({ visible: false })}
      />
    </>
  );
};

export default Comp;
