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

import { isEmpty } from 'lodash';
import React from 'react';
import moment from 'moment';
import { KeyValueList } from 'common';

interface IProps {
  data?: RELEASE.detail;
}

const ReleaseDetailInfo = ({ data }: IProps) => {
  if (!data || isEmpty(data)) {
    return null;
  }

  const listRender = (list: string[]) => {
    return (
      <ul className="image-list">
        {list.map((image: string) => (
          <li key={image} className="image-item">
            {image}
          </li>
        ))}
      </ul>
    );
  };

  const copy = { ...data };
  if (copy.createdAt) copy.createdAt = moment(copy.createdAt).format('YYYY-MM-DD HH:mm:ss');
  if (copy.updatedAt) copy.updatedAt = moment(copy.updatedAt).format('YYYY-MM-DD HH:mm:ss');
  return (
    <div className="release-detail-page">
      <KeyValueList data={{ Image: copy }} listRender={listRender} markdownTextFields={['desc']} shrink />
    </div>
  );
};

export default ReleaseDetailInfo;
