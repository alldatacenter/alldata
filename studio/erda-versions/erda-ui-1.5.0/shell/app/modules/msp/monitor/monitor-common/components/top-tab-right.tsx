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
import { ModuleSelector } from 'monitor-common';
import { TimeSelectWithStore } from 'msp/components/time-select';
import './top-nav-right.scss';

interface IProps {
  params?: any;
}

const TopTabRight = (props: IProps) => {
  const { params } = props;
  return (
    <div className="top-nav-right flex justify-between">
      <div>{params && <ModuleSelector query={params} />}</div>
      <TimeSelectWithStore />
    </div>
  );
};

export default TopTabRight;
