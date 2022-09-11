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

import React, { useState, useRef } from 'react';
import { Button, Card, Steps, Space, message } from 'antd';
import { parse } from 'qs';
import { PageContainer, Container, FooterToolbar } from '@/components/PageContainer';
import { useHistory, useLocation, useSet } from '@/hooks';
import { useTranslation } from 'react-i18next';
import Info from './Info';

const { Step } = Steps;

const Create: React.FC = () => {
  const { t } = useTranslation();
  const history = useHistory();

  const location = useLocation();

  const qs = parse(location.search.slice(1));

  const [current, setCurrent] = useState(+qs.step || 0);
  const [, { add: addOpened, has: hasOpened }] = useSet([current]);
  const [confirmLoading, setConfirmLoading] = useState(false);

  const [id, setId] = useState(qs.id);

  const infoRef = useRef();

  const steps = [
    {
      title: t('pages.ConsumeCreate.ConsumerInformation'),
      content: <Info ref={infoRef} id={id} />,
      useCache: true,
      ref: infoRef,
    },
  ];

  const onOk = async current => {
    const currentStepObj = steps[current] as any;
    const onOk = currentStepObj.ref?.current?.onOk;
    setConfirmLoading(true);
    try {
      const result = onOk && (await onOk());
      if (current === 0) {
        setId(result);
        history.push({
          search: `?id=${result}&step=1`,
        });
      }
    } finally {
      setConfirmLoading(false);
    }
  };

  const onSubmit = async current => {
    await onOk(current);
    message.success(t('basic.OperatingSuccess'));
    history.push('/consume');
  };

  const Footer = () => (
    <Space style={{ display: 'flex', justifyContent: 'center' }}>
      {current > 0 && (
        <Button onClick={() => setCurrent(current - 1)}>{t('pages.ConsumeCreate.Prev')}</Button>
      )}
      {current !== steps.length - 1 && (
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
      {current === steps.length - 1 && (
        <Button type="primary" onClick={() => onSubmit(current)}>
          {t('pages.ConsumeCreate.Submit')}
        </Button>
      )}
      <Button onClick={() => history.push('/consume')}>{t('pages.ConsumeCreate.Back')}</Button>
    </Space>
  );

  return (
    <PageContainer
      breadcrumb={[{ name: t('pages.ConsumeCreate.NewConsume') }]}
      useDefaultContainer={false}
    >
      <Steps current={current} size="small" style={{ marginBottom: 20, width: 400 }}>
        {steps.map(item => (
          <Step key={item.title} title={item.title} />
        ))}
      </Steps>

      <Container>
        <Card>
          {steps.map((item, index) => (
            // Lazy load the content of the step, and at the same time make the loaded useCache content not destroy
            <div key={item.title} style={{ display: `${index === current ? 'block' : 'none'}` }}>
              {index === current || (item.useCache && hasOpened(index)) ? item.content : null}
            </div>
          ))}
        </Card>
      </Container>

      <FooterToolbar extra={<Footer />} />
    </PageContainer>
  );
};

export default Create;
