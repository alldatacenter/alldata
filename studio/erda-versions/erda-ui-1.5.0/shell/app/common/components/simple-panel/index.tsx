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

interface IPanel {
  title: string;
  children: any;
  style?: React.CSSProperties;
  className?: string;
}
const SimplePanel = ({ title, children, style, className = '' }: IPanel) => {
  return (
    <div className={`ec-simple-panel ${className}`} style={style}>
      <div className="ec-simple-panel-title">{title}</div>
      <div className="ec-simple-panel-body">{children}</div>
    </div>
  );
};

export default SimplePanel;
