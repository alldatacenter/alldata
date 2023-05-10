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

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

type ComponentAction struct {
	sdk      *cptype.SDK
	bdl      *bundle.Bundle
	gsHelper *gshelper.GSHelper

	Type       string                 `json:"type"`
	Props      map[string]interface{} `json:"props"`
	State      state                  `json:"state"`
	Operations map[string]interface{} `json:"operations"`
	Data       map[string]interface{} `json:"data"`

	pipelineIDFromExecuteHistoryTable   uint64
	pipelineIDFromExecuteTaskBreadcrumb uint64
	visible                             bool
	sceneID                             uint64
}

type state struct {
	Name   string `json:"name"`
	Unfold bool   `json:"unfold"`
}

type Breadcrumb struct {
	Key  string `json:"key"`
	Item string `json:"item"`
}

func (a *ComponentAction) Import(c *cptype.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, a); err != nil {
		return err
	}
	return nil
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "executeTaskBreadcrumb",
		func() servicehub.Provider { return &ComponentAction{} })
}

func (a *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	gh := gshelper.NewGSHelper(gs)
	a.Type = "Breadcrumb"
	if err := a.Import(c); err != nil {
		logrus.Errorf("import component failed, err:%v", err)
		return err
	}
	a.pipelineIDFromExecuteHistoryTable = gh.GetExecuteHistoryTablePipelineID()
	a.sceneID = gh.GetFileTreeSceneID()

	a.sdk = cputil.SDK(ctx)
	a.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	a.gsHelper = gshelper.NewGSHelper(gs)

	if a.pipelineIDFromExecuteHistoryTable == 0 {
		c.Data = map[string]interface{}{}
		c.State = map[string]interface{}{"visible": true}
		return nil
	}
	// listen on operation
	switch event.Operation {
	case cptype.RenderingOperation, cptype.InitializeOperation:
		setID := a.gsHelper.GetGlobalSelectedSetID()
		if a.sceneID == 0 && setID == 0 {
			return nil
		}
		res := []Breadcrumb{}
		if !a.State.Unfold {
			if a.sceneID != 0 {
				req := apistructs.AutotestSceneRequest{SceneID: a.sceneID}
				req.UserID = a.sdk.Identity.UserID
				scene, err := a.bdl.GetAutoTestScene(req)
				if err != nil {
					logrus.Errorf("get autoTestScene failed, err: %v", err)
					return err
				}
				a.State.Name = scene.Name
			}
			if setID != 0 {
				req := apistructs.SceneSetRequest{SetID: setID}
				req.UserID = a.sdk.Identity.UserID
				sceneSet, err := a.bdl.GetSceneSet(req)
				if err != nil {
					logrus.Errorf("get autoTestSceneSet failed, err: %v", err)
					return err
				}
				a.State.Name = sceneSet.Name
			}
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
		fmt.Println("name:  ", a.State.Name, "  pipelineID:  ", a.pipelineIDFromExecuteHistoryTable)
		res = append(res, Breadcrumb{
			Key:  strconv.FormatUint(a.pipelineIDFromExecuteHistoryTable, 10),
			Item: a.State.Name,
		})
		a.visible = len(res) < 2
		a.Data = map[string]interface{}{"list": res}
	case cptype.OperationKey(apistructs.ExecuteTaskBreadcrumbSelectItem):
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
		a.visible = len(lists) < 2
		a.Data = map[string]interface{}{"list": lists}
		a.pipelineIDFromExecuteTaskBreadcrumb, err = strconv.ParseUint(ret.Meta.Key, 10, 64)
		if err != nil {
			logrus.Errorf("pipelineId ParseUint failed, err: %v", err)
			return err
		}
		gh.SetExecuteHistoryTablePipelineID(a.pipelineIDFromExecuteTaskBreadcrumb)
	}
	c.Operations = getOperations()
	c.Data = a.Data
	var err error
	defer func() {
		fail := a.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()

	// set global state
	gh.SetExecuteTaskBreadcrumbPipelineID(a.pipelineIDFromExecuteTaskBreadcrumb)
	gh.SetExecuteTaskBreadcrumbVisible(a.visible)

	return nil
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
	var props cptype.ComponentProps
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
