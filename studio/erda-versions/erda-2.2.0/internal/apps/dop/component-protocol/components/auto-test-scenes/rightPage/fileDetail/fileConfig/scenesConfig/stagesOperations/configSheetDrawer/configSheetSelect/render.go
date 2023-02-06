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

package configSheetSelect

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
)

type ComponentAction struct {
	CtxBdl protocol.ContextBundle

	sdk        *cptype.SDK
	bdl        *bundle.Bundle
	Data       Data                  `json:"data"`
	State      state                 `json:"state"`
	Props      props                 `json:"props"`
	Operations map[string]operations `json:"operations"`
	Type       string                `json:"type"`
}

type props struct {
	Placeholder string `json:"placeholder"`
	Title       string `json:"title"`
}
type state struct {
	ConfigSheetId string `json:"configSheetId"`
	StepId        uint64 `json:"stepId"`
	Visible       bool   `json:"visible"`
	Value         Value  `json:"value"`
}

type Value struct {
	Value string `json:"value"`
	Label string `json:"label"`
}

type operations struct {
	Key      string      `json:"key"`
	Reload   bool        `json:"reload"`
	FillMeta string      `json:"fillMeta"`
	Meta     interface{} `json:"meta"`
}
type Data struct {
	TreeData []interface{} `json:"treeData"`
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

	if a.Operations != nil {
		operationsValue, err := json.Marshal(a.Operations)
		if err != nil {
			return err
		}
		var operations map[string]interface{}
		err = json.Unmarshal(operationsValue, &operations)
		if err != nil {
			return err
		}
		c.Operations = operations
	}

	var data cptype.ComponentData = map[string]interface{}{}
	data["treeData"] = a.Data.TreeData
	c.Data = data
	c.State = state
	c.Type = a.Type
	c.Props = props
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
	var value Data
	value.TreeData = treeData

	operationsJson, err := json.Marshal(c.Operations)
	if err != nil {
		return err
	}
	var operation map[string]operations
	err = json.Unmarshal(operationsJson, &operation)
	if err != nil {
		return err
	}

	a.State = state
	a.Type = c.Type
	a.Data = value
	a.Props = prop
	a.Operations = operation
	return nil
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	err := ca.unmarshal(c)
	if err != nil {
		return err
	}

	cv, ok := c.State["visible"]
	if !ok {
		return nil
	}
	if !cv.(bool) {
		return nil
	}

	defer func() {
		fail := ca.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()

	ca.sdk = cputil.SDK(ctx)
	ca.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)

	operationDataJson, err := json.Marshal(event.OperationData)
	if err != nil {
		return err
	}
	var metaMap map[string]interface{}
	err = json.Unmarshal(operationDataJson, &metaMap)
	if err != nil {
		return err
	}

	metaValue, ok := metaMap["meta"]
	var meta map[string]interface{}
	if ok {
		metaJson, err := json.Marshal(metaValue)
		if err != nil {
			return err
		}
		err = json.Unmarshal(metaJson, &meta)
		if err != nil {
			return err
		}
	}
	fmt.Println("meta: ", meta)
	switch event.Operation {
	case cptype.OperationKey(apistructs.OnSearchOperation):
		if err := ca.HandlerSearchValue(meta); err != nil {
			return err
		}
	case cptype.OperationKey(apistructs.OnChangeOperation):
		if err := ca.HandlerChangeValue(meta); err != nil {
			return err
		}
	case cptype.OperationKey(apistructs.OnLoadDataOperation):
		if err := ca.HandlerDefaultValue(meta, ctx); err != nil {
			return err
		}
	case cptype.InitializeOperation, cptype.RenderingOperation:
		if err := ca.HandlerDefaultValue(meta, ctx); err != nil {
			return err
		}
	}
	return nil
}

func (i *ComponentAction) HandlerChangeValue(meta map[string]interface{}) error {
	metaValue := meta["value"].(map[string]interface{})
	i.State.ConfigSheetId = metaValue["value"].(string)
	i.State.Visible = true
	i.State.Value.Value = metaValue["value"].(string)
	i.State.Value.Label = metaValue["label"].(string)
	return nil
}

