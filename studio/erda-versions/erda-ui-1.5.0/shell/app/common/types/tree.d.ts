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

declare namespace TREE {
  interface NODE {
    type: 'f' | 'd'; // FILE | DIR
    inode: string; // id
    pinode: string; // parentId
    scope: string;
    scopeID: string;
    name: string;
    desc?: string;
    meta?: { [p: string]: any };
  }

  interface GetSubTreeParams {
    pinode: string;
    scope?: string;
    scopeID?: string;
  }
  type CreateRootNodeParams = Omit<NODE, 'meta' | 'inode'>;
  type CreateNodeParams = Omit<NODE, 'meta' | 'inode' | 'scope' | 'scopeID'>;
  type UpdateNodeParams = Pick<NODE, 'inode' | 'name' | 'desc'>;

  interface FuzzySearch {
    scope: string;
    scopeID: string;
    prefixFuzzy?: string;
    suffixFuzzy?: string;
    fuzzy?: string;
    recursive?: boolean;
    fromPinode?: string;
  }

  interface scopeParams {
    scope: string;
    scopeID: string;
  }
}
