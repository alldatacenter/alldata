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
import {
  BoardGrid as DCBoardGrid,
  dcRegisterComp,
  PureBoardGrid as DCPureBoardGrid,
  registTheme,
  setLocale,
  setTheme,
} from '@erda-ui/dashboard-configurator';
import Table from 'common/components/table';
import { theme } from 'app/charts/theme';

registTheme('erda', theme);
dcRegisterComp.use('table', (props: Parameters<typeof Table>['0']) => {
  return <Table hideHeader {...props} />;
});

export const BoardGrid = ({ ...restProps }: DC.BoardGridProps) => {
  const locale = window.localStorage.getItem('locale') || 'zh';
  setLocale(locale);
  setTheme('erda');
  return <DCBoardGrid {...restProps} />;
};

export const PureBoardGrid = ({ ...restProps }: DC.PureBoardGridProps) => {
  const locale = window.localStorage.getItem('locale') || 'zh';
  setLocale(locale);
  setTheme('erda');
  return <DCPureBoardGrid {...restProps} />;
};
BoardGrid.Pure = PureBoardGrid;

export default BoardGrid;
