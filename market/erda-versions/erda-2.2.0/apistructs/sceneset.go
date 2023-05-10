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

package apistructs

import (
	"fmt"
	"regexp"
	"strconv"
	"time"

	"github.com/erda-project/erda/pkg/strutil"
)

const (
	SceneSetsAutotestExecType = "sceneSets"
	SceneAutotestExecType     = "scene"

	SceneSetNameMaxLength int = 50
	SceneSetDescMaxLength int = 255
)

type SceneSet struct {
	ID          uint64    `json:"id"`
	Name        string    `json:"name"`
	SpaceID     uint64    `json:"spaceID"`
	PreID       uint64    `json:"preID"`
	Description string    `json:"description"`
	CreatorID   string    `json:"creatorID"`
	UpdaterID   string    `json:"updatorID"`
	CreatedAt   time.Time `json:"createdAt"`
	UpdatedAt   time.Time `json:"updatedAt"`
}

// type SceneSetCreateRequest struct {
// 	Name        string `json:"name"`
// 	SpaceID     uint64 `json:"spaceID"`
// 	Description string `json:"description"`
// 	PreID       uint64 `json:"preID"`
// }

type SceneSetRequest struct {
	Name        string `json:"name"`
	SpaceID     uint64 `json:"spaceID"`
	Description string `json:"description"`
	PreID       uint64 `json:"preID"`
	SetID       uint64 `json:"setID"`
	DropKey     int64  `json:"dropKey"`
	Position    int64  `json:"position,omitempty"` // 插入位置
	ProjectId   uint64 `json:"projectID"`
	IdentityInfo
}

type AutotestExecuteSceneSetRequest struct {
	AutoTestSceneSet       SceneSet          `json:"sceneSet"`
	ClusterName            string            `json:"clusterName"`
	Labels                 map[string]string `json:"labels"`
	UserID                 string            `json:"userId"`
	ConfigManageNamespaces string            `json:"configManageNamespaces"`
	IdentityInfo           IdentityInfo      `json:"identityInfo"`
}

func (req *SceneSetRequest) Validate() error {
	if err := strutil.Validate(req.Name, strutil.MaxRuneCountValidator(SceneSetNameMaxLength)); err != nil {
		return err
	}
	if err := strutil.Validate(req.Description, strutil.MaxRuneCountValidator(SceneSetDescMaxLength)); err != nil {
		return err
	}
	if ok, _ := regexp.MatchString("^[a-zA-Z\u4e00-\u9fa50-9_-]*$", req.Name); !ok {
		return fmt.Errorf("the name not match %s", "^[a-zA-Z\u4e00-\u9fa50-9_-]*$")
	}
	return nil
}

// type SceneSetUpdateRequest struct {
// 	Name        string `json:"name"`
// 	Description string `json:"description"`
// }

type GetSceneSetResponse struct {
	Header
	Data SceneSet
}

type CreateSceneSetResponse struct {
	Header
	Id uint64
}

type GetSceneSetsResponse struct {
	Header
	Data []SceneSet
}

type UpdateSceneSetResponse struct {
	Header
	Data SceneSet
}

type DeleteSceneSetResponse struct {
	Header
	res string
}

func (req *SceneSetRequest) URLQueryString() map[string][]string {
	query := make(map[string][]string)
	if req.Name != "" {
		query["name"] = append(query["name"], req.Name)
	}
	if req.SpaceID != 0 {
		query["spaceID"] = []string{strconv.FormatInt(int64(req.SpaceID), 10)}
	}
	if req.Description != "" {
		query["description"] = append(query["description"], req.Description)
	}
	if req.PreID != 0 {
		query["preID"] = []string{strconv.FormatInt(int64(req.PreID), 10)}
	}
	if req.DropKey != 0 {
		query["dropKey"] = []string{strconv.FormatInt(int64(req.DropKey), 10)}
	}
	return query
}
