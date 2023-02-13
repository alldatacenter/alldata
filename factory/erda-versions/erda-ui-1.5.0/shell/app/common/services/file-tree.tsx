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

import agent from 'agent';

/** -----统一协议树标准接口-----* */

// 获取Tree列表
export function getCategoryById(payload: TREE.GetSubTreeParams): TREE.NODE[] {
  return agent
    .get('/api/autotests/filetree')
    .query(payload)
    .then((response: any) => response.body);
}

// 查询单个节点详情
export const getTreeNodeDetail = ({ id }: { id: string }): TREE.NODE => {
  return agent.get(`/api/autotests/filetree/${id}`).then((response: any) => response.body);
};

// 创建节点
export function createTreeNode(payload: TREE.CreateNodeParams): TREE.NODE {
  return agent
    .post('/api/autotests/filetree')
    .send(payload)
    .then((response: any) => response.body);
}

// 创建根节点
export function createRootTreeNode(payload: TREE.CreateRootNodeParams): TREE.NODE {
  return agent
    .post('/api/autotests/filetree')
    .send(payload)
    .then((response: any) => response.body);
}

// 更新节点
export function updateTreeNode(payload: TREE.UpdateNodeParams): TREE.NODE {
  const { inode, ...rest } = payload;
  return agent
    .put(`/api/autotests/filetree/${inode}`)
    .send(rest)
    .then((response: any) => response.body);
}

// 删除节点
export function deleteTreeNode(payload: { inode: string }): TREE.NODE {
  const { inode } = payload;
  return agent.delete(`/api/autotests/filetree/${inode}`).then((response: any) => response.body);
}

// 移动节点
export function moveTreeNode(payload: { inode: string; pinode: string }): TREE.NODE {
  const { inode, pinode } = payload;
  return agent
    .post(`/api/autotests/filetree/${inode}/actions/move`)
    .send({ pinode })
    .then((response: any) => response.body);
}

// 复制节点
export function copyTreeNode(payload: { inode: string; pinode: string }): TREE.NODE {
  const { inode, pinode } = payload;
  return agent
    .post(`/api/autotests/filetree/${inode}/actions/copy`)
    .send({ pinode })
    .then((response: any) => response.body);
}

// 寻祖
export function getAncestors(payload: { inode: string }): TREE.NODE[] {
  const { inode } = payload;
  return agent.get(`/api/autotests/filetree/${inode}/actions/find-ancestors`).then((response: any) => response.body);
}

// 模糊查询
export function fuzzySearch(payload: TREE.FuzzySearch): TREE.NODE[] {
  return agent
    .get('/api/autotests/filetree/actions/fuzzy-search')
    .query(payload)
    .then((response: any) => response.body);
}

/** -----统一协议树标准接口  新版接口（3.21）-----* */

export const getTreeNodeDetailNew = ({ id, ...rest }: { id: string; scope: string; scopeID: string }): TREE.NODE => {
  return agent
    .get(`/api/project-pipeline/filetree/${encodeURIComponent(id)}`)
    .query(rest)
    .then((response: any) => response.body);
};

export const getCategoryByIdNew = (query: TREE.GetSubTreeParams): TREE.NODE[] => {
  return agent
    .get('/api/project-pipeline/filetree')
    .query(query)
    .then((response: any) => response.body);
};

export const fuzzySearchNew = (query: TREE.FuzzySearch): TREE.NODE[] => {
  return agent
    .get('/api/project-pipeline/filetree/actions/fuzzy-search')
    .query(query)
    .then((response: any) => response.body);
};

// 创建节点
export function createTreeNodeNew(payload: TREE.CreateNodeParams): TREE.NODE {
  return agent
    .post('/api/project-pipeline/filetree')
    .send(payload)
    .then((response: any) => response.body);
}

// 删除节点
export function deleteTreeNodeNew(payload: { inode: string }): TREE.NODE {
  const { inode, ...rest } = payload;
  return agent
    .delete(`/api/project-pipeline/filetree/${encodeURIComponent(inode)}`)
    .send(rest)
    .then((response: any) => response.body);
}

// 寻祖
export function getAncestorsNew(payload: { inode: string }): TREE.NODE[] {
  const { inode, ...rest } = payload;
  return agent
    .get(`/api/project-pipeline/filetree/${encodeURIComponent(inode)}/actions/find-ancestors`)
    .query(rest)
    .then((response: any) => response.body);
}
