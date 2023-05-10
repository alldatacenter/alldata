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

// CreateAutoTestSceneOutput 添加场景入参
func (svc *Service) CreateAutoTestSceneOutput(req apistructs.AutotestSceneRequest) (uint64, error) {
	if ok, _ := regexp.MatchString("^[a-zA-Z0-9_-]*$", req.Name); !ok {
		return 0, apierrors.ErrCreateAutoTestSceneOutput.InvalidState("出参名称只可输入英文、数字、中划线或下划线")
	}

	scene := &dao.AutoTestSceneOutput{
		Name:        req.Name,
		Value:       req.Value,
		Description: req.Description,
		SpaceID:     req.SpaceID,
		SceneID:     req.SceneID,
		CreatorID:   req.UserID,
		UpdaterID:   req.UserID,
	}
	if err := svc.db.CreateAutoTestSceneOutput(scene); err != nil {
		return 0, err
	}
	return scene.ID, nil
}

// UpdateAutoTestSceneOutput 更新场景出参
func (svc *Service) UpdateAutoTestSceneOutput(req apistructs.AutotestSceneOutputUpdateRequest) (uint64, error) {
	var (
		updateList, createList []dao.AutoTestSceneOutput
		deleteFlag             bool
	)
	list, err := svc.db.ListAutoTestSceneOutput(req.SceneID)
	if err != nil {
		return 0, nil
	}
	OutputMap := make(map[uint64]dao.AutoTestSceneOutput)
	haveMap := make(map[uint64]bool)
	for _, v := range list {
		OutputMap[v.ID] = v
	}
	for _, v := range req.List {
		if v.ID == 0 {
			if ok, _ := regexp.MatchString("^[a-zA-Z0-9_-]*$", v.Name); !ok {
				continue
			}
			createList = append(createList, dao.AutoTestSceneOutput{
				Name:        v.Name,
				Value:       v.Value,
				Description: v.Description,
				SpaceID:     req.SpaceID,
				SceneID:     req.SceneID,
				CreatorID:   req.UserID,
				UpdaterID:   req.UserID,
			})
			continue
		}
		old := OutputMap[v.ID]
		if old.Name != v.Name || old.Value != v.Value || old.Description != v.Description {
			old.Name = v.Name
			old.Value = v.Value
			old.Description = v.Description
			old.UpdaterID = req.UserID
			updateList = append(updateList, old)
		}
		haveMap[v.ID] = true
	}
	for _, v := range OutputMap {
		if haveMap[v.ID] == true {
			continue
		}
		deleteFlag = true
		err := svc.db.DeleteAutoTestSceneOutput(v.ID)
		if err != nil {
			return 0, err
		}
	}
	for i := range updateList {
		if err := svc.db.UpdateAutotestSceneOutput(&updateList[i]); err != nil {
			return 0, err
		}
	}
	if err := svc.db.CreateAutoTestSceneOutputs(createList); err != nil {
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

// GetAutoTestSceneOutput 获取场景入参
func (svc *Service) GetAutoTestSceneOutput(id uint64) (*apistructs.AutoTestSceneOutput, error) {
	scene, err := svc.db.GetAutoTestSceneOutput(id)
	if err != nil {
		return nil, err
	}
	Output := &apistructs.AutoTestSceneOutput{
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
	return Output, nil
}

// ListAutoTestSceneOutput 获取场景入参列表
func (svc *Service) ListAutoTestSceneOutput(sceneID uint64) ([]apistructs.AutoTestSceneOutput, error) {
	scs, err := svc.db.ListAutoTestSceneOutput(sceneID)
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

// DeleteAutoTestSceneOutput 删除场景入参
func (svc *Service) DeleteAutoTestSceneOutput(id uint64) (uint64, error) {
	output, err := svc.db.GetAutoTestSceneOutput(id)
	if err != nil {
		return 0, err
	}

	err = svc.db.DeleteAutoTestSceneOutput(output.ID)
	if err != nil {
		return 0, err
	}

	return output.ID, nil
}
