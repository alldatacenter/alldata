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

package scenesStages

import (
	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/common/gshelper"
)

const (
	InitializeOperation      cptype.OperationKey = "__Initialize__"
	RenderingOperation       cptype.OperationKey = "__Rendering__"
	AddParallelOperationKey  cptype.OperationKey = "addParallel"
	CopyParallelOperationKey cptype.OperationKey = "copyParallel"
	CopyToOperationKey       cptype.OperationKey = "copyTo"
	MoveItemOperationKey     cptype.OperationKey = "moveItem"
	MoveGroupOperationKey    cptype.OperationKey = "moveGroup"
	EditOperationKey         cptype.OperationKey = "edit"
	DeleteOperationKey       cptype.OperationKey = "delete"
	SplitOperationKey        cptype.OperationKey = "split"
	SwitchOperationKey       cptype.OperationKey = "switch"
	ClickOperationKey        cptype.OperationKey = "clickItem"
)

type OperationFunc []func(s *SceneStage) error

var OperationRender = map[cptype.OperationKey]OperationFunc{
	InitializeOperation:      []func(s *SceneStage) error{RenderList},
	RenderingOperation:       []func(s *SceneStage) error{RenderList},
	AddParallelOperationKey:  []func(s *SceneStage) error{RenderAddParallel},
	CopyParallelOperationKey: []func(s *SceneStage) error{RenderCopyParallel, RenderList},
	CopyToOperationKey:       []func(s *SceneStage) error{RenderCopyTo},
	MoveItemOperationKey:     []func(s *SceneStage) error{RenderItemMove, RenderList},
	MoveGroupOperationKey:    []func(s *SceneStage) error{RenderGroupMove, RenderList},
	EditOperationKey:         []func(s *SceneStage) error{RenderEdit},
	DeleteOperationKey:       []func(s *SceneStage) error{RenderDelete, RenderList},
	SplitOperationKey:        []func(s *SceneStage) error{RenderSplit, RenderList},
	ClickOperationKey:        []func(s *SceneStage) error{RenderClick},
	SwitchOperationKey:       []func(s *SceneStage) error{RenderSwitch, RenderList},
}

func RenderList(s *SceneStage) error {
	_, scenes, err := s.atTestPlan.ListAutotestScene(apistructs.AutotestSceneRequest{
		SetID: s.gsHelper.GetGlobalSelectedSetID(),
	})
	if err != nil {
		return err
	}

	s.Data.Type = "sort-item"
	s.Data.List = func() []StageData {
		stages := make([]StageData, 0, len(scenes))
		for _, v := range scenes {
			stages = append(stages, NewStageData(s, v, s.atTestPlan))
		}
		return stages
	}()
	s.Operations = make(map[string]interface{})

	s.Operations[MoveItemOperationKey.String()] = OperationInfo{
		OperationBaseInfo: OperationBaseInfo{Key: MoveItemOperationKey.String(), Reload: true},
	}
	s.Operations[MoveGroupOperationKey.String()] = OperationInfo{
		OperationBaseInfo: OperationBaseInfo{Key: MoveGroupOperationKey.String(), Reload: true},
	}
	s.Operations[ClickOperationKey.String()] = OperationInfo{
		OperationBaseInfo: OperationBaseInfo{Key: ClickOperationKey.String(), Reload: true, FillMeta: "data"},
	}
	return nil
}

func RenderAddParallel(s *SceneStage) error {
	meta, err := GetOpsInfo(s.event.OperationData)
	if err != nil {
		return err
	}

	s.State.ActionType = "AddScene"
	s.State.Visible = true
	s.State.SceneID = meta.ID
	s.State.SceneSetKey = s.gsHelper.GetGlobalSelectedSetID()
	s.State.IsAddParallel = true
	return nil
}

func RenderCopyParallel(s *SceneStage) error {
	meta, err := GetOpsInfo(s.event.OperationData)
	if err != nil {
		return err
	}
	_, err = s.atTestPlan.CopyAutotestScene(apistructs.AutotestSceneCopyRequest{
		SpaceID:      uint64(s.sdk.InParams["spaceId"].(float64)),
		PreID:        meta.ID,
		SceneID:      meta.ID,
		SetID:        s.gsHelper.GetGlobalSelectedSetID(),
		IdentityInfo: apistructs.IdentityInfo{UserID: s.sdk.Identity.UserID},
	}, false, nil)
	return err
}

