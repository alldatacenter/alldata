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

// 同名的css已经在最外的skeleton.css引过了，所以这里不需要额外加scss
export const LoadingSkeleton = () => {
  return (
    <>
      <div className="skeleton-header">
        <div className="header-row">
          <div className="skeleton-line skeleton-bg" style={{ width: '100px' }} />
        </div>
        <div className="header-row">
          <div className="skeleton-line skeleton-bg" style={{ width: '200px' }} />
        </div>
      </div>
      <div className="main-holder">
        <div id="enter-loading" />
      </div>
    </>
  );
};

export default LoadingSkeleton;
