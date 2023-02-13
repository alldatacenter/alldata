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
import { goTo } from 'common/utils';
import i18n from 'i18n';

interface ISelectOption {
  name: string;
  value: string;
}

export const getAccountsFieldsList = (selectOptions: ISelectOption[], initialSelect = '') => {
  return [
    {
      name: 'accessKeyID',
      label: 'Access Key ID',
    },
    {
      name: 'accessKeySecret',
      label: 'Access Key Secret',
    },
    {
      name: 'vendor',
      label: i18n.t('vender'),
      type: 'select',
      options: selectOptions,
      initialValue: initialSelect,
    },
    {
      name: 'description',
      label: i18n.t('description'),
      type: 'textArea',
      required: false,
    },
  ];
};

export const addAuthTooltipTitle = (
  <span>
    {i18n.t('cmp:please add ')}&nbsp;
    <span onClick={() => goTo(goTo.pages.cloudAccounts)} className="fake-link">
      {i18n.t('cmp:cloud account')}
    </span>
  </span>
);
