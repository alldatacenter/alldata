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
import { Modal, Table, Radio, RadioChangeEvent } from 'antd';
import { ModalProps } from 'antd/es/modal';
import { useRequest, useUpdateEffect } from '@/ui/hooks';
import i18n from '@/i18n';
import { ColumnsType } from 'antd/es/table';
import { timestampFormat } from '@/core/utils';

export interface Props extends ModalProps {
  inlongGroupId: string;
  inlongStreamId?: string;
  record?: Record<string, any>;
}

const Comp: React.FC<Props> = ({ inlongGroupId, inlongStreamId, ...modalProps }) => {
  const [position, setPosition] = useState(1);
  interface DataType {
    id: React.Key;
    dt: string;
    body: string;
  }

  const detailColumns: ColumnsType<DataType> = [
    {
      title: 'ID',
      dataIndex: 'id',
    },
    {
      title: i18n.t('pages.GroupDetail.Stream.Dt'),
      dataIndex: 'dt',
      render: text => text && timestampFormat(text),
    },
    {
      title: i18n.t('pages.GroupDetail.Stream.Content'),
      dataIndex: 'body',
      ellipsis: true,
      render: text => <a>{text}</a>,
    },
  ];

  const { data: previewData, run: getPreviewData } = useRequest(
    {
      url: '/stream/listMessages',
      params: {
        groupId: inlongGroupId,
        streamId: inlongStreamId,
        messageCount: position,
      },
    },
    {
      refreshDeps: [position],
      manual: inlongStreamId !== '' ? false : true,
    },
  );

  const onChange = ({ target: { value } }: RadioChangeEvent) => {
    setPosition(value);
  };

  useUpdateEffect(() => {
    if (modalProps.open) {
      if (inlongStreamId) {
        getPreviewData();
      }
    }
  }, [modalProps.open]);

  return (
    <Modal
      {...modalProps}
      title={i18n.t('pages.GroupDetail.Stream.Preview')}
      width={950}
      footer={null}
    >
      <div style={{ marginBottom: 20, marginTop: 20 }}>
        <span>{i18n.t('pages.GroupDetail.Stream.Number')}: </span>
        <Radio.Group defaultValue="1" style={{ marginLeft: 20 }} onChange={onChange}>
          <Radio.Button value="1">1</Radio.Button>
          <Radio.Button value="5">5</Radio.Button>
          <Radio.Button value="10">10</Radio.Button>
          <Radio.Button value="50">50</Radio.Button>
        </Radio.Group>
      </div>
      <Table
        columns={detailColumns}
        dataSource={previewData}
        rowKey={'id'}
        expandable={{
          expandedRowRender: record => <p style={{ margin: 0 }}>{record.body}</p>,
          rowExpandable: record => record.id !== 'Not Expandable',
          expandRowByClick: true,
        }}
      ></Table>
    </Modal>
  );
};

export default Comp;
