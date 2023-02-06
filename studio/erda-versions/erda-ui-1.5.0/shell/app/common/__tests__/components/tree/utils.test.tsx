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
import { shallow } from 'enzyme';
import {
  getIcon,
  findTargetNode,
  isAncestor,
  walkTree,
  convertNodeType,
  sortCategory,
} from 'common/components/tree-category/utils';

describe('TreeUtils', () => {
  const treeNode = [
    {
      key: 'node1',
      parentKey: 'root',
      children: [
        {
          key: 'node1_1',
          parentKey: 'node1',
          children: [
            {
              key: 'node1_1_1',
              parentKey: 'node1_1',
            },
          ],
        },
        {
          key: 'node1_2',
          parentKey: 'node1',
        },
      ],
    },
    {
      key: 'node2',
      parentKey: 'root',
      children: [
        {
          key: 'node2_1',
          parentKey: 'node2',
        },
        {
          key: 'node2_2',
          parentKey: 'node2',
          children: [
            {
              key: 'node2_2_1',
              parentKey: 'node2_2',
            },
            {
              key: 'node2_2_2',
              parentKey: 'node2_2',
            },
          ],
        },
      ],
    },
  ];
  describe('getIcon', () => {
    const iconMap = {
      info: <div className="icon-info">info</div>,
      warning: <div className="icon-warning">warning</div>,
    };
    it('should render dm icon', () => {
      const wrapper = shallow(<div>{getIcon({ isLeaf: true, type: 'info' })}</div>);
      expect(wrapper.children().prop('type')).toBe('dm');
      expect(wrapper.children()).toHaveStyle({ height: '16px' });
    });
    it('should render folder icon', () => {
      const wrapper = shallow(<div>{getIcon({ isLeaf: false, type: 'info' })}</div>);
      expect(wrapper.children().prop('type')).toBe('folder');
      expect(wrapper.children()).toHaveStyle({ height: '16px' });
    });
    it('should render with type', () => {
      const wrapper = shallow(<div>{getIcon({ isLeaf: false, type: 'info' }, iconMap)}</div>);
      expect(wrapper.find('.icon-info')).toExist();
    });
    it('should render with iconType', () => {
      const wrapper = shallow(<div>{getIcon({ isLeaf: false, iconType: 'warning' }, iconMap)}</div>);
      expect(wrapper.find('.icon-warning')).toExist();
    });
    it('should render with iconType', () => {
      const wrapper = shallow(<div>{getIcon({ isLeaf: false, iconType: 'warning2' }, iconMap)}</div>);
    });
  });
  describe('findTargetNode', () => {
    it('findTargetNode should work fine', () => {
      expect(findTargetNode('node2_2_2', treeNode)).toStrictEqual({
        key: 'node2_2_2',
        parentKey: 'node2_2',
      });
      expect(findTargetNode('enpty', treeNode)).toBe(null);
      expect(findTargetNode('enpty', [])).toBe(null);
    });
  });
  describe('isAncestor', () => {
    it('isAncestor should work fine', () => {
      expect(isAncestor(treeNode, 'root', 'root')).toBeFalsy();
      expect(isAncestor(treeNode, 'root')).toBeFalsy();
      expect(isAncestor(treeNode, 'node1_1', 'node1')).toBeTruthy();
      expect(isAncestor(treeNode, 'node1_1_1', 'node1')).toBeTruthy();
      expect(isAncestor(treeNode, 'node1_1_1', 'node2')).toBeFalsy();
    });
  });
  describe('convertNodeType', () => {
    it('convertNodeType should work fine', () => {
      const node = {
        inode: 'node_key',
        name: 'node_name',
        type: 'f',
        pinode: 'P_node_kye',
      } as TREE.NODE;
      expect(convertNodeType(node)).toStrictEqual({
        key: 'node_key',
        title: 'node_name',
        titleAlias: 'node_name',
        isLeaf: true,
        parentKey: 'P_node_kye',
        originData: node,
      });
    });
  });
  describe('walkTree', () => {
    it('walkTree should work fine', () => {
      const node = [
        {
          key: 'node1',
          parentKey: 'root',
          children: [
            {
              key: 'node1_1',
              parentKey: 'node1',
              children: [
                {
                  key: 'node1_1_1',
                  parentKey: 'node1_1',
                },
              ],
            },
          ],
        },
      ];
      walkTree(node, { disabled: true, type: 'file' }, 'node1_1_1');
      expect(node).toStrictEqual([
        {
          children: [
            {
              children: [
                {
                  key: 'node1_1_1',
                  parentKey: 'node1_1',
                },
              ],
              disabled: true,
              key: 'node1_1',
              parentKey: 'node1',
              type: 'file',
            },
          ],
          disabled: true,
          key: 'node1',
          parentKey: 'root',
          type: 'file',
        },
      ]);
    });
  });
  describe('sortCategory', () => {
    it('sortCategory should work fine', () => {
      const data = [
        {
          titleAlias: 'folder2',
          isLeaf: false,
        },
        {
          titleAlias: 'file2',
          isLeaf: true,
        },
        {
          titleAlias: 'folder1',
          isLeaf: false,
        },
        {
          titleAlias: 'file1',
          isLeaf: true,
        },
      ];
      expect(sortCategory(data)).toStrictEqual([
        {
          isLeaf: false,
          titleAlias: 'folder1',
        },
        {
          isLeaf: false,
          titleAlias: 'folder2',
        },
        {
          isLeaf: true,
          titleAlias: 'file1',
        },
        {
          isLeaf: true,
          titleAlias: 'file2',
        },
      ]);
    });
  });
});
