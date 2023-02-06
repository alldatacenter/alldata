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

package chart

import (
	"context"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/types"
)

var (
	Allocated       = "Allocated"
	Free_Allocate   = "Free-Allocate"
	Cannot_Allocate = "Cannot-Allocate"

	Memory = "memory"
	CPU    = "cpu"
	Pods   = "pods"

	//DefaultFormat = "{d}%\n"
)

type Chart struct {
	SDK    *cptype.SDK
	Ctx    context.Context
	CtxBdl *bundle.Bundle
	Data   ChartData `json:"data"`
	Type   string    `json:"type"`
	//Props  Props     `json:"props"`
}
type ChartInterface interface {
	ChartRender(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error
}

func (cht *Chart) ChartRender(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData, ResourceType string) error {
	cht.CtxBdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	cht.SDK = cputil.SDK(ctx)
	cht.Data = ChartData{Data: (*gs)[ResourceType+"Chart"].([]DataItem)}
	switch ResourceType {
	case CPU:
		cht.Data.Label = cht.SDK.I18n("Cpu")
	case Memory:
		cht.Data.Label = cht.SDK.I18n("Mem")
	case Pods:
		cht.Data.Label = cht.SDK.I18n("Pods")
	}
	return common.Transfer(cht.Data, &c.Data)
}

type ChartData struct {
	Label string     `json:"label"`
	Data  []DataItem `json:"data"`
}

type Props struct {
	Name   string `json:"name"`
	Option Option `json:"option"`
	Title  string `json:"title"`
	Style  Style  `json:"style"`
}

type Option struct {
	Color  []string `json:"color"`
	Legend Legend   `json:"legend"`
	Grid   Grid     `json:"grid"`
	Series []Serie  `json:"series"`
}

type Title struct {
	Text      string    `json:"text"`
	TextStyle TextStyle `json:"textStyle"`
}

type TextStyle struct {
	FrontSize int `json:"frontSize"`
}

type Serie struct {
	Type   string `json:"type"`
	Radius string `json:"radius"`
}

type Legend struct {
	Data   []string `json:"data"`
	Bottom string   `json:"bottom"`
}

type Grid struct {
	Bottom       int  `json:"bottom"`
	Top          int  `json:"top"`
	ContainLabel bool `json:"containLabel"`
}

type Style struct {
	Flex int `json:"flex"`
}

type DataItem struct {
	Value     float64 `json:"value"`
	Name      string  `json:"name"`
	Formatter string  `json:"formatter"`
	Color     string  `json:"color"`
}

type Label struct {
	Formatter string `json:"formatter"`
}

//
//func (cht *Chart) GetProps(name string) Props {
//	return Props{
//		Name: name,
//	}
//}
