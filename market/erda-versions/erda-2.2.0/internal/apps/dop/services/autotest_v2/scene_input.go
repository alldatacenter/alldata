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

package autotestv2

import (
	"regexp"
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

// CreateAutoTestSceneInput 添加场景入参
func (svc *Service) CreateAutoTestSceneInput(req apistructs.AutotestSceneRequest) (uint64, error) {
	if ok, _ := regexp.MatchString("^[a-zA-Z0-9_-]*$", req.Name); !ok {
		return 0, apierrors.ErrCreateAutoTestSceneInput.InvalidState("入参名称只可输入英文、数字、中划线或下划线")
	}

	input := &dao.AutoTestSceneInput{
		Name:        req.Name,
		Value:       req.Value,
		Temp:        req.Temp,
		Description: req.Description,
		SpaceID:     req.SpaceID,
		SceneID:     req.SceneID,
		CreatorID:   req.UserID,
		UpdaterID:   req.UserID,
	}
	if err := svc.db.CreateAutoTestSceneInput(input); err != nil {
		return 0, err
	}

	if err := svc.db.UpdateAutotestSceneUpdater(input.SceneID, req.UserID); err != nil {
		return 0, err
	}
	return input.ID, nil
}

// UpdateAutoTestSceneInput 更新场景入参
func (svc *Service) UpdateAutoTestSceneInput(req apistructs.AutotestSceneInputUpdateRequest) (uint64, error) {
	var (
		updateList, createList []dao.AutoTestSceneInput
		deleteFlag             bool
	)
	list, err := svc.db.ListAutoTestSceneInput(req.SceneID)
	if err != nil {
		return 0, nil
	}
	inputMap := make(map[uint64]dao.AutoTestSceneInput)
	haveMap := make(map[uint64]bool)
	for _, v := range list {
		inputMap[v.ID] = v
	}
	for _, v := range req.List {
		if ok, _ := regexp.MatchString("^[a-zA-Z0-9_-]*$", v.Name); !ok {
			continue
		}
		if v.ID == 0 {
			createList = append(createList, dao.AutoTestSceneInput{
				Name:        v.Name,
				Value:       v.Value,
				Temp:        v.Temp,
				Description: v.Description,
				SpaceID:     req.SpaceID,
				SceneID:     req.SceneID,
				CreatorID:   req.UserID,
				UpdaterID:   req.UserID,
			})
			continue
		}
		old := inputMap[v.ID]
		if old.Name != v.Name || old.Value != v.Value || old.Temp != v.Temp || old.Description != v.Description {
			old.Name = v.Name
			old.Value = v.Value
			old.Description = v.Description
			old.Temp = v.Temp
			old.UpdaterID = req.UserID
			updateList = append(updateList, old)
		}
		haveMap[v.ID] = true
	}
	for _, v := range inputMap {
		if haveMap[v.ID] == true {
			continue
		}
		deleteFlag = true
		err := svc.db.DeleteAutoTestSceneInput(v.ID)
		if err != nil {
			return 0, err
		}
	}
	for i := range updateList {
		if err := svc.db.UpdateAutotestSceneInput(&updateList[i]); err != nil {
			return 0, err
		}
	}
	if err := svc.db.CreateAutoTestSceneInputs(createList); err != nil {
		return 0, err
	}

	if len(updateList) > 0 || len(createList) > 0 || deleteFlag {
		if err := svc.db.UpdateAutotestSceneUpdater(req.SceneID, req.UserID); err != nil {
			return 0, err
		}
		if err := svc.db.UpdateAutotestSceneUpdateAt(req.SceneID, time.Now()); err != nil {
			return 0, err
		}
	}

	return uint64(len(updateList) + len(createList)), nil
}

// DeleteAutoTestSceneInput 删除场景入参
func (svc *Service) DeleteAutoTestSceneInput(id uint64) (uint64, error) {

	rsp, err := svc.db.GetAutoTestSceneInput(id)
	if err != nil {
		return 0, err
	}

	err = svc.db.DeleteAutoTestSceneInput(id)
	if err != nil {
		return 0, err
	}

	return rsp.ID, nil
}

// GetAutoTestSceneInput 获取场景入参
func (svc *Service) GetAutoTestSceneInput(id uint64) (*apistructs.AutoTestSceneInput, error) {
	scene, err := svc.db.GetAutoTestSceneInput(id)
	if err != nil {
		return nil, err
	}
	input := &apistructs.AutoTestSceneInput{
		AutoTestSceneParams: apistructs.AutoTestSceneParams{
			ID:        scene.ID,
			SpaceID:   scene.SpaceID,
			CreatorID: scene.CreatorID,
			UpdaterID: scene.UpdaterID,
		},
		Name:        scene.Name,
		Description: scene.Description,
		Value:       scene.Value,
		Temp:        scene.Temp,
		SceneID:     scene.SceneID,
	}
	return input, nil
}

// ListAutoTestSceneInput 获取场景入参列表
func (svc *Service) ListAutoTestSceneInput(sceneID uint64) ([]apistructs.AutoTestSceneInput, error) {
	scs, err := svc.db.ListAutoTestSceneInput(sceneID)
	if err != nil {
		return nil, err
	}
	var scenes []apistructs.AutoTestSceneInput
	for _, scene := range scs {
		s := apistructs.AutoTestSceneInput{
			AutoTestSceneParams: apistructs.AutoTestSceneParams{
				ID:        scene.ID,
				SpaceID:   scene.SpaceID,
				CreatorID: scene.CreatorID,
				UpdaterID: scene.UpdaterID,
			},
			Name:        scene.Name,
			Description: scene.Description,
			Value:       scene.Value,
			Temp:        scene.Temp,
			SceneID:     scene.SceneID,
		}
		scenes = append(scenes, s)
	}
	return scenes, nil
}

// ListAutoTestSceneInputByScenes 批量获取场景入参
func (svc *Service) ListAutoTestSceneInputByScenes(sceneIDs []uint64) ([]apistructs.AutoTestSceneInput, error) {
	scs, err := svc.db.ListAutoTestSceneInputByScenes(sceneIDs)
	if err != nil {
		return nil, err
	}
	var scenes []apistructs.AutoTestSceneInput
	for _, scene := range scs {
		s := apistructs.AutoTestSceneInput{
			AutoTestSceneParams: apistructs.AutoTestSceneParams{
				ID:        scene.ID,
				SpaceID:   scene.SpaceID,
				CreatorID: scene.CreatorID,
				UpdaterID: scene.UpdaterID,
			},
			Name:        scene.Name,
			Description: scene.Description,
			Value:       scene.Value,
			Temp:        scene.Temp,
			SceneID:     scene.SceneID,
		}
		scenes = append(scenes, s)
	}
	return scenes, nil
}

// ListAutoTestSceneInputByScenes 批量获取场景入参
func (svc *Service) ListAutoTestSceneOutputByScenes(sceneIDs []uint64) ([]apistructs.AutoTestSceneOutput, error) {
	scs, err := svc.db.ListAutoTestSceneOutputByScenes(sceneIDs)
	if err != nil {
		return nil, err
	}
	var scenes []apistructs.AutoTestSceneOutput
	for _, scene := range scs {
		s := apistructs.AutoTestSceneOutput{
			AutoTestSceneParams: apistructs.AutoTestSceneParams{
				ID:        scene.ID,
				SpaceID:   scene.SpaceID,
				CreatorID: scene.CreatorID,
				UpdaterID: scene.UpdaterID,
			},
			Name:        scene.Name,
			Description: scene.Description,
			Value:       scene.Value,
			SceneID:     scene.SceneID,
		}
		scenes = append(scenes, s)
	}
	return scenes, nil
}
