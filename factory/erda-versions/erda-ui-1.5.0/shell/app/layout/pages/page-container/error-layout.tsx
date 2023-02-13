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

import Header from 'layout/pages/page-container/components/header';
import SideBar from 'layout/pages/page-container/components/sidebar';
import React from 'react';
import './page-container.scss';
import './error-layout.scss';

interface IProps {
  layoutClass: string;
  children: React.ReactChildren;
}

export const ErrorLayout = ({ layoutClass, children }: IProps) => {
  return (
    <div className={layoutClass}>
      <SideBar />
      <div className="dice-body">
        <Header />
        <div id="main">{children}</div>
      </div>
    </div>
  );
};
