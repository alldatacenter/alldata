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
import { map, isArray } from 'lodash';
import Text from '../text/text';
import './text-group.scss';

const TextGroup = (props: CP_TEXT_GROUP.Props) => {
  const { execOperation, updateState, props: configProps, operations, type } = props;
  const { value, visible = true, gapSize = 'normal', align = 'left' } = configProps || {};

  if (!visible) return null;

  if (isArray(value)) {
    return (
      <div className={`dice-cp-text-group ${align}`}>
        {map(value, (item, index) => (
          <div key={`${index}`} className={`${item?.gapSize || gapSize}`}>
            <Text
              type="Text"
              execOperation={execOperation}
              updateState={updateState}
              props={item?.props}
              operations={operations}
            />
          </div>
        ))}
      </div>
    );
  }

  return null;
};

export default TextGroup;
