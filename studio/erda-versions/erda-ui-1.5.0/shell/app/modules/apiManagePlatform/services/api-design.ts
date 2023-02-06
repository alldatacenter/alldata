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

// 获取分支/文档节点列表
export const getTreeList = (payload: API_SETTING.ITreeListQuery): API_SETTING.IFileTree[] => {
  return agent
    .get('/api/apim/api-docs/filetree')
    .query(payload)
    .then((response: any) => response.body);
};

// 删除文档节点
export const deleteTreeNode = (payload: { inode: string }) => {
  return agent.delete(`/api/apim/api-docs/filetree/${payload.inode}`).then((response: any) => response.body);
};

// 创建文档节点
export const createTreeNode = (payload: { name: string; pinode: string }): API_SETTING.IFileTree => {
  return agent
    .post('/api/apim/api-docs/filetree')
    .send({
      name: payload.name,
      pinode: payload.pinode,
      type: 'f',
      meta: {
        content: '',
      },
    })
    .then((response: any) => response.body);
};

// 重命名文档节点
export const renameTreeNode = (payload: { name: string; inode: string }): API_SETTING.IFileTree => {
  return agent
    .put(`/api/apim/api-docs/filetree/${payload.inode}`)
    .send({ name: payload.name })
    .then((response: any) => response.body);
};

// 移动文档节点
export const moveTreeNode = (payload: { pinode: string; inode: string }): API_SETTING.IFileTree => {
  return agent
    .post(`/api/apim/api-docs/filetree/${payload.inode}/actions/move`)
    .send({ pinode: payload.pinode })
    .then((response: any) => response.body);
};

// 复制文档节点
export const copyTreeNode = (payload: { pinode: string; inode: string }): API_SETTING.ICommonTreeData => {
  return agent
    .post(`/api/apim/api-docs/filetree/${payload.inode}/actions/copy`)
    .send({ pinode: payload.pinode })
    .then((response: any) => response.body);
};

// 文档节点详情
export const getApiDetail = (inode: string): API_SETTING.IApiDetail => {
  return agent.get(`/api/apim/api-docs/filetree/${inode}`).then((response: any) => response.body);
};

// 发布api文档
export const publishApi = (payload: API_SETTING.IPublishAPi) => {
  return agent
    .post('/api/api-assets')
    .send(payload)
    .then((response: any) => response.body);
};

// 获取api assets列表
export const getApiAssets = (payload: API_SETTING.IApiAssetsQuery) => {
  return agent
    .get('/api/api-assets')
    .query(payload)
    .then((response: any) => response.body);
};

// 获取库表中的参数列表
export const getSchemaParams = (payload: { inode: string }): API_SETTING.ISchemaParams => {
  return agent.get(`/api/apim/schemas/filetree/${payload.inode}`).then((response: any) => response.body);
};

// 文档校验
export const validateApiSwagger = (payload: { content: string }): API_SETTING.ISwaggerValidator => {
  return agent
    .post('/api/apim/validate-swagger')
    .send(payload)
    .then((response: any) => response.body);
};
