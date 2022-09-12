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

import React, { useState, useRef, useImperativeHandle, forwardRef } from 'react';
import ReactDom from 'react-dom';
import { Form, Collapse, Button, Empty, Modal, Space, message } from 'antd';
import FormGenerator, { FormItemContent } from '@/components/FormGenerator';
import { defaultSize } from '@/configs/pagination';
import { useRequest } from '@/hooks';
import request from '@/utils/request';
import { useTranslation } from 'react-i18next';
import { dataToValues, valuesToData } from './helper';
import { CommonInterface } from '../common';
import StreamItemModal from './StreamItemModal';
import { getFilterFormContent, genExtraContent, genFormContent } from './config';
import { genStatusTag } from './status';
import styles from './index.module.less';

type Props = CommonInterface;

const Comp = ({ inlongGroupId, readonly, mqType }: Props, ref) => {
  const { t } = useTranslation();

  const [form] = Form.useForm();

  const [activeKey, setActiveKey] = useState('0');

  const [options, setOptions] = useState({
    pageSize: defaultSize,
    pageNum: 1,
  });

  // False means there is no data being edited, true means it is adding, and the real ID corresponds to the data being edited
  const [editingId, setEditingId] = useState<number | boolean>(false);

  const [realTimeValues, setRealTimeValues] = useState({ list: [] });

  const topRightRef = useRef();

  const [streamItemModal, setStreamItemModal] = useState({
    visible: false,
    record: {},
    inlongGroupId,
  });

  const { data = realTimeValues, run: getList, mutate } = useRequest(
    {
      url: '/stream/listAll',
      method: 'POST',
      data: {
        ...options,
        inlongGroupId,
      },
    },
    {
      refreshDeps: [options],
      formatResult: result => ({
        list: dataToValues(result.list),
        total: result.total,
      }),
      onSuccess: data => {
        form.setFieldsValue({ list: data.list });
        setRealTimeValues({ list: data.list });
        setEditingId(false);
      },
    },
  );

  const onFilter = values => {
    setOptions(prev => ({
      ...prev,
      ...values,
    }));
  };

  const getTouchedValues = async () => {
    const { list } = await form.validateFields();
    // The necessary key, which must be transmitted every time the interface is adjusted
    const reservedFields = new Set([
      'id',
      'inlongGroupId',
      'inlongStreamId',
      'dataSourceBasicId',
      'dataSourceType',
      'havePredefinedFields',
    ]);
    const output = list.map((item, index) => {
      return Object.entries(item).reduce((acc, [key, value]) => {
        if (form.isFieldTouched(['list', index, key]) || reservedFields.has(key)) {
          acc[key] = value;
        }
        return acc;
      }, {});
    });
    return { list: output };
  };

  const onSave = async record => {
    if (record.id) {
      // update
      const { list } = await getTouchedValues();
      const values = list.find(item => item.id === record.id);
      const data = valuesToData(values ? [values] : [], inlongGroupId);
      await request({
        url: '/stream/update',
        method: 'POST',
        data: data?.[0]?.streamInfo,
      });
    } else {
      // create
      const { list } = await form.validateFields();
      const values = list?.[0];
      const data = valuesToData(values ? [values] : [], inlongGroupId);
      await request({
        url: '/stream/save',
        method: 'POST',
        data: data?.[0]?.streamInfo,
      });
    }
    await getList();
    setEditingId(false);
    message.success(t('basic.OperatingSuccess'));
  };

  const onOk = () => {
    if (editingId) {
      return Promise.reject('Please save the data');
    } else {
      return Promise.resolve();
    }
  };

  useImperativeHandle(ref, () => ({
    onOk,
  }));

  const onEdit = record => {
    // setEditingId(record.id);
    // setActiveKey(index.toString());
    setStreamItemModal(prev => ({ ...prev, visible: true, record }));
  };

  const onCancel = async () => {
    setEditingId(false);
    await getList();
  };

  const onDelete = record => {
    Modal.confirm({
      title: t('basic.DeleteConfirm'),
      onOk: async () => {
        await request({
          url: '/stream/delete',
          method: 'DELETE',
          params: {
            groupId: inlongGroupId,
            streamId: record?.inlongStreamId,
          },
        });
        await getList();
        message.success(t('basic.DeleteSuccess'));
      },
    });
  };

  const genExtra = (record, index) => {
    const list = genExtraContent({
      editingId,
      record,
      mqType,
      onSave,
      onEdit,
      onCancel,
      onDelete,
    });
    return (
      <>
        {list.map((item, k) => (
          <Button
            size="small"
            type="link"
            key={k}
            disabled={item.disabled}
            onClick={e => {
              e.stopPropagation();
              item.onRun(record, index);
            }}
          >
            {item.label}
          </Button>
        ))}
      </>
    );
  };

  const genHeader = (record = {}, index) => {
    return (
      <div className={styles.collapseHeader}>
        {(record as any).inlongStreamId ? (
          ['inlongStreamId', 'name', 'modifier', 'createTime', 'status'].map(key => (
            <div key={key} className={styles.collapseHeaderItem}>
              {key === 'status' ? genStatusTag(record?.[key]) : record?.[key]}
            </div>
          ))
        ) : (
          <div className={styles.collapseHeaderItem}>
            {t('pages.AccessDetail.DataStream.NewDataStream')}
          </div>
        )}
        {!readonly && genExtra(record, index)}
      </div>
    );
  };

  return (
    <>
      <div className={styles.topFilterContainer}>
        <FormGenerator layout="inline" content={getFilterFormContent()} onFilter={onFilter} />
        <div ref={topRightRef}></div>
      </div>

      <Form
        form={form}
        labelAlign="left"
        labelCol={{ xs: 6, sm: 4 }}
        wrapperCol={{ xs: 18, sm: 20 }}
        onValuesChange={(c, v) => setRealTimeValues(v)}
      >
        <Form.List name="list" initialValue={data.list}>
          {(fields, { add }) => (
            <>
              {topRightRef.current &&
                !readonly &&
                ReactDom.createPortal(
                  <Space>
                    <Button
                      type="primary"
                      disabled={!!editingId}
                      onClick={async () => {
                        setEditingId(true);
                        await add({}, 0);
                        setTimeout(() => {
                          setRealTimeValues(form.getFieldsValue());
                          const newActiveKey = Math.max(...fields.map(item => item.key)) + 1 + '';
                          setActiveKey(newActiveKey);
                        }, 0);
                        mutate({ list: [{}].concat(data.list), total: data.list.length + 1 });
                      }}
                    >
                      {t('pages.AccessDetail.DataStream.CreateDataStream')}
                    </Button>
                  </Space>,
                  topRightRef.current,
                )}

              {fields.length ? (
                <Collapse
                  accordion
                  activeKey={activeKey}
                  onChange={key => setActiveKey(key as any)}
                  style={{ backgroundColor: 'transparent', border: 'none' }}
                >
                  {fields.map((field, index) => (
                    <Collapse.Panel
                      header={genHeader(data?.list?.[index], index)}
                      key={field.key.toString()}
                      style={{
                        marginBottom: 10,
                        border: '1px solid #e5e5e5',
                        backgroundColor: '#f6f7fb',
                      }}
                    >
                      <FormItemContent
                        values={realTimeValues.list?.[index]}
                        content={genFormContent(
                          editingId,
                          { ...realTimeValues.list?.[index] },
                          inlongGroupId,
                          readonly,
                          mqType,
                        ).map(item => {
                          const obj = { ...item } as any;
                          if (obj.name) {
                            obj.name = [field.name, obj.name];
                            obj.fieldKey = [field.fieldKey, obj.name];
                          }
                          if (obj.suffix && obj.suffix.name) {
                            obj.suffix.name = [field.name, obj.suffix.name];
                            obj.suffix.fieldKey = [field.fieldKey, obj.suffix.name];
                          }

                          return obj;
                        })}
                      />
                    </Collapse.Panel>
                  ))}
                </Collapse>
              ) : (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />
              )}
            </>
          )}
        </Form.List>
      </Form>

      <StreamItemModal
        {...streamItemModal}
        mqType={mqType}
        onOk={async () => {
          await getList();
          setEditingId(false);
          setStreamItemModal(prev => ({ ...prev, visible: false }));
        }}
        onCancel={() => setStreamItemModal(prev => ({ ...prev, visible: false }))}
      />
    </>
  );
};

export default forwardRef(Comp);
