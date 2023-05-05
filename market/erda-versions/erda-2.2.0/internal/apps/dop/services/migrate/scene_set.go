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
	"sort"

	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/strutil"
)

type RichLeafDir struct {
	DirNode   *dao.AutoTestFileTreeNode
	Ancestors []*dao.AutoTestFileTreeNode
}

// createDirNodeSceneSets 生成场景集
// 返回 inode 与 sceneSet 对应关系
// 多级目录结构需要被展平
func (svc *Service) createDirNodeSceneSets(sceneBaseInfo *SceneBaseInfo, allDirNodeMap map[Inode]*dao.AutoTestFileTreeNode) (map[Inode]*dao.SceneSet, error) {
	// 找到所有叶子目录节点
	allPinodeDir := make(map[Inode]struct{})
	for _, dir := range allDirNodeMap {
		// 所有父节点都不是叶子目录节点
		allPinodeDir[Inode(dir.Pinode)] = struct{}{}
	}
	allLeafDir := make(map[Inode]*dao.AutoTestFileTreeNode)
	for _, dir := range allDirNodeMap {
		if dir.Pinode == "0" {
			continue
		}
		if _, ok := allPinodeDir[Inode(dir.Inode)]; ok {
			continue
		}
		allLeafDir[Inode(dir.Inode)] = dir
	}

	// 为所有叶子目录找到完整祖先
	var richLeafDirs []RichLeafDir
	for _, leafDir := range allLeafDir {
		richLeaf := RichLeafDir{DirNode: leafDir}
		// 递归直到 rootSet
		currentPinode := leafDir.Pinode
		for currentPinode != "0" {
			// 获取对应节点
			currentNode := allDirNodeMap[Inode(currentPinode)]
			// 追加为 ancestors
			richLeaf.Ancestors = append(richLeaf.Ancestors, currentNode)
			// 更新 pinode，开始下次循环
			currentPinode = currentNode.Pinode
		}
		richLeafDirs = append(richLeafDirs, richLeaf)
	}

	// 创建场景集
	leafDirSceneSetRelations := make(map[Inode]*dao.SceneSet)
	for _, lr := range richLeafDirs {
		polishedSceneSetName, totalSceneSetName := lr.genSceneSetName()
		sceneSet := &dao.SceneSet{
			Name:        polishedSceneSetName,
			Description: totalSceneSetName,
			SpaceID:     sceneBaseInfo.Space.ID,
			PreID:       sceneBaseInfo.GetPreSceneSetID(),
			CreatorID:   lr.DirNode.CreatorID,
			UpdaterID:   lr.DirNode.UpdaterID,
			BaseModel:   dbengine.BaseModel{CreatedAt: lr.DirNode.CreatedAt, UpdatedAt: lr.DirNode.UpdatedAt},
		}
		if err := svc.db.CreateSceneSet(sceneSet); err != nil {
			return nil, printReturnErr(fmt.Errorf("failed to create default scene set, err: %v", err))
		}
		sceneBaseInfo.LastSceneSet = sceneSet
		sceneBaseInfo.AllSceneSets = append(sceneBaseInfo.AllSceneSets, sceneSet)
		leafDirSceneSetRelations[Inode(lr.DirNode.Inode)] = sceneSet
	}

	return leafDirSceneSetRelations, nil
}

// genSceneSetName 根据父目录生成场景集名，超过长度限制的字符会被忽略
func (dir *RichLeafDir) genSceneSetName() (string, string) {
	names := []string{dir.DirNode.Name}
	for _, ancestor := range dir.Ancestors {
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

// reorderSceneSetsByDirectoryOrder 场景集按字典序重新排序
func (svc *Service) reorderSceneSetsByDirectoryOrder(sceneBaseInfo *SceneBaseInfo) {
	if len(sceneBaseInfo.AllSceneSets) == 0 {
		return
	}

	// 使用 sort.Strings 排序
	var setNames []string
	allSceneSetMap := make(map[string]*dao.SceneSet)
	// sceneSet 可能同名，加数字保证不唯一
	randomI := 0
	for _, set := range sceneBaseInfo.AllSceneSets {
		setName := set.Name
		if _, ok := allSceneSetMap[set.Name]; ok {
			setName = fmt.Sprintf("%s_%d", setName, randomI)
			randomI++
		}
		allSceneSetMap[setName] = set
		setNames = append(setNames, setName)
	}
	sort.Strings(setNames)

	// 排好序重新设置 preID
	for i, setName := range setNames {
		set := allSceneSetMap[setName]
		if i == 0 {
			set.PreID = 0
		} else {
			set.PreID = allSceneSetMap[setNames[i-1]].ID
		}
		_ = svc.updateSceneSetPreID(set)
	}
}
