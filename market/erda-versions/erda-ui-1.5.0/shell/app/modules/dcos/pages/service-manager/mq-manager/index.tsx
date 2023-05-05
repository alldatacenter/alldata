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
import MQInfoBox from './info';
import Topic from './topic';
import Group from './group';

export const mqTabs = {
  info: {
    key: 'info',
    name: i18n.t('dop:basic information'),
    content: <MQInfoBox />,
  },
  topic: {
    key: 'topic',
    name: i18n.t('cmp:topic management'),
    content: <Topic />,
  },
  group: {
    key: 'group',
    name: i18n.t('cmp:group management'),
    content: <Group />,
  },
};

const MQManager = () => {
  const tabKey = routeInfoStore.useStore((s) => s.params.tabKey);
  return get(mqTabs, `${tabKey}.content`) || null;
};

export default MQManager;
