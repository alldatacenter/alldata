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
	"strconv"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

func (svc *Service) MigrateFromAutoTestV1(projectIDs ...string) error {
	// 分项目进行迁移
	// 从数据库查询 scope=project-autotest-testcase 且 pinode=0 的起始节点
	projectRootSets, err := svc.listProjectRootSets()
	if err != nil {
		return printReturnErr(fmt.Errorf("failed to list project root sets, err: %v", err))
	}
	migrateAllProjects := true
	needMigrateProjectIDMap := make(map[string]struct{})
	if len(projectIDs) > 0 {
		migrateAllProjects = false
		for _, projectID := range projectIDs {
			needMigrateProjectIDMap[projectID] = struct{}{}
		}
	}
	for _, rootSet := range projectRootSets {
		// 不迁移所有项目，则校验当前项目是否需要迁移
		if !migrateAllProjects {
			if _, needMigrate := needMigrateProjectIDMap[rootSet.ScopeID]; !needMigrate {
				continue
			}
		}
		if err := svc.migrateOneProjectRootSet(rootSet); err != nil {
			return printReturnErr(fmt.Errorf("faield to migrate one project root set, projectID: %s, err: %v", rootSet.ScopeID, err))
		}
	}
	return nil
}

// migrateOneProjectRootSet 迁移一个项目下的测试用例
func (svc *Service) migrateOneProjectRootSet(rootSet *dao.AutoTestFileTreeNode) error {
	// 解析项目 ID
	projectID, err := strconv.ParseUint(rootSet.ScopeID, 10, 64)
	if err != nil {
		return printReturnErr(fmt.Errorf("failed to parse projectID, scopeID: %s, err: %v", rootSet.ScopeID, err))
	}
	logrus.Infof("begin migrate project %d", projectID)
	defer logrus.Infof("end migrate project %d", projectID)

	// 查询所有测试相关节点，包括目录（测试集）和用例
	allNodes, err := svc.listProjectAllNodes(projectID)
	if err != nil {
		return printReturnErr(fmt.Errorf("faield to list project all nodes, err: %v", err))
	}
	var allCaseNodeInodes []string
	allDirNodeMap := make(map[Inode]*dao.AutoTestFileTreeNode)
	allCaseNodeMap := make(map[Inode]*dao.AutoTestFileTreeNode)
	for _, node := range allNodes {
		if node.Type == apistructs.UnifiedFileTreeNodeTypeFile {
			allCaseNodeMap[Inode(node.Inode)] = node
			allCaseNodeInodes = append(allCaseNodeInodes, node.Inode)
		} else {
			allDirNodeMap[Inode(node.Inode)] = node
		}
	}
	// 查询所有用例 meta（目录没有 meta，无需查询）
	allCaseNodeMetas, err := svc.listProjectAllNodeMetas(allCaseNodeInodes)
	if err != nil {
		return printReturnErr(fmt.Errorf("failed to list project all node metas, err: %v", err))
	}
	allNodeMetaMap := make(map[Inode]*dao.AutoTestFileTreeNodeMeta)
	for _, meta := range allCaseNodeMetas {
		allNodeMetaMap[Inode(meta.Inode)] = meta
	}

	// 创建场景所需的基础信息
	sceneBaseInfo, err := svc.createSceneBaseInfo(projectID)
	if err != nil {
		return printReturnErr(fmt.Errorf("failed to create sceneBaseInfo, err: %v", err))
	}

	// 获取所有目录节点，创建场景集（空目录也需要创建场景集）
	leafDirSceneSetRelations, err := svc.createDirNodeSceneSets(sceneBaseInfo, allDirNodeMap)
	if err != nil {
		return printReturnErr(fmt.Errorf("failed to create sceneSets, err: %v", err))
	}

	// 获取所有叶子节点（用例），并且已经包含完整的父节点链
	allRichCaseNodeMap := make(map[Inode]*CaseNodeWithAncestors)
	for _, node := range allCaseNodeMap {
		if node.Type != apistructs.UnifiedFileTreeNodeTypeFile {
			continue
		}
		richCaseNode, err := svc.genCaseNodeWithAncestors(node, sceneBaseInfo, allDirNodeMap, allNodeMetaMap, leafDirSceneSetRelations)
		if err != nil {
			return printReturnErr(err)
		}
		allRichCaseNodeMap[Inode(node.Inode)] = richCaseNode
	}

	// 统一处理用例节点
	oldInodeNewSceneRelations := make(map[Inode]*dao.AutoTestScene)
	invalidCaseNodeMap, err := svc.handleAllCaseNodes(sceneBaseInfo, allRichCaseNodeMap, oldInodeNewSceneRelations)
	if err != nil {
		return printReturnErr(fmt.Errorf("failed to handle all case nodes, err: %v", err))
	}

	// 所有场景集按字典序重新排序
	svc.reorderSceneSetsByDirectoryOrder(sceneBaseInfo)
	// 所有场景按字典序重新排序
	svc.reorderScenesByDirectoryOrder(sceneBaseInfo)

	// 打印无效用例日志(强行迁移)
	for inode, caseNode := range invalidCaseNodeMap {
		logrus.Warnf("case %s need force create, please check! name: %s", inode, caseNode.Node.Name)
	}
	return nil
}

