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

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
)

// SceneBaseInfo 场景基础信息
type SceneBaseInfo struct {
	Space *dao.AutoTestSpace

	LastSceneSet *dao.SceneSet
	AllSceneSets []*dao.SceneSet

	AllSceneMap map[uint64][]*dao.AutoTestScene // key: sceneSetID, value: scenes, last in the end
}

func (si *SceneBaseInfo) GetPreSceneIDUnderSceneSet(sceneSetID uint64) uint64 {
	if scenes, ok := si.AllSceneMap[sceneSetID]; ok && len(scenes) > 0 {
		return scenes[len(scenes)-1].ID
	}
	return 0
}

func (si *SceneBaseInfo) GetPreSceneSetID() uint64 {
	if si.LastSceneSet != nil {
		return si.LastSceneSet.ID
	}
	return 0
}

// createSceneBaseInfo 创建场景所需的基础信息
func (svc *Service) createSceneBaseInfo(projectID uint64) (*SceneBaseInfo, error) {
	// 创建默认空间
	space, err := svc.db.CreateAutoTestSpace(&dao.AutoTestSpace{
		Name:          "Default Space",
		ProjectID:     int64(projectID),
		Description:   "平台升级自动创建的测试空间",
		CreatorID:     systemUserID,
		UpdaterID:     systemUserID,
		Status:        apistructs.TestSpaceOpen,
		SourceSpaceID: nil,
		DeletedAt:     nil,
	})
	if err != nil {
		return nil, printReturnErr(fmt.Errorf("failed to create default space, err: %v", err))
	}
	sceneBaseInfo := SceneBaseInfo{
		Space:       space,
		AllSceneMap: make(map[uint64][]*dao.AutoTestScene),
	}
	return &sceneBaseInfo, nil
}

func (si *SceneBaseInfo) appendSceneUnderSet(scene *dao.AutoTestScene) {
	si.AllSceneMap[scene.SetID] = append(si.AllSceneMap[scene.SetID], scene)
}
