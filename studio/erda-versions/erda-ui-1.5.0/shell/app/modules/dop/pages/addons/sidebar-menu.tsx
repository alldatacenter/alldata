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

import i18n from 'i18n';
import React from 'react';
import { ErdaIcon } from 'common';

export const getSideMenu = ({ rootPath }: { rootPath: string }) => {
  const sideMenu = [
    {
      href: `${rootPath}/overview`,
      icon: <ErdaIcon type="list-view" />,
      text: i18n.t('dop:addon info'),
    },
    {
      href: `${rootPath}/settings`,
      icon: <ErdaIcon type="config1" />,
      text: i18n.t('dop:addon setting'),
    },
  ];
  return sideMenu;
};
