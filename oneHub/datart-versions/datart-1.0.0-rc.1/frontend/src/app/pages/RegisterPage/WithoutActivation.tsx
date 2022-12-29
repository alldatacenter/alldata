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

import { CheckCircleOutlined } from '@ant-design/icons';
import { Button } from 'antd';
import * as AuthLayout from 'app/components/styles/AuthLayout';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { FC, useCallback } from 'react';
import { useHistory } from 'react-router-dom';
import styled from 'styled-components/macro';
import { SPACE_MD, SPACE_TIMES, SPACE_XS } from 'styles/StyleConstants';

interface WithoutActivationProps {
  onContinue: () => void;
}

export const WithoutActivation: FC<WithoutActivationProps> = ({
  onContinue,
}) => {
  const history = useHistory();
  const t = useI18NPrefix('register');

  const toLogin = useCallback(() => {
    history.push('/login');
  }, [history]);

  return (
    <AuthLayout.Form>
      <Success />
      <Title>{t('registerSuccess')}</Title>
      <Content>
        {t('tipDesc5')}
        <Button type="link" size="small" onClick={toLogin}>
          {t('goLogin')}
        </Button>
        {t('tipDesc6')}
        <Button type="link" size="small" onClick={onContinue}>
          {t('continue')}
        </Button>
      </Content>
    </AuthLayout.Form>
  );
};

const Success = styled(CheckCircleOutlined)`
  display: block;
  padding-bottom: ${SPACE_MD};
  font-size: ${SPACE_TIMES(12)};
  color: ${p => p.theme.success};
  text-align: center;
`;

const Title = styled.h1`
  padding-bottom: ${SPACE_XS};
  text-align: center;
`;

const Content = styled.p`
  margin: ${SPACE_XS} 0;
  text-align: center;
`;
