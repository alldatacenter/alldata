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
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
)

const (
	maxSetNameLen     = 191
	maxSetDescLen     = 512
	rootDirNodePinode = "0"
	defaultYml        = "version: \"1.1\"\nstages: []"
)

// 查询 pinode 下同名 inode 是否存在，若存在，则加(n)
// tips: dir 和 file 都作为 node 一视同仁，例如：已存在文件 a，那么创建目录 a 会被重命名为 a(1)
func (svc *Service) ensureNodeName(pinode, name string) (string, error) {
	prefixNodes, err := svc.db.ListAutoTestFileTreeNodeByPinodeAndNamePrefix(pinode, name)
	if err != nil {
		return "", err
	}
	hasSameName := false                                                // 是否存在同名
	sameNameAppendNumberMap := make(map[int]struct{}, len(prefixNodes)) // 通过前缀匹配找到的已经存在的 (n) 目录
	for _, prefixNode := range prefixNodes {
		if prefixNode.Name == name {
			hasSameName = true
			continue
		}
		if prefixNode.Name[len(name)] == '(' && prefixNode.Name[len(prefixNode.Name)-1] == ')' {
			numberStr := prefixNode.Name[len(name)+1 : len(prefixNode.Name)-1]
			number, err := strconv.Atoi(numberStr)
			if err == nil {
				sameNameAppendNumberMap[number] = struct{}{}
			}
		}
	}
	// 有同名，则需要加 (n)
	// 假设 a(1) 不存在，a(2) 存在，则自动分配的为 a(1)
	if hasSameName {
		loopI := 1
		for {
			if _, ok := sameNameAppendNumberMap[loopI]; !ok {
				name = fmt.Sprintf("%s(%d)", name, loopI)
				break
			}
			loopI++
		}
	}

	return name, nil
}

func generateNodeMeta(meta *dao.AutoTestFileTreeNodeMeta) map[string]interface{} {
	result := make(map[string]interface{})
	if meta == nil {
		result[apistructs.AutoTestFileTreeNodeMetaKeyPipelineYml] = defaultYml
		return result
	}
	for k, v := range meta.Extra {
		result[k] = v
	}
	// handle snippet action
	if meta.SnippetAction.SnippetConfig != nil {
		meta.SnippetAction.SnippetConfig.Name = meta.Inode
	}
	// extra 里的 pipelineYml/snippetAction 会被 字段值 覆盖
	result[apistructs.AutoTestFileTreeNodeMetaKeyPipelineYml] = meta.PipelineYml
	result[apistructs.AutoTestFileTreeNodeMetaKeySnippetAction] = meta.SnippetAction
	return result
}

func convertToUnifiedFileTreeNode(node *dao.AutoTestFileTreeNode, meta *dao.AutoTestFileTreeNodeMeta) *apistructs.UnifiedFileTreeNode {
	return &apistructs.UnifiedFileTreeNode{
		Type:      node.Type,
		Scope:     node.Scope,
		ScopeID:   node.ScopeID,
		Inode:     node.Inode,
		Pinode:    node.Pinode,
		Name:      node.Name,
		Desc:      node.Desc,
		CreatorID: node.CreatorID,
		UpdaterID: node.UpdaterID,
		CreatedAt: node.CreatedAt,
		UpdatedAt: node.UpdatedAt,
		Meta:      generateNodeMeta(meta),
	}
}

func batchConvertToUnifiedFileTreeNodes(nodes ...dao.AutoTestFileTreeNode) []apistructs.UnifiedFileTreeNode {
	var results []apistructs.UnifiedFileTreeNode
	for _, node := range nodes {
		results = append(results, *convertToUnifiedFileTreeNode(&node, nil))
	}
	return results
}

func (svc *Service) findNodeUnderTargetNode(checkInode, targetInode string) (bool, error) {
	subNodes, err := svc.db.ListAutoTestFileTreeNodeByPinode(targetInode)
	if err != nil {
		return false, err
	}
	var subInodes []string
	for _, subNode := range subNodes {
		if subNode.Inode == checkInode {
			return true, nil
		}
		if subNode.Type.IsDir() {
			subInodes = append(subInodes, subNode.Inode)
		}
	}
	for _, subInode := range subInodes {
		find, err := svc.findNodeUnderTargetNode(checkInode, subInode)
		if err != nil {
			return false, err
		}
		if find {
			return true, nil
		}
	}
	return false, nil
}
