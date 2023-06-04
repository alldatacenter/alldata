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

import { message } from 'antd';
import { EmptyFiller } from 'app/components/EmptyFiller';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useCallback, useEffect } from 'react';
import { useHistory } from 'react-router';
import styled from 'styled-components';
import { confirmInvite } from './service';

export const ConfirmInvitePage = () => {
  const history = useHistory();
  const t = useI18NPrefix('confirmInvite');

  const onConfirm = useCallback(
    token => {
      confirmInvite(token).then(() => {
        message.success(t('join'));
        history.replace('/');
      });
    },
    [history, t],
  );
  useEffect(() => {
    const searchParams = new URLSearchParams(window.location.search);
    const token = searchParams.get('invite_token');
    if (token) {
      onConfirm(token);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return (
    <Wrapper>
      <EmptyFiller loading title={t('confirming')} />
    </Wrapper>
  );
};

const Wrapper = styled.div`
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
`;
