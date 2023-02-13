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
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

func (svc *Service) DeleteFileTreeNode(req apistructs.UnifiedFileTreeNodeDeleteRequest) (*apistructs.UnifiedFileTreeNode, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrGetAutoTestFileTreeNode.InvalidParameter(err)
	}
	// 查询
	node, err := svc.GetFileTreeNode(apistructs.UnifiedFileTreeNodeGetRequest{
		Inode:        req.Inode,
		IdentityInfo: req.IdentityInfo,
	})
	if err != nil {
		return nil, apierrors.ErrDeleteAutoTestFileTreeNode.InvalidParameter(err)
	}

	// 删除历史记录
	svc.DeleteFileTreeNodeHistory(req.Inode)

	// 删除节点
	if err := svc.db.DeleteAutoTestFileTreeNodeByInode(req.Inode); err != nil {
		return nil, apierrors.ErrDeleteAutoTestFileTreeNode.InternalError(err)
	}

	// 递归操作
	go func() {
		if node.Type.IsDir() {
			subNodes, err := svc.db.ListAutoTestFileTreeNodeByPinode(req.Inode)
			if err != nil {
				logrus.Errorf("failed to list autotest sub nodes when delete dir node, inode: %s, err: %v", req.Inode, err)
			}
			for _, subNode := range subNodes {
				inode := subNode.Inode
				if _, err := svc.DeleteFileTreeNode(apistructs.UnifiedFileTreeNodeDeleteRequest{
					Inode:        inode,
					IdentityInfo: req.IdentityInfo,
				}); err != nil {
					logrus.Errorf("failed to recursive delete file tree node, inode: %s, err: %v", inode, err)
				}

				// 删除历史记录
				svc.DeleteFileTreeNodeHistory(inode)
			}
		}
	}()

	return node, nil
}
