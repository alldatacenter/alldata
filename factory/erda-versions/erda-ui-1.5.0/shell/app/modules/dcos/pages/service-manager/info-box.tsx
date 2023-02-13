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
import { Title, Panel } from 'common';
import { map, get } from 'lodash';

interface IProps {
  details: CLOUD_SERVICE.IDetailsResp[];
  pannelProps?: Obj;
}

const InfoBox = (props: IProps) => {
  const { details, pannelProps = {} } = props;
  return (
    <div>
      {map(details, (detail) => {
        return (
          <div key={detail.label} className="mb-3">
            <Title level={2} title={detail.label} />
            <Panel
              fields={map(get(detail, 'items'), (item) => {
                return {
                  label: item.name,
                  value: item.value,
                };
              })}
              {...pannelProps}
            />
          </div>
        );
      })}
    </div>
  );
};

export default InfoBox;
