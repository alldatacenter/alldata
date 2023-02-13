// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import { Icon as CustomIcon } from 'common';
import i18n from 'i18n';
import React from 'react';
import './env-card.scss';

interface IProps {
  type: string;
}

const envMap = {
  DEV: {
    icon: 'kf',
    text: i18n.t('dev environment'),
    subText: 'Development',
  },
  TEST: {
    icon: 'cs',
    text: i18n.t('test environment'),
    subText: 'Test',
  },
  STAGING: {
    icon: 'yf',
    text: i18n.t('staging environment'),
    subText: 'Staging',
  },
  PROD: {
    icon: 'sc',
    text: i18n.t('prod environment'),
    subText: 'Production',
  },
};

export const EnvCard = ({ type }: IProps) => {
  return (
    <div className="env-card-container">
      <div className="env-card">
        <div className="env-icon">
          <CustomIcon color style={{ width: '40px', height: '40px' }} type={envMap[type].icon} className="stage-icon" />
        </div>
        <div className="env-name-container">
          <span className="env-name font-bold nowrap">{envMap[type].text}</span>
          <span className="env-sub-name">{envMap[type].subText}</span>
        </div>
      </div>
      <span className="before-arrow triangle-left">
        <span />
      </span>
      <span className="after-arrow triangle-left">
        <span />
      </span>
    </div>
  );
};
