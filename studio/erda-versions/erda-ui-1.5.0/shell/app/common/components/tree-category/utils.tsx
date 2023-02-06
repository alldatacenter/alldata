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

import React from 'react';
import { find, set } from 'lodash';
import { TreeNode } from './tree';
import { Icon as CustomIcon } from 'common';

/**
 * 在treeData中遍历找到目标节点
 * @param nodeKey
 */
export const findTargetNode = (nodeKey: string, nodeTree: TreeNode[]): null | TreeNode => {
  if (nodeTree && nodeTree.length > 0) {
    let target = find(nodeTree, { key: nodeKey }) || null;
    if (!target) {
      let i = 0;
      while (!target && i < nodeTree.length) {
        const { children } = nodeTree[i];
        if (children) {
          target = findTargetNode(nodeKey, children);
        }
        i += 1;
      }
    }
    return target;
  }
  return null;
};

/**
 * 测试parentKey是否是childKey的直系祖先
 * 使用深度优先遍历从树顶往下找
 * 如果先找到childKey，判断parentKey是否在它的子孙上，如果在子孙里能找到返回false，否则true
 * 如果先找到parentKey，判断childKey是否在它的子孙上，如果能找到返回true，否则false
 * 如果都没找到返回false
 * 找到任意一个点，就只遍历这个点的子孙，剩下的兄弟不处理
 * @param nodeTree
 * @param childKey
 * @param parentKey
 */
export const isAncestor = (nodeTree: TreeNode[], childKey?: string, parentKey?: string | null) => {
  if (!parentKey || !childKey) {
    return false;
  }
  if (parentKey === childKey) {
    return false;
  }
  let result = false;
  let end = false;
  let capturedParent = false;
  let capturedChild = false;
  const root = nodeTree[0];
  const deepLoop = (node: TreeNode) => {
    if (end) {
      return;
    }
    if (node.key === childKey) {
      capturedChild = true;
      if (capturedParent) {
        result = true;
        end = true;
      }
    } else if (node.key === parentKey) {
      capturedParent = true;
      if (capturedChild) {
        result = false;
        end = true;
      }
    }
    const { children } = node;
    if (children && children.length > 0) {
      for (let i = 0; i < children.length; i++) {
        deepLoop(children[i]);
      }
      if (capturedParent !== capturedChild) {
        // 找到任意一个点，其子孙遍历完，不管有没有找到另一个都结束
        end = true;
      }
    } else if (capturedParent !== capturedChild) {
      end = true;
    }
  };
  deepLoop(root);
  return result;
};

/**
 * 递归遍历树，将options中的属性赋给所有节点，除了excludeKey
 * @param treeData
 * @param options
 * @param excludeKey
 */
export const walkTree = (treeData: TreeNode[], options: { disabled?: boolean }, excludeKey: string) => {
  treeData.forEach((node) => {
    Object.keys(options).forEach((optionKey) => {
      if (excludeKey === node.key) {
        return;
      }
      set(node, optionKey, options[optionKey]);
      if (node.children) {
        walkTree(node.children, options, excludeKey);
      }
    });
  });
};

const defaultFolderIcon = () => <CustomIcon color type="folder" style={{ height: '16px' }} />;
const defaultFileIcon = () => <CustomIcon color type="dm" style={{ height: '16px' }} />;

/**
 * 获取指定类型或者默认的图标
 * @param param
 * @param iconMap
 */
export const getIcon = (
  { isLeaf, type, iconType }: { isLeaf?: boolean; type?: string; iconType?: string },
  iconMap?: { [p: string]: JSX.Element },
): JSX.Element => {
  if (iconMap) {
    if (iconType && iconMap[iconType]) {
      return iconMap[iconType];
    } else if (type && iconMap[type]) {
      return iconMap[type];
    }
  }
  return isLeaf ? defaultFileIcon() : defaultFolderIcon();
};

/**
 * 将后端标准文件节点类型转化成组件Node类型
 * @param node
 */
export const convertNodeType = (node: TREE.NODE): TreeNode => {
  const { inode, name, type, pinode } = node;
  return { key: inode, title: name, titleAlias: name, isLeaf: type === 'f', parentKey: pinode, originData: node };
};

/**
 * 给文件夹列表排序，文件夹在前，文件在后，按文件名称升序排列
 * @param nodes
 */
export const sortCategory = (nodes: TreeNode[]) => {
  const folders = [...nodes.filter((node) => !node.isLeaf)];
  const files = [...nodes.filter((node) => node.isLeaf)];
  const collator = new Intl.Collator(undefined, { numeric: true, sensitivity: 'base' });
  folders.sort((x, y) => collator.compare(x.titleAlias, y.titleAlias));
  files.sort((x, y) => collator.compare(x.titleAlias, y.titleAlias));
  return [...folders, ...files];
};
