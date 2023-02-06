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

package tabExecuteButton

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/auto-test-plan-detail/i18n"
)

type ComponentAction struct {
	CtxBdl protocol.ContextBundle
	Type   string `json:"type"`
	Props  Props  `json:"props"`
	State  State  `json:"state"`
}

func (a *ComponentAction) SetBundle(b protocol.ContextBundle) error {
	if b.Bdl == nil {
		err := fmt.Errorf("invalie bundle")
		return err
	}
	a.CtxBdl = b
	return nil
}

func (a *ComponentAction) marshal(c *apistructs.Component) error {
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
	var prop map[string]interface{}
	err = json.Unmarshal(propValue, &prop)
	if err != nil {
		return err
	}

	c.Props = prop
	c.State = state
	c.Type = a.Type
	return nil
}

func (a *ComponentAction) unmarshal(c *apistructs.Component) error {
	stateValue, err := json.Marshal(c.State)
	if err != nil {
		return err
	}
	var state State
	err = json.Unmarshal(stateValue, &state)
	if err != nil {
		return err
	}

	propValue, err := json.Marshal(c.Props)
	if err != nil {
		return err
	}
	var props Props
	err = json.Unmarshal(propValue, &props)
	if err != nil {
		return err
	}

	a.State = state
	a.Type = c.Type
	a.Props = props
	return nil
}

type State struct {
	Env        string               `json:"env"`
	TestPlanID uint64               `json:"testPlanID"`
	ActiveKey  apistructs.ActiveKey `json:"activeKey"`
}

type ClientMetaData struct {
	Env        string `json:"env"`
	TestPlanID uint64 `json:"testPlanID"`
	ConfigEnv  string `json:"ConfigEnv"`
	OrgID      uint64 `json:"orgID"`
	ProjectID  uint64 `json:"projectID"`
}

type Props struct {
	Test  string `json:"text"`
	Type  string `json:"type"`
	Menus []Menu `json:"menu"`
}

type Menu struct {
	Text       string                 `json:"text"`
	Key        string                 `json:"key"`
	Operations map[string]interface{} `json:"operations"`
}

type ClickOperation struct {
	Key    string      `json:"key"`
	Reload bool        `json:"reload"`
	Meta   interface{} `json:"meta"`
}

func (ca *ComponentAction) Render(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	err := ca.unmarshal(c)
	if err != nil {
		return err
	}

	if ca.State.TestPlanID <= 0 {
		return nil
	}

	defer func() {
		fail := ca.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()

	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	err = ca.SetBundle(bdl)
	if err != nil {
		return err
	}

	switch event.Operation {
	case apistructs.InitializeOperation, apistructs.RenderingOperation:
		err := ca.handleDefault()
		if err != nil {
			return err
		}
	case "execute":
		err := ca.handleClick(event, gs)
		if err != nil {
			return err
		}
	}

	return nil
}

func (a *ComponentAction) handleDefault() error {
	testPlanID := a.State.TestPlanID
	testPlanV2, err := a.CtxBdl.Bdl.GetTestPlanV2(testPlanID)
	if err != nil {
		return err
	}
	spaces, err := a.CtxBdl.Bdl.GetTestSpace(testPlanV2.Data.SpaceID)
	if err != nil {
		return err
	}

	project, err := a.CtxBdl.Bdl.GetProject(uint64(spaces.ProjectID))
	if err != nil {
		return err
	}

	testClusterName, ok := project.ClusterConfig[string(apistructs.TestWorkspace)]
	if !ok {
		return fmt.Errorf("not found cluster")
	}

	i18nLocale := a.CtxBdl.Bdl.GetLocale(a.CtxBdl.Locale)
	a.Props.Type = "primary"
	a.Props.Test = i18nLocale.Get(i18n.I18nKeyExecute)

	var autoTestGlobalConfigListRequest apistructs.AutoTestGlobalConfigListRequest
	autoTestGlobalConfigListRequest.ScopeID = strconv.Itoa(int(project.ID))
	autoTestGlobalConfigListRequest.Scope = "project-autotest-testcase"
	autoTestGlobalConfigListRequest.UserID = a.CtxBdl.Identity.UserID
	configs, err := a.CtxBdl.Bdl.ListAutoTestGlobalConfig(autoTestGlobalConfigListRequest)
	if err != nil {
		return err
	}

	a.Props.Menus = []Menu{
		{
			Text: i18nLocale.Get(i18n.I18nKeyPlanNull),
			Key:  "无",
			Operations: map[string]interface{}{
				apistructs.ClickOperation.String(): ClickOperation{
					Key:    "execute",
					Reload: true,
					Meta: ClientMetaData{
						Env:        testClusterName,
						TestPlanID: a.State.TestPlanID,
						ConfigEnv:  "",
						OrgID:      project.OrgID,
						ProjectID:  project.ID,
					},
				},
			},
		},
	}

	for _, v := range configs {
		a.Props.Menus = append(a.Props.Menus, Menu{
			Text: v.DisplayName,
			Key:  v.Ns,
			Operations: map[string]interface{}{
				apistructs.ClickOperation.String(): ClickOperation{
					Key:    "execute",
					Reload: true,
					Meta: ClientMetaData{
						Env:        testClusterName,
						TestPlanID: a.State.TestPlanID,
						ConfigEnv:  v.Ns,
						OrgID:      project.OrgID,
						ProjectID:  project.ID,
					},
				},
			},
		})
	}
	return nil
}

func (a *ComponentAction) handleClick(event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	metaJson, err := json.Marshal(event.OperationData["meta"])
	if err != nil {
		return err
	}
	var metaData ClientMetaData
	err = json.Unmarshal(metaJson, &metaData)
	if err != nil {
		return err
	}
	var req apistructs.AutotestExecuteTestPlansRequest
	req.TestPlan.ID = metaData.TestPlanID
	req.ClusterName = metaData.Env
	req.ConfigManageNamespaces = metaData.ConfigEnv
	req.UserID = a.CtxBdl.Identity.UserID
	req.Labels = map[string]string{
		apistructs.LabelProjectID: strconv.FormatUint(metaData.ProjectID, 10),
		apistructs.LabelOrgID:     strconv.FormatUint(metaData.OrgID, 10),
	}
	a.State.ActiveKey = "Execute"
	pipeline, err := a.CtxBdl.Bdl.ExecuteDiceAutotestTestPlan(req)
	if err != nil {
		(*gs)[protocol.GlobalInnerKeyError.String()] = err.Error()
	} else {
		logrus.Infof("run testplan pipeline success testplan id: %v, pipelineID %v", req.TestPlan.ID, pipeline.Data.ID)
	}
	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentAction{}
}
