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

package migrate

import (
	"fmt"

	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
	"github.com/erda-project/erda/pkg/strutil"
)

type CaseNodeWithAncestors struct {
	Node           *dao.AutoTestFileTreeNode
	Meta           *dao.AutoTestFileTreeNodeMeta
	PipelineYmlObj *pipelineyml.PipelineYml
	Ancestors      []*dao.AutoTestFileTreeNode       // ancestors 最后一个元素为项目根节点
	LatestStep     *dao.AutoTestSceneStep            // 最新的一个 step
	SceneSet       *dao.SceneSet                     // 所属的场景集
	Scene          *dao.AutoTestScene                // 当前用例节点创建出来的场景
	StepMap        map[uint64]*dao.AutoTestSceneStep // 步骤集合
}

func (cn *CaseNodeWithAncestors) getPreStepID() uint64 {
	if cn.LatestStep != nil {
		return cn.LatestStep.ID
	}
	return 0
}

const (
	maxSceneSetNameCharLen = 191
)

// genSceneSetName 根据父目录生成场景集名，超过长度限制的字符会被忽略
func (cn *CaseNodeWithAncestors) genSceneSetName() (string, string) {
	var names []string
	for _, ancestor := range cn.Ancestors {
		names = append(names, ancestor.Name)
	}
	strutil.ReverseSlice(names)
	// 若 names 长度大于 1，则忽略第一个根目录
	if len(names) > 1 {
		names = names[1:]
	}
	joinName := strutil.Join(names, "_", true)
	totalJoinName := joinName
	if len([]rune(joinName)) > maxSceneSetNameCharLen {
		joinName = string([]rune(joinName)[:47]) + "..."
	}
	return joinName, totalJoinName
}

// genCaseNodeWithAncestors 生成富用例节点，包含所有祖先节点信息
func (svc *Service) genCaseNodeWithAncestors(
	caseNode *dao.AutoTestFileTreeNode,
	sceneBaseInfo *SceneBaseInfo,
	allDirNodeMap map[Inode]*dao.AutoTestFileTreeNode,
	allCaseNodeMetaMap map[Inode]*dao.AutoTestFileTreeNodeMeta,
	leafDirSceneSetRelations map[Inode]*dao.SceneSet,
) (*CaseNodeWithAncestors, error) {

	var rich CaseNodeWithAncestors
	rich.Node = caseNode
	rich.Meta = allCaseNodeMetaMap[Inode(caseNode.Inode)]
	rich.StepMap = make(map[uint64]*dao.AutoTestSceneStep)
	rich.SceneSet = leafDirSceneSetRelations[Inode(rich.Node.Pinode)]

	// 递归直到 rootSet
	currentPinode := rich.Node.Pinode
	for currentPinode != "0" {
		// 获取对应节点
		currentNode := allDirNodeMap[Inode(currentPinode)]
		// 追加为 ancestors
		rich.Ancestors = append(rich.Ancestors, currentNode)
		// 更新 pinode，开始下次循环
		currentPinode = currentNode.Pinode
	}

	// 当关联的场景集为空，需要创建
	if rich.SceneSet == nil {
		parentDirNode := allDirNodeMap[Inode(rich.Node.Pinode)]
		polishedSceneSetName, totalSceneSetName := rich.genSceneSetName()
		sceneSet := &dao.SceneSet{
			Name:        polishedSceneSetName,
			Description: totalSceneSetName,
			SpaceID:     sceneBaseInfo.Space.ID,
			PreID:       sceneBaseInfo.GetPreSceneSetID(),
			CreatorID:   parentDirNode.CreatorID,
			UpdaterID:   parentDirNode.UpdaterID,
			BaseModel:   dbengine.BaseModel{CreatedAt: parentDirNode.CreatedAt, UpdatedAt: parentDirNode.UpdatedAt},
		}
		if err := svc.db.CreateSceneSet(sceneSet); err != nil {
			return nil, printReturnErr(fmt.Errorf("failed to create case related scene set, err: %v", err))
		}
		rich.SceneSet = sceneSet
		sceneBaseInfo.LastSceneSet = sceneSet
		sceneBaseInfo.AllSceneSets = append(sceneBaseInfo.AllSceneSets, sceneSet)
		leafDirSceneSetRelations[Inode(rich.Node.Pinode)] = sceneSet
	}

	return &rich, nil
}
