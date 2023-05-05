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
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/strutil"
)

func (svc *Service) CreateFileTreeNode(req apistructs.UnifiedFileTreeNodeCreateRequest) (*apistructs.UnifiedFileTreeNode, error) {
	// 创建前校验
	node, err := svc.validateNodeBeforeCreate(req)
	if err != nil {
		return nil, apierrors.ErrCreateAutoTestFileTreeNode.InvalidParameter(err)
	}
	// 创建
	if err := svc.db.CreateAutoTestFileTreeNode(node); err != nil {
		return nil, apierrors.ErrCreateAutoTestFileTreeNode.InternalError(err)
	}
	// 转换
	return convertToUnifiedFileTreeNode(node, nil), nil
}

func (svc *Service) validateNodeBeforeCreate(req apistructs.UnifiedFileTreeNodeCreateRequest) (*dao.AutoTestFileTreeNode, error) {
	// 构造 node
	node := dao.AutoTestFileTreeNode{
		Type:      req.Type,
		Scope:     req.Scope,
		ScopeID:   req.ScopeID,
		Pinode:    req.Pinode,
		Name:      req.Name,
		Desc:      req.Desc,
		CreatorID: req.IdentityInfo.UserID,
		UpdaterID: req.IdentityInfo.UserID,
	}
	// 参数校验
	if !req.Type.Valid() {
		return nil, fmt.Errorf("invalid node type: %s", req.Type.String())
	}
	if req.Type.IsDir() {
		if req.Pinode == "" {
			// root dir
			if err := req.ValidateRootDir(); err != nil {
				return nil, err
			}
			// 根节点 pinode 设置为 0，而不是类 Linux pinode=inode，方便数据库插入和查找
			node.Pinode = rootDirNodePinode
		} else {
			// non-root dir
			if err := req.ValidateNonRootDir(); err != nil {
				return nil, err
			}
		}
	}
	if req.Type.IsFile() {
		if err := req.ValidateFile(); err != nil {
			return nil, err
		}
	}
	// 字段最大长度校验
	if err := strutil.Validate(node.Name, strutil.MaxLenValidator(maxSetNameLen)); err != nil {
		return nil, err
	}
	if err := strutil.Validate(node.Desc, strutil.MaxLenValidator(maxSetDescLen)); err != nil {
		return nil, err
	}

	if node.Pinode == rootDirNodePinode {
		// 根目录，一个 scope id 下有且只有一个根目录。若已存在则报错
		rootSet, exist, err := svc.db.GetAutoTestFileTreeScopeRootDir(node.Scope, node.ScopeID)
		if err != nil {
			return nil, err
		}
		if exist {
			return nil, fmt.Errorf("root dir already exist, inode: %s", rootSet.Inode)
		}
	} else {
		// 有父节点，校验名称是否重名
		pNode, exist, err := svc.db.GetAutoTestFileTreeNodeByInode(node.Pinode)
		if err != nil {
			return nil, err
		}
		if !exist {
			return nil, fmt.Errorf("parent node not exist")
		}
		// 文件节点的 pNode 类型需要是 dir
		if node.Type.IsFile() && !pNode.Type.IsDir() {
			return nil, fmt.Errorf("file node's parent node type must be dir")
		}
		// 查询 pinode 下同名 inode 是否存在，若存在，则加(n)
		ensuredName, err := svc.ensureNodeName(node.Pinode, node.Name)
		if err != nil {
			return nil, err
		}
		node.Name = ensuredName
		// 从 pinode 继承 scope & scopeID
		node.Scope = pNode.Scope
		node.ScopeID = pNode.ScopeID
	}

	// 分配 inode
	node.Inode = uuid.SnowFlakeID()
	return &node, nil
}
