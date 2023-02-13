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
import i18n from 'i18n';
import { get } from 'lodash';
import routeInfoStore from 'core/stores/route';
import Info from './info';
import Database from './database';
import Account from './rds-account';

export const rdsTabs = {
  info: {
    key: 'info',
    name: i18n.t('dop:basic information'),
    content: <Info />,
  },
  database: {
    key: 'database',
    name: i18n.t('cmp:database management'),
    content: <Database />,
  },
  account: {
    key: 'account',
    name: i18n.t('account management'),
    content: <Account />,
  },
};

const RedisManager = () => {
  const tabKey = routeInfoStore.useStore((s) => s.params.tabKey);
  return get(rdsTabs, `${tabKey}.content`) || null;
};

export default RedisManager;
