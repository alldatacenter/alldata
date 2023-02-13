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

package mt_block_detail_item

import (
	"context"
	"fmt"
	"math"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common/gshelper"
	"github.com/erda-project/erda/pkg/numeral"
	"github.com/erda-project/erda/pkg/strutil"
)

func init() {
	base.InitProviderWithCreator(common.ScenarioKeyTestDashboard, "mt_block_detail_item",
		func() servicehub.Provider { return &Text{} })
}

type Text struct {
}

type TextValue struct {
	Value      string // 88%       / 100
	Kind       string // pass rate / done num
	ValueColor string
}

type (
	Props struct {
		RenderType string     `json:"renderType"`
		Value      PropsValue `json:"value"`
	}
	PropsValue struct {
		Direction string         `json:"direction"`
		Text      PropsValueText `json:"text"`
	}
	PropsValueText     []PropsValueTextItem
	PropsValueTextItem struct {
		StyleConfig PropsValueTextStyleConfig `json:"styleConfig"`
		Text        string                    `json:"text"`
	}
	PropsValueTextStyleConfig struct {
		Bold       bool   `json:"bold,omitempty"`
		Color      string `json:"color"`
		FontSize   uint64 `json:"fontSize,omitempty"`
		LineHeight uint64 `json:"lineHeight,omitempty"`
	}
)

const (
	ColorTextMain   = "text-main"
	ColorTextDesc   = "text-desc"
	ColorTextRed    = "red"
	ColorTextGreen  = "green"
	ColorTextOrange = "orange"
	ColorTextGrey   = "grey"
)

func (t *Text) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	// get all mt plans
	h := gshelper.NewGSHelper(gs)
	mtPlans := h.GetMtBlockFilterTestPlanList()

	var tv TextValue
	switch c.Name {
	case "mt_case_num_total":
		tv = makeMtCaseNumTotal(ctx, mtPlans)
	case "mt_case_num_succ":
		tv = makeMtCaseNumSucc(ctx, mtPlans)
	case "mt_case_num_block":
		tv = makeMtCaseNumBlock(ctx, mtPlans)
	case "mt_case_num_fail":
		tv = makeMtCaseNumFail(ctx, mtPlans)
	case "mt_case_num_init":
		tv = makeMtCaseNumInit(ctx, mtPlans)
	case "mt_case_rate_passed":
		tv = makeMtCaseRatePassed(ctx, mtPlans)
	case "mt_case_rate_executed":
		tv = makeMtCaseRateExecuted(ctx, mtPlans)
	default:
		return fmt.Errorf("invalid text: %s", c.Name)
	}

	c.Props = tv.convertToProps()

	return nil
}

func (tv TextValue) convertToProps() cptype.ComponentProps {
	return cputil.MustConvertProps(Props{
		RenderType: "linkText",
		Value: PropsValue{
			Direction: "col",
			Text: []PropsValueTextItem{
				{
					StyleConfig: PropsValueTextStyleConfig{
						Bold:       true,
						Color:      tv.ValueColor,
						FontSize:   20,
						LineHeight: 32,
					},
					Text: tv.Value,
				},
				{
					StyleConfig: PropsValueTextStyleConfig{
						Bold:       false,
						Color:      ColorTextDesc,
						FontSize:   0,
						LineHeight: 22,
					},
					Text: tv.Kind,
				},
			},
		},
	})
}

func makeMtCaseNumTotal(ctx context.Context, mtPlans []apistructs.TestPlan) TextValue {
	var total uint64
	for _, plan := range mtPlans {
		total += plan.RelsCount.Total
	}
	return TextValue{
		Value:      strutil.String(total),
		Kind:       cputil.I18n(ctx, "test-case-num-total"),
		ValueColor: ColorTextMain,
	}
}

func makeMtCaseNumSucc(ctx context.Context, mtPlans []apistructs.TestPlan) TextValue {
	var succ uint64
	for _, plan := range mtPlans {
		succ += plan.RelsCount.Succ
	}
	return TextValue{
		Value:      strutil.String(succ),
		Kind:       cputil.I18n(ctx, "test-case-num-succ"),
		ValueColor: ColorTextGreen,
	}
}

func makeMtCaseNumBlock(ctx context.Context, mtPlans []apistructs.TestPlan) TextValue {
	var block uint64
	for _, plan := range mtPlans {
		block += plan.RelsCount.Block
	}
	return TextValue{
		Value:      strutil.String(block),
		Kind:       cputil.I18n(ctx, "test-case-num-block"),
		ValueColor: ColorTextOrange,
	}
}

func makeMtCaseNumFail(ctx context.Context, mtPlans []apistructs.TestPlan) TextValue {
	var fail uint64
	for _, plan := range mtPlans {
		fail = fail + plan.RelsCount.Fail
	}
	return TextValue{
		Value:      strutil.String(fail),
		Kind:       cputil.I18n(ctx, "test-case-num-fail"),
		ValueColor: ColorTextRed,
	}
}

func makeMtCaseNumInit(ctx context.Context, mtPlans []apistructs.TestPlan) TextValue {
	var init uint64
	for _, plan := range mtPlans {
		init = init + plan.RelsCount.Init
	}
	return TextValue{
		Value:      strutil.String(init),
		Kind:       cputil.I18n(ctx, "test-case-num-init"),
		ValueColor: ColorTextGrey,
	}
}

func makeMtCaseRatePassed(ctx context.Context, mtPlans []apistructs.TestPlan) TextValue {
	var total, passed uint64
	for _, plan := range mtPlans {
		total += plan.RelsCount.Total
		passed += plan.RelsCount.Succ
	}
	rate := float64(passed) / float64(total) * 100
	if math.IsNaN(rate) {
		rate = 0.00
	}
	rate = numeral.Round(rate, 2)
	return TextValue{
		Value:      strutil.String(rate) + "%",
		Kind:       cputil.I18n(ctx, "test-case-rate-passed"),
		ValueColor: ColorTextMain,
	}
}

func makeMtCaseRateExecuted(ctx context.Context, mtPlans []apistructs.TestPlan) TextValue {
	var total, executed uint64
	for _, plan := range mtPlans {
		total += plan.RelsCount.Total
		executed = executed + (plan.RelsCount.Total - plan.RelsCount.Init)
	}
	rate := float64(executed) / float64(total) * 100
	if math.IsNaN(rate) {
		rate = 0.00
	}
	rate = numeral.Round(rate, 2)
	return TextValue{
		Value:      strutil.String(rate) + "%",
		Kind:       cputil.I18n(ctx, "test-case-rate-executed"),
		ValueColor: ColorTextMain,
	}
}
