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
import { Button } from 'antd';
import { Icon as CustomIcon } from 'common';
import i18n from 'i18n';
import { CaseStatus } from './status-toggle';
import './pass-and-next.scss';

interface IProps {
  hasNext: boolean;
  current?: keyof typeof CaseStatus;
  onClick: (k: keyof typeof CaseStatus) => any;
}

export const PassAndNext = ({ hasNext, current, onClick }: IProps) => {
  return (
    <div className="pass-and-next">
      <Button onClick={() => onClick(CaseStatus.INIT)} disabled={current === CaseStatus.INIT}>
        <CustomIcon className="rounded-full bg-icon text-white" type="wh" />
        {i18n.t('dop:not performed')}
      </Button>
      <Button
        className="border-green"
        onClick={() => onClick(CaseStatus.PASSED)}
        disabled={current === CaseStatus.PASSED}
      >
        <CustomIcon className="rounded-full bg-green text-white" type="tg" />
        {i18n.t('dop:pass')}
      </Button>
      <Button
        className="border-yellow"
        onClick={() => onClick(CaseStatus.BLOCK)}
        disabled={current === CaseStatus.BLOCK}
      >
        <CustomIcon className="rounded-full bg-yellow text-white" type="zs" />
        {i18n.t('dop:blocking')}
      </Button>
      <Button className="border-red" onClick={() => onClick(CaseStatus.FAIL)} disabled={current === CaseStatus.FAIL}>
        <CustomIcon className="rounded-full bg-red text-white" type="wtg" />
        {i18n.t('dop:not passed')}
      </Button>
      {hasNext ? <span className="ml-1">{i18n.t('dop:and next')}</span> : null}
    </div>
  );
};
