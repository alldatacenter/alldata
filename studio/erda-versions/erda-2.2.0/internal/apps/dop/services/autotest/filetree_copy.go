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
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/crypto/uuid"
)

func (svc *Service) CopyFileTreeNode(req apistructs.UnifiedFileTreeNodeCopyRequest) (*apistructs.UnifiedFileTreeNode, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrCopyAutoTestFileTreeNode.InvalidParameter(err)
	}
	// 查询待复制的 node
	node, err := svc.GetFileTreeNode(apistructs.UnifiedFileTreeNodeGetRequest{
		Inode:        req.Inode,
		IdentityInfo: req.IdentityInfo,
	})
	if err != nil {
		return nil, apierrors.ErrCopyAutoTestFileTreeNode.InvalidParameter(err)
	}
	// 查询 pinode
	pNode, err := svc.GetFileTreeNode(apistructs.UnifiedFileTreeNodeGetRequest{
		Inode:        req.Pinode,
		IdentityInfo: req.IdentityInfo,
	})
	if err != nil {
		return nil, apierrors.ErrCopyAutoTestFileTreeNode.InvalidParameter(err)
	}
	// pNode 需要是目录
	if !pNode.Type.IsDir() {
		return nil, apierrors.ErrCopyAutoTestFileTreeNode.InvalidParameter(fmt.Errorf("must copy to a dir node"))
	}
	// scope & scopeID 需要一致
	if err := node.CheckSameScope(*pNode); err != nil {
		return nil, apierrors.ErrCopyAutoTestFileTreeNode.InvalidParameter(err)
	}
	// 目标 node 不能是当前 node 的子节点
	targetIsSubNode, err := svc.findNodeUnderTargetNode(req.Pinode, req.Inode)
	if err != nil {
		return nil, apierrors.ErrCopyAutoTestFileTreeNode.InternalError(err)
	}
	if targetIsSubNode {
		return nil, apierrors.ErrCopyAutoTestFileTreeNode.InvalidParameter(fmt.Errorf("cannot copy to a sub node"))
	}

	// 复制节点 -> 创建新节点
	dupNode := duplicateNode(node)
	dupNode.Pinode = pNode.Inode
	dupNode.CreatorID = req.IdentityInfo.UserID
	dupNode.UpdaterID = req.IdentityInfo.UserID
	ensuredName, err := svc.ensureNodeName(dupNode.Pinode, dupNode.Name)
	if err != nil {
		return nil, apierrors.ErrCopyAutoTestFileTreeNode.InternalError(err)
	}
	dupNode.Name = ensuredName
	if err := svc.db.CreateAutoTestFileTreeNode(dupNode); err != nil {
		return nil, apierrors.ErrCopyAutoTestFileTreeNode.InternalError(err)
	}

	originMeta, exist, err := svc.db.GetAutoTestFileTreeNodeMetaByInode(node.Inode)
	if err != nil {
		return nil, apierrors.ErrCopyAutoTestFileTreeNode.InternalError(err)
	}
	// 若存在 meta 则复制
	if exist {
		dupNodeMeta := duplicateNodeMeta(originMeta)
		dupNodeMeta.Inode = dupNode.Inode
		if err := svc.db.CreateAutoTestFileTreeNodeMeta(dupNodeMeta); err != nil {
			return nil, apierrors.ErrCopyAutoTestFileTreeNode.InternalError(err)
		}
	}

	// 递归操作
	go func() {
		if node.Type.IsDir() {
			subNodes, err := svc.db.ListAutoTestFileTreeNodeByPinode(node.Inode)
			if err != nil {
				logrus.Errorf("failed to list autotest sub nodes when copy dir node, inode: %s, err: %v", req.Inode, err)
			}
			for _, subNode := range subNodes {
				inode := subNode.Inode
				if _, err := svc.CopyFileTreeNode(apistructs.UnifiedFileTreeNodeCopyRequest{
					Inode:        inode,
					Pinode:       dupNode.Inode,
					IdentityInfo: req.IdentityInfo,
				}); err != nil {
					logrus.Errorf("failed to recursive copy file tree node, inode: %s, err: %v", inode, err)
				}
			}
		}
	}()

	return convertToUnifiedFileTreeNode(dupNode, nil), nil
}

func duplicateNode(o *apistructs.UnifiedFileTreeNode) *dao.AutoTestFileTreeNode {
	return &dao.AutoTestFileTreeNode{
		Type:      o.Type,
		Scope:     o.Scope,
		ScopeID:   o.ScopeID,
		Pinode:    o.Pinode,
		Inode:     uuid.SnowFlakeID(),
		Name:      o.Name,
		Desc:      o.Desc,
		CreatorID: "",
		UpdaterID: "",
	}
}

func duplicateNodeMeta(o *dao.AutoTestFileTreeNodeMeta) *dao.AutoTestFileTreeNodeMeta {
	return &dao.AutoTestFileTreeNodeMeta{
		Inode:         o.Inode,
		PipelineYml:   o.PipelineYml,
		SnippetAction: o.SnippetAction,
		Extra:         o.Extra,
	}
}
