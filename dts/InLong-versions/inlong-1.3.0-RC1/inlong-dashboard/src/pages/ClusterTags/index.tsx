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

import React, { useState, useCallback, useMemo } from 'react';
import { Button, Card, List, Col, Row, Descriptions, Input, Modal, message } from 'antd';
import { PageContainer } from '@/components/PageContainer';
import { useRequest } from '@/hooks';
import i18n from '@/i18n';
import request from '@/utils/request';
import ClusterList from './ClusterList';
import TagDetailModal from './TagDetailModal';
import styles from './index.module.less';

const Comp: React.FC = () => {
  const [options, setOptions] = useState({
    keyword: '',
    pageSize: 20,
    pageNum: 1,
  });

  const [tagId, setTagId] = useState<number>();

  const [tagDetailModal, setTagDetailModal] = useState<Record<string, unknown>>({
    visible: false,
  });

  const {
    data,
    loading,
    run: getList,
  } = useRequest(
    {
      url: '/cluster/tag/list',
      method: 'POST',
      data: {
        ...options,
      },
    },
    {
      refreshDeps: [options],
      onSuccess: result => {
        const defaultTag = result?.list?.[0];
        if (defaultTag) {
          setTagId(defaultTag.id);
        }
      },
    },
  );

  const currentTag = useMemo(() => {
    return data?.list.find(item => item.id === tagId) || {};
  }, [tagId, data]);

  const onListPageChange = (pageNum, pageSize) => {
    setOptions(prev => ({
      ...prev,
      pageNum,
      pageSize,
    }));
  };

  const onEdit = useCallback(({ id }) => {
    setTagDetailModal({ visible: true, id });
  }, []);

  const onDelete = useCallback(
    ({ id }) => {
      Modal.confirm({
        title: i18n.t('basic.DeleteConfirm'),
        content: i18n.t('pages.ClusterTags.DelConfirm'),
        onOk: async () => {
          await request({
            url: `/cluster/tag/delete/${id}`,
            method: 'DELETE',
          });
          await getList();
          message.success(i18n.t('basic.DeleteSuccess'));
        },
      });
    },
    [getList],
  );

  return (
    <PageContainer useDefaultBreadcrumb={false} useDefaultContainer={false}>
      <Row gutter={20}>
        <Col style={{ flex: '0 0 350px' }}>
          <List
            size="small"
            itemLayout="horizontal"
            loading={loading}
            pagination={{
              size: 'small',
              onChange: onListPageChange,
              pageSize: 20,
              total: data?.total,
            }}
            dataSource={data?.list}
            header={
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0 16px' }}>
                <Input.Search
                  style={{ flex: '0 1 150px' }}
                  onSearch={keyword =>
                    setOptions(prev => ({
                      ...prev,
                      keyword,
                    }))
                  }
                />
                <Button type="primary" onClick={() => setTagDetailModal({ visible: true })}>
                  {i18n.t('basic.Create')}
                </Button>
              </div>
            }
            renderItem={(item: Record<string, any>) => (
              <List.Item
                actions={[
                  {
                    title: i18n.t('basic.Edit'),
                    action: onEdit,
                  },
                  {
                    title: i18n.t('basic.Delete'),
                    action: onDelete,
                  },
                ].map((k, idx) => (
                  <Button
                    key={idx}
                    type="link"
                    size="small"
                    style={{ padding: 0 }}
                    onClick={e => {
                      e.stopPropagation();
                      k.action(item);
                    }}
                  >
                    {k.title}
                  </Button>
                ))}
                className={`${styles.listItem} ${tagId === item.id ? 'is-selected' : ''}`}
                onClick={() => setTagId(item.id)}
              >
                {item.clusterTag}
              </List.Item>
            )}
            style={{ background: '#fff', height: '100%' }}
          />
        </Col>

        <Col style={{ flex: '1' }}>
          <Card style={{ marginBottom: 20 }}>
            <Descriptions title={currentTag.clusterTag}>
              <Descriptions.Item label={i18n.t('pages.ClusterTags.InCharges')}>
                {currentTag.inCharges}
              </Descriptions.Item>
              <Descriptions.Item label={i18n.t('pages.ClusterTags.Modifier')}>
                {currentTag.modifier}
              </Descriptions.Item>
              <Descriptions.Item label={i18n.t('pages.ClusterTags.ModifyTime')}>
                {currentTag.modifyTime}
              </Descriptions.Item>
            </Descriptions>
          </Card>

          <Card>
            <ClusterList clusterTag={currentTag.clusterTag} />
          </Card>
        </Col>
      </Row>

      <TagDetailModal
        {...tagDetailModal}
        visible={tagDetailModal.visible as boolean}
        onOk={async () => {
          await getList();
          setTagDetailModal({ visible: false });
        }}
        onCancel={() => setTagDetailModal({ visible: false })}
      />
    </PageContainer>
  );
};

export default Comp;
