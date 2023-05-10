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

export interface TestsetListDTO {
  // 是否引用项目
  isReference: boolean;
  // 非回收测试集列表集合
  // TestsetDTO
  noRecycledList: TestsetDTO[];
  // 项目id
  projectId: string;
  // 项目名称
  projectName: string;
}

export interface TestsetDTO {
  // 后端无注释
  createdAt: string;
  // 后端无注释
  creatorId: string;
  // 后端无注释
  defaultId: boolean;
  // 后端无注释
  directoryName: string;
  // 后端无注释
  id: string;
  // 后端无注释
  isDeleted: boolean;
  // 后端无注释
  name: string;
  // 后端无注释
  order: string;
  // 后端无注释
  parentId: string;
  // 后端无注释
  projectId: string;
  // 后端无注释
  recycled: boolean;
  // 后端无注释
  tenantId: string;
  // 后端无注释
  updatedAt: string;
  // 后端无注释
  updatedId: string;
}

export interface UseCaseListDTO {
  // 测试执行人
  actorId: string;
  // 测试执行人
  actorUserName: string;
  // 自动化
  automation: string;
  // 后端无注释
  createdAt: string;
  // 后端无注释
  defaultId: boolean;
  // 用例集路径
  directoryName: string;
  // 后端无注释
  id: string;
  // 是否可恢复,默认可恢复
  isRecover: boolean;
  // 标签列表中文名称
  // LabelDTO
  labels: LABEL.Item[];
  // 模块
  module: string;
  // 模块名字
  moduleName: string;
  // 用例名称
  name: string;
  // 前置条件
  preCondition: string;
  // 优先级
  priority: string;
  // 项目id
  projectId: string;
  // 是否回收，0：不回收，1：回收
  recycled: boolean;
  // 备注
  remark: string;
  // 测试执行结果
  result: string;
  // 测试步骤及结果
  stepAndResult: string;
  // 用例集id
  testSetId: string;
  // 后端无注释
  updatedAt: string;
  // 更新人
  updatedId: string;
  // 更新人名字
  updatedUserName: string;
  // 用例类型
  usecaseType: string;
}
