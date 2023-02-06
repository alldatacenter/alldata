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

package spaceAddButton

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
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-space-list/i18n"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

type props struct {
	Text       string      `json:"text"`
	Type       string      `json:"type"`
	Operations interface{} `json:"operations"`
	TipProps   TipProps    `json:"tipProps"`
}

type TipProps struct {
	Placement string `json:"placement,omitempty"`
}

type AddButtonCandidateOp struct {
	Click struct {
		Reload  bool                   `json:"reload"`
		Key     string                 `json:"key"`
		Command map[string]interface{} `json:"command"`
	} `json:"click"`
}
type AddButtonCandidate struct {
	Disabled    bool                 `json:"disabled"`
	DisabledTip string               `json:"disabledTip"`
	Key         string               `json:"key"`
	Operations  AddButtonCandidateOp `json:"operations"`
	PrefixIcon  string               `json:"prefixIcon"`
	Text        string               `json:"text"`
}

type ComponentAction struct {
	sdk *cptype.SDK
	bdl *bundle.Bundle
}

type inParams struct {
	ProjectID int64 `json:"projectId"`
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	ca.sdk = cputil.SDK(ctx)
	ca.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)

	inParamsBytes, err := json.Marshal(ca.sdk.InParams)
	if err != nil {
		return fmt.Errorf("failed to marshal inParams, inParams:%+v, err:%v", ca.sdk.InParams, err)
	}

	var inParams inParams
	if err := json.Unmarshal(inParamsBytes, &inParams); err != nil {
		return err
	}

	createAccess, err := ca.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   ca.sdk.Identity.UserID,
		Scope:    apistructs.ProjectScope,
		ScopeID:  uint64(inParams.ProjectID),
		Resource: apistructs.TestSpaceResource,
		Action:   apistructs.CreateAction,
	})
	if err != nil {
		return err
	}

	var disabled bool
	var disabledTip string
	if !createAccess.Access {
		disabled = true
		disabledTip = ca.sdk.I18n(i18n.I18nKeyNoPermission)
	}
	prop := props{
		Text: ca.sdk.I18n(i18n.I18nKeyAdd),
		Type: "primary",
		TipProps: TipProps{
			Placement: "left",
		},
	}
	c.Props = cputil.MustConvertProps(prop)
	c.Operations = map[string]interface{}{
		"click": struct {
			Reload      bool                   `json:"reload"`
			Key         string                 `json:"key"`
			Command     map[string]interface{} `json:"command"`
			Disabled    bool                   `json:"disabled"`
			DisabledTip string                 `json:"disabledTip"`
		}{
			Reload:      false,
			Key:         "addSpace",
			Disabled:    disabled,
			DisabledTip: disabledTip,
			Command: map[string]interface{}{
				"key":    "set",
				"target": "spaceFormModal",
				"state": map[string]interface{}{
					"visible": true,
					"formData": map[string]interface{}{
						"archiveStatus": apistructs.TestSpaceInit,
					},
				},
			},
		},
	}
	return nil
}

func init() {
	base.InitProviderWithCreator("auto-test-space-list", "spaceAddButton",
		func() servicehub.Provider { return &ComponentAction{} })
}
