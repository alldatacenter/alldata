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

package fileTree

import (
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/common/gshelper"
)

type ComponentFileTree struct {
	sdk        *cptype.SDK
	bdl        *bundle.Bundle
	CompName   string                 `json:"name"`
	Data       []SceneSet             `json:"data"`
	Operations map[string]interface{} `json:"operations"`
	State      State                  `json:"state"`
	Props      map[string]interface{} `json:"props"`
	gsHelper   *gshelper.GSHelper
}

type SceneSet struct {
	Key           string                 `json:"key"`
	Title         string                 `json:"title"`
	Icon          string                 `json:"icon"`
	IsColorIcon   bool                   `json:"isColorIcon"`
	IsLeaf        bool                   `json:"isLeaf"`
	ClickToExpand bool                   `json:"clickToExpand"`
	Selectable    bool                   `json:"selectable"`
	Operations    map[string]interface{} `json:"operations"`
	Children      []Scene                `json:"children"`
	Type          string                 `json:"type"`
	Draggable     bool                   `json:"draggable"`
	DropPosition  []int                  `json:"dropPosition"`
}

type Scene struct {
	Key          string                 `json:"key"`
	Title        string                 `json:"title"`
	Icon         string                 `json:"icon"`
	IsLeaf       bool                   `json:"isLeaf"`
	Operations   map[string]interface{} `json:"operations"`
	Type         string                 `json:"type"`
	Draggable    bool                   `json:"draggable"`
	DropPosition []int                  `json:"dropPosition"`
}

type OperationBase struct {
	Key    string `json:"key"`
	Text   string `json:"text"`
	Reload bool   `json:"reload"`
}

type AddSceneOperation struct {
	OperationBase
}

type InParams struct {
	SpaceId      uint64 `json:"spaceId"`
	SelectedKeys string `json:"selectedKeys"`
	SceneID      string `json:"sceneId__urlQuery"`
	SetID        string `json:"setId__urlQuery"`
	ProjectId    uint64 `json:"projectId"`
}

type DragParams struct {
	DragKey  string `json:"dragKey"`
	DropKey  string `json:"dropKey"`
	Position int64  `json:"position"`
	DragType string `json:"dragType"`
	DropType string `json:"dropType"`
}

type State struct {
	ExpandedKeys            []string   `json:"expandedKeys"`
	SelectedKeys            []string   `json:"selectedKeys"`
	FormVisible             bool       `json:"formVisible"`
	SceneSetKey             int        `json:"sceneSetKey"`
	ActionType              string     `json:"actionType"`
	SetId__urlQuery         string     `json:"setId__urlQuery"`
	SceneId__urlQuery       string     `json:"sceneId__urlQuery"`
	SceneId                 uint64     `json:"sceneId"`
	DragParams              DragParams `json:"dragParams"`
	PageNo                  int        `json:"pageNo"`
	IsClickScene            bool       `json:"isClickScene"`
	IsClickFolderTable      bool       `json:"isClickFolderTable"`
	ClickFolderTableSceneID uint64     `json:"clickFolderTableSceneID"`
	IsAddParallel           bool       `json:"isAddParallel"`
}

type Operation struct {
	Key    string `json:"key"`
	Reload bool   `json:"reload"`
}
