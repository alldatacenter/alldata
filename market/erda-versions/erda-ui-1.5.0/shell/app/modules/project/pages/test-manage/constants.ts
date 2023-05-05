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

export const priorityList = ['P0', 'P1', 'P2', 'P3'];

export const colors: string[] = ['red', 'orange', 'blue', 'green', 'purple', 'gray'];

export const labelType = 'issue';

export const TEMP_MARK = 'tmp';

export enum TestSetMenuType {
  root = 'root',
  normal = 'normal',
  recycled = 'recycled',
}

export enum TestOperation {
  add = 'add',
  rename = 'rename',
  copy = 'copy',
  clip = 'clip',
  paste = 'paste',
  delete = 'delete',
  plan = 'plan',
  recover = 'recover',
  deleteEntirely = 'delete-entirely',
  move = 'move',
  env = 'env',
  // 变更字段信息
  priority = 'priority',
  tag = 'tag',
  testPlanTestCasesExecutionResult = 'testPlanTestCasesExecutionResult',
  testPlanStatus = 'testPlanStatus',
}

export type editModeEnum = 'copy' | 'clip' | '';
