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

import React, { useState, useEffect } from 'react';
import { Badge, Button, Card, Table } from 'antd';
import { useRequest } from '@/ui/hooks';
import { useDefaultMeta } from '@/plugins';
import i18n from '@/i18n';
import { ColumnsType } from 'antd/es/table';
import DetailModal from './DetailModal';

export interface Props {
  inlongGroupId: string;
  inlongStreamId?: string;
  isSource: boolean;
  columns: ColumnsType;
  //   readonly?: string;
}

const Comp: React.FC<Props> = ({ inlongGroupId, inlongStreamId, isSource, columns }) => {
  const [createModal, setCreateModal] = useState<Record<string, unknown>>({
    open: false,
  });

  const { data, run: getList } = useRequest(
    streamId => ({
      url: `/stream/get`,
      params: {
        groupId: inlongGroupId,
        streamId,
      },
    }),
    {
      manual: true,
    },
  );

  const { data: sinkData, run: getSinkData } = useRequest(
    {
      url: `/sink/list`,
      method: 'POST',
      data: {
        inlongGroupId,
        inlongStreamId,
      },
    },
    {
      manual: true,
    },
  );

  useEffect(() => {
    if (inlongStreamId !== '') {
      getList(inlongStreamId);
      getSinkData();
    }
  }, [getList, getSinkData, inlongStreamId]);

  return (
    <>
      <Card
        size="small"
        title={
          <Badge size="small" count={data?.total} offset={[12, 3]}>
            {isSource === true
              ? i18n.t('components.FieldList.Source')
              : i18n.t('components.FieldList.Sink')}
          </Badge>
        }
        style={{ height: '100%' }}
      >
        <Table
          columns={columns}
          dataSource={isSource === true ? data?.fieldList : sinkData?.list[0]?.sinkFieldList}
          footer={() => (
            <Button style={{ margin: 'auto' }} onClick={() => setCreateModal({ open: true })}>
              {i18n.t('components.FieldList.AddField')}
            </Button>
          )}
        ></Table>
      </Card>
      <DetailModal
        {...createModal}
        inlongGroupId={inlongGroupId}
        inlongStreamId={inlongStreamId}
        isSource={isSource}
        open={createModal.open as boolean}
        onOk={async () => {
          await getList(inlongStreamId);
          await getSinkData();
          setCreateModal({ open: false });
        }}
        onCancel={() => setCreateModal({ open: false })}
      />
    </>
  );
};

export default Comp;
