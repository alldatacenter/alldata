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
import { Dropdown, Menu, Avatar } from 'antd';
import { map, get, find } from 'lodash';
import { Icon as CustomIcon } from 'common';
import { getAvatarChars } from 'common/utils';
import { WithAuth } from 'user/common';
import i18n from 'i18n';

import './table-view.scss';

export const memberSelectorValueItem = (user: any) => {
  const { avatar, nick, name, label, value } = user;
  const displayName = nick || label || value || i18n.t('common:none');
  return (
    <div className="flex items-center hover-active issue-field-selector">
      <Avatar src={avatar} size="small">
        {nick ? getAvatarChars(nick) : i18n.t('none')}
      </Avatar>
      <span className={'ml-2 text-sm'} title={name}>
        {displayName}
      </span>
      <CustomIcon className="arrow-icon" type="di" />
    </div>
  );
};

interface IFieldProps {
  hasAuth: boolean;
  options: any[];
  value: string;
  record: ISSUE.Issue;
  field: string;
  updateRecord: (val: string, field: string, record: ISSUE.Issue) => void;
}

export const FieldSelector = (props: IFieldProps) => {
  const { hasAuth, options, value, updateRecord, record, field } = props;
  const chosenVal = get(
    find(options, (op) => op.value === value),
    'iconLabel',
  );
  const ValueRender = (
    <div className="flex items-center hover-active issue-field-selector" onClick={(e: any) => e.stopPropagation()}>
      {chosenVal}
      <CustomIcon type="di" className="arrow-icon" />
    </div>
  );

  if (hasAuth === false) return <WithAuth pass={hasAuth}>{ValueRender}</WithAuth>;

  const onClick = (e: any) => {
    e.domEvent.stopPropagation();
    updateRecord(e.key, field, record);
  };
  const menu = (
    <Menu onClick={onClick}>
      {map(options, (op) => (
        <Menu.Item disabled={op.disabled} key={op.value}>
          {op.iconLabel}
        </Menu.Item>
      ))}
    </Menu>
  );
  return (
    <Dropdown overlay={menu} trigger={['click']}>
      {ValueRender}
    </Dropdown>
  );
};