func RenderCopyTo(s *SceneStage) error {
	meta, err := GetOpsInfo(s.event.OperationData)
	if err != nil {
		return err
	}

	s.State.ActionType = "CopyTo"
	s.State.Visible = true
	s.State.SceneID = meta.ID
	s.State.SceneSetKey = s.gsHelper.GetGlobalSelectedSetID()
	return nil
}

func RenderItemMove(s *SceneStage) error {
	dragGroupKey := uint64(s.State.DragParams.DragGroupKey)
	dropGroupKey := uint64(s.State.DragParams.DropGroupKey)
	setID := s.gsHelper.GetGlobalSelectedSetID()

	req := apistructs.AutotestSceneMoveRequest{
		IdentityInfo: apistructs.IdentityInfo{UserID: s.sdk.Identity.UserID},
		FirstID:      uint64(s.State.DragParams.DragKey),
		LastID:       uint64(s.State.DragParams.DragKey),
		TargetID: func() uint64 {
			if s.State.DragParams.DropKey == -1 { // Move to the end and be independent group
				return 0
			}
			return uint64(s.State.DragParams.DropKey)
		}(),
		IsGroup: false,
		SetID:   setID,
	}

	// Find preID
	if s.State.DragParams.DropKey == -1 { // Move to the end and be independent group
		_, scenes, err := s.atTestPlan.ListAutotestScene(apistructs.AutotestSceneRequest{SetID: setID})
		if err != nil {
			return err
		}
		req.PreID = scenes[len(scenes)-1].ID
	} else {
		switch s.State.DragParams.Position {
		case 0: // Inside target
			return nil
		case 1: // Behind the target
			scene, err := s.atTestPlan.GetAutotestScene(apistructs.AutotestSceneRequest{SceneID: uint64(s.State.DragParams.DragKey)})
			if err != nil {
				return err
			}
			req.PreID = uint64(s.State.DragParams.DropKey)
			// The order of the linked list has not changed in the same group
			if req.PreID == scene.PreID && dragGroupKey == dropGroupKey {
				return nil
			}

		case -1: // In front of the target
			scene, err := s.atTestPlan.GetAutotestScene(apistructs.AutotestSceneRequest{SceneID: uint64(s.State.DragParams.DropKey)})
			if err != nil {
				return err
			}
			req.PreID = scene.PreID
			// The order of the linked list has not changed in the same group
			if req.PreID == req.LastID && dragGroupKey == dropGroupKey {
				return nil
			}
		default:
			return errors.New("unknown position")
		}
	}
	return s.atTestPlan.MoveAutotestSceneV2(req)
}

func RenderGroupMove(s *SceneStage) error {
	dragGroupKey := uint64(s.State.DragParams.DragGroupKey)
	dropGroupKey := uint64(s.State.DragParams.DropGroupKey)
	setID := s.gsHelper.GetGlobalSelectedSetID()

	req := apistructs.AutotestSceneMoveRequest{
		IdentityInfo: apistructs.IdentityInfo{UserID: s.sdk.Identity.UserID},
		IsGroup:      true,
		SetID:        setID,
	}

	sceneDragGroup, err := s.atTestPlan.ListAutotestSceneByGroupID(setID, dragGroupKey)
	if err != nil {
		return err
	}
	if len(sceneDragGroup) <= 0 {
		return errors.New("the dragGroupKey is not exists")
	}
	firstSceneDrag, lastSceneDrag := findFirstLastSceneInGroup(sceneDragGroup)
	req.FirstID = firstSceneDrag.ID
	req.LastID = lastSceneDrag.ID

	switch s.State.DragParams.Position {
	case 0: // Inside target
		return nil
	case -1: // In front of the target
		sceneDropGroup, err := s.atTestPlan.ListAutotestSceneByGroupID(setID, dropGroupKey)
		if err != nil {
			return err
		}
		if len(sceneDropGroup) <= 0 {
			return errors.New("the dropGroupKey is not exists")
		}
		firstSceneDrop, _ := findFirstLastSceneInGroup(sceneDropGroup)
		req.PreID = firstSceneDrop.PreID
		req.TargetID = firstSceneDrop.ID
		// The order of the linked list has not changed
		if req.PreID == req.LastID {
			return nil
		}
	case 1: // Behind the target
		sceneDropGroup, err := s.atTestPlan.ListAutotestSceneByGroupID(setID, dropGroupKey)
		if err != nil {
			return err
		}
		if len(sceneDropGroup) <= 0 {
			return errors.New("the dropGroupKey is not exists")
		}
		_, lastSceneDrop := findFirstLastSceneInGroup(sceneDropGroup)
		req.PreID = lastSceneDrop.ID
		req.TargetID = lastSceneDrop.ID
		// The order of the linked list has not changed
		if req.PreID == firstSceneDrag.PreID {
			return nil
		}
	default:
		return errors.New("unknown position")
	}
	return s.atTestPlan.MoveAutotestSceneV2(req)
}

