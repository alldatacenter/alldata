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

export const logStatusMap = {
  INIT: 'default',
  PACKING: 'processing',
  FAILED: 'error',
  OK: 'success',
};

export const activityConfig = {
  iconMap: {
    P_CREATE: 'createProject',
    P_CREATE_BRANCH: 'branch',
    P_PACK_BRANCH: 'packup',
    P_DEPLOY_BRANCH: 'publish',
    P_DELETE_BRANCH: 'shanchu',
    P_OPERATE_MEMBER: 'person',
  },
  textMap: {
    P_PACK_BRANCH: '查看打包日志',
    P_DEPLOY_BRANCH: '查看部署日志',
  },
  typeMap: {
    P_PACK_BRANCH: 'builds',
    P_DEPLOY_BRANCH: 'deployments',
  },
};

export const stepList = [
  {
    key: 'INIT',
    name: i18n.t('initializing'),
  },
  {
    key: 'ADDON_REQUESTING',
    name: i18n.t('dop:plugin deployment'),
  },
  {
    key: 'SCRIPT_APPLYING',
    name: i18n.t('dop:script deployment'),
  },
  {
    key: 'SERVICE_DEPLOYING',
    name: i18n.t('dop:service deployment'),
  },
  {
    key: 'COMPLETED',
    name: i18n.t('dop:completed'),
  },
];

export const activieyMap = {
  S: {},
  U: {},
  O: {},
  P: {
    P_ADD: '创建项目',
    P_DEL: '删除项目',
    P_MOD: '编辑项目',
    P_MEMBER_ADD: '添加成员',
    P_MEMBER_DEL: '删除成员',
    P_MEMBER_MOD: '修改成员角色',
  },
  A: {
    A_ADD: '创建应用',
    A_DEL: '删除应用',
    A_MOD: '编辑应用',
    A_MEMBER_ADD: '添加成员',
    A_MEMBER_DEL: '删除成员',
    A_MEMBER_MOD: '修改成员角色',
  },
  R: {
    R_ADD: '发布分支',
    R_DEL: '删除分支',
    R_MOD: '修改配置',
    R_DEPLOY: '部署',
  },
  B: {
    B_CREATE: '创建构建',
    B_START: '开始构建',
    B_CANCEL: '取消构建',
    B_FAILED: '构建失败',
    B_END: '构建完成',
  },
};

export const actionMap = {
  P_ADD: '创建项目',
  P_DEL: '删除项目',
  P_MOD: '编辑项目',
  P_MEMBER_ADD: '添加成员',
  P_MEMBER_DEL: '删除成员',
  P_MEMBER_MOD: '修改成员角色',
  A_ADD: '创建应用',
  A_DEL: '删除应用',
  A_MOD: '编辑应用',
  A_MEMBER_ADD: '添加成员',
  A_MEMBER_DEL: '删除成员',
  A_MEMBER_MOD: '修改成员角色',
  R_ADD: '添加实例',
  R_DEL: '删除实例',
  R_MOD: '修改配置',
  R_DEPLOY_START: '开始部署',
  R_DEPLOY_FAIL: '部署失败',
  R_DEPLOY_CANCEL: '取消部署',
  R_DEPLOY_OK: '成功部署',
  B_CREATE: '创建构建',
  B_START: '开始构建',
  B_CANCEL: '取消构建',
  B_FAILED: '构建失败',
  B_END: '构建完成',
  G_PUSH: '推送代码',
};
