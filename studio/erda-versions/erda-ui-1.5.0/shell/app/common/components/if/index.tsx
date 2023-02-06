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

const IF = ({ children, check }: any) => {
  if (!children) return null;
  const bool = typeof check === 'function' ? check() : check;

  if (React.Children.count(children) === 1) {
    return bool ? children : null;
  }
  const ifSection = [] as any;
  const elseSection = [] as any;
  let hasElse = false;
  React.Children.forEach(children, (child) => {
    if (child.type && child.type.displayName === 'ELSE') {
      hasElse = true;
    }
    if (hasElse) {
      elseSection.push(child);
    } else {
      ifSection.push(child);
    }
  });
  if (bool) {
    return <>{ifSection}</>;
  } else if (hasElse) {
    return <>{elseSection}</>;
  }
  return null;
};

IF.ELSE = ELSE;

function ELSE() {
  return null;
}

ELSE.displayName = 'ELSE';

export default IF;
