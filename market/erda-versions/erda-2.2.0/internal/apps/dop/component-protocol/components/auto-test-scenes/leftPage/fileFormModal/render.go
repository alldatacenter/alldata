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

package fileFormModal

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/leftPage/fileTree"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	autotestv2 "github.com/erda-project/erda/internal/apps/dop/services/autotest_v2"
)

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "fileFormModal",
		func() servicehub.Provider { return &ComponentFileFormModal{} })
}

func (a *ComponentFileFormModal) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) (err error) {
	a.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	a.sdk = cputil.SDK(ctx)
	a.atTestPlan = ctx.Value(types.AutoTestPlanService).(*autotestv2.Service)
	a.gsHelper = gshelper.NewGSHelper(gs)
	err = a.unmarshal(c)
	if err != nil {
		return err
	}

	defer func() {
		fail := a.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()

	if cputil.SDK(ctx).InParams == nil {
		return fmt.Errorf("params is empty")
	}

	inParamsBytes, err := json.Marshal(cputil.SDK(ctx).InParams)
	if err != nil {
		return fmt.Errorf("failed to marshal inParams, inParams:%+v, err:%v", cputil.SDK(ctx).InParams, err)
	}

	var inParams fileTree.InParams
	if err := json.Unmarshal(inParamsBytes, &inParams); err != nil {
		return err
	}

	a.Operations = map[string]interface{}{}
	a.Operations["submit"] = Operation{
		Key:    "SubmitScene",
		Reload: true,
	}
	// defaultFields := a.Props.Fields
	switch event.Operation {
	case cptype.InitializeOperation, cptype.RenderingOperation:
		if err := a.renderHelper(inParams, event); err != nil {
			return err
		}
	case cptype.OperationKey(apistructs.SubmitSceneOperationKey):
		if err := a.renderSubmitHelper(inParams, event); err != nil {
			return err
		}
	}
	return nil
}

func (a *ComponentFileFormModal) initFields() {
	a.Props = Props{
		Title: "添加",
		Fields: []Entry{
			{
				Key:       "name",
				Label:     a.sdk.I18n("name"),
				Required:  true,
				Component: "input",
				Rules: []Rule{
					{
						Pattern: `/^[a-z\u4e00-\u9fa5A-Z0-9_-]*$/`,
						Msg:     a.sdk.I18n("sceneSetRuleMessage"),
					},
				},
				ComponentProps: ComponentProps{
					MaxLength: 50,
				},
			},
			{
				Key:       "desc",
				Label:     a.sdk.I18n("desc"),
				Component: "textarea",
				Required:  false,
				ComponentProps: ComponentProps{
					MaxLength: 1000,
				},
			},
		},
	}
}

func (a *ComponentFileFormModal) initSceneSetFields(inParams fileTree.InParams) error {
	a.Props = Props{
		Title: "添加",
		Fields: []Entry{
			{
				Key:       "name",
				Label:     a.sdk.I18n("name"),
				Required:  true,
				Component: "input",
				Rules: []Rule{
					{
						Pattern: `/^[a-z\u4e00-\u9fa5A-Z0-9_-]*$/`,
						Msg:     a.sdk.I18n("sceneSetRuleMessage"),
					},
				},
				ComponentProps: ComponentProps{
					MaxLength: 50,
				},
			},
			{
				Key:       "scenesSet",
				Label:     a.sdk.I18n("selectSceneSet"),
				Component: "select",
				Required:  true,
				ComponentProps: ComponentProps{
					Placeholder: a.sdk.I18n("selectSceneSetPlace"),
				},
			},
			{
				Key:       "policy",
				Label:     a.sdk.I18n("referPolicy"),
				Component: "select",
				Required:  true,
				ComponentProps: ComponentProps{
					Placeholder: a.sdk.I18n("referPolicyPlace"),
					Options: []interface{}{
						PolicyOption{
							a.sdk.I18n("new-run"),
							apistructs.NewRunPolicyType,
						},
						PolicyOption{
							a.sdk.I18n("try-latest-success-result"),
							apistructs.TryLatestSuccessResultPolicyType,
						},
						PolicyOption{
							a.sdk.I18n("try-latest-result"),
							apistructs.TryLatestResultPolicyType,
						},
					},
				},
			},
			{
				Key:       "desc",
				Label:     a.sdk.I18n("desc"),
				Component: "textarea",
				Required:  false,
				ComponentProps: ComponentProps{
					MaxLength: 1000,
				},
			},
		},
	}

	req := apistructs.SceneSetRequest{
		SpaceID: inParams.SpaceId,
	}
	req.UserID = a.sdk.Identity.UserID
	rsp, err := a.bdl.GetSceneSets(req)
	if err != nil {
		return err
	}
	for _, v := range rsp {
		if strconv.Itoa(int(v.ID)) == inParams.SetID {
			continue
		}
		a.Props.Fields[1].ComponentProps.Options = append(a.Props.Fields[1].ComponentProps.Options, struct {
			Name  string `json:"name"`
			Value uint64 `json:"value"`
		}{
			v.Name,
			v.ID,
		})
	}
	return nil
}

func (a *ComponentFileFormModal) initSceneCopyToFields(inParams fileTree.InParams) error {
	a.Props = Props{
		Title: "添加",
		Fields: []Entry{
			{
				Key:       "scenesSet",
				Label:     a.sdk.I18n("selectSceneSet"),
				Component: "select",
				Required:  true,
				ComponentProps: ComponentProps{
					Placeholder: a.sdk.I18n("selectSceneSetPlace"),
				},
			},
		},
	}

	req := apistructs.SceneSetRequest{
		SpaceID: inParams.SpaceId,
	}
	req.UserID = a.sdk.Identity.UserID
	rsp, err := a.bdl.GetSceneSets(req)
	if err != nil {
		return err
	}
	for _, v := range rsp {
		if strconv.Itoa(int(v.ID)) == inParams.SetID {
			continue
		}
		a.Props.Fields[0].ComponentProps.Options = append(a.Props.Fields[0].ComponentProps.Options, struct {
			Name  string `json:"name"`
			Value uint64 `json:"value"`
		}{
			v.Name,
			v.ID,
		})
	}
	return nil
}

func (a *ComponentFileFormModal) renderHelper(inParams fileTree.InParams, event cptype.ComponentEvent) error {
	switch a.State.ActionType {
	case "AddScene":
		a.initFields()
		a.Props.Title = a.sdk.I18n("addScene")
		a.State.FormData = FormData{
			Name:        "",
			Description: "",
		}
	case "addRefSceneSet":
		err := a.initSceneSetFields(inParams)
		if err != nil {
			return err
		}
		a.Props.Title = a.sdk.I18n("refSceneSet")
		a.State.FormData = FormData{
			Name:        "",
			Description: "",
			ScenesSet:   nil,
			Policy:      apistructs.NewRunPolicyType,
		}
	case "UpdateSceneSet":
		a.initFields()
		a.Props.Title = a.sdk.I18n("editSceneSet")
		a.Props.Fields = []Entry{a.Props.Fields[0]}
		if err := a.GetSceneSet(); err != nil {
			return err
		}
	case "UpdateScene":
		a.initFields()
		a.Props.Title = a.sdk.I18n("editScene")
		if err := a.GetScene(inParams); err != nil {
			return err
		}
	case "ClickAddSceneSetButton":
		a.initFields()
		a.Props.Title = a.sdk.I18n("addSceneSet")
		a.Props.Fields = []Entry{a.Props.Fields[0]}
		a.State.FormData = FormData{
			Name:        "",
			Description: "",
		}
	case "CopyTo":
		a.initSceneCopyToFields(inParams)
		a.Props.Title = a.sdk.I18n("copyToAnotherSceneSet")
		a.Props.Fields = []Entry{a.Props.Fields[0]}
		a.State.FormData = FormData{
			ScenesSet: nil,
		}
	}
	return nil
}

func (a *ComponentFileFormModal) renderSubmitHelper(inParams fileTree.InParams, event cptype.ComponentEvent) error {
	switch a.State.ActionType {
	case "AddScene":
		if err := a.AddScene(inParams); err != nil {
			return err
		}
	case "addRefSceneSet":
		if err := a.AddRefSceneSet(inParams); err != nil {
			return err
		}
	case "UpdateSceneSet":
		if err := a.UpdateSceneSet(inParams); err != nil {
			return err
		}
	case "UpdateScene":
		if err := a.UpdateScene(inParams); err != nil {
			return err
		}
	case "ClickAddSceneSetButton":
		if err := a.AddSceneSet(inParams); err != nil {
			return err
		}
	case "CopyTo":
		if err := a.CopyTo(inParams); err != nil {
			return err
		}
	}
	a.State.Visible = false
	a.State.SceneId = 0
	return nil
}

func (a *ComponentFileFormModal) AddSceneSet(inParams fileTree.InParams) error {
	formData := a.State.FormData
	req := apistructs.SceneSetRequest{
		Name:        formData.Name,
		Description: formData.Description,
		SpaceID:     inParams.SpaceId,
		ProjectId:   inParams.ProjectId,
	}
	req.UserID = a.sdk.Identity.UserID
	_, err := a.bdl.CreateSceneSet(req)
	if err != nil {
		return err
	}
	return nil
}

func (a *ComponentFileFormModal) GetSceneSet() error {
	setId := a.State.SceneSetKey
	req := apistructs.SceneSetRequest{
		SetID: uint64(setId),
	}
	req.UserID = a.sdk.Identity.UserID
	s, err := a.bdl.GetSceneSet(req)
	if err != nil {
		return err
	}
	a.State.FormData = FormData{
		Name:        s.Name,
		Description: s.Description,
	}
	return nil
}

func (a *ComponentFileFormModal) GetScene(inParams fileTree.InParams) error {
	id := a.gsHelper.GetFileTreeSceneID()
	req := apistructs.AutotestSceneRequest{
		SceneID: id,
	}
	req.UserID = a.sdk.Identity.UserID
	s, err := a.bdl.GetAutoTestScene(req)
	if err != nil {
		return err
	}
	a.State.FormData = FormData{
		Name:        s.Name,
		Description: s.Description,
	}
	if s.RefSetID > 0 {
		a.Props.Fields = []Entry{
			{
				Key:       "name",
				Label:     a.sdk.I18n("name"),
				Required:  true,
				Component: "input",
				Rules: []Rule{
					{
						Pattern: `/^[a-z\u4e00-\u9fa5A-Z0-9_-]*$/`,
						Msg:     a.sdk.I18n("sceneSetRuleMessage"),
					},
				},
				ComponentProps: ComponentProps{
					MaxLength: 50,
				},
			},
			{
				Key:       "policy",
				Label:     a.sdk.I18n("referPolicy"),
				Component: "select",
				Required:  true,
				ComponentProps: ComponentProps{
					Placeholder: a.sdk.I18n("referPolicyPlace"),
					Options: []interface{}{
						PolicyOption{
							a.sdk.I18n("new-run"),
							apistructs.NewRunPolicyType,
						},
						PolicyOption{
							a.sdk.I18n("try-latest-success-result"),
							apistructs.TryLatestSuccessResultPolicyType,
						},
						PolicyOption{
							a.sdk.I18n("try-latest-result"),
							apistructs.TryLatestResultPolicyType,
						},
					},
				},
			},
			{
				Key:       "desc",
				Label:     a.sdk.I18n("desc"),
				Component: "textarea",
				Required:  false,
				ComponentProps: ComponentProps{
					MaxLength: 1000,
				},
			},
		}
		a.State.FormData.Policy = s.Policy
	}
	return nil
}

func (a *ComponentFileFormModal) AddScene(inParams fileTree.InParams) error {
	formData := a.State.FormData
	var (
		preScene *apistructs.AutoTestScene
		err      error
	)
	if a.State.IsAddParallel && a.State.SceneId != 0 {
		preScene, err = a.atTestPlan.GetAutotestScene(apistructs.AutotestSceneRequest{SceneID: a.State.SceneId})
		if err != nil {
			return err
		}
	}
	preID := a.State.SceneId
	_, err = a.atTestPlan.CreateAutotestScene(apistructs.AutotestSceneRequest{
		Name:        formData.Name,
		Description: formData.Description,
		SetID:       uint64(a.State.SceneSetKey),
		SceneGroupID: func() uint64 {
			if preScene == nil {
				return 0
			}
			if preScene.GroupID == 0 {
				return preScene.ID
			}
			return preScene.GroupID
		}(),
		PreID:        preID,
		IdentityInfo: apistructs.IdentityInfo{UserID: a.sdk.Identity.UserID},
	})
	if err != nil {
		return err
	}
	return nil
}

func (a *ComponentFileFormModal) AddRefSceneSet(inParams fileTree.InParams) error {
	formData := a.State.FormData

	if formData.ScenesSet == nil || *formData.ScenesSet <= 0 {
		return fmt.Errorf("failed to reference scene set, scene set id is empty")
	}

	setId := a.State.SceneSetKey
	req := apistructs.AutotestSceneRequest{
		AutoTestSceneParams: apistructs.AutoTestSceneParams{
			SpaceID: inParams.SpaceId,
		},
		Name:        formData.Name,
		Description: formData.Description,
		SetID:       uint64(setId), //formData.SetID,
		RefSetID:    *formData.ScenesSet,
		Policy:      formData.Policy,
	}
	req.UserID = a.sdk.Identity.UserID
	_, err := a.bdl.CreateAutoTestScene(req)
	if err != nil {
		return err
	}
	return nil
}

func (a *ComponentFileFormModal) UpdateSceneSet(inParams fileTree.InParams) error {
	formData := a.State.FormData
	setId := a.State.SceneSetKey
	req := apistructs.SceneSetRequest{
		Name:        formData.Name,
		Description: formData.Description,
		SpaceID:     inParams.SpaceId,
		SetID:       uint64(setId),
		ProjectId:   inParams.ProjectId,
	}
	req.UserID = a.sdk.Identity.UserID
	_, err := a.bdl.UpdateSceneSet(req)
	if err != nil {
		return err
	}
	return nil
}

func (a *ComponentFileFormModal) UpdateScene(inParams fileTree.InParams) error {
	formData := a.State.FormData
	id := a.gsHelper.GetFileTreeSceneID()
	req := apistructs.AutotestSceneSceneUpdateRequest{
		Name:        formData.Name,
		Description: formData.Description,
		SceneID:     id,
		Policy:      formData.Policy,
	}
	req.UserID = a.sdk.Identity.UserID
	_, err := a.bdl.UpdateAutoTestScene(req)
	if err != nil {
		return err
	}
	return nil
}

func (a *ComponentFileFormModal) marshal(c *cptype.Component) error {
	stateValue, err := json.Marshal(a.State)
	if err != nil {
		return err
	}
	var state map[string]interface{}
	err = json.Unmarshal(stateValue, &state)
	if err != nil {
		return err
	}

	propValue, err := json.Marshal(a.Props)
	if err != nil {
		return err
	}
	var props cptype.ComponentProps
	err = json.Unmarshal(propValue, &props)
	if err != nil {
		return err
	}

	c.Props = props
	c.State = state
	c.Operations = a.Operations
	// c.Type = a.Type
	return nil
}

func (a *ComponentFileFormModal) unmarshal(c *cptype.Component) error {
	stateValue, err := json.Marshal(c.State)
	if err != nil {
		return err
	}
	var state State
	err = json.Unmarshal(stateValue, &state)
	if err != nil {
		return err
	}

	var prop Props
	propJson, err := json.Marshal(c.Props)
	if err != nil {
		return err
	}
	err = json.Unmarshal(propJson, &prop)
	if err != nil {
		return err
	}
	a.State = state
	// a.Type = c.Type
	a.Props = prop
	return nil
}

func (a *ComponentFileFormModal) CopyTo(inParams fileTree.InParams) error {
	formData := a.State.FormData

	if formData.ScenesSet == nil || *formData.ScenesSet <= 0 {
		return fmt.Errorf("failed to copy scene to other set, scene set id is empty")
	}
	_, scenes, err := a.atTestPlan.ListAutotestScene(apistructs.AutotestSceneRequest{SetID: *formData.ScenesSet})
	if err != nil {
		return err
	}

	_, err = a.atTestPlan.CopyAutotestScene(apistructs.AutotestSceneCopyRequest{
		SpaceID: inParams.SpaceId,
		PreID: func() uint64 {
			if len(scenes) == 0 {
				return 0
			}
			return scenes[len(scenes)-1].ID
		}(),
		SceneID:      a.State.SceneId,
		SetID:        *formData.ScenesSet,
		IdentityInfo: apistructs.IdentityInfo{UserID: a.sdk.Identity.UserID},
	}, false, nil)
	return err
}
