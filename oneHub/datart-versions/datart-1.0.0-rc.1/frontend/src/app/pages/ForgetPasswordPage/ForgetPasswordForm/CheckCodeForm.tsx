/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Button, Form, Input, Radio } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import React, { FC, useCallback, useMemo, useState } from 'react';
import styled from 'styled-components';
import { SPACE_LG } from 'styles/StyleConstants';
import { FindWays } from '../constants';
import { captchaforResetPassword } from '../service';
import { CaptchaParams } from '../types';

interface CheckCodeFormProps {
  onNextStep: (token: string) => void;
}
export const CheckCodeForm: FC<CheckCodeFormProps> = ({ onNextStep }) => {
  const [form] = Form.useForm();
  const [type, setType] = useState<FindWays>(FindWays.Email);
  const [token, setToken] = useState<string>();
  const [ticket, setTicket] = useState('');
  const [submitLoading, setSubmitLoading] = useState(false);
  const t = useI18NPrefix('forgotPassword');
  const tg = useI18NPrefix('global');

  const initialValues = useMemo(() => {
    return { type: FindWays.Email };
  }, []);

  const onFinish = useCallback((values: CaptchaParams) => {
    setSubmitLoading(true);
    captchaforResetPassword(values)
      .then(token => {
        setToken(token);
        setTicket(values?.principal);
      })
      .finally(() => {
        setSubmitLoading(false);
      });
  }, []);

  const isEmail = useMemo(() => {
    return type === FindWays.Email;
  }, [type]);

  const onTypeChange = useCallback(
    e => {
      const v = e.target.value;
      setType(v);
      form.setFieldsValue({ principal: undefined });
      setTicket('');
      setToken('');
    },
    [form],
  );

  const typeOptions = useMemo(
    () =>
      Object.values(FindWays).map(w => ({
        label: t(w.toLowerCase()),
        value: w,
      })),
    [t],
  );

  const ticketFormItem = useMemo(() => {
    return isEmail ? (
      <Form.Item
        name="principal"
        rules={[
          {
            required: true,
            message: `${t('email')}${tg('validation.required')}`,
          },
          {
            type: 'email',
            message: t('emailInvalid'),
          },
        ]}
      >
        <Input size="large" placeholder={t('enterEmail')} />
      </Form.Item>
    ) : (
      <Form.Item
        name="principal"
        rules={[
          {
            required: true,
            message: `${t('username')}${tg('validation.required')}`,
          },
        ]}
      >
        <Input size="large" placeholder={t('enterUsername')} />
      </Form.Item>
    );
  }, [isEmail, t, tg]);

  const goNext = useCallback(() => {
    onNextStep(token as string);
  }, [onNextStep, token]);

  const tips = useMemo(
    () =>
      token && token.length ? (
        <TipsWrapper>
          {t('desc1')}
          {type === FindWays.UserName ? (
            <>
              <b>{ticket}</b>
              <span>{t('desc2')}</span>
            </>
          ) : (
            <b>{ticket}</b>
          )}
          {t('desc3')}
        </TipsWrapper>
      ) : (
        <></>
      ),
    [ticket, type, token, t],
  );

  return (
    <Form initialValues={initialValues} onFinish={onFinish} form={form}>
      <Form.Item name="type">
        <Radio.Group
          size="large"
          options={typeOptions}
          onChange={onTypeChange}
        />
      </Form.Item>
      {ticketFormItem}
      <Form.Item className="last">
        {token ? (
          <>
            {tips}
            <BigButton type="primary" onClick={goNext} size="large">
              {t('nextStep')}
            </BigButton>
          </>
        ) : (
          <BigButton
            loading={submitLoading}
            type="primary"
            htmlType="submit"
            size="large"
          >
            {t('send')}
          </BigButton>
        )}
      </Form.Item>
    </Form>
  );
};

const BigButton = styled(Button)`
  width: 100%;
`;
const TipsWrapper = styled.div`
  margin-bottom: ${SPACE_LG};
`;
