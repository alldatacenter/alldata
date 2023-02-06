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

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

func (svc *Service) MoveFileTreeNode(req apistructs.UnifiedFileTreeNodeMoveRequest) (*apistructs.UnifiedFileTreeNode, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrMoveAutoTestFileTreeNode.InvalidParameter(err)
	}
	// 查询 node
	node, err := svc.GetFileTreeNode(apistructs.UnifiedFileTreeNodeGetRequest{
		Inode:        req.Inode,
		IdentityInfo: req.IdentityInfo,
	})
	if err != nil {
		return nil, apierrors.ErrMoveAutoTestFileTreeNode.InvalidParameter(err)
	}
	// 目标目录无变化，无需移动
	if node.Pinode == req.Pinode {
		return node, nil
	}
	// 查询 pinode
	_, err = svc.GetFileTreeNode(apistructs.UnifiedFileTreeNodeGetRequest{
		Inode:        req.Pinode,
		IdentityInfo: req.IdentityInfo,
	})
	if err != nil {
		return nil, apierrors.ErrMoveAutoTestFileTreeNode.InvalidParameter(err)
	}
	// 目标 node 不能是当前 node 的子节点
	targetIsSubNode, err := svc.findNodeUnderTargetNode(req.Pinode, req.Inode)
	if err != nil {
		return nil, apierrors.ErrCopyAutoTestFileTreeNode.InternalError(err)
	}
	if targetIsSubNode {
		return nil, apierrors.ErrCopyAutoTestFileTreeNode.InvalidParameter(fmt.Errorf("cannot move to a sub node"))
	}
	// 移动 -> 更新 node.pinode，并校验 name
	ensuredName, err := svc.ensureNodeName(req.Pinode, node.Name)
	if err != nil {
		return nil, apierrors.ErrCopyAutoTestFileTreeNode.InternalError(err)
	}
	// 创建历史记录
	if err := svc.CreateFileTreeNodeHistory(req.Inode); err != nil {
		logrus.Errorf("node id %s history create error: %v", req.Inode, err)
	}
	// 移动pinode
	if err := svc.db.MoveAutoTestFileTreeNode(req.Inode, req.Pinode, ensuredName, req.IdentityInfo.UserID); err != nil {
		return nil, apierrors.ErrMoveAutoTestFileTreeNode.InvalidParameter(err)
	}
	// 查询
	movedNode, err := svc.GetFileTreeNode(apistructs.UnifiedFileTreeNodeGetRequest{
		Inode:        req.Inode,
		IdentityInfo: req.IdentityInfo,
	})
	if err != nil {
		return nil, apierrors.ErrMoveAutoTestFileTreeNode.InternalError(err)
	}
	return movedNode, nil
}
