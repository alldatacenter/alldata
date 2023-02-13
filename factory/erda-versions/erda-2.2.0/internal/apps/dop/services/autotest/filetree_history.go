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
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

func (svc *Service) QueryFileTreeNodeHistory(req apistructs.UnifiedFileTreeNodeHistorySearchRequest) ([]*apistructs.UnifiedFileTreeNode, error) {

	results, err := svc.db.ListAutoTestFileTreeNodeHistoryByinode(req.Inode)
	if err != nil {
		return nil, err
	}

	// 查询
	node, exist, err := svc.db.GetAutoTestFileTreeNodeByInode(req.Inode)
	if err != nil {
		return nil, apierrors.ErrGetAutoTestFileTreeNode.InternalError(err)
	}
	if !exist {
		return nil, apierrors.ErrGetAutoTestFileTreeNode.NotFound()
	}

	var historyInfos []*apistructs.UnifiedFileTreeNode
	for _, v := range results {
		historyInfos = append(historyInfos, historyConvertToUnifiedFileTreeNode(&v, node))
	}

	return historyInfos, nil
}

func historyConvertToUnifiedFileTreeNode(history *dao.AutoTestFileTreeNodeHistory, node *dao.AutoTestFileTreeNode) *apistructs.UnifiedFileTreeNode {
	nodeInfo := &apistructs.UnifiedFileTreeNode{
		Inode:     history.Inode,
		Pinode:    history.Pinode,
		Name:      history.Name,
		Desc:      history.Desc,
		CreatorID: history.CreatorID,
		UpdaterID: history.UpdaterID,
		CreatedAt: history.CreatedAt,
		UpdatedAt: history.UpdatedAt,
		Meta: map[string]interface{}{
			apistructs.AutoTestFileTreeNodeMetaKeyHistoryID:     history.ID,
			apistructs.AutoTestFileTreeNodeMetaKeyPipelineYml:   history.PipelineYml,
			apistructs.AutoTestFileTreeNodeMetaKeySnippetAction: history.SnippetAction,
			apistructs.AutoTestFileTreeNodeMetaKeyExtra:         history.Extra,
		},
	}

	if node != nil {
		nodeInfo.Scope = node.Scope
		nodeInfo.ScopeID = node.ScopeID
		nodeInfo.Type = node.Type
	}

	return nodeInfo
}
