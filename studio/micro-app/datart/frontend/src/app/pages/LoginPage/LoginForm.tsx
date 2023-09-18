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

import { Button, Form, Input } from 'antd';
import * as AuthLayout from 'app/components/styles/AuthLayout';
import usePrefixI18N from 'app/hooks/useI18NPrefix';
import { User } from 'app/slice/types';
import { StorageKeys } from 'globalConstants';
import React, { useCallback, useState } from 'react';
import { Link, useHistory } from 'react-router-dom';
import styled from 'styled-components/macro';
import {
  BORDER_RADIUS,
  LINE_HEIGHT_ICON_LG,
  LINE_HEIGHT_ICON_XXL,
  SPACE_MD,
  SPACE_XS,
} from 'styles/StyleConstants';
import { getToken } from 'utils/auth';
import persistence from 'utils/persistence';
import { AUTH_CLIENT_ICON_MAPPING } from './constants';

interface LoginFormProps {
  loading: boolean;
  loggedInUser?: User | null;
  oauth2Clients: Array<{ name: string; value: string }>;
  registerEnable?: boolean;
  inShare?: boolean;
  onLogin?: (value) => void;
}

export function LoginForm({
  loading,
  loggedInUser,
  oauth2Clients,
  registerEnable = true,
  inShare = false,
  onLogin,
}: LoginFormProps) {
  const [switchUser, setSwitchUser] = useState(false);
  const history = useHistory();
  const [form] = Form.useForm();
  const logged = !!getToken();
  const t = usePrefixI18N('login');
  const tg = usePrefixI18N('global');

  const toApp = useCallback(() => {
    history.replace('/');
  }, [history]);

  const onSwitch = useCallback(() => {
    setSwitchUser(true);
  }, []);

  const toAuthClient = useCallback(
    clientUrl => () => {
      if (inShare) {
        persistence.session.save(
          StorageKeys.AuthRedirectUrl,
          window.location.href,
        );
      }
      window.location.href = clientUrl;
    },
    [inShare],
  );

  return (
    <AuthLayout.Form>
      {logged && !switchUser && !inShare ? (
        <>
          <h2>{t('alreadyLoggedIn')}</h2>
          <UserPanel onClick={toApp}>
            <p>{loggedInUser?.username}</p>
            <span>{t('enter')}</span>
          </UserPanel>
          <Button type="link" size="large" block onClick={onSwitch}>
            {t('switch')}
          </Button>
        </>
      ) : (
        <Form form={form} onFinish={onLogin}>
          <Form.Item
            name="username"
            rules={[
              {
                required: true,
                message: `${t('username')}${tg('validation.required')}`,
              },
            ]}
          >
            <Input placeholder={t('username')} size="large" />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[
              {
                required: true,
                message: `${t('password')}${tg('validation.required')}`,
              },
            ]}
          >
            <Input placeholder={t('password')} type="password" size="large" />
          </Form.Item>
          <Form.Item className="last" shouldUpdate>
            {() => (
              <Button
                type="primary"
                htmlType="submit"
                size="large"
                loading={loading}
                disabled={
                  loading ||
                  // !form.isFieldsTouched(true) ||
                  !!form.getFieldsError().filter(({ errors }) => errors.length)
                    .length
                }
                block
              >
                {t('login')}
              </Button>
            )}
          </Form.Item>
          {!inShare && (
            <Links>
              <LinkButton to="/forgetPassword">
                {t('forgotPassword')}
              </LinkButton>
              {registerEnable && (
                <LinkButton to="/register">{t('register')}</LinkButton>
              )}
            </Links>
          )}
          {oauth2Clients.length > 0 && (
            <>
              <AuthTitle>{t('authTitle')}</AuthTitle>
              {oauth2Clients.map(({ name, value }) => (
                <AuthButton
                  key={value}
                  size="large"
                  icon={AUTH_CLIENT_ICON_MAPPING[name.toLowerCase()]}
                  onClick={toAuthClient(value)}
                  block
                >
                  {name}
                </AuthButton>
              ))}
            </>
          )}
        </Form>
      )}
    </AuthLayout.Form>
  );
}

const Links = styled.div`
  display: flex;
`;

const LinkButton = styled(Link)`
  flex: 1;
  line-height: ${LINE_HEIGHT_ICON_LG};

  &:nth-child(2) {
    text-align: right;
  }
`;

const AuthTitle = styled.p`
  line-height: ${LINE_HEIGHT_ICON_XXL};
  color: ${p => p.theme.textColorLight};
  text-align: center;
`;

const AuthButton = styled(Button)`
  margin-bottom: ${SPACE_XS};

  &:last-child {
    margin-bottom: 0;
  }
`;

const UserPanel = styled.div`
  display: flex;
  padding: ${SPACE_MD};
  margin: ${SPACE_MD} 0;
  cursor: pointer;
  background-color: ${p => p.theme.bodyBackground};
  border-radius: ${BORDER_RADIUS};

  &:hover {
    background-color: ${p => p.theme.emphasisBackground};
  }

  p {
    flex: 1;
  }

  span {
    color: ${p => p.theme.textColorLight};
  }
`;
