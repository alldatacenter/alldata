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

package spaceFormModal

import (
	"context"
	"encoding/json"
	"fmt"
	"regexp"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-space-list/i18n"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	spec "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/component_spec/form_modal"
	"github.com/erda-project/erda/pkg/strutil"
)

type SpaceFormModal struct {
	sdk *cptype.SDK
	bdl *bundle.Bundle

	Type       string                 `json:"type"`
	Props      spec.Props             `json:"props"`
	Operations map[string]interface{} `json:"operations"`
	State      State                  `json:"state"`
}

type State struct {
	Reload   bool                   `json:"reload"`
	Visible  bool                   `json:"visible"`
	FormData map[string]interface{} `json:"formData"`
}

type operation struct {
	Key    string `json:"key"`
	Reload bool   `json:"reload"`
}

type inParams struct {
	ProjectID int64 `json:"projectId"`
}

type AutoTestSpace struct {
	ID            uint64                                `json:"id"`
	ProjectID     int64                                 `json:"projectId"`
	Name          string                                `json:"name"`
	Desc          string                                `json:"desc"`
	ArchiveStatus apistructs.AutoTestSpaceArchiveStatus `json:"archiveStatus"`
}

const (
	regular = "^[a-z\u4e00-\u9fa5A-Z0-9_-]+( )+(.)*$"
)

func (a *SpaceFormModal) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	a.sdk = cputil.SDK(ctx)
	a.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	a.Props.Fields = []spec.Field{
		{
			Key:       "name",
			Label:     cputil.I18n(ctx, "spaceName"),
			Component: "input",
			Required:  true,
			ComponentProps: spec.ComponentProps{
				MaxLength: 50,
			},
			Rules: []spec.FieldRule{
				{
					Pattern: `/^[.a-z\u4e00-\u9fa5A-Z0-9_-\s]*$/`,
					Msg:     cputil.I18n(ctx, "sceneSetRuleMessage"),
				},
			},
		},
		{
			Key:       "archiveStatus",
			Label:     cputil.I18n(ctx, "state"),
			Component: "select",
			Required:  true,
			ComponentProps: spec.ComponentProps{
				Options: []spec.Option{
					{
						Name:  a.sdk.I18n(i18n.I18nKeyAutoTestSpaceInit),
						Value: apistructs.TestSpaceInit,
					},
					{
						Name:  a.sdk.I18n(i18n.I18nKeyAutoTestSpaceInProgress),
						Value: apistructs.TestSpaceInProgress,
					},
					{
						Name:  a.sdk.I18n(i18n.I18nKeyAutoTestSpaceCompleted),
						Value: apistructs.TestSpaceCompleted,
					},
				},
			},
		},
		{
			Key:       "desc",
			Label:     cputil.I18n(ctx, "desc"),
			Component: "textarea",
			Required:  false,
			ComponentProps: spec.ComponentProps{
				MaxLength: 1000,
			},
		},
	}
	a.Operations = make(map[string]interface{})
	a.Operations["submit"] = operation{
		Key:    "submit",
		Reload: true,
	}

	inParamsBytes, err := json.Marshal(a.sdk.InParams)
	if err != nil {
		return fmt.Errorf("failed to marshal inParams, inParams:%+v, err:%v", a.sdk.InParams, err)
	}

	var inParams inParams
	if err := json.Unmarshal(inParamsBytes, &inParams); err != nil {
		return err
	}
	// listen on operation
	switch apistructs.OperationKey(event.Operation) {
	case apistructs.AutoTestSpaceSubmitOperationKey:
		if _, ok := a.State.FormData["id"]; ok {
			if err := a.handlerUpdateOperation(c, inParams, event); err != nil {
				return err
			}
		} else {
			if err := a.handlerCreateOperation(c, inParams, event); err != nil {
				return err
			}
		}
		a.State.Reload = true
		a.State.Visible = false
	}
	return nil
}

func (a *SpaceFormModal) handlerCreateOperation(c *cptype.Component, inParams inParams, event cptype.ComponentEvent) error {

	cond := AutoTestSpace{}
	filterCond, ok := c.State["formData"]
	if ok {
		filterCondS, err := json.Marshal(filterCond)
		if err != nil {
			return err
		}
		if err := json.Unmarshal(filterCondS, &cond); err != nil {
			return err
		}
	}
	if err := strutil.Validate(cond.Desc, strutil.MaxRuneCountValidator(1000)); err != nil {
		return err
	}
	if err := strutil.Validate(cond.Name, strutil.MaxRuneCountValidator(50)); err != nil {
		return err
	}
	reg := regexp.MustCompile("^[a-zA-Z0-9_.|\\-|\\s|\u4e00-\u9fa5]+$")
	if reg == nil { //解释失败，返回nil
		return fmt.Errorf("regexp err")
	}
	if !reg.MatchString(cond.Name) {
		return fmt.Errorf("请输入中文、英文、数字、中划线或下划线")
	}
	err := a.bdl.CreateTestSpace(
		&apistructs.AutoTestSpaceCreateRequest{
			Name:          cond.Name,
			ProjectID:     inParams.ProjectID,
			Description:   cond.Desc,
			ArchiveStatus: cond.ArchiveStatus,
		}, a.sdk.Identity.UserID)
	if err != nil {
		return err
	}
	return nil
}

func (a *SpaceFormModal) handlerUpdateOperation(c *cptype.Component, inParams inParams, event cptype.ComponentEvent) error {
	cond := AutoTestSpace{}
	filterCond, ok := c.State["formData"]
	if ok {
		filterCondS, err := json.Marshal(filterCond)
		if err != nil {
			return err
		}
		if err := json.Unmarshal(filterCondS, &cond); err != nil {
			return err
		}
	}
	if err := strutil.Validate(cond.Desc, strutil.MaxRuneCountValidator(1000)); err != nil {
		return err
	}
	if err := strutil.Validate(cond.Name, strutil.MaxRuneCountValidator(50)); err != nil {
		return err
	}
	reg := regexp.MustCompile("^[a-zA-Z0-9_.|\\-|\\s|\u4e00-\u9fa5]+$")
	if reg == nil { //解释失败，返回nil
		return fmt.Errorf("regexp err")
	}
	if !reg.MatchString(cond.Name) {
		return fmt.Errorf("请输入中文、英文、数字、中划线或下划线")
	}
	res, err := a.bdl.GetTestSpace(cond.ID)
	if err != nil {
		return err
	}
	if res.Status != apistructs.TestSpaceOpen {
		return fmt.Errorf("当前状态不允许编辑")
	}
	return a.bdl.UpdateTestSpace(&apistructs.AutoTestSpace{
		ID:            cond.ID,
		Name:          cond.Name,
		Description:   cond.Desc,
		ArchiveStatus: cond.ArchiveStatus,
	}, a.sdk.Identity.UserID)
}

func init() {
	base.InitProviderWithCreator("auto-test-space-list", "spaceFormModal",
		func() servicehub.Provider { return &SpaceFormModal{} })
}
