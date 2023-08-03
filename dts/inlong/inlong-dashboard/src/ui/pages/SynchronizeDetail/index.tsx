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

import React, { useMemo, useState, useRef, useEffect } from 'react';
import { Tabs, Button, Card, message, Steps, Space } from 'antd';
import { PageContainer, FooterToolbar } from '@/ui/components/PageContainer';
import { parse } from 'qs';
import { useParams, useRequest, useSet, useHistory, useLocation } from '@/ui/hooks';
import { useTranslation } from 'react-i18next';
import request from '@/core/utils/request';
import Info from './Info';
import DataStream from './SyncT';
import i18n from '@/i18n';

const Comp: React.FC = () => {
  const { t } = useTranslation();
  const location = useLocation();
  const history = useHistory();
  const { id: groupId } = useParams<{ id: string }>();
  const { id: streamId } = useParams<{ id: string }>();

  const qs = parse(location.search.slice(1));

  const [current, setCurrent] = useState(+qs.step || 0);
  const [, { add: addOpened, has: hasOpened }] = useSet([current]);
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [id, setId] = useState(groupId || '');
  const [sid, setSid] = useState(streamId || '');
  const childRef = useRef(null);
  const [mqType, setMqType] = useState();

  const [isCreate] = useState(location.pathname.indexOf('/sync/create') === 0);

  useEffect(() => {
    if (!hasOpened(current)) addOpened(current);
  }, [current, addOpened, hasOpened]);

  const { data } = useRequest(`/group/get/${id}`, {
    ready: !!id,
    refreshDeps: [id],
    onSuccess: result => setMqType(result.mqType),
  });

  const isReadonly = useMemo(() => [0, 101, 102].includes(data?.status), [data]);

  const list = useMemo(
    () => [
      {
        label: t('pages.SynchronizeDetail.Info'),
        value: 'syncInfo',
        content: Info,
      },
      {
        label: t('pages.SynchronizeDetail.SourceAndSink'),
        value: 'syncSource',
        content: DataStream,
      },
    ],
    [],
  );

  const onOk = async current => {
    const onOk = childRef?.current?.onOk;
    setConfirmLoading(true);
    try {
      const result = onOk && (await onOk());
      if (current === 0) {
        setMqType(result.mqType);
        setId(result.inlongGroupId);
        setSid(result.inlongStreamId);
      }
      history.push({
        pathname: `/sync/create/${result?.inlongGroupId || id}`,
        search: `?step=${current + 1}`,
      });
    } finally {
      setConfirmLoading(false);
    }
  };

  const onSubmit = async () => {
    const sourceData = await request({
      url: `/source/list`,
      method: 'POST',
      data: {
        inlongGroupId: id,
      },
    });
    const sinkData = await request({
      url: `/sink/list`,
      method: 'POST',
      data: {
        inlongGroupId: id,
      },
    });

    if (sourceData.total < 1 && sinkData.total < 1) {
      message.warning(t('pages.SynchronizeDetail.Info.SubmittedWarn'));
      return;
    }
    await request({
      url: `/group/startProcess/${id}`,
      method: 'POST',
    });
    message.success(t('pages.SynchronizeDetail.Info.SubmittedSuccessfully'));
    history.push('/sync');
  };

  const Footer = () => (
    <Space style={{ display: 'flex', justifyContent: 'center' }}>
      {current > 0 && (
        <Button disabled={confirmLoading} onClick={() => setCurrent(current - 1)}>
          {i18n.t('pages.SynchronizeDetail.Info.Previous')}
        </Button>
      )}
      {current !== list.length - 1 && (
        <Button
          type="primary"
          loading={confirmLoading}
          onClick={async () => {
            await onOk(current).catch(err => {
              if (err?.errorFields?.length) {
                message.error(t('pages.SynchronizeDetail.Info.CheckMsg'));
              } else if (typeof err === 'string') {
                message.error(err);
              }
              return Promise.reject(err);
            });

            const newCurrent = current + 1;
            setCurrent(newCurrent);
          }}
        >
          {t('pages.SynchronizeDetail.Info.NextStep')}
        </Button>
      )}
      {current === list.length - 1 && (
        <Button type="primary" onClick={onSubmit}>
          {t('pages.SynchronizeDetail.Info.Submit')}
        </Button>
      )}
      <Button onClick={() => history.push('/sync')}>
        {t('pages.SynchronizeDetail.Info.Back')}
      </Button>
    </Space>
  );

  const Div = isCreate ? Card : Tabs;

  return (
    <PageContainer
      breadcrumb={[
        {
          name: isCreate ? t('pages.SynchronizeDetail.Info.Create') : `${id}`,
        },
      ]}
      useDefaultContainer={!isCreate}
    >
      {isCreate && (
        <Steps
          current={current}
          size="small"
          style={{ width: 900, margin: 'auto', marginBottom: 20 }}
          onChange={c => setCurrent(c)}
        >
          {list.map((item, index) => (
            <Steps.Step
              key={item.label}
              title={item.label}
              disabled={index > current && !hasOpened(index)}
            />
          ))}
        </Steps>
      )}

      <Div>
        {list.map(({ content: Content, ...item }, index) => {
          // Lazy load the content of the step, and at the same time make the loaded useCache content not destroy
          const child =
            !isCreate || hasOpened(index) ? (
              <Content
                inlongGroupId={id}
                inlongStreamId={sid}
                readonly={isReadonly}
                isCreate={isCreate}
                ref={index === current ? childRef : null}
                mqType={mqType}
              />
            ) : null;

          return isCreate ? (
            <div key={item.label} style={{ display: `${index === current ? 'block' : 'none'}` }}>
              {child}
            </div>
          ) : (
            <Tabs.TabPane tab={item.label} key={item.value}>
              {child}
            </Tabs.TabPane>
          );
        })}
      </Div>

      {isCreate && <FooterToolbar extra={<Footer />} />}
    </PageContainer>
  );
};

export default Comp;
