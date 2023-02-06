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
import { SplitPage } from 'layout/common';
import { EmptyHolder } from 'common';

const CP_SPLIT_PAGE = (props: CP_SPLIT_PAGE.Props) => {
  const { left, right } = props || {};
  return (
    <SplitPage>
      <SplitPage.Left className="pipeline-manage-left">{left || <EmptyHolder />}</SplitPage.Left>
      <SplitPage.Right className="pipeline-manage-left">{right || <EmptyHolder />}</SplitPage.Right>
    </SplitPage>
  );
};

export default CP_SPLIT_PAGE;
