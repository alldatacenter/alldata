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
import { ErdaAlert } from 'common';
import { isArray, map } from 'lodash';
import './alert.scss';

const CP_Alert = (props: CP_ALERT.Props) => {
  const { props: configProps } = props || {};
  const { message, visible = true, ...rest } = configProps || {};

  if (!visible) return null;
  const msgComp = isArray(message) ? (
    <div>
      {map(message, (item, idx) => (
        <div className="mb-2" key={idx}>
          {item}
        </div>
      ))}
    </div>
  ) : (
    message
  );
  return <ErdaAlert className="config-page-alert" message={msgComp} showIcon {...rest} />;
};

export default CP_Alert;
