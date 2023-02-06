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
export const appRoleMap = {
  Owner: { name: i18n.t('user:application owner'), value: 'Owner' },
  Lead: { name: i18n.t('user:application Supervisor'), value: 'Lead' },
  Dev: { name: i18n.t('user:Developer'), value: 'Dev' },
  QA: { name: i18n.t('user:Tester'), value: 'QA' },
  Support: { name: i18n.t('user:Q&A'), value: 'Support', isBuildIn: true },
  Ops: { name: i18n.t('cmp:operator'), value: 'Ops' },
  Creator: { name: i18n.t('user:Creator'), value: 'Creator', isCustomRole: true },
  Assignee: { name: i18n.t('user:Assignee'), value: 'Assignee', isCustomRole: true },
  Guest: { name: i18n.t('user:Guest'), value: 'Guest' },
};

// 通过权限配置页面导出数据覆盖，勿手动修改
export const appPerm = {
  name: i18n.t('application'),
  externalRepo: {
    name: i18n.t('user:external code repository'),
    edit: {
      pass: false,
      name: i18n.t('edit'),
      role: ['Owner', 'Lead'],
    },
  },
  repo: {
    name: i18n.t('dop:repository'),
    read: {
      pass: false,
      name: i18n.t('dop:view'),
      role: ['Owner', 'Lead', 'Dev', 'QA', 'Support', 'Ops', 'Guest'],
    },
    branch: {
      name: i18n.t('dop:branch management'),
      writeNormal: {
        pass: false,
        name: i18n.t('user:ordinary branch management (create/commit/modify/delete)'),
        role: ['Owner', 'Lead', 'Dev', 'QA'],
      },
      writeProtected: {
        pass: false,
        name: i18n.t('user:protect branch management (create/commit/modify/delete)'),
        role: ['Owner', 'Lead'],
      },
      setDefaultBranch: {
        pass: false,
        role: ['Owner', 'Lead'],
        name: i18n.t('user:set default branch'),
      },
      addTag: {
        pass: false,
        role: ['Owner', 'Lead', 'Dev', 'QA', 'Support', 'Ops'],
        name: i18n.t('dop:add label'),
      },
      deleteTag: {
        pass: false,
        role: ['Lead', 'Owner', 'Dev', 'QA', 'Support', 'Ops'],
        name: i18n.t('user:remove label'),
      },
    },
    mr: {
      name: i18n.t('dop:merge requests'),
      create: {
        pass: false,
        name: i18n.t('add'),
        role: ['Owner', 'Lead', 'Dev', 'QA'],
      },
      edit: {
        pass: false,
        name: i18n.t('edit'),
        role: ['Owner', 'Lead', 'Creator'],
      },
      close: {
        pass: false,
        name: i18n.t('close'),
        role: ['Owner', 'Lead', 'Creator'],
      },
    },
    backup: {
      name: i18n.t('user:backup management'),
      backupRepo: {
        pass: false,
        role: ['Owner', 'Lead'],
        name: i18n.t('dop:new backup'),
      },
      deleteBackup: {
        pass: false,
        role: ['Owner', 'Lead'],
        name: i18n.t('user:delete backup'),
      },
    },
  },
  pipeline: {
    name: i18n.t('pipeline'),
    read: {
      pass: false,
      name: i18n.t('dop:view'),
      role: ['Owner', 'Lead', 'Dev', 'QA', 'Support', 'Ops', 'Guest'],
    },
    executeNormal: {
      pass: false,
      name: i18n.t('user:operation of ordinary branches (start/cancel timing/execute immediately/retry/stop)'),
      role: ['Owner', 'Lead', 'Dev', 'QA'],
    },
    executeProtected: {
      pass: false,
      name: i18n.t('user:operation protected branch (start/cancel timing/execute immediately/retry/stop)'),
      role: ['Owner', 'Lead'],
    },
  },
  runtime: {
    name: i18n.t('dop:deployment center'),
    read: {
      pass: false,
      role: ['Owner', 'Lead', 'Dev', 'QA', 'Support', 'Ops'],
      name: i18n.t('dop:view'),
    },
    devDeployOperation: {
      pass: false,
      name: i18n.t('user:DEV operation (restart/rollback/domain name setting/service expansion)'),
      role: ['Owner', 'Lead', 'Dev'],
    },
    devDelete: {
      pass: false,
      name: i18n.t('user:dEV delete'),
      role: ['Owner', 'Lead', 'Dev'],
    },
    testDeployOperation: {
      pass: false,
      name: i18n.t('user:TEST operation (restart/rollback/domain name setting/service expansion)'),
      role: ['Owner', 'Lead', 'QA'],
    },
    testDelete: {
      pass: false,
      name: i18n.t('user:tEST delete'),
      role: ['Owner', 'Lead', 'QA'],
    },
    stagingDeployOperation: {
      pass: false,
      name: i18n.t('user:STAGING operation (restart/rollback/domain name setting/service expansion)'),
      role: ['Owner', 'Lead'],
    },
    stagingDelete: {
      pass: false,
      name: i18n.t('user:sTAGING delete'),
      role: ['Owner', 'Lead'],
    },
    prodDeployOperation: {
      pass: false,
      name: i18n.t('user:PROD operation (restart/rollback/domain name setting/service expansion)'),
      role: ['Owner', 'Lead'],
    },
    prodDelete: {
      pass: false,
      name: i18n.t('user:PROD delete'),
      role: ['Owner', 'Lead'],
    },
    devConsole: {
      pass: false,
      name: i18n.t('user:DEV details console'),
      role: ['Owner', 'Lead', 'Dev', 'Ops', 'Support'],
    },
    testConsole: {
      pass: false,
      name: i18n.t('user:TEST details console'),
      role: ['Owner', 'Lead', 'QA', 'Ops', 'Support'],
    },
    stagingConsole: {
      pass: false,
      name: i18n.t('user:STAGING details console'),
      role: ['Owner', 'Lead', 'Support'],
    },
    prodConsole: {
      pass: false,
      name: i18n.t('user:PROD details console'),
      role: ['Owner', 'Lead', 'Support'],
    },
  },
  setting: {
    name: i18n.t('dop:application setting'),
    editApp: {
      pass: false,
      name: i18n.t('user:edit application information'),
      role: ['Owner', 'Lead'],
    },
    deleteApp: {
      pass: false,
      name: i18n.t('user:delete application information'),
      role: ['Owner', 'Lead'],
    },
    branchRule: {
      name: i18n.t('dop:branch rule'),
      operation: {
        pass: false,
        name: i18n.t('user:operation (add, delete, modify)'),
        role: ['Owner', 'Lead'],
      },
    },
    repoSetting: {
      name: i18n.t('dop:repository settings'),
      lockRepo: {
        pass: false,
        role: ['Lead', 'Owner'],
        name: i18n.t('user:lock warehouse'),
      },
    },
    read: {
      pass: false,
      role: ['Owner', 'Lead', 'Dev', 'QA', 'Support', 'Ops'],
      name: i18n.t('dop:view'),
    },
  },
  member: {
    name: i18n.t('cmp:member management'),
    addAppMember: {
      pass: false,
      name: i18n.t('user:application member > add'),
      role: ['Owner', 'Lead'],
    },
    editAppMember: {
      pass: false,
      name: i18n.t('user:application member > edit'),
      role: ['Owner', 'Lead'],
    },
    deleteAppMember: {
      pass: false,
      name: i18n.t('user:application member > remove'),
      role: ['Owner', 'Lead'],
    },
  },
  apiManage: {
    name: i18n.t('API'),
    apiMarket: {
      name: i18n.t('API market'),
      read: {
        pass: false,
        name: i18n.t('dop:view'),
        role: ['Owner', 'Lead', 'Dev', 'QA', 'Ops'],
      },
      edit: {
        pass: false,
        name: i18n.t('user:edit resources'),
        role: ['Owner', 'Lead'],
      },
      delete: {
        pass: false,
        name: i18n.t('user:delete resource'),
        role: ['Owner', 'Lead'],
      },
      publicAsset: {
        pass: false,
        name: i18n.t('user:public'),
        role: ['Owner', 'Lead'],
      },
      addVersion: {
        pass: false,
        name: i18n.t('user:add version'),
        role: ['Owner', 'Lead'],
      },
      deleteVersion: {
        pass: false,
        name: i18n.t('user:delete version'),
        role: ['Owner', 'Lead'],
      },
      relatedProjectOrApp: {
        pass: false,
        name: i18n.t('user:associated projects/applications'),
        role: ['Owner', 'Lead'],
      },
      relatedInstance: {
        pass: false,
        name: i18n.t('related instance'),
        role: ['Owner', 'Lead'],
      },
    },
    accessManage: {
      name: i18n.t('access management'),
      edit: {
        pass: false,
        name: i18n.t('edit'),
        role: ['Owner', 'Lead'],
      },
      delete: {
        pass: false,
        name: i18n.t('delete'),
        role: ['Owner', 'Lead'],
      },
      approve: {
        pass: false,
        name: i18n.t('user:approve'),
        role: ['Owner', 'Lead'],
      },
    },
  },
  apiDesign: {
    name: i18n.t('user:API design'),
    read: {
      pass: false,
      role: ['Owner', 'Lead', 'Dev', 'QA', 'Support', 'Ops'],
      name: i18n.t('dop:view'),
    },
  },
  dataTask: {
    name: i18n.t('dop:data task'),
    read: {
      pass: false,
      role: ['Owner', 'Lead', 'Dev', 'QA', 'Support', 'Ops'],
      name: i18n.t('dop:view'),
    },
  },
  dataModel: {
    name: i18n.t('dop:data model'),
    read: {
      pass: false,
      role: ['Owner', 'Lead', 'Dev', 'QA', 'Support', 'Ops'],
      name: i18n.t('dop:view'),
    },
  },
  dataMarket: {
    name: i18n.t('dop:data market'),
    read: {
      pass: false,
      role: ['Owner', 'Lead', 'Dev', 'QA', 'Support', 'Ops'],
      name: i18n.t('dop:view'),
    },
  },
  codeQuality: {
    name: i18n.t('dop:code quality'),
    read: {
      pass: false,
      role: ['Owner', 'Lead', 'Dev', 'QA', 'Support', 'Ops'],
      name: i18n.t('dop:view'),
    },
  },
  release: {
    name: i18n.t('artifact management'),
    read: {
      pass: false,
      role: ['Owner', 'Lead', 'Dev', 'QA', 'Support', 'Ops', 'Guest'],
      name: i18n.t('dop:view'),
    },
    info: {
      name: i18n.t('dop:details'),
      edit: {
        pass: false,
        role: ['Owner', 'Lead', 'Dev', 'QA', 'Support', 'Ops'],
        name: i18n.t('edit'),
      },
    },
  },
};
