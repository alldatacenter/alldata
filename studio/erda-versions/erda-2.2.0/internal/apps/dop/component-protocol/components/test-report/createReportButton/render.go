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

package createReportButton

import (
	"context"
	"encoding/json"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

type ComponentAction struct {
	sdk    *cptype.SDK
	ctxBdl *bundle.Bundle

	Name       string                 `json:"name"`
	Type       string                 `json:"type"`
	Props      map[string]interface{} `json:"props"`
	Operations map[string]interface{} `json:"operations"`
	InParams   InParams               `json:"-"`
}

type InParams struct {
	ProjectID uint64 `json:"projectId,omitempty"`
}

func (i *ComponentAction) setInParams(ctx context.Context) error {
	b, err := json.Marshal(cputil.SDK(ctx).InParams)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &i.InParams); err != nil {
		return err
	}
	return err
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := ca.setInParams(ctx); err != nil {
		return err
	}
	ca.Name = "addButton"
	ca.Type = "Button"
	ca.Operations = map[string]interface{}{
		"click": map[string]interface{}{
			"key":    "addTest",
			"reload": false,
			"command": map[string]interface{}{
				"key":    "goto",
				"target": "projectTestReportCreate",
			},
		},
	}
	ca.ctxBdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	ca.sdk = cputil.SDK(ctx)
	access, err := ca.ctxBdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   ca.sdk.Identity.UserID,
		Scope:    apistructs.ProjectScope,
		ScopeID:  ca.InParams.ProjectID,
		Resource: "testReport",
		Action:   apistructs.CreateAction,
	})
	if err != nil {
		return err
	}
	ca.Props = map[string]interface{}{
		"text":    cputil.I18n(ctx, "createProject"),
		"type":    "primary",
		"visible": access.Access,
	}
	return nil
}

func init() {
	base.InitProviderWithCreator("test-report", "createReportButton", func() servicehub.Provider {
		return &ComponentAction{}
	})
}
