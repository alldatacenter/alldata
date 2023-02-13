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

import React from 'react';
import Intro from 'common/components/intro';
import { goTo } from 'common/utils';
import i18n from 'i18n';
import { DOC_MSP_API_GATEWAY, DOC_MSP_CONFIG_CENTER, DOC_MSP_LOG_ANALYSIS, DOC_MSP_REGISTER } from 'common/constants';

const introMap = {
  LogAnalyze: {
    name: i18n.t('log analysis'),
    docUrl: DOC_MSP_LOG_ANALYSIS,
  },
  APIGateway: {
    name: i18n.t('API gateway'),
    docUrl: DOC_MSP_API_GATEWAY,
  },
  RegisterCenter: {
    name: i18n.t('dop:registration center'),
    docUrl: DOC_MSP_REGISTER,
  },
  ConfigCenter: {
    name: i18n.t('dop:configCenter'),
    docUrl: DOC_MSP_CONFIG_CENTER,
  },
};
const IntroPage = ({ scope }: { scope: keyof typeof introMap }) => {
  const { name, docUrl } = introMap[scope];
  return (
    <Intro
      content={i18n.t('msp:you have not added {name} in the current environment', { name })}
      action={i18n.t('msp:usage guide')}
      onAction={() => {
        goTo(docUrl, { jumpOut: true });
      }}
    />
  );
};

export default IntroPage;
