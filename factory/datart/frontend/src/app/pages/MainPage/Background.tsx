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

import {
  AppstoreAddOutlined,
  ReloadOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import React, { useCallback, useState } from 'react';
import { useSelector } from 'react-redux';
import styled, { keyframes } from 'styled-components/macro';
import {
  FONT_SIZE_ICON_MD,
  FONT_WEIGHT_MEDIUM,
  LEVEL_1000,
  SPACE_TIMES,
} from 'styles/StyleConstants';
import { OrganizationForm } from './OrganizationForm';
import {
  selectInitializationError,
  selectOrganizations,
  selectUserSettingLoading,
  selectUserSettings,
} from './slice/selectors';

export function Background() {
  const [formVisible, setFormVisible] = useState(false);
  const userSettings = useSelector(selectUserSettings);
  const organizations = useSelector(selectOrganizations);
  const userSettingLoading = useSelector(selectUserSettingLoading);
  const error = useSelector(selectInitializationError);
  const t = useI18NPrefix('main.background');

  const showForm = useCallback(() => {
    setFormVisible(true);
  }, []);

  const hideForm = useCallback(() => {
    setFormVisible(false);
  }, []);

  let visible = true;
  let content;

  if (userSettingLoading) {
    content = (
      <Hint>
        <SettingOutlined className="img loading" />
        <p>{t('loading')}</p>
      </Hint>
    );
  } else if (error) {
    content = (
      <Hint>
        <a href="/">
          <ReloadOutlined className="img" />
        </a>
        <p>{t('initError')}</p>
      </Hint>
    );
  } else if (
    !userSettingLoading &&
    !(userSettings && userSettings.length) &&
    !organizations.length
  ) {
    content = (
      <>
        <Hint className="add" onClick={showForm}>
          <AppstoreAddOutlined className="img" />
          <p>{t('createOrg')}</p>
        </Hint>
        <OrganizationForm visible={formVisible} onCancel={hideForm} />
      </>
    );
  } else {
    visible = false;
  }

  return <Container visible={visible}>{content}</Container>;
}

const loadingAnimation = keyframes`
  0% { transform: rotate(0deg); }
  50% { transform: rotate(180deg); }
  100% { transform: rotate(360deg); }
`;

const Container = styled.div<{ visible: boolean }>`
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: ${LEVEL_1000};
  display: ${p => (p.visible ? 'flex' : 'none')};
  flex: 1;
  align-items: center;
  justify-content: center;
  background-color: ${p => p.theme.bodyBackground};
`;

const Hint = styled.div`
  display: flex;
  flex-direction: column;

  &.add {
    cursor: pointer;
  }

  .img {
    display: block;
    font-size: ${SPACE_TIMES(16)};
    color: ${p => p.theme.textColorLight};

    &.loading {
      animation: ${loadingAnimation} 2s linear;
    }
  }

  p {
    font-size: ${FONT_SIZE_ICON_MD};
    font-weight: ${FONT_WEIGHT_MEDIUM};
    line-height: ${SPACE_TIMES(16)};
    color: ${p => p.theme.textColorLight};
    user-select: none;
  }
`;
