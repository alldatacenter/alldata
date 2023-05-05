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

package exportScenesButton

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "exportScenesButton", func() servicehub.Provider {
		return &ComponentAction{}
	})
}

type InParams struct {
	SpaceId      uint64 `json:"spaceId"`
	SelectedKeys string `json:"selectedKeys"`
	SceneID      string `json:"sceneId__urlQuery"`
	SetID        string `json:"setId__urlQuery"`
	ProjectId    uint64 `json:"projectId"`
}

type ComponentAction struct {
	sdk      *cptype.SDK
	bdl      *bundle.Bundle
	gsHelper *gshelper.GSHelper
}

type SceneSetOperation struct {
	Key        string                `json:"key"`
	Text       string                `json:"text"`
	Reload     bool                  `json:"reload"`
	Show       bool                  `json:"show"`
	SuccessMsg string                `json:"successMsg"`
	Meta       SceneSetOperationMeta `json:"meta"`
}

type SceneSetOperationMeta struct {
	ParentKey uint64 `json:"parentKey,omitempty"`
	Key       uint64 `json:"key,omitempty"`
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	ca.sdk = cputil.SDK(ctx)
	ca.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	ca.gsHelper = gshelper.NewGSHelper(gs)
	switch event.Operation {
	case cptype.OperationKey(apistructs.ExportSceneSetOperationKey):
		inParamsBytes, err := json.Marshal(cputil.SDK(ctx).InParams)
		if err != nil {
			return fmt.Errorf("failed to marshal inParams, inParams:%+v, err:%v", cputil.SDK(ctx).InParams, err)
		}

		var inParams InParams
		if err := json.Unmarshal(inParamsBytes, &inParams); err != nil {
			return err
		}
		if err := ca.RenderExportSceneSet(inParams); err != nil {
			return err
		}
		c.State = map[string]interface{}{
			"actionType":  apistructs.ExportSceneSetOperationKey,
			"visible":     true,
			"sceneSetKey": ca.gsHelper.GetGlobalSelectedSetID(),
		}
	case cptype.InitializeOperation, cptype.RenderingOperation:
		id := ca.gsHelper.GetGlobalSelectedSetID()
		export := SceneSetOperation{
			Key:        "exportSceneSet",
			Text:       ca.sdk.I18n("exportSceneSet"),
			Reload:     true,
			Show:       true,
			SuccessMsg: "导出完成，请在导入导出记录中下载导出结果",
			Meta: SceneSetOperationMeta{
				ParentKey: id,
			},
		}
		c.Type = "Button"
		c.Props = map[string]interface{}{
			"text": ca.sdk.I18n("exportSceneSet"),
		}
		c.Operations = map[string]interface{}{
			"click": export,
		}
	}
	return nil
}

func (ca *ComponentAction) RenderExportSceneSet(inParams InParams) error {
	req := apistructs.AutoTestSceneSetExportRequest{
		ID:        ca.gsHelper.GetGlobalSelectedSetID(),
		FileType:  apistructs.TestSceneSetFileTypeExcel,
		ProjectID: inParams.ProjectId,
	}
	req.UserID = ca.sdk.Identity.UserID
	if err := ca.bdl.ExportAutotestSceneSet(ca.sdk.Identity.UserID, req); err != nil {
		return err
	}
	return nil
}
