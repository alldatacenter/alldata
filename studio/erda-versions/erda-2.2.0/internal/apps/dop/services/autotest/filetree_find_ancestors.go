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
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

func (svc *Service) FindFileTreeNodeAncestors(req apistructs.UnifiedFileTreeNodeFindAncestorsRequest) ([]apistructs.UnifiedFileTreeNode, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrFindAutoTestFileTreeNodeAncestors.InvalidParameter(err)
	}

	// 查询当前节点
	currentNode, err := svc.GetFileTreeNode(apistructs.UnifiedFileTreeNodeGetRequest{
		Inode:        req.Inode,
		IdentityInfo: req.IdentityInfo,
	})
	if err != nil {
		return nil, apierrors.ErrFindAutoTestFileTreeNodeAncestors.InvalidParameter(err)
	}

	// 递归查询父节点
	var dbAncestors []dao.AutoTestFileTreeNode
	if err := svc.recursivelyFindAncestors(currentNode.Pinode, &dbAncestors); err != nil {
		return nil, apierrors.ErrFindAutoTestFileTreeNodeAncestors.InternalError(err)
	}

	// 转换
	ancestors := batchConvertToUnifiedFileTreeNodes(dbAncestors...)
	ancestors = append(ancestors, *currentNode)

	return ancestors, nil
}

// 层层递归，按照目录顺序，根目录在前
// 递归结果 ancestors 需要调用方初始化传入
func (svc *Service) recursivelyFindAncestors(pinode string, ancestors *[]dao.AutoTestFileTreeNode) error {
	if pinode == rootDirNodePinode {
		return nil
	}
	pNode, exist, err := svc.db.GetAutoTestFileTreeNodeByInode(pinode)
	if err != nil {
		return err
	}
	if !exist {
		return fmt.Errorf("parent node not exist")
	}
	*ancestors = append([]dao.AutoTestFileTreeNode{*pNode}, *ancestors...)
	if err := svc.recursivelyFindAncestors(pNode.Pinode, ancestors); err != nil {
		return err
	}
	return nil
}