// handleAllCaseNodes 统一处理所有用例节点
// sceneBaseInfo: 包含 space 和 sceneSet 信息
// allCaseNodeMap: 所有用例节点
// oldInodeNewSceneRelations: 老的 inode 与新的 scene 对应关系
func (svc *Service) handleAllCaseNodes(
	sceneBaseInfo *SceneBaseInfo,
	allCaseNodeMap map[Inode]*CaseNodeWithAncestors,
	oldInodeNewSceneRelations map[Inode]*dao.AutoTestScene,
) (map[Inode]*CaseNodeWithAncestors, error) {

	var err error
	// 将要创建为场景的用例节点
	caseWaitCreateMap := make(map[Inode]*CaseNodeWithAncestors)
	// 一开始所有节点都等待创建
	for inode, node := range allCaseNodeMap {
		caseWaitCreateMap[inode] = node
	}
	// 遍历 case
	// 用例满足以下情况时可以创建场景:
	// 1. 当节点里所有任务均为非嵌套任务
	// 2. 或存在的嵌套任务已创建
	lastLengthOfCaseWaitCreate := len(caseWaitCreateMap)
	caseNeedForceCreateMap := make(map[Inode]*CaseNodeWithAncestors)
	for len(caseWaitCreateMap) > 0 {
		time.Sleep(time.Millisecond * 100)
		for inode, caseNode := range caseWaitCreateMap {
			y := caseNode.PipelineYmlObj
			// yaml 可能已经解析过，但不满足创建场景的条件，因此 yamlObj 不为空，跳过解析
			if y == nil {
				// 空用例，无 meta，构建虚拟 meta
				if caseNode.Meta == nil {
					caseNode.Meta = &dao.AutoTestFileTreeNodeMeta{
						Inode:       caseNode.Node.Inode,
						PipelineYml: "version: 1.1",
					}
				}
				// 解析 yaml 文件
				y, err = pipelineyml.New([]byte(caseNode.Meta.PipelineYml))
				if err != nil {
					logrus.Errorf("failed to parse pipelineyml of case, caseInode: %s, err: %v", caseNode.Node.Inode, err)
					// 无法创建则删除该用例
					delete(caseWaitCreateMap, Inode(caseNode.Node.Inode))
					continue
				}
				caseNode.PipelineYmlObj = y
			}
			// 当 yaml 里所有节点均为普通节点或 snippet 已经创建，则可以创建该场景
			needWait := false
			_, needForceCreate := caseNeedForceCreateMap[inode]
			// 需要创建的 actions 列表
			actionsWaitCreateMap := make(map[string]*pipelineyml.Action)
			var actionsWaitCreates []*pipelineyml.Action
		findNeedWait:
			for _, stage := range y.Spec().Stages {
				for _, typedAction := range stage.Actions {
					for _, action := range typedAction {
						// old snippet config to sceneID
						if action.Type == apistructs.ActionTypeSnippet {
							snippetScope := action.SnippetConfig.Labels[apistructs.LabelSnippetScope]
							switch snippetScope {
							case apistructs.FileTreeScopeAutoTestConfigSheet: // 配置单在场景中是一个步骤，不阻塞场景创建
							case apistructs.FileTreeScopeAutoTest:
								fallthrough
							default: // 老数据未在 labels 里声明 scope，默认为 auto test
								// 嵌套场景未创建，则该场景需要等待嵌套场景创建后才能创建
								if _, sceneCreated := oldInodeNewSceneRelations[Inode(action.SnippetConfig.Name)]; !sceneCreated {
									needWait = true
									if !needForceCreate {
										break findNeedWait
									}
								}
							}
						}
						actionsWaitCreateMap[action.Alias.String()] = action
						actionsWaitCreates = append(actionsWaitCreates, action)
					}
				}
			}
			// 需要等待，则跳过该用例，等待下次检查
			if needWait && !needForceCreate {
				continue
			}

			// 提前创建场景和场景步骤，获取 taskName <-> stepID 映射关系，在场景中使用 stepID 进行引用
			// 先创建场景
			if _, err := svc.createOneSceneShell(sceneBaseInfo, caseNode); err != nil {
				return nil, err
			}
			// 创建场景步骤
			taskNameStepRelations := make(map[string]*dao.AutoTestSceneStep) // key: taskName, value: stepID
			for _, action := range actionsWaitCreates {
				step, err := svc.createOneSimpleSceneStep(sceneBaseInfo, caseNode, action)
				if err != nil {
					return nil, err
				}
				taskNameStepRelations[action.Alias.String()] = step
				caseNode.StepMap[step.ID] = step
			}
			// 更新 yaml（taskName -> stepID），重新解析
			newPipelineYml := replacePipelineYmlOutputsForTaskStepID(caseNode.Meta.PipelineYml, taskNameStepRelations, caseNode.Node.Inode)
			newPipelineYml = replacePipelineYmlParams(newPipelineYml, taskNameStepRelations, caseNode.Node.Inode)
			// 解析 yaml 文件
			y, err = pipelineyml.New([]byte(newPipelineYml))
			if err != nil {
				logrus.Errorf("failed to parse replaced pipelineyml of case, caseInode: %s, err: %v", caseNode.Node.Inode, err)
				continue
			}
			caseNode.PipelineYmlObj = y

			actionNameStepRelations := make(map[string]*dao.AutoTestSceneStep) // key: taskName, value: step
			for _, stage := range y.Spec().Stages {
				for _, typedAction := range stage.Actions {
					for _, action := range typedAction {
						matchedStep := taskNameStepRelations[action.Alias.String()]
						actionNameStepRelations[action.Alias.String()] = matchedStep
					}
				}
			}
			// 拿 action 新数据更新 value
			// 更新场景
			if err := svc.updateOneScene(sceneBaseInfo, caseNode, oldInodeNewSceneRelations, actionNameStepRelations); err != nil {
				return nil, printReturnErr(fmt.Errorf("failed to update scene, err: %v", err))
			}
			// 添加 relation 关联，依赖该用例的用例可以创建场景了
			oldInodeNewSceneRelations[Inode(caseNode.Node.Inode)] = caseNode.Scene
			// 创建成功删除该节点
			delete(caseWaitCreateMap, Inode(caseNode.Node.Inode))
		}
		// 本次遍历完待创建的 case 没有减少，说明剩下的这些用例存在死循环引用或找不到的引用
		if len(caseWaitCreateMap) == lastLengthOfCaseWaitCreate {
			for inode, caseNode := range caseWaitCreateMap {
				logrus.Warnf("case %s need force create", inode)
				caseNeedForceCreateMap[inode] = caseNode
			}
		}
		lastLengthOfCaseWaitCreate = len(caseWaitCreateMap)
	}
	return caseNeedForceCreateMap, nil
}
