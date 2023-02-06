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

package fileInfo

import (
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

type ComponentFileInfo struct {
	sdk *cptype.SDK
	bdl *bundle.Bundle

	AutotestSceneRequest apistructs.AutotestSceneRequest
	CommonFileInfo
}

type CommonFileInfo struct {
	Version    string                                           `json:"version,omitempty"`
	Name       string                                           `json:"name,omitempty"`
	Type       string                                           `json:"type,omitempty"`
	Props      map[string]interface{}                           `json:"props,omitempty"`
	Operations map[apistructs.OperationKey]apistructs.Operation `json:"operations,omitempty"`
	Data       Data                                             `json:"data,omitempty"`
	InParams   InParams                                         `json:"inParams,omitempty"`
}

type InParams struct {
	SceneID    uint64 `json:"sceneId__urlQuery"`
	SceneSetID uint64 `json:"sceneSetId__urlQuery"`
}

type Data struct {
	apistructs.AutoTestScene
	CreateATString string `json:"createAtString"`
	UpdateATString string `json:"updateAtString"`
}

type PropColumn struct {
	Label      string `json:"label"`
	ValueKey   string `json:"valueKey"`
	RenderType string `json:"renderType,omitempty"`
}
