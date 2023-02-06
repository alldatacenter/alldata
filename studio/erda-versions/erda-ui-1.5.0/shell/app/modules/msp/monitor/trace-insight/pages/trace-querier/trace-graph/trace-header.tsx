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
import { Tooltip } from 'antd';
import { Icon as CustomIcon } from 'common';
import './index.scss';
import i18n from 'i18n';
import { displayTimeString } from './utils';

export const TraceHeader = (props: {
  duration: number;
  width: number;
  setExpandedKeys: (params: string[]) => void;
  allKeys: string[];
  expandedKeys: string[];
}) => {
  const { duration, setExpandedKeys, allKeys, expandedKeys, width } = props;
  const avg = duration / 4;
  const pointTimers = [avg, avg * 2, avg * 3, avg * 4];
  const isExpanded = expandedKeys?.length > 0;
  return (
    <div className="trace-header text-gray font-semibold text-sm my-2 p-1 border border-solid border-light-border">
      <div className="text-sub font-semibold flex items-center">
        <span className="left text-sub font-semibold" style={{ width }}>
          Services
        </span>
        <Tooltip title={i18n.t('msp:expand all')}>
          <CustomIcon
            type="chevron-down"
            onClick={() => setExpandedKeys(allKeys)}
            className={`text-xs ${
              isExpanded ? 'text-holder' : 'text-primary cursor-pointer'
            } border-solid border-2 w-4 h-4 flex justify-center items-center`}
          />
        </Tooltip>
        <Tooltip title={i18n.t('msp:fold all')}>
          <CustomIcon
            type="chevron-up"
            onClick={() => setExpandedKeys([])}
            className={`text-xs ${
              isExpanded ? 'text-primary cursor-pointer' : 'text-holder'
            } border-solid border-2 w-4 h-4 flex justify-center items-center ml-2`}
          />
        </Tooltip>
      </div>

      {pointTimers.map((timer, index) => {
        return (
          <div className="right" key={`${`${index}${timer}`}`}>
            {displayTimeString(timer)}
          </div>
        );
      })}
    </div>
  );
};
