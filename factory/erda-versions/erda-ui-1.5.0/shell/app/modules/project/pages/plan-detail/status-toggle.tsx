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
import { Menu, Dropdown } from 'antd';
import { Icon as CustomIcon, ErdaIcon } from 'common';
import { map } from 'lodash';
import i18n from 'i18n';

export enum CaseStatus {
  INIT = 'INIT',
  PASSED = 'PASSED',
  FAIL = 'FAIL',
  BLOCK = 'BLOCK',
}

export const caseStateMap = {
  INIT: { name: i18n.t('dop:not performed'), value: 'INIT' },
  PASSED: { name: i18n.t('passed'), value: 'PASSED' },
  FAIL: { name: i18n.t('dop:not passed'), value: 'FAIL' },
  BLOCK: { name: i18n.t('dop:blocking'), value: 'BLOCK' },
};

interface IProps {
  isPlan?: boolean;
  state: TEST_CASE.CaseResult | TEST_PLAN.PlanStatus;
  onChange: (k: string) => any;
}

export const StatusToggle = ({ isPlan, state, onChange }: IProps) => {
  const curState = state || CaseStatus.INIT;

  let stateMap = {
    INIT: {
      child: (
        <span>
          <CustomIcon className="bg-icon rounded-full text-white" type="wh" />
          <span className="text-dark-6">{i18n.t('dop:not performed')}</span>
        </span>
      ),
    },
    PASSED: {
      child: (
        <span>
          <CustomIcon className="bg-green rounded-full text-white" type="tg" />
          <span className="text-green">{i18n.t('passed')}</span>
        </span>
      ),
    },
    FAIL: {
      child: (
        <span>
          <CustomIcon className="bg-red rounded-full text-white" type="wtg" />
          <span className="text-red">{i18n.t('dop:not passed')}</span>
        </span>
      ),
    },
    BLOCK: {
      child: (
        <span>
          <CustomIcon className="bg-yellow rounded-full text-white" type="zs" />
          <span className="text-yellow">{i18n.t('dop:blocking')}</span>
        </span>
      ),
    },
  } as any;
  if (isPlan) {
    stateMap = {
      PAUSE: {
        child: (
          <span>
            <CustomIcon className="bg-yellow rounded-full text-white" type="zs" />
            <span className="text-yellow">{i18n.t('pause')}</span>
          </span>
        ),
      },
      DOING: {
        child: (
          <span>
            <CustomIcon className="bg-blue rounded-full text-white" type="wh" />
            <span className="text-blue">{i18n.t('processing')}</span>
          </span>
        ),
      },
      DONE: {
        child: (
          <span>
            <CustomIcon className="bg-green rounded-full text-white" type="tg" />
            <span className="text-green">{i18n.t('dop:completed')}</span>
          </span>
        ),
      },
    };
  }

  const handleClick = ({ key, domEvent }: any) => {
    domEvent.stopPropagation();
    onChange(key);
  };
  const menu = (
    <Menu onClick={handleClick}>
      {map(stateMap, (v, k) => {
        return <Menu.Item key={k}>{v.child}</Menu.Item>;
      })}
    </Menu>
  );

  return (
    <Dropdown overlay={menu} placement="bottomRight">
      <span className="flex">
        {(stateMap[curState] || {}).child} <ErdaIcon type="down" size="16" />
      </span>
    </Dropdown>
  );
};
