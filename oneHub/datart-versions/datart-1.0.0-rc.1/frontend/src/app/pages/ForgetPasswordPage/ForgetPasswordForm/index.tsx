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

import * as AuthLayout from 'app/components/styles/AuthLayout';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import React, { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import styled from 'styled-components/macro';
import { LINE_HEIGHT_ICON_LG } from 'styles/StyleConstants';
import { CheckCodeForm } from './CheckCodeForm';
import { ResetPasswordForm } from './ResetPasswordForm';

export function ForgetPasswordForm() {
  const [isCheckForm, setIsCheckForm] = useState(true);
  const [token, setToken] = useState('');
  const t = useI18NPrefix('forgotPassword');

  const onNextStep = useCallback((token: string) => {
    setIsCheckForm(false);
    setToken(token);
  }, []);
  return (
    <AuthLayout.Form>
      {isCheckForm ? (
        <CheckCodeForm onNextStep={onNextStep} />
      ) : (
        <ResetPasswordForm token={token} />
      )}
      <LinkButton to="/login">{t('return')}</LinkButton>
    </AuthLayout.Form>
  );
}

const LinkButton = styled(Link)`
  display: block;
  line-height: ${LINE_HEIGHT_ICON_LG};
`;
