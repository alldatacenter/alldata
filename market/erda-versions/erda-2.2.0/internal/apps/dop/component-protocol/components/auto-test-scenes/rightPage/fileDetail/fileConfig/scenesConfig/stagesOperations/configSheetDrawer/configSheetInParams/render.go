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

package configSheetInParams

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
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

type ComponentAction struct {
	sdk        *cptype.SDK
	bdl        *bundle.Bundle
	State      state                 `json:"state"`
	Props      props                 `json:"props"`
	Operations map[string]operations `json:"operations"`
	Type       string                `json:"type"`
}

type props struct {
	Fields  []interface{} `json:"fields"`
	Title   string        `json:"title"`
	Visible bool          `json:"visible"`
}
type state struct {
	FormData      interface{} `json:"formData"`
	StepID        uint64      `json:"stepId"`
	ConfigSheetID string      `json:"configSheetId"`
	Visible       bool        `json:"visible"`
}
type operations struct {
	Key      string                 `json:"key"`
	Reload   bool                   `json:"reload"`
	FillMeta string                 `json:"fillMeta"`
	Meta     interface{}            `json:"meta"`
	Command  map[string]interface{} `json:"command"`
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "configSheetInParams",
		func() servicehub.Provider { return &ComponentAction{} })
}

func (a *ComponentAction) marshal(c *cptype.Component) error {
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
	var props map[string]interface{}
	err = json.Unmarshal(propValue, &props)
	if err != nil {
		return err
	}

	operationsValue, err := json.Marshal(a.Operations)
	if err != nil {
		return err
	}
	var operations map[string]interface{}
	err = json.Unmarshal(operationsValue, &operations)
	if err != nil {
		return err
	}

	c.State = state
	c.Type = a.Type
	c.Props = props
	c.Operations = operations
	return nil
}

