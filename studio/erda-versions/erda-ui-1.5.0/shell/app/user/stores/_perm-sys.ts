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

export const sysRoleMap = {
  Manager: { name: i18n.t('user:System Administrator'), value: 'Manager' },
  Auditor: { name: i18n.t('user:Auditors'), value: 'Auditor' },
};

export const sysPerm = {
  name: i18n.t('dop:system'),
  view: {
    role: ['Manager'],
    pass: false,
    name: i18n.t(
      'user:view organization management/user management/global configuration/cluster management pages (pages other than audit logs)',
    ),
  },
};
