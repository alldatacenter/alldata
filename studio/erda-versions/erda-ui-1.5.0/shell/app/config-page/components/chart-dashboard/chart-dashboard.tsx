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
import { isEmpty } from 'lodash';
import { BoardGrid, Holder } from 'common';

const DashBoard = React.memo(BoardGrid.Pure);

const ChartDashboard = (props: CP_CHART_DASHBOARD.Props) => {
  const { props: configProps, state } = props;
  const { layout } = configProps || {};
  return (
    <Holder when={isEmpty(layout)}>
      <DashBoard layout={layout} globalVariable={state?.globalVariable} />
    </Holder>
  );
};

export default ChartDashboard;
