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

package envGlobalTable

import (
	"context"
	"encoding/json"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/auto-test-plan-detail/i18n"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/auto-test-plan-detail/types"
)

type ComponentFileInfo struct {
	CtxBdl protocol.ContextBundle

	CommonFileInfo
}

type CommonFileInfo struct {
	Version    string                                           `json:"version,omitempty"`
	Name       string                                           `json:"name,omitempty"`
	Type       string                                           `json:"type,omitempty"`
	Props      map[string]interface{}                           `json:"props,omitempty"`
	State      State                                            `json:"state,omitempty"`
	Operations map[apistructs.OperationKey]apistructs.Operation `json:"operations,omitempty"`
	Data       map[string]interface{}                           `json:"data,omitempty"`
}

type DataList struct {
	Name    string `json:"name"`
	Content string `json:"content"`
	Type    string `json:"type"`
	Desc    string `json:"desc"`
}

type PropColumn struct {
	Title     string `json:"title"`
	DataIndex string `json:"dataIndex"`
	Width     int    `json:"width"`
	Fixed     string `json:"fixed"`
}

type State struct{}

func (a *ComponentFileInfo) unmarshal(c *apistructs.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, a); err != nil {
		return err
	}
	return nil
}

func (i *ComponentFileInfo) Render(ctx context.Context, c *apistructs.Component, _ apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) (err error) {
	if err := i.unmarshal(c); err != nil {
		logrus.Errorf("unmarshal component failed, err:%v", err)
		return err
	}
	defer func() {
		fail := i.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()
	i.CtxBdl = ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	i18nLocale := i.CtxBdl.Bdl.GetLocale(i.CtxBdl.Locale)

	envData := (*gs)[types.AutotestGlobalKeyEnvData].(apistructs.AutoTestAPIConfig)

	i.Props = make(map[string]interface{})
	i.Props["columns"] = []PropColumn{
		{
			Title:     i18nLocale.Get(i18n.I18nKeyName),
			DataIndex: "name",
			Width:     100,
		},
		{
			Title:     i18nLocale.Get(i18n.I18nKeyParameterType),
			DataIndex: "type",
			Width:     100,
		},
		{
			Title:     i18nLocale.Get(i18n.I18nKeyParameterContent),
			DataIndex: "content",
			Width:     100,
		},
		{
			Title:     i18nLocale.Get(i18n.I18nKeyDescription),
			DataIndex: "desc",
			Width:     100,
		},
	}
	i.Data = make(map[string]interface{})
	var list []DataList
	for _, v := range envData.Global {
		list = append(list, DataList{
			Name:    v.Name,
			Content: v.Value,
			Type:    v.Type,
			Desc:    v.Desc,
		})
	}
	i.Data["list"] = list
	return
}

func (a *ComponentFileInfo) marshal(c *apistructs.Component) error {
	var state map[string]interface{}
	stateValue, err := json.Marshal(a.State)
	if err != nil {
		return err
	}
	err = json.Unmarshal(stateValue, &state)
	if err != nil {
		return err
	}

	if c.Data == nil {
		d := make(apistructs.ComponentData)
		c.Data = d
	}
	(*c).Data = a.Data

	c.Props = a.Props
	c.State = state
	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentFileInfo{}
}
