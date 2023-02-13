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
import './index.scss';

interface IProps {
  children?: React.ReactNode;
  className?: string;
  pageHeader?: React.ReactElement;
  globalNavigation: React.ReactElement;
  sideNavigation?: React.ReactElement;
}

const Shell = ({ children, className, pageHeader, globalNavigation, sideNavigation }: IProps) => {
  return (
    <div className={`erda-shell h-full ${className || ''}`}>
      <div className="flex flex-row h-full flex-auto">
        <div className="h-full flex flex-row">
          <div className="h-full relative">{globalNavigation}</div>

          <div className="flex flex-auto flex-col overflow-x-hidden h-full relative">{sideNavigation}</div>
        </div>
        <div className="flex flex-auto flex-col h-full overflow-x-hidden">
          {pageHeader}
          <div className="erda-main-content relative overflow-auto">{children}</div>
        </div>
      </div>
    </div>
  );
};

export default Shell;
