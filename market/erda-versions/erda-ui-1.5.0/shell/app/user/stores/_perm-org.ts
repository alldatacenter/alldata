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

// 通过权限配置页面导出角色数据覆盖，勿手动修改
export const orgRoleMap = {
  Manager: { name: i18n.t('user:Org Manager'), value: 'Manager' },
  Auditor: { name: i18n.t('user:Org Auditor'), value: 'Auditor' },
  Dev: { name: i18n.t('user:Org Developer'), value: 'Dev' },
  Ops: { name: i18n.t('user:Org Operator'), value: 'Ops' },
  EdgeOps: { name: i18n.t('user:Edge Operator'), value: 'EdgeOps' },
  DataManager: { name: i18n.t('user:Data manager'), value: 'DataManager' },
  DataEngineer: { name: i18n.t('user:Data Developer'), value: 'DataEngineer' },
  PublisherManager: { name: i18n.t('user:Release Manager'), value: 'PublisherManager' },
  Support: { name: i18n.t('user:Supporter'), value: 'Support', isBuildIn: true }, // 内置角色
  Reporter: { name: i18n.t('user:Reporter'), value: 'Reporter' },
  Guest: { name: i18n.t('user:Guest'), value: 'Guest' },
};

// 通过权限配置页面导出数据覆盖，勿手动修改
export const orgPerm = {
  name: i18n.t('org'),
  dop: {
    name: i18n.t('user:DevOps platform'),
    read: {
      pass: false,
      name: i18n.t('dop:view'),
      role: ['Manager', 'Dev', 'Support', 'DataManager', 'Reporter', 'PublisherManager', 'Guest'],
    },
    apiManage: {
      name: i18n.t('user:API management'),
      read: {
        pass: false,
        role: ['Manager', 'Dev', 'Support', 'DataManager', 'Reporter', 'PublisherManager', 'Guest'],
        name: i18n.t('dop:view'),
      },
    },
    addonService: {
      name: i18n.t('addon service'),
      read: {
        pass: false,
        role: ['Manager', 'Dev', 'Support', 'DataManager', 'Reporter', 'PublisherManager', 'Guest'],
        name: i18n.t('dop:view'),
      },
    },
    publisher: {
      name: i18n.t('publisher:my release'),
      read: {
        pass: false,
        role: ['Manager', 'Dev', 'Support', 'DataManager', 'Reporter', 'PublisherManager', 'Guest'],
        name: i18n.t('dop:view'),
      },
    },
  },
  entryMsp: {
    pass: false,
    name: i18n.t('msp'),
    role: ['Manager', 'Dev', 'Support', 'DataManager'],
  },
  entryFastData: {
    pass: false,
    name: i18n.t('Fast data'),
    role: ['Manager', 'DataManager', 'DataEngineer', 'Support'],
  },
  entryOrgCenter: {
    pass: false,
    name: i18n.t('user:Org Center'),
    role: ['Manager', 'Support', 'Auditor'],
  },
  apiAssetEdit: {
    pass: false,
    name: i18n.t('user:API resource editing'),
    role: ['Manager'],
  },
  cmp: {
    name: i18n.t('user:cloud Management Platform'),
    showApp: {
      pass: false,
      role: ['Manager', 'Ops', 'Support'],
      name: i18n.t('user:application menu display'),
    },
    alarms: {
      name: i18n.t('O & M alarm'),
      addNotificationGroup: {
        pass: false,
        role: ['Manager', 'Support'],
        name: i18n.t('user:add notification group'),
      },
    },
  },
  publisher: {
    name: i18n.t('user:release management'),
    operation: {
      pass: false,
      role: ['PublisherManager', 'Manager'],
      name: i18n.t('user:operation (adding/publishing/unShelve, etc.)'),
    },
  },
  ecp: {
    name: i18n.t('ecp:Edge computing'),
    view: {
      pass: false,
      role: ['Manager', 'EdgeOps', 'Support'],
      name: i18n.t('dop:view'),
    },
    operate: {
      pass: false,
      role: ['Manager', 'EdgeOps'],
      name: i18n.t('user:operation (new/edit/delete/publish/offline/restart)'),
    },
  },
  orgCenter: {
    name: i18n.t('orgCenter'),
    viewAuditLog: {
      pass: false,
      role: ['Manager', 'Auditor', 'Support'],
      name: i18n.t('user:view audit log'),
    },
    viewAnnouncement: {
      pass: false,
      role: ['Manager', 'Support'],
      name: i18n.t('user:view Announcement Management'),
    },
    viewSetting: {
      pass: false,
      role: ['Manager', 'Support'],
      name: i18n.t('user:view Organization Settings'),
    },
    viewApproval: {
      pass: false,
      role: ['Manager', 'Support'],
      name: i18n.t('user:view Approval Management'),
    },
    viewCertificate: {
      pass: false,
      role: ['Manager', 'Support'],
      name: i18n.t('user:view certificate management'),
    },
    viewMarket: {
      pass: false,
      role: ['Manager', 'Support'],
      name: i18n.t('user:view Market Management'),
    },
    viewProjects: {
      pass: false,
      role: ['Manager', 'Support'],
      name: i18n.t('user:view project management'),
    },
  },
};
