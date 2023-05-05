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

package executeTaskBreadcrumb

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

type ComponentAction struct {
	CtxBdl     protocol.ContextBundle
	Type       string                 `json:"type"`
	Props      map[string]interface{} `json:"props"`
	State      state                  `json:"state"`
	Operations map[string]interface{} `json:"operations"`
	Data       map[string]interface{} `json:"data"`
}

type state struct {
	PipelineID uint64 `json:"pipelineId"`
	Name       string `json:"name"`
	Visible    bool   `json:"visible"`
	Unfold     bool   `json:"unfold"`
}

type Breadcrumb struct {
	Key  string `json:"key"`
	Item string `json:"item"`
}

type inParams struct {
	ProjectID  int64  `json:"projectId"`
	TestPlanID uint64 `json:"testPlanID"`
}

func (a *ComponentAction) Import(c *apistructs.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, a); err != nil {
		return err
	}
	return nil
}

func (a *ComponentAction) Render(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	a.Type = "Breadcrumb"
	if err := a.Import(c); err != nil {
		logrus.Errorf("import component failed, err:%v", err)
		return err
	}

	a.CtxBdl = ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)

	if a.CtxBdl.InParams == nil {
		return fmt.Errorf("params is empty")
	}

	inParamsBytes, err := json.Marshal(a.CtxBdl.InParams)
	if err != nil {
		return fmt.Errorf("failed to marshal inParams, inParams:%+v, err:%v", a.CtxBdl.InParams, err)
	}

	var inParams inParams
	if err := json.Unmarshal(inParamsBytes, &inParams); err != nil {
		return err
	}

	if a.State.PipelineID == 0 {
		c.Data = map[string]interface{}{}
		c.State = map[string]interface{}{"visible": true}
		return nil
	}
	// listen on operation
	switch event.Operation {
	case apistructs.RenderingOperation, apistructs.InitializeOperation:
		if inParams.TestPlanID == 0 {
			return nil
		}
		res := []Breadcrumb{}
		if !a.State.Unfold {
			testPlan, err := a.CtxBdl.Bdl.GetTestPlanV2(inParams.TestPlanID)
			if err != nil {
				logrus.Errorf("get autoTestScene failed, err: %v", err)
				return err
			}
			a.State.Name = testPlan.Data.Name
		} else {
			b, err := json.Marshal(a.Data["list"])
			if err != nil {
				return err
			}
			if err := json.Unmarshal(b, &res); err != nil {
				return err
			}
		}
		a.State.Unfold = false
		// changePage bug修复
		l := len(res)
		if l == 0 || res[l-1].Item != a.State.Name {
			res = append(res, Breadcrumb{
				Key:  strconv.FormatUint(a.State.PipelineID, 10),
				Item: a.State.Name,
			})
		}
		a.State.Visible = l < 2
		a.Data = map[string]interface{}{"list": res}
	case apistructs.ExecuteTaskBreadcrumbSelectItem:
		res := []Breadcrumb{}
		b, err := json.Marshal(a.Data["list"])
		if err != nil {
			return err
		}
		if err := json.Unmarshal(b, &res); err != nil {
			return err
		}
		ret := struct {
			Meta Breadcrumb `json:"meta"`
		}{}
		b, err = json.Marshal(event.OperationData)
		if err != nil {
			return err
		}
		if err := json.Unmarshal(b, &ret); err != nil {
			return err
		}

		lists := []Breadcrumb{}
		for _, each := range res {
			lists = append(lists, each)
			if each.Key == ret.Meta.Key {
				break
			}
		}
		a.State.Visible = len(lists) < 2
		a.Data = map[string]interface{}{"list": lists}
		a.State.PipelineID, err = strconv.ParseUint(ret.Meta.Key, 10, 64)
		if err != nil {
			logrus.Errorf("pipelineId ParseUint failed, err: %v", err)
			return err
		}
	}
	c.Operations = getOperations()
	c.Data = a.Data
	defer func() {
		fail := a.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()
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
	var props interface{}
	err = json.Unmarshal(propValue, &props)
	if err != nil {
		return err
	}

	c.Props = props
	c.State = state
	c.Type = a.Type
	return nil
}

func getOperations() map[string]interface{} {
	return map[string]interface{}{
		"click": map[string]interface{}{
			"key":      "selectItem",
			"reload":   true,
			"fillMeta": "key",
			"meta":     map[string]interface{}{"key": ""},
		},
	}
}

func RenderCreator() protocol.CompRender {
	return &ComponentAction{
		Props:      map[string]interface{}{},
		Operations: map[string]interface{}{},
		Data:       map[string]interface{}{},
	}
}
