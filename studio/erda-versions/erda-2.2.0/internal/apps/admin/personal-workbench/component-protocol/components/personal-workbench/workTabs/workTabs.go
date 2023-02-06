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

package workTabs

import (
	"context"
	"fmt"
	"sync"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/common"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/common/gshelper"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/services/workbench"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/types"
)

type Option struct {
	Value string `json:"value"`
	Label string `json:"label"`
}

type Data struct {
	Options []Option `json:"options"`
}

type Operation struct {
	ClientData ClientData `json:"clientData"`
}

type ClientData struct {
	Value string `json:"value"`
}

type State struct {
	Value string `json:"value"`
}

type WorkTabs struct {
	SDK        *cptype.SDK
	Bdl        *bundle.Bundle
	Wb         *workbench.Workbench
	Data       Data                 `json:"data"`
	Operations map[string]Operation `json:"operations"`
	State      State                `json:"state"`
}

func (wt *WorkTabs) GetState() State {
	return State{Value: apistructs.WorkbenchItemProj.String()}
}

func (wt *WorkTabs) SetState(state cptype.ComponentState) {
	err := common.Transfer(state, wt.State)
	if err != nil {
		logrus.Error(err)
		return
	}
}

func (wt *WorkTabs) GetOperation() {
	wt.Operations = map[string]Operation{"onChange": {ClientData{Value: apistructs.WorkbenchItemProj.String()}}}
}

// SetComponentValue mapping properties to Component
func (wt *WorkTabs) SetComponentValue(c *cptype.Component) error {
	var err error
	if err = common.Transfer(wt.State, &c.State); err != nil {
		return err
	}
	if err = common.Transfer(wt.Data, &c.Data); err != nil {
		return err
	}
	//if err = common.Transfer(l.Props, &c.Props); err != nil {
	//	return err
	//}
	if err = common.Transfer(wt.Operations, &c.Operations); err != nil {
		return err
	}
	return nil
}

func (wt *WorkTabs) GetData(gs *cptype.GlobalStateData, Type string) (Data, error) {
	var (
		projNum int
		appNum  int
		err     error
	)
	wtData := Data{Options: []Option{
		{Value: apistructs.WorkbenchItemProj.String(), Label: wt.SDK.I18n("project")},
		{Value: apistructs.WorkbenchItemApp.String(), Label: wt.SDK.I18n("app")},
	}}
	apiIdentity := apistructs.Identity{}
	apiIdentity.UserID = wt.SDK.Identity.UserID
	apiIdentity.OrgID = wt.SDK.Identity.OrgID
	wg := &sync.WaitGroup{}
	wg.Add(2)
	go func() {
		projNum, err = wt.Wb.GetProjNum(apiIdentity, "")
		if err != nil {
			logrus.Errorf("tabs get project list err %v", err)
		}
		wg.Done()
	}()
	go func() {
		appNum, err = wt.Wb.GetAppNum(apiIdentity, "")
		if err != nil {
			logrus.Errorf("tabs get app list err %v", err)
		}
		wg.Done()
	}()
	wg.Wait()
	wtData.Options[0].Label += fmt.Sprintf("(%d)", projNum)
	wtData.Options[1].Label += fmt.Sprintf("(%d)", appNum)
	return wtData, nil
}

// Render is empty implement.
func (wt *WorkTabs) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	err := common.Transfer(c.State, &wt.State)
	if err != nil {
		return err
	}
	if wt.State.Value == "" {
		// init with project
		wt.State.Value = apistructs.WorkbenchItemProj.String()
	}
	wt.SDK = cputil.SDK(ctx)
	gh := gshelper.NewGSHelper(gs)
	wt.Wb = wt.SDK.Ctx.Value(types.Workbench).(*workbench.Workbench)
	wt.GetOperation()
	switch event.Operation {
	case cptype.InitializeOperation, cptype.RenderingOperation:
	case common.EventChangeEventTab:
		cd := ClientData{}
		err = common.Transfer(event.OperationData["clientData"], &cd)
		if err != nil {
			return err
		}
		wt.State.Value = cd.Value
	default:
		logrus.Errorf("scenario %v component WorkTabs does not support event %v", scenario, event)
		return nil
	}
	gh.SetWorkbenchItemType(wt.State.Value)
	wtData, err := wt.GetData(gs, wt.State.Value)
	if err != nil {
		return err
	}
	wt.Data = wtData
	err = wt.SetComponentValue(c)
	if err != nil {
		return err
	}
	return nil
}

func init() {
	base.InitProviderWithCreator(common.ScenarioKey, "workTabs", func() servicehub.Provider {
		return &WorkTabs{}
	})
}
