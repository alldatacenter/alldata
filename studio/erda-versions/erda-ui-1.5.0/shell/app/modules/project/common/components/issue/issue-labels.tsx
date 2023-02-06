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

import 'project/pages/settings/components/project-label.scss';
import './issue-labels.scss';

export const renderLabels = (labelColorMap: Obj, lbs: string[], clsName = '', maxShow?: number) => {
  if (!lbs || lbs.length === 0) return null;
  const curShowLen = maxShow || lbs.length;

  return (
    <div className={`project-label-list ${clsName}`}>
      {lbs.slice(0, curShowLen).map((l) => (
        <span
          key={l}
          className={`label-item small nowrap text-${labelColorMap[l]} bg-${labelColorMap[l]} bg-opacity-10`}
        >
          {l}
        </span>
      ))}
      {lbs.length > curShowLen ? (
        <Tooltip
          title={
            <div className="project-label-list small">
              {lbs.slice(curShowLen, lbs.length).map((l) => (
                <span
                  key={l}
                  className={`label-item small nowrap text-${labelColorMap[l]} bg-${labelColorMap[l]} bg-opacity-10`}
                >
                  {l}
                </span>
              ))}
            </div>
          }
        >
          <CustomIcon type="more" />
        </Tooltip>
      ) : null}
    </div>
  );
};
