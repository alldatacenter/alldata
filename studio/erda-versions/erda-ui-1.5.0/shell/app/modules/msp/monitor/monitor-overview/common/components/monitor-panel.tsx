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
import { Link } from 'react-router-dom';
import { resolvePath } from 'common/utils';
import { Icon as CustomIcon } from 'common';
import './monitor-panel.scss';

interface IProps {
  [pro: string]: any;
  className?: string;
  title?: string;
  children: JSX.Element[] | JSX.Element;
  getCustomComp?: any;
  moreUrl?: string;
}

const MonitorPanel = ({ className, title, children, getCustomComp, moreUrl }: IProps) => {
  return (
    <div className={`monitor-panel ${className}`}>
      <div className="monitor-panel-header font-medium">
        <span className="title">{title}</span>
        <div className="title-right">
          {typeof getCustomComp === 'function' ? getCustomComp() : null}
          {moreUrl ? (
            <Link to={resolvePath(moreUrl)}>
              <CustomIcon className="detail" type="double-right-caret" />
            </Link>
          ) : null}
        </div>
      </div>
      {children}
    </div>
  );
};

export default MonitorPanel;
