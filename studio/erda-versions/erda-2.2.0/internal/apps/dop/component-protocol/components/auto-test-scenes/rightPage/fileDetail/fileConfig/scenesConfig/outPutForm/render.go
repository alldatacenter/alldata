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

package outPutForm

import (
	"context"
	"encoding/json"
	"fmt"

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

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "outPutForm",
		func() servicehub.Provider { return &ComponentOutPutForm{} })
}

// GenComponentState 获取state
func (i *ComponentOutPutForm) GenComponentState(c *cptype.Component) error {
	if c == nil || c.State == nil {
		return nil
	}
	var state State
	cont, err := json.Marshal(c.State)
	if err != nil {
		logrus.Errorf("marshal component state failed, content:%v, err:%v", c.State, err)
		return err
	}
	err = json.Unmarshal(cont, &state)
	if err != nil {
		logrus.Errorf("unmarshal component state failed, content:%v, err:%v", cont, err)
		return err
	}
	i.State = state
	return nil
}

func (i *ComponentOutPutForm) RenderProtocol(c *cptype.Component, g *cptype.GlobalStateData) {
	if c.Data == nil {
		d := make(cptype.ComponentData)
		c.Data = d
	}
	if c.State == nil {
		d := make(cptype.ComponentState)
		c.State = d
	}
	if c.Operations == nil {
		c.Operations = make(cptype.ComponentOperations)
	}
	c.State["list"] = i.Data.List
	c.Props = i.Props

	c.Operations["save"] = OperationBaseInfo{
		Key:    "save",
		Reload: true,
	}
}

func GetOpsInfo(opsData interface{}) (*OpMetaInfo, error) {
	if opsData == nil {
		err := fmt.Errorf("empty operation data")
		return nil, err
	}
	var op OperationInfo
	cont, err := json.Marshal(opsData)
	if err != nil {
		logrus.Errorf("marshal inParams failed, content:%v, err:%v", opsData, err)
		return nil, err
	}
	err = json.Unmarshal(cont, &op)
	if err != nil {
		logrus.Errorf("unmarshal move out request failed, content:%v, err:%v", cont, err)
		return nil, err
	}
	meta := op.Meta
	return &meta, nil
}

func (i *ComponentOutPutForm) GetInParams(ctx context.Context) error {
	var inParams InParams
	cont, err := json.Marshal(cputil.SDK(ctx).InParams)
	if err != nil {
		logrus.Errorf("marshal inParams failed, content:%v, err:%v", cputil.SDK(ctx).InParams, err)
		return err
	}
	err = json.Unmarshal(cont, &inParams)
	if err != nil {
		logrus.Errorf("unmarshal move out request failed, content:%v, err:%v", cont, err)
		return err
	}
	i.InParams = inParams
	return nil
}

func (i *ComponentOutPutForm) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) (err error) {
	gh := gshelper.NewGSHelper(gs)
	i.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	i.sdk = cputil.SDK(ctx)
	err = i.GenComponentState(c)
	if err != nil {
		return
	}
	if err = i.GetInParams(ctx); err != nil {
		return err
	}

	i.Props = make(map[string]interface{})
	i.State.AutotestSceneRequest.UserID = i.sdk.Identity.UserID
	i.State.AutotestSceneRequest.SetID = i.InParams.SceneSetID
	i.State.AutotestSceneRequest.SceneID = gh.GetFileTreeSceneID()

	visible := make(map[string]interface{})
	visible["visible"] = true
	if i.State.AutotestSceneRequest.SceneID == 0 {
		visible["visible"] = false
		c.Props = visible
		return
	}
	i.Props = visible

	switch event.Operation {
	case cptype.InitializeOperation, cptype.RenderingOperation:
		err = i.RenderListOutPutForm(gs)
		if err != nil {
			return err
		}
	case cptype.OperationKey(apistructs.AutoTestSceneInputUpdateOperationKey):
		err = i.RenderUpdateOutPutForm()
		if err != nil {
			return err
		}
		err = i.RenderListOutPutForm(gs)
		if err != nil {
			return err
		}
	}
	i.RenderProtocol(c, gs)
	return
}
