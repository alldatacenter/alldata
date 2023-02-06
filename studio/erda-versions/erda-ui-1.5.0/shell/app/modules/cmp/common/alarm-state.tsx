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

import { Badge } from 'common';
import React from 'react';
import i18n from 'i18n';

const ALARM_STATE_MAP = {
  alert: { status: 'warning', label: i18n.t('cmp:alarm') },
  recover: { status: 'success', label: i18n.t('cmp:recover') },
};

interface IProps {
  state: string;
}

export const AlarmState = (props: IProps) => {
  const { state } = props;
  const { status, label } = ALARM_STATE_MAP[state] || {};

  return (
    <Badge status={status} text={label}/>
  );
};

