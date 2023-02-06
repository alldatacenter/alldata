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
import { sumBy } from 'lodash';
import { Tooltip } from 'antd';
import { colorMap, statusColorMap } from 'app/config-page/utils';
import './linear-distribution.scss';

const CP_LINEAR_DISTRIBUTION = (props: CP_LINEAR_DISTRIBUTION.Props) => {
  const { props: configProps, data } = props;
  const { size = 'normal' } = configProps || {};
  const { list, total: _total } = data || {};
  const total = _total ?? sumBy(list, 'value');

  const labelArr: JSX.Element[] = [];
  const linearArr: JSX.Element[] = [];
  list?.forEach((item, idx) => {
    const { tip, label, color, value, status } = item;
    const _color = colorMap[color] || statusColorMap[status] || color;
    labelArr.push(
      <div key={`${idx}`} className="cp-linear-distribution-label flex justify-items-center items-center">
        <span className="label-dot rounded-full" style={{ backgroundColor: _color }} />
        <span>{label}</span>
      </div>,
    );
    linearArr.push(
      <Tooltip title={tip} key={`${idx}`}>
        <div className={'h-full cp-linear-distribution-item bg-white'} style={{ width: `${(value / total) * 100}%` }}>
          <span className="block h-full w-full" style={{ backgroundColor: _color }} />
        </div>
      </Tooltip>,
    );
  });

  return (
    <div className="my-4">
      <div className="mb-2 flex">{labelArr}</div>
      <div className={`w-full cp-linear-distribution size-${size}`}>{linearArr}</div>
    </div>
  );
};

export default CP_LINEAR_DISTRIBUTION;
