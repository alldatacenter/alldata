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
	"encoding/json"
	"fmt"
	"sort"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/strutil"
)

// createOneSceneShell 创建一个场景壳子，不包含具体步骤等
func (svc *Service) createOneSceneShell(sceneBaseInfo *SceneBaseInfo, caseNode *CaseNodeWithAncestors) (*dao.AutoTestScene, error) {
	// 创建场景壳子
	scene := &dao.AutoTestScene{
		Name:        caseNode.Node.Name,
		Description: caseNode.Node.Desc,
		SpaceID:     sceneBaseInfo.Space.ID,
		SetID:       caseNode.SceneSet.ID,
		PreID:       sceneBaseInfo.GetPreSceneIDUnderSceneSet(caseNode.SceneSet.ID),
		CreatorID:   caseNode.Node.CreatorID,
		UpdaterID:   caseNode.Node.UpdaterID,
		Status:      apistructs.DefaultSceneStatus,
		BaseModel:   dbengine.BaseModel{CreatedAt: caseNode.Node.CreatedAt, UpdatedAt: caseNode.Node.UpdatedAt},
	}
	if err := svc.db.CreateAutotestScene(scene); err != nil {
		return nil, fmt.Errorf("failed to create scene shell, err: %v", err)
	}
	// 设置 last scene
	sceneBaseInfo.appendSceneUnderSet(scene)
	caseNode.Scene = scene
	return scene, nil
}

// createOneScene 创建一个场景
func (svc *Service) updateOneScene(
	sceneBaseInfo *SceneBaseInfo,
	caseNode *CaseNodeWithAncestors,
	oldInodeNewSceneRelations map[Inode]*dao.AutoTestScene, // 原 inode 与 新 scene 的关联关系
	actionNameStepRelations map[string]*dao.AutoTestSceneStep,
) error {

	// 1. 创建场景入参
	var sceneInputs []dao.AutoTestSceneInput
	params := caseNode.PipelineYmlObj.Spec().Params
	var runParamValues []apistructs.PipelineRunParam
	runParamValuesI, ok := caseNode.Meta.Extra[apistructs.AutoTestFileTreeNodeMetaKeyRunParams]
	if ok {
		b, err := json.Marshal(runParamValuesI)
		if err != nil {
			logrus.Warnf("failed to marshal runParamValuesI, err: %v", err)
		}
		if err := json.Unmarshal(b, &runParamValues); err != nil {
			logrus.Warnf("failed to unmarshal runParamValues, err: %v", err)
		}
	}
	runParamValuesMap := make(map[string]string)
	for _, runParam := range runParamValues {
		// 本次值 temp 也包含老的语法，需要升级语法
		tempValue := strutil.String(runParam.Value)
		// 复用 yaml 升级方法
		tempValue = replacePipelineYmlOutputsForTaskStepID(tempValue, actionNameStepRelations, caseNode.Node.Inode)
		tempValue = replacePipelineYmlParams(tempValue, actionNameStepRelations, caseNode.Node.Inode)
		runParamValuesMap[runParam.Name] = tempValue
	}
	for _, param := range params {
		sceneInputs = append(sceneInputs, dao.AutoTestSceneInput{
			Name:        param.Name,
			Value:       strutil.String(param.Default),
			Temp:        runParamValuesMap[param.Name],
			Description: param.Desc,
			SceneID:     caseNode.Scene.ID,
			SpaceID:     sceneBaseInfo.Space.ID,
			CreatorID:   caseNode.Node.CreatorID,
			UpdaterID:   caseNode.Node.UpdaterID,
			BaseModel:   dbengine.BaseModel{CreatedAt: caseNode.Node.CreatedAt, UpdatedAt: caseNode.Node.UpdatedAt},
		})
	}
	if err := svc.db.CreateAutoTestSceneInputs(sceneInputs); err != nil {
		return fmt.Errorf("failed to create scene inputs, err: %v", err)
	}

	// 2. 更新场景步骤
	// 更新场景步骤，增加步骤详细内容，更新占位符
	// 按顺序创建
	for _, stage := range caseNode.PipelineYmlObj.Spec().Stages {
		for _, typedAction := range stage.Actions {
			for _, action := range typedAction {
				step := actionNameStepRelations[action.Alias.String()]
				var err error
				switch step.Type {
				case apistructs.StepTypeAPI:
					err = svc.updateValueForStepAPI(step, action, caseNode)
				case apistructs.StepTypeWait:
					// no wait now, migrate to custom-script
				case apistructs.StepTypeCustomScript:
					err = svc.updateValueForStepCustomScript(step, action, caseNode)
				case apistructs.StepTypeConfigSheet:
					err = svc.updateValueStepForConfigSheet(step, action, caseNode)
				case apistructs.StepTypeScene:
					err = svc.updateValueForStepScene(step, action, caseNode, oldInodeNewSceneRelations)
				}
				if err != nil {
					return fmt.Errorf("failed to generate updated value for step %s, err: %v", step.Name, err)
				}
				// update scene to db
				if err := svc.updateSceneStepValue(step); err != nil {
					return fmt.Errorf("failed to update value for step %s to db, err: %v", step.Name, err)
				}
			}
		}
	}

	// 3. 创建场景出参
	var sceneOutputs []dao.AutoTestSceneOutput
	for _, output := range caseNode.PipelineYmlObj.Spec().Outputs {
		sceneOutputs = append(sceneOutputs, dao.AutoTestSceneOutput{
			Name:        output.Name,
			Value:       output.Ref,
			Description: output.Desc,
			SceneID:     caseNode.Scene.ID,
			SpaceID:     sceneBaseInfo.Space.ID,
			CreatorID:   caseNode.Node.CreatorID,
			UpdaterID:   caseNode.Node.UpdaterID,
			BaseModel:   dbengine.BaseModel{CreatedAt: caseNode.Node.CreatedAt, UpdatedAt: caseNode.Node.UpdatedAt},
		})
	}
	if err := svc.db.CreateAutoTestSceneOutputs(sceneOutputs); err != nil {
		return fmt.Errorf("failed to create scene outputs, err: %v", err)
	}

	return nil
}

// reorderScenesByDirectoryOrder 场景集下的场景按字典序重新排序
func (svc *Service) reorderScenesByDirectoryOrder(sceneBaseInfo *SceneBaseInfo) {
	if len(sceneBaseInfo.AllSceneMap) == 0 {
		return
	}

	for _, scenes := range sceneBaseInfo.AllSceneMap {
		// 使用 sort.Strings 排序
		var sceneNames []string
		sceneMap := make(map[string]*dao.AutoTestScene)
		// scene 可能同名，加数字保证不唯一
		randomI := 0
		for _, scene := range scenes {
			sceneName := scene.Name
			if _, ok := sceneMap[scene.Name]; ok {
				sceneName = fmt.Sprintf("%s_%d", sceneName, randomI)
				randomI++
			}
			sceneMap[sceneName] = scene
			sceneNames = append(sceneNames, sceneName)
		}
		sort.Strings(sceneNames)

		// 排好序重新设置 preID
		for i, sceneName := range sceneNames {
			scene := sceneMap[sceneName]
			if i == 0 {
				scene.PreID = 0
			} else {
				scene.PreID = sceneMap[sceneNames[i-1]].ID
			}
			_ = svc.updateScenePreID(scene)
		}
	}
}
