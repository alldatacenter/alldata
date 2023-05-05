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
import { Icon as CustomIcon } from 'common';
import { Tooltip, Select } from 'antd';
import { map } from 'lodash';
import { ISSUE_FIELD_TYPES, FIELD_TYPE_ICON_MAP } from 'org/common/config';
import 'org/common/issue-field-icon.scss';

interface IProps {
  type: string;
  withName?: boolean;
}

const { Option } = Select;

export const IssueIcon = ({ type, withName = false }: IProps) => {
  const iconObj = FIELD_TYPE_ICON_MAP[type];
  if (!iconObj) return null;
  const { name, icon, color } = iconObj || {};
  const IconRender = (
    <div className={`issues-field-type-icon-box ${color} mr-2`}>
      <CustomIcon type={icon} color />
    </div>
  );
  return (
    <div className="flex items-center justify-start">
      {withName ? (
        <>
          {IconRender}
          {name}
        </>
      ) : (
        <Tooltip title={name}>{IconRender}</Tooltip>
      )}
    </div>
  );
};

export const getFieldTypeOption = () =>
  map(ISSUE_FIELD_TYPES, (item) => {
    const value: ISSUE_FIELD.IIssueType = FIELD_TYPE_ICON_MAP[item]?.value as ISSUE_FIELD.IIssueType;
    return (
      <Option key={value} value={value}>
        <IssueIcon type={item} withName />
      </Option>
    );
  });
