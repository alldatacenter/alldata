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
import { scopeMap } from './config';

const autoTestConfig = {
  scope: scopeMap.autoTest.scope,
  text: {
    // 文案
    fileTreeTitle: i18n.t('dop:test set'), // 文件树title、
    addFolder: i18n.t('dop:new sub testset'), // 添加文件夹
    addFile: i18n.t('common:new use case'), // 添加文件
    searchFolder: i18n.t('dop:test set'), // 搜索文件夹
    searchFile: i18n.t('common:test case'), // 搜索文件
    executeButton: i18n.t('test'), // 执行按钮
  },
  runPipelineSource: 'autotest',
  executeEnvChosen: true, // 执行时是否需要选择环境: 测试用例、测试计划需要
  executeClusterChosen: false, // 执行时是否需要选择集群：项目流水线需要
};

const projectPipelineConfig = {
  scope: scopeMap.projectPipeline.scope,
  text: {
    // 文案
    fileTreeTitle: i18n.t('pipeline'), // 文件树title、
    addFolder: i18n.t('add {name}', { name: i18n.t('directory') }), // 添加文件夹
    addFile: i18n.t('add {name}', { name: i18n.t('pipeline') }), // 添加文件
    searchFolder: i18n.t('directory'), // 搜索文件夹
    searchFile: i18n.t('pipeline'), // 搜索文件
    executeButton: i18n.t('execute'), // 执行按钮
  },
  runPipelineSource: 'project-pipeline',
  executeEnvChosen: false, // 执行时是否需要选择环境: 测试用例、测试计划需要
  executeClusterChosen: true, // 执行时是否需要选择集群：项目流水线需要
};

const configSheetConfig = {
  scope: scopeMap.configSheet.scope,
  text: {
    // 文案
    fileTreeTitle: i18n.t('config sheet'), // 文件树title、
    addFolder: i18n.t('add {name}', { name: i18n.t('directory') }), // 添加文件夹
    addFile: i18n.t('add {name}', { name: i18n.t('config sheet') }), // 添加文件
    searchFolder: i18n.t('directory'), // 搜索文件夹
    searchFile: i18n.t('config sheet'), // 搜索文件
    executeButton: i18n.t('execute'), // 执行按钮
  },
  runPipelineSource: 'config-sheet',
  executeEnvChosen: false, // 执行时是否需要选择环境: 测试用例、测试计划需要
  executeClusterChosen: false, // 执行时是否需要选择集群：项目流水线需要
};

const appPipelineConfig = {
  scope: scopeMap.appPipeline.scope,
  text: {
    // 文案
    fileTreeTitle: i18n.t('pipeline'), // 文件树title、
    addFolder: i18n.t('add {name}', { name: i18n.t('directory') }), // 添加文件夹
    addFile: i18n.t('add {name}', { name: i18n.t('pipeline') }), // 添加文件
    searchFolder: i18n.t('directory'), // 搜索文件夹
    searchFile: i18n.t('pipeline'), // 搜索文件
    executeButton: i18n.t('execute'), // 执行按钮
  },
  runPipelineSource: 'project-pipeline',
  executeEnvChosen: false, // 执行时是否需要选择环境: 测试用例、测试计划需要
  executeClusterChosen: true, // 执行时是否需要选择集群：项目流水线需要
};

export type ISCOPE_CONFIG = typeof autoTestConfig;

export const scopeConfig = {
  autoTest: autoTestConfig,
  projectPipeline: projectPipelineConfig,
  configSheet: configSheetConfig,
  appPipeline: appPipelineConfig,
  projectLevelAppPipeline: appPipelineConfig,
} as Obj<ISCOPE_CONFIG>;