func (a *ComponentAction) unmarshal(c *cptype.Component) error {
	stateValue, err := json.Marshal(c.State)
	if err != nil {
		return err
	}
	var state state
	err = json.Unmarshal(stateValue, &state)
	if err != nil {
		return err
	}

	propValue, err := json.Marshal(c.Props)
	if err != nil {
		return err
	}
	var prop props
	err = json.Unmarshal(propValue, &prop)
	if err != nil {
		return err
	}

	var treeData []interface{}
	dataJson, err := json.Marshal(c.Data["treeData"])
	if err != nil {
		return err
	}
	err = json.Unmarshal(dataJson, &treeData)
	if err != nil {
		return err
	}
	a.State = state
	a.Type = c.Type
	a.Props = prop
	//a.Operations = operation
	return nil
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	ca.sdk = cputil.SDK(ctx)
	err := ca.unmarshal(c)
	if err != nil {
		return err
	}

	if ca.State.StepID <= 0 {
		ca.Props.Fields = nil
		ca.Props.Title = ""
		return nil
	}

	if ca.State.Visible == false {
		return nil
	}
	defer func() {
		fail := ca.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()
	ca.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	if err != nil {
		return err
	}

	switch event.Operation {
	case cptype.OperationKey(apistructs.OnSubmit):
		if err := ca.HandlerSubmitValue(); err != nil {
			return err
		}
	case cptype.OperationKey(apistructs.OnCancel):
		if err := ca.HandlerCancelValue(); err != nil {
			return err
		}
	case cptype.InitializeOperation, cptype.RenderingOperation:
		err := ca.handleDefault(ctx, gs)
		if err != nil {
			return err
		}
	}
	return nil
}

func (i *ComponentAction) handleDefault(ctx context.Context, gs *cptype.GlobalStateData) error {
	// 选中的 step
	var configSheetID string
	var autotestGetSceneStepReq apistructs.AutotestGetSceneStepReq
	autotestGetSceneStepReq.ID = i.State.StepID
	autotestGetSceneStepReq.UserID = i.sdk.Identity.UserID
	step, err := i.bdl.GetAutoTestSceneStep(autotestGetSceneStepReq)
	if err != nil {
		return err
	}
	// 选中的 step 的配置单 id
	if step.Value != "" {
		var value = make(map[string]interface{})
		err := json.Unmarshal([]byte(step.Value), &value)
		if err != nil {
			return err
		}
		i.State.FormData = value["runParams"]
		configSheetID = value["configSheetID"].(string)
	} else {
		i.State.FormData = nil
		i.Props.Fields = nil
		i.Props.Title = ""
	}

	if i.State.ConfigSheetID != "" && i.State.ConfigSheetID != "0" {
		configSheetID = i.State.ConfigSheetID
	}
	if configSheetID == "" || configSheetID == "0" {
		i.Props.Fields = nil
		i.Props.Title = ""
		i.Props.Visible = false
		i.State.Visible = false
		return nil
	} else {
		i.State.ConfigSheetID = configSheetID
	}

	// 查询配置单的入参
	var req apistructs.UnifiedFileTreeNodeGetRequest
	req.Scope = apistructs.FileTreeScopeAutoTestConfigSheet
	req.ScopeID = strconv.Itoa(int(cputil.GetInParamByKey(ctx, "projectId").(float64)))
	req.Inode = configSheetID
	req.UserID = i.sdk.Identity.UserID
	orgID, err := strconv.Atoi(i.sdk.Identity.OrgID)
	if err != nil {
		return err
	}
	result, err := i.bdl.GetFileTreeNode(req, uint64(orgID))
	if err != nil {
		(*gs)[protocol.GlobalInnerKeyError.String()] = fmt.Sprintf("failed to query file tree nodes, please check config sheets")
		result = &apistructs.UnifiedFileTreeNode{Meta: map[string]interface{}{}}
	}
	_, ok := result.Meta[apistructs.AutoTestFileTreeNodeMetaKeyPipelineYml]
	if ok {
		yml := result.Meta[apistructs.AutoTestFileTreeNodeMetaKeyPipelineYml].(string)
		pipelineYml, err := pipelineyml.New([]byte(yml))
		if err != nil {
			return err
		}
		params := pipelineYml.Spec().Params

		var fromData = map[string]interface{}{}
		for _, v := range params {

			fromData[v.Name] = v.Default

			var find = false
			for _, va := range i.Props.Fields {
				values := va.(map[string]interface{})
				if values["key"] == v.Name {
					find = true
				}
			}
			if !find {
				i.Props.Fields = append(i.Props.Fields, map[string]interface {
				}{
					"label":     v.Name,
					"component": "input",
					"required":  v.Required,
					"key":       v.Name,
				})
			}
		}

		if i.State.FormData == nil {
			i.State.FormData = fromData
		}

		if params != nil && len(params) >= 0 {
			i.Props.Title = "节点入参"
		} else {
			i.Props.Fields = nil
			i.Props.Title = ""
		}
	} else {
		i.Props.Fields = nil
		i.Props.Title = ""
	}

	i.Props.Visible = true
	i.Operations = map[string]operations{
		"submit": {
			Key:    "submit",
			Reload: true,
		},
		"cancel": {
			Key:    "cancel",
			Reload: true,
			Command: map[string]interface{}{
				"key":    "set",
				"target": "configSheetDrawer",
				"state":  map[string]interface{}{"visible": false},
			},
		},
	}
	return nil
}

func (i *ComponentAction) HandlerSubmitValue() error {
	var value = make(map[string]interface{})
	var unifiedFileTreeNodeGetRequest apistructs.UnifiedFileTreeNodeGetRequest
	unifiedFileTreeNodeGetRequest.UserID = i.sdk.Identity.UserID
	unifiedFileTreeNodeGetRequest.Scope = apistructs.FileTreeScopeAutoTestConfigSheet
	unifiedFileTreeNodeGetRequest.Inode = i.State.ConfigSheetID
	orgIDInt, err := strconv.Atoi(i.sdk.Identity.OrgID)
	if err != nil {
		return err
	}

	node, err := i.bdl.GetFileTreeNode(unifiedFileTreeNodeGetRequest, uint64(orgIDInt))
	if err != nil {
		return err
	}

	value["configSheetName"] = node.Name
	value["configSheetID"] = i.State.ConfigSheetID
	value["runParams"] = i.State.FormData

	valueJson, err := json.Marshal(value)
	if err != nil {
		return err
	}
	var req apistructs.AutotestSceneRequest
	req.ID = i.State.StepID
	req.Value = string(valueJson)
	req.Name = node.Name
	req.UserID = i.sdk.Identity.UserID
	_, err = i.bdl.UpdateAutoTestSceneStep(req)
	if err != nil {
		return err
	}

	i.Props.Visible = false
	i.Props.Fields = nil
	i.State.Visible = false
	i.State.StepID = 0
	i.State.ConfigSheetID = ""
	return nil
}

func (i *ComponentAction) HandlerCancelValue() error {
	i.State.Visible = false
	i.Props.Visible = false
	i.Props.Fields = nil
	i.State.StepID = 0
	i.State.ConfigSheetID = ""
	return nil
}
