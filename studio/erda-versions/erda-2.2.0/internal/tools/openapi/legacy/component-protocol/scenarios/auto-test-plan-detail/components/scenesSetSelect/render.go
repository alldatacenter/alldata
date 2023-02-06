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

package scenesSetSelect

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	i18nkey "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/auto-test-plan-detail/i18n"
	"github.com/erda-project/erda/pkg/i18n"
)

type ComponentAction struct {
	CtxBdl     protocol.ContextBundle
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
	ScenesSetId    string `json:"scenesSetId"`
	TestPlanStepId uint64 `json:"testPlanStepId"`
	Visible        bool   `json:"visible"`
	Value          Value  `json:"value"`
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

	var data apistructs.ComponentData = map[string]interface{}{}
	data["treeData"] = a.Data.TreeData
	c.Data = data
	c.State = state
	c.Type = a.Type
	c.Props = props
	c.Operations = operations
	return nil
}

func (a *ComponentAction) unmarshal(c *apistructs.Component) error {
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

	a.State = state
	a.Type = c.Type
	a.Data = value
	a.Props = prop
	//a.Operations = operation
	return nil
}

func (a *ComponentAction) SetBundle(b protocol.ContextBundle) error {
	if b.Bdl == nil {
		err := fmt.Errorf("invalie bundle")
		return err
	}
	a.CtxBdl = b
	return nil
}

func (ca *ComponentAction) Render(ctx context.Context, c *apistructs.Component,
	scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	err := ca.unmarshal(c)
	if err != nil {
		return err
	}
	defer func() {
		fail := ca.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()

	if !c.State["visible"].(bool) {
		return nil
	}

	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	i18nLocale := bdl.Bdl.GetLocale(bdl.Locale)
	err = ca.SetBundle(bdl)
	if err != nil {
		return err
	}

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

	ca.Operations = map[string]operations{
		apistructs.OnSearchOperation.String(): {
			Key:      apistructs.OnSearchOperation.String(),
			Reload:   true,
			FillMeta: "searchKey",
		},
		apistructs.OnChangeOperation.String(): {
			Key:      apistructs.OnChangeOperation.String(),
			Reload:   true,
			FillMeta: "value",
		},
		apistructs.OnLoadDataOperation.String(): {
			Key:      apistructs.OnLoadDataOperation.String(),
			Reload:   true,
			FillMeta: "pId",
		},
	}

	switch event.Operation {
	case apistructs.OnSearchOperation:
		if err := ca.HandlerDefaultValue(meta, bdl.InParams, i18nLocale); err != nil {
			return err
		}
	case apistructs.OnChangeOperation:
		if err := ca.HandlerChangeValue(meta); err != nil {
			return err
		}
	case apistructs.OnLoadDataOperation:
		if err := ca.HandlerDefaultValue(meta, bdl.InParams, i18nLocale); err != nil {
			return err
		}
	case apistructs.InitializeOperation, apistructs.RenderingOperation:
		if err := ca.HandlerDefaultValue(meta, bdl.InParams, i18nLocale); err != nil {
			return err
		}
	}
	return nil
}

func (i *ComponentAction) HandlerChangeValue(meta map[string]interface{}) error {
	metaValue := meta["value"].(map[string]interface{})
	i.State.ScenesSetId = strconv.Itoa(int(metaValue["value"].(float64)))
	i.State.Visible = true

	i.State.Value.Value = strconv.Itoa(int(metaValue["value"].(float64)))
	i.State.Value.Label = metaValue["label"].(string)
	return nil
}

func (i *ComponentAction) HandlerSearchValue(meta map[string]interface{}) error {
	var req apistructs.UnifiedFileTreeNodeFuzzySearchRequest
	req.UserID = i.CtxBdl.Identity.UserID
	req.Scope = apistructs.FileTreeScopeAutoTestConfigSheet
	req.Fuzzy = meta["searchKey"].(string)

	orgID, err := strconv.Atoi(i.CtxBdl.Identity.OrgID)
	if err != nil {
		return err
	}
	result, err := i.CtxBdl.Bdl.FuzzySearchFileTreeNodes(req, uint64(orgID))
	if err != nil {
		return err
	}

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

func (i *ComponentAction) HandlerDefaultValue(meta map[string]interface{}, inParams map[string]interface{}, i18nLocale *i18n.LocaleResource) error {
	resp, err := i.CtxBdl.Bdl.GetTestPlanV2Step(i.State.TestPlanStepId)
	if err != nil {
		return err
	}

	if resp.SceneSetID > 0 {
		i.State.ScenesSetId = strconv.Itoa(int(resp.SceneSetID))
		i.State.Value.Value = i.State.ScenesSetId
		i.State.Value.Label = resp.SceneSetName
	} else {
		i.State.Value = Value{}
		i.State.ScenesSetId = ""
	}

	testPlan, err := i.CtxBdl.Bdl.GetTestPlanV2(resp.PlanID)
	if err != nil {
		return err
	}

	var req apistructs.SceneSetRequest
	req.UserID = i.CtxBdl.Identity.UserID
	req.SpaceID = testPlan.Data.SpaceID
	sceneSets, err := i.CtxBdl.Bdl.GetSceneSets(req)
	if err != nil {
		return err
	}

	i.Data.TreeData = []interface{}{}
	for _, v := range sceneSets {

		if meta["searchKey"] != nil && meta["searchKey"].(string) != "" {
			if !strings.Contains(v.Name, meta["searchKey"].(string)) {
				continue
			}
		}
		i.Data.TreeData = append(i.Data.TreeData, map[string]interface{}{
			"key":        v.ID,
			"id":         v.ID,
			"pId":        "0",
			"title":      v.Name,
			"value":      v.ID,
			"isLeaf":     true,
			"selectable": true,
		})
	}
	i.Type = "TreeSelect"
	i.Props = props{
		Placeholder: i18nLocale.Get(i18nkey.I18nKeyChoose),
		Title:       i18nLocale.Get(i18nkey.I18nKeySelectSceneSet),
	}
	i.State.Visible = true
	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentAction{}
}
