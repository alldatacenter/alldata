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

import { convertNodeType, sortCategory } from 'common/components/tree-category/utils';
import { createFlatStore } from 'core/cube';
import { map } from 'lodash';
import {
  getCategoryById,
  createRootTreeNode,
  createTreeNode,
  updateTreeNode,
  deleteTreeNode,
  moveTreeNode,
  copyTreeNode,
  getAncestors,
  fuzzySearch,
  getCategoryByIdNew,
  getTreeNodeDetailNew,
  fuzzySearchNew,
  createTreeNodeNew,
  deleteTreeNodeNew,
  getAncestorsNew,
} from '../services/file-tree';
import { TreeNode } from 'common/components/tree-category/tree';

interface IState {
  curNodeDetail: TREE.NODE;
}
const initState: IState = {
  curNodeDetail: {} as TREE.NODE,
};

const fileTreeStore = createFlatStore({
  name: 'fileTree',
  state: initState,
  effects: {
    async getCategoryById({ call }, payload: TREE.GetSubTreeParams): Promise<TreeNode[]> {
      try {
        const categories = await call(getCategoryById, payload);
        return sortCategory(map(categories, convertNodeType));
      } catch (error) {
        return [];
      }
    },
    async createRootTreeNode({ call }, payload: TREE.CreateRootNodeParams): Promise<TreeNode | null> {
      try {
        const rootNode = await call(createRootTreeNode, payload);
        return convertNodeType(rootNode);
      } catch (error) {
        return null;
      }
    },
    async createTreeNode({ call }, payload: TREE.CreateNodeParams): Promise<TreeNode | null> {
      try {
        const node = await call(createTreeNode, payload);
        return convertNodeType(node);
      } catch (error) {
        return null;
      }
    },
    async updateTreeNode({ call }, payload: TREE.UpdateNodeParams) {
      const updatedNode = await call(updateTreeNode, payload);
      return convertNodeType(updatedNode);
    },
    async deleteTreeNode({ call }, payload: { inode: string }) {
      return call(deleteTreeNode, payload);
    },
    async moveTreeNode({ call }, payload: { inode: string; pinode: string }) {
      return call(moveTreeNode, payload);
    },
    async copyTreeNode({ call }, payload: { inode: string; pinode: string }) {
      return call(copyTreeNode, payload);
    },
    async getAncestors({ call }, payload: { inode: string }) {
      const ancestors = await call(getAncestors, payload);
      return map(ancestors, convertNodeType).slice(0, -1); // 去掉自己返回
    },
    async fuzzySearch({ call }, payload: TREE.FuzzySearch) {
      const categories = await call(fuzzySearch, payload);
      return map(categories, convertNodeType);
    },

    async getTreeNodeDetailNew({ call, update }, payload: { id: string; scope: string; scopeID: string }) {
      const curNodeDetail = await call(getTreeNodeDetailNew, payload);
      update({ curNodeDetail });
      return curNodeDetail;
    },
    async getCategoryByIdNew({ call }, payload: TREE.GetSubTreeParams): Promise<TreeNode[]> {
      try {
        const categories = await call(getCategoryByIdNew, payload);
        return sortCategory(map(categories, convertNodeType));
      } catch (error) {
        return [];
      }
    },
    async deleteTreeNodeNew({ call }, payload: { inode: string }) {
      return call(deleteTreeNodeNew, payload);
    },
    async getAncestorsNew({ call }, payload: { inode: string }) {
      const ancestors = await call(getAncestorsNew, payload);
      return map(ancestors, convertNodeType).slice(0, -1); // 去掉自己返回
    },
    async fuzzySearchNew({ call }, payload: TREE.FuzzySearch) {
      const categories = await call(fuzzySearchNew, payload);
      return map(categories, convertNodeType);
    },
    async createTreeNodeNew({ call }, payload: TREE.CreateNodeParams): Promise<TreeNode | null> {
      try {
        const node = await call(createTreeNodeNew, payload);
        return convertNodeType(node);
      } catch (error) {
        return null;
      }
    },
  },
  reducers: {
    clearTreeNodeDetail(state) {
      state.curNodeDetail = {} as TREE.NODE;
    },
  },
});

export default fileTreeStore;