func (i *ComponentAction) HandlerSearchValue(meta map[string]interface{}) error {
	var req apistructs.UnifiedFileTreeNodeFuzzySearchRequest
	req.UserID = i.sdk.Identity.UserID
	req.Scope = apistructs.FileTreeScopeAutoTestConfigSheet
	req.Fuzzy = meta["searchKey"].(string)
	req.ScopeID = strconv.Itoa(int(i.sdk.InParams["projectId"].(float64)))

	orgID, err := strconv.Atoi(i.sdk.Identity.OrgID)
	if err != nil {
		return err
	}
	result, err := i.bdl.FuzzySearchQaFileTreeNodes(req, uint64(orgID))
	if err != nil {
		return err
	}
	fmt.Println("result: ", result)
	i.Data.TreeData = []interface{}{}

	for _, v := range result {
		var isLeaf bool
		if v.Type == apistructs.UnifiedFileTreeNodeTypeDir {
			isLeaf = false
		} else {
			isLeaf = true
		}
		i.Data.TreeData = append(i.Data.TreeData, map[string]interface{}{
			"key":        v.Inode,
			"id":         v.Inode,
			"pId":        v.Pinode,
			"title":      v.Name,
			"value":      v.Inode,
			"isLeaf":     isLeaf,
			"selectable": isLeaf,
		})
	}
	return nil
}

func (i *ComponentAction) HandlerDefaultValue(meta map[string]interface{}, ctx context.Context) error {
	var autotestGetSceneStepReq apistructs.AutotestGetSceneStepReq
	autotestGetSceneStepReq.ID = i.State.StepId
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
		if value["configSheetID"] != nil && value["configSheetID"].(string) != "" {
			i.State.ConfigSheetId = value["configSheetID"].(string)

			i.State.Value.Value = i.State.ConfigSheetId
			i.State.Value.Label = value["configSheetName"].(string)
		}
	} else {
		i.State.Value = Value{}
		i.State.ConfigSheetId = ""
	}

	var req apistructs.UnifiedFileTreeNodeListRequest
	req.UserID = i.sdk.Identity.UserID
	req.Scope = apistructs.FileTreeScopeAutoTestConfigSheet
	req.ScopeID = strconv.Itoa(int(cputil.GetInParamByKey(ctx, "projectId").(float64)))

	flag := meta == nil
	if !flag {
		switch meta["pId"].(type) {
		case map[string]interface{}:
			flag = len(meta["pId"].(map[string]interface{})) == 0
		case string:
			flag = meta["pId"].(string) == ""
		}
	}
	if flag || meta["pId"] == nil {
		req.Pinode = "0"
	} else {
		req.Pinode = meta["pId"].(string)
	}

	orgID, err := strconv.Atoi(i.sdk.Identity.OrgID)
	if err != nil {
		return err
	}

	result, err := i.bdl.ListFileTreeNodes(req, uint64(orgID))
	if err != nil {
		return err
	}

	if i.Data.TreeData == nil {
		i.Data.TreeData = []interface{}{}
	}
	for _, v := range result {
		var isLeaf bool
		if v.Type == apistructs.UnifiedFileTreeNodeTypeDir {
			isLeaf = false
		} else {
			isLeaf = true
		}
		var find = false
		for _, va := range i.Data.TreeData {
			values := va.(map[string]interface{})
			if values["id"] == v.Inode {
				find = true
			}
		}
		if !find {
			i.Data.TreeData = append(i.Data.TreeData, map[string]interface{}{
				"key":        v.Inode,
				"id":         v.Inode,
				"pId":        v.Pinode,
				"title":      v.Name,
				"value":      v.Inode,
				"isLeaf":     isLeaf,
				"selectable": isLeaf,
			})
		}
	}
	i.Type = "TreeSelect"
	i.Props = props{
		Placeholder: "请选择",
		Title:       cputil.I18n(ctx, "selectConfig"),
	}
	i.Operations = map[string]operations{
		apistructs.OnSearchOperation.String(): {
			Key:      "onSearch",
			Reload:   true,
			FillMeta: "searchKey",
		},
		apistructs.OnChangeOperation.String(): {
			Key:      "onChange",
			Reload:   true,
			FillMeta: "value",
		},
		apistructs.OnLoadDataOperation.String(): {
			Key:      "onLoadData",
			Reload:   true,
			FillMeta: "pId",
		},
	}
	i.State.Visible = true
	return nil
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "configSheetSelect",
		func() servicehub.Provider { return &ComponentAction{} })
}
