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

import { InfoCircleOutlined } from '@ant-design/icons';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import React, { memo } from 'react';
import { useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import {
  LINE_HEIGHT_BODY,
  LINE_HEIGHT_HEADING,
  SPACE_MD,
  SPACE_TIMES,
  SPACE_XS,
} from 'styles/StyleConstants';
import { selectCurrentEditingViewAttr } from '../../slice/selectors';

export const Error = memo(() => {
  const error = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'error' }),
  ) as string;
  const t = useI18NPrefix('view');

  return (
    <Wrapper>
      <h3>
        <InfoCircleOutlined className="icon" />
        {t('errorTitle')}
      </h3>
      <p>{error}</p>
    </Wrapper>
  );
});

const Wrapper = styled.div`
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  padding: ${SPACE_MD};
  overflow-y: auto;
  color: ${p => p.theme.error};
  background-color: ${p => p.theme.bodyBackground};

  .icon {
    margin-right: ${SPACE_XS};
  }

  h3 {
    line-height: ${LINE_HEIGHT_HEADING};
  }

  p {
    padding: 0 ${SPACE_TIMES(6)};
    line-height: ${LINE_HEIGHT_BODY};
  }
`;
