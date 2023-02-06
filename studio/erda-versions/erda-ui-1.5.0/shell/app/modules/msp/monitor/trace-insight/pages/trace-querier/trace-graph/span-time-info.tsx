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
import { mkDurationStr } from 'trace-insight/common/utils/traceSummary';

interface IProps {
  totalSpanTime: number;
  selfSpanTime: number;
}

export const SpanTimeInfo = ({ totalSpanTime, selfSpanTime }: IProps) => (
  <div className="flex justify-center overflow-hidden">
    <div className="border-0 border-r border-solid border-grey flex flex-col items-center px-6 py-1">
      <div className="flex justify-center font-semibold ">
        <span className="text-navy text-base whitespace-nowrap">{mkDurationStr(selfSpanTime / 1000)}</span>
      </div>
      <div className="text-sm text-darkgray whitespace-nowrap ">{i18n.t('msp:current span time')}</div>
    </div>
    <div className="flex flex-col items-center px-6 py-1">
      <div className="flex justify-center font-semibold">
        <span className="text-navy text-base whitespace-nowrap">{mkDurationStr(totalSpanTime / 1000)}</span>
      </div>
      <div className="text-sm text-darkgray whitespace-nowrap">{i18n.t('msp:total span time')}</div>
    </div>
  </div>
);
