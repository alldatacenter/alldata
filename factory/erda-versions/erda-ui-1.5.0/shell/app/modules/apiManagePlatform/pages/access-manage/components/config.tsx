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

import i18n from 'i18n';

export const authenticationMap: { [k in API_ACCESS.AuthenticationEnum]: { name: string; value: k } } = {
  'key-auth': {
    value: 'key-auth',
    name: i18n.t('msp:key authentication'),
  },
  'sign-auth': {
    value: 'sign-auth',
    name: i18n.t('msp:parameter signature authentication'),
  },
};

export const authorizationMap: { [k in API_ACCESS.AuthorizationEnum]: { name: string; value: k } } = {
  auto: { value: 'auto', name: i18n.t('automatic authorization') },
  manual: { value: 'manual', name: i18n.t('manual authorization') },
};

export const contractStatueMap: {
  [k in API_ACCESS.ContractStatue]: {
    value: k;
    name: string;
    actions: Array<{
      name: string;
      action: string;
      value: API_ACCESS.ContractStatue | 'delete';
    }>;
  };
} = {
  // 已通过
  proved: {
    value: 'proved',
    name: i18n.t('passed'),
    actions: [
      {
        name: i18n.t('revoke'),
        action: 'revoke',
        value: 'unproved',
      },
      {
        name: i18n.t('delete'),
        action: 'delete',
        value: 'delete',
      },
    ],
  },
  // 待审核
  proving: {
    value: 'proving',
    name: i18n.t('pending'),
    actions: [
      {
        name: i18n.t('agree'),
        action: 'agree',
        value: 'proved',
      },
      {
        name: i18n.t('refuse'),
        action: 'refuse',
        value: 'disproved',
      },
    ],
  },
  // 已拒绝
  disproved: {
    value: 'disproved',
    name: i18n.t('rejected'),
    actions: [
      {
        name: i18n.t('delete'),
        action: 'delete',
        value: 'delete',
      },
    ],
  },
  // 已撤销
  unproved: {
    value: 'unproved',
    name: i18n.t('revoked'),
    actions: [
      {
        name: i18n.t('restore'),
        action: 'restore',
        value: 'proved',
      },
      {
        name: i18n.t('delete'),
        action: 'delete',
        value: 'delete',
      },
    ],
  },
};
export const envMap = {
  DEV: i18n.t('develop'),
  TEST: i18n.t('test'),
  STAGING: i18n.t('staging'),
  PROD: i18n.t('prod'),
};

export const slaUnitMap: { [k in API_ACCESS.SlaLimitUnit]: string } = {
  s: i18n.t('msp:times/second'),
  m: i18n.t('msp:times/minute'),
  h: i18n.t('msp:times/hour'),
  d: i18n.t('msp:times/day'),
};

export const slaAuthorizationMap: { [k in API_ACCESS.SlaApproval]: { name: string; value: k } } = {
  auto: { value: 'auto', name: i18n.t('automatic authorization') },
  manual: { value: 'manual', name: i18n.t('manual authorization') },
};

export const addonStatusMap: { [k in API_ACCESS.AddonStatus]: { status: k; name: string } } = {
  PENDING: {
    status: 'PENDING',
    name: i18n.t('runtime:to be published'),
  },
  ATTACHING: {
    status: 'ATTACHING',
    name: i18n.t('dop:starting'),
  },
  ATTACHED: {
    status: 'ATTACHED',
    name: i18n.t('running'),
  },
  ATTACHFAILED: {
    status: 'ATTACHFAILED',
    name: i18n.t('startup failed'),
  },
  DETACHING: {
    status: 'DETACHING',
    name: i18n.t('dop:deleting'),
  },
  DETACHED: {
    status: 'DETACHED',
    name: i18n.t('deleted'),
  },
  OFFLINE: {
    status: 'OFFLINE',
    name: i18n.t('not started'),
  },
  UPGRADE: {
    status: 'UPGRADE',
    name: i18n.t('upgraded'),
  },
  ROLLBACK: {
    status: 'ROLLBACK',
    name: i18n.t('rolled back'),
  },
  UNKNOWN: {
    status: 'UNKNOWN',
    name: i18n.t('unknown'),
  },
};