func RenderEdit(s *SceneStage) error {
	meta, err := GetOpsInfo(s.event.OperationData)
	if err != nil {
		return err
	}
	s.State.ActionType = "UpdateScene"
	s.State.Visible = true
	s.State.SceneID = meta.ID
	s.State.SceneSetKey = s.gsHelper.GetGlobalSelectedSetID()
	s.gsHelper.SetFileTreeSceneID(s.State.SceneID)
	return nil
}

func RenderDelete(s *SceneStage) error {
	meta, err := GetOpsInfo(s.event.OperationData)
	if err != nil {
		return err
	}
	if meta.ID == 0 {
		return nil
	}
	err = s.atTestPlan.DeleteAutotestScene(meta.ID, apistructs.IdentityInfo{UserID: s.sdk.Identity.UserID})
	if err != nil {
		return nil
	}
	// clear
	s.gsHelper.SetFileTreeSceneID(0)
	s.State.SceneID = 0
	return nil
}

func RenderSplit(s *SceneStage) error {
	setID := s.gsHelper.GetGlobalSelectedSetID()
	meta, err := GetOpsInfo(s.event.OperationData)
	if err != nil {
		return err
	}

	req := apistructs.AutotestSceneMoveRequest{
		IdentityInfo: apistructs.IdentityInfo{UserID: s.sdk.Identity.UserID},
		FirstID:      meta.ID,
		LastID:       meta.ID,
		TargetID:     0,
		IsGroup:      false,
		SetID:        setID,
	}
	sceneGroup, err := s.atTestPlan.ListAutotestSceneByGroupID(setID, uint64(meta.Data["groupID"].(float64)))
	if err != nil {
		return err
	}
	if len(sceneGroup) <= 0 {
		return errors.New("the group is not exists")
	}

	if len(sceneGroup) == 1 {
		return nil
	}
	_, lastScene := findFirstLastSceneInGroup(sceneGroup)
	req.PreID = lastScene.ID
	return s.atTestPlan.MoveAutotestSceneV2(req)
}

func RenderSwitch(s *SceneStage) error {
	return nil
}

func findFirstLastSceneInGroup(scenes []apistructs.AutoTestScene) (firstScene, lastScene apistructs.AutoTestScene) {
	sceneIDMap := make(map[uint64]apistructs.AutoTestScene, len(scenes))
	preIDMap := make(map[uint64]apistructs.AutoTestScene, len(scenes))
	for _, v := range scenes {
		sceneIDMap[v.ID] = v
		preIDMap[v.PreID] = v
	}
	for k := range preIDMap {
		if _, ok := sceneIDMap[k]; !ok {
			firstScene = preIDMap[k]
			break
		}
	}
	for k := range sceneIDMap {
		if _, ok := preIDMap[k]; !ok {
			lastScene = sceneIDMap[k]
			break
		}
	}
	return
}

func RenderClick(s *SceneStage) error {
	meta, err := GetOpsInfo(s.event.OperationData)
	if err != nil {
		return err
	}

	scene, err := s.atTestPlan.GetAutotestScene(apistructs.AutotestSceneRequest{
		SceneID:      uint64(meta.Data["id"].(float64)),
		IdentityInfo: apistructs.IdentityInfo{UserID: s.sdk.Identity.UserID},
	})
	if err != nil {
		return err
	}
	s.State.IsClickFolderTableRow = true
	s.gsHelper.SetFileTreeSceneID(scene.ID)
	if scene.RefSetID != 0 {
		s.gsHelper.SetGlobalSelectedSetID(scene.RefSetID)
		s.gsHelper.SetFileTreeSceneSetKey(scene.RefSetID)
		return RenderList(s)
	} else {
		s.gsHelper.SetGlobalActiveConfig(gshelper.SceneConfigKey)
		s.State.SceneID = scene.ID
	}

	return nil
}
