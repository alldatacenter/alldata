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

package fileInfo

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/auto-test-plan-detail/i18n"
)

// SetCtxBundle 设置bundle
func (i *ComponentFileInfo) SetCtxBundle(b protocol.ContextBundle) error {
	if b.Bdl == nil || b.I18nPrinter == nil {
		err := fmt.Errorf("invalie context bundle")
		return err
	}
	logrus.Infof("inParams:%+v, identity:%+v", b.InParams, b.Identity)
	i.ctxBdl = b
	return nil
}

// GenComponentState 获取state
func (i *ComponentFileInfo) GenComponentState(c *apistructs.Component) error {
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

// GetReq 从InParams中获取请求参数
func (i ComponentFileInfo) GetReq() error {
	var inParams InParams
	cont, err := json.Marshal(i.ctxBdl.InParams)
	if err != nil {
		logrus.Errorf("marshal inParams failed, content:%v, err:%v", i.ctxBdl.InParams, err)
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

func (i *ComponentFileInfo) RenderProtocol(c *apistructs.Component, g *apistructs.GlobalStateData) {
	if c.Data == nil {
		d := make(apistructs.ComponentData)
		c.Data = d
	}
	(*c).Data["data"] = i.Data
	c.Props = i.Props
}

func (i *ComponentFileInfo) GetInParams() error {
	var inParams InParams
	cont, err := json.Marshal(i.ctxBdl.InParams)
	if err != nil {
		logrus.Errorf("marshal inParams failed, content:%v, err:%v", i.ctxBdl.InParams, err)
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

func (i *ComponentFileInfo) Render(ctx context.Context, c *apistructs.Component, _ apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) (err error) {
	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	if err = i.SetCtxBundle(bdl); err != nil {
		return err
	}
	if err = i.GenComponentState(c); err != nil {
		return err
	}
	if err = i.GetInParams(); err != nil {
		logrus.Errorf("get filter request failed, content:%+v, err:%v", *gs, err)
		return err
	}

	i18nLocale := bdl.Bdl.GetLocale(bdl.Locale)
	// visible
	i.Props = make(map[string]interface{})
	i.Props["visible"] = i.State.Visible
	if i.State.Visible == false {
		c.Props = i.Props
		return nil
	}

	rsp, err := i.ctxBdl.Bdl.GetTestPlanV2(i.State.TestPlanId)
	if err != nil {
		return err
	}

	createrName, err := i.ctxBdl.Bdl.GetCurrentUser(rsp.Data.Creator)
	if err == nil {
		rsp.Data.Creator = createrName.Nick
	}

	if rsp.Data.Updater != "" {
		updaterName, err := i.ctxBdl.Bdl.GetCurrentUser(rsp.Data.Updater)
		if err == nil {
			rsp.Data.Updater = updaterName.Nick
		}
	}

	i.Data = Data{
		Name:           rsp.Data.Name,
		CreatorID:      rsp.Data.Creator,
		UpdaterID:      rsp.Data.Updater,
		CreateATString: rsp.Data.CreateAt.Format("2006-01-02 15:04:05"),
		UpdateATString: rsp.Data.UpdateAt.Format("2006-01-02 15:04:05"),
	}

	i.Props["fields"] = []PropColumn{
		{
			Label:    i18nLocale.Get(i18n.I18nKeyName),
			ValueKey: "name",
		},
		{
			Label:    i18nLocale.Get(i18n.I18nKeyCreator),
			ValueKey: "creatorID",
		},
		{
			Label:    i18nLocale.Get(i18n.I18nKeyCreationTime),
			ValueKey: "createAtString",
		},
		{
			Label:    i18nLocale.Get(i18n.I18nKeyUpdater),
			ValueKey: "updaterID",
		},
		{
			Label:    i18nLocale.Get(i18n.I18nKeyUpdateTime),
			ValueKey: "updateAtString",
		},
	}

	i.RenderProtocol(c, gs)
	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentFileInfo{}
}
