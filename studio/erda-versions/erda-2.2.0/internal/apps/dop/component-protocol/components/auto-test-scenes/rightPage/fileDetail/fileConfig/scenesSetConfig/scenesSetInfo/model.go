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

package scenesSetInfo

import (
	"context"
	"encoding/json"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	autotestv2 "github.com/erda-project/erda/internal/apps/dop/services/autotest_v2"
)

type ScenesSetInfo struct {
	CommonFileInfo

	sdk        *cptype.SDK
	atTestPlan *autotestv2.Service
	bdl        *bundle.Bundle
	gsHelper   *gshelper.GSHelper
}

type CommonFileInfo struct {
	Version    string                                           `json:"version,omitempty"`
	Name       string                                           `json:"name,omitempty"`
	Type       string                                           `json:"type,omitempty"`
	Props      map[string]interface{}                           `json:"props,omitempty"`
	State      State                                            `json:"state,omitempty"`
	Operations map[apistructs.OperationKey]apistructs.Operation `json:"operations,omitempty"`
	Data       Data                                             `json:"data,omitempty"`
	InParams   InParams                                         `json:"inParams,omitempty"`
}

type InParams struct {
	SceneID    uint64 `json:"sceneId__urlQuery"`
	SceneSetID uint64 `json:"sceneSetId__urlQuery"`
}

type Data struct {
	apistructs.SceneSet
	CreateATString string `json:"createAtString"`
	UpdateATString string `json:"updateAtString"`
}

type PropColumn struct {
	Label      string `json:"label"`
	ValueKey   string `json:"valueKey"`
	RenderType string `json:"renderType,omitempty"`
}

type State struct {
	//AutotestSceneSetRequest apistructs.SceneSetRequest `json:"autotestSceneSetRequest"`
	SetID uint64 `json:"setID"`
}

func (s *ScenesSetInfo) initFromProtocol(ctx context.Context, c *cptype.Component, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err = json.Unmarshal(b, s); err != nil {
		return err
	}

	s.sdk = cputil.SDK(ctx)
	s.atTestPlan = ctx.Value(types.AutoTestPlanService).(*autotestv2.Service)
	s.gsHelper = gshelper.NewGSHelper(gs)
	s.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	return nil
}
