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

import React, { useState, useMemo, useRef, useEffect } from 'react';
import { Tabs, Button, Card, message, Steps, Space } from 'antd';
import { useTranslation } from 'react-i18next';
import { parse } from 'qs';
import { PageContainer, FooterToolbar } from '@/components/PageContainer';
import { useParams, useRequest, useSet, useHistory, useLocation } from '@/hooks';
import request from '@/utils/request';
import Info from './Info';

const Comp: React.FC = () => {
  const { t } = useTranslation();
  const history = useHistory();
  const location = useLocation();
  const _id = +useParams<{ id: string }>().id;

  const qs = parse(location.search.slice(1));

  const [current, setCurrent] = useState(+qs.step || 0);
  const [, { add: addOpened, has: hasOpened }] = useSet([current]);
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [id, setId] = useState(_id);
  const childRef = useRef(null);
  const [isCreate] = useState(location.pathname.indexOf('/consume/create') === 0);

  useEffect(() => {
    if (!hasOpened(current)) addOpened(current);
  }, [current, addOpened, hasOpened]);

  const { data } = useRequest(`/consume/get/${id}`, {
    ready: !!id,
    refreshDeps: [id],
  });

  const isReadonly = useMemo(() => [11, 20, 22].includes(data?.status), [data]);

  const list = useMemo(
    () => [
      {
        label: isCreate
          ? t('pages.ConsumeCreate.NewSubscribe')
          : t('pages.ConsumeDetail.SubscribeDetails'),
        value: 'consumeDetail',
        content: Info,
      },
    ],
    [t],
  );

  const onOk = async current => {
    const onOk = childRef?.current?.onOk;
    setConfirmLoading(true);
    try {
      const result = onOk && (await onOk());
      if (current === 0) {
        setId(result);
        history.push({
          search: `?id=${result}&step=1`,
        });
        return result;
      }
      return id;
    } finally {
      setConfirmLoading(false);
    }
  };

  const onSubmit = async _id => {
    await request({
      url: `/consume/startProcess/${_id}`,
      method: 'POST',
      data,
    });
    message.success(t('basic.OperatingSuccess'));
    history.push('/consume');
  };

  const Footer = () => (
    <Space style={{ display: 'flex', justifyContent: 'center' }}>
      {current > 0 && (
        <Button onClick={() => setCurrent(current - 1)}>{t('pages.ConsumeCreate.Prev')}</Button>
      )}
      {current !== list.length - 1 && (
        <Button
          type="primary"
          loading={confirmLoading}
          onClick={async () => {
            await onOk(current);
            const newCurrent = current + 1;
            setCurrent(newCurrent);
            if (!hasOpened(newCurrent)) addOpened(newCurrent);
          }}
        >
          {t('pages.ConsumeCreate.Next')}
        </Button>
      )}
      {current === list.length - 1 && (
        <Button
          type="primary"
          onClick={async () => {
            const id = await onOk(current);
            await onSubmit(id);
          }}
        >
          {t('pages.ConsumeCreate.Submit')}
        </Button>
      )}
      <Button onClick={() => history.push('/consume')}>{t('pages.ConsumeCreate.Back')}</Button>
    </Space>
  );

  const Div = isCreate ? Card : Tabs;

  return (
    <PageContainer
      breadcrumb={[
        {
          name: isCreate
            ? t('pages.ConsumeCreate.NewSubscribe')
            : `${t('pages.ConsumeDetail.SubscribeDetails')}${data?.id}`,
        },
      ]}
      useDefaultContainer={!isCreate}
    >
      {isCreate && (
        <Steps
          current={current}
          size="small"
          style={{ marginBottom: 20, width: 600 }}
          onChange={c => setCurrent(c)}
        >
          {list.map(item => (
            <Steps.Step key={item.label} title={item.label} />
          ))}
        </Steps>
      )}

      <Div>
        {list.map(({ content: Content, ...item }, index) => {
          // Lazy load the content of the step, and at the same time make the loaded useCache content not destroy
          const child =
            !isCreate || hasOpened(index) ? (
              <Content
                id={id}
                readonly={isReadonly}
                isCreate={isCreate}
                ref={index === current ? childRef : null}
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
