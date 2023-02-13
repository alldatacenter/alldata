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
import { IF } from 'common';
import DomainSelector from './domain-selector';
import { TimeSelectWithStore } from 'msp/components/time-select';
import './tab-right.scss';

const TabRight = ({ type = '', children }: { type?: string; children?: any }) => {
  return (
    <div className="gi-top-nav-right filter-box flex justify-between mb-3">
      <div>
        <IF check={['qps', 'traffic', 'latency'].includes(type)}>
          <DomainSelector />
        </IF>
        {children || null}
      </div>
      <TimeSelectWithStore />
    </div>
  );
};

export default TabRight;
