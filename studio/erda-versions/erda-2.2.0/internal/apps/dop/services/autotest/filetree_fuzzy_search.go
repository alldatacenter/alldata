// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package autotest

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

func (svc *Service) FuzzySearchFileTreeNodes(req apistructs.UnifiedFileTreeNodeFuzzySearchRequest) ([]apistructs.UnifiedFileTreeNode, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrFuzzySearchAutoTestFileTreeNodes.InvalidParameter(err)
	}

	// 若起始父节点未指定，默认为 0
	if req.FromPinode == "" {
		req.FromPinode = rootDirNodePinode
	}

	// 计算 pinodes
	var pinodes []string
	// 若需要递归，则递归查询从起始节点开始的子目录 id 列表
	if req.Recursive {
		// 起始节点为根目录且递归，则无需递归查询子目录 id，直接用 scope & scopeID 过滤即可
		if req.FromPinode == rootDirNodePinode {
			pinodes = nil
		} else {
			// 加入指定的 pinode
			pinodes = append(pinodes, req.FromPinode)
			// 递归
			if err := svc.recursiveGetDirInodes(req.FromPinode, &pinodes); err != nil {
				return nil, apierrors.ErrFuzzySearchAutoTestFileTreeNodes.InternalError(err)
			}
		}
	}

	// 搜索
	dbNodes, err := svc.db.FuzzySearchAutoTestFileTreeNodes(req.Scope, req.ScopeID, req.PrefixFuzzy, req.SuffixFuzzy, req.Fuzzy, pinodes, req.CreatorID)
	if err != nil {
		return nil, apierrors.ErrFuzzySearchAutoTestFileTreeNodes.InternalError(err)
	}

	// 转换
	nodes := batchConvertToUnifiedFileTreeNodes(dbNodes...)

	return nodes, nil
}

func (svc *Service) recursiveGetDirInodes(pinode string, inodes *[]string) error {
	nodes, err := svc.db.ListAutoTestFileTreeNodeByPinode(pinode)
	if err != nil {
		return err
	}
	for _, node := range nodes {
		// 忽略非目录
		if !node.Type.IsDir() {
			continue
		}
		*inodes = append(*inodes, node.Inode)

		if err := svc.recursiveGetDirInodes(node.Inode, inodes); err != nil {
			return err
		}
	}
	return nil
}
