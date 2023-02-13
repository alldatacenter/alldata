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

package quality_chart

import (
	"context"
	"fmt"
	"strconv"

	"github.com/go-echarts/go-echarts/v2/charts"
	"github.com/go-echarts/go-echarts/v2/opts"
	"github.com/shopspring/decimal"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/code_coverage"
	"github.com/erda-project/erda/pkg/numeral"
	"github.com/erda-project/erda/pkg/strutil"
)

func init() {
	base.InitProviderWithCreator(common.ScenarioKeyTestDashboard, "quality_chart",
		func() servicehub.Provider { return &Q{} })
}

type Q struct {

	// in params
	projectID uint64

	// depends
	dbClient *dao.DBClient
	coco     *code_coverage.CodeCoverage
}

type (
	Props struct {
		RadarOption map[string]interface{} `json:"option"`
		Style       Style                  `json:"style"`
		Title       string                 `json:"title"`
		Tip         []TipLine              `json:"tip"`
	}
	Style struct {
		Height uint64 `json:"height"`
	}
	TipLine struct {
		Text  string       `json:"text"`
		Style TipLineStyle `json:"style"`
	}
	TipLineStyle struct {
		FontWeight  FontWeight `json:"fontWeight,omitempty"`  // bold
		PaddingLeft uint64     `json:"paddingLeft,omitempty"` // such as: 16
	}
	FontWeight string
)

const (
	fontWeightBold   FontWeight = "bold"
	fontWeightNormal FontWeight = ""
)

func (q *Q) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) (e error) {
	defer func() {
		if r := recover(); r != nil {
			e = fmt.Errorf("%v", r)
		}
	}()

	h := gshelper.NewGSHelper(gs)
	q.projectID, _ = strconv.ParseUint(cputil.GetInParamByKey(ctx, "projectId").(string), 10, 64)
	q.dbClient = ctx.Value(types.DBClient).(*dao.DBClient)
	q.coco = ctx.Value(types.CodeCoverageService).(*code_coverage.CodeCoverage)

	// calc score
	mtScore := q.calcMtPlanScore(ctx, h)
	atScore := q.calcAtPlanScore(ctx, h)
	bugScore := q.calcUnclosedBugScore(ctx, h)
	cocoScore := q.calcCodeCoverage(ctx, h)
	bugReopenScore := q.calcBugReopenRate(ctx, h)

	// global score
	globalScore := polishToFloat64Score(q.calcGlobalQualityScore(ctx, mtScore, atScore, bugScore, cocoScore, bugReopenScore))
	h.SetGlobalQualityScore(globalScore)

	// radar options
	radar := charts.NewRadar()
	radar.Indicator = []*opts.Indicator{
		{Name: cputil.I18n(ctx, "radar-manual-test-plan"), Max: 100, Min: 0, Color: ""},
		{Name: cputil.I18n(ctx, "radar-auto-test-plan"), Max: 100, Min: 0, Color: ""},
		{Name: cputil.I18n(ctx, "radar-unclosed-bug"), Max: 100, Min: 0, Color: ""},
		{Name: cputil.I18n(ctx, "radar-code-coverage"), Max: 100, Min: 0, Color: ""},
		{Name: cputil.I18n(ctx, "radar-bug-reopen-rate"), Max: 100, Min: 0, Color: ""},
	}
	radar.AddSeries(
		cputil.I18n(ctx, "radar-quality"),
		[]opts.RadarData{
			{
				Name: "",
				Value: []float64{
					polishToFloat64Score(mtScore),
					polishToFloat64Score(atScore),
					polishToFloat64Score(bugScore),
					polishToFloat64Score(cocoScore),
					polishToFloat64Score(bugReopenScore),
				},
			},
		},
		charts.WithAreaStyleOpts(opts.AreaStyle{Color: "", Opacity: 0.2}),
		charts.WithLabelOpts(opts.Label{Show: true, Color: "", Position: "", Formatter: ""}),
	)
	radar.SetGlobalOptions(
		charts.WithTooltipOpts(opts.Tooltip{Show: true, Trigger: "item"}),
		charts.WithTitleOpts(opts.Title{Title: strutil.String(globalScore)}),
	)
	radar.JSON()

	// set props
	c.Props = cputil.MustConvertProps(Props{
		RadarOption: radar.JSON(),
		Style:       Style{Height: 265},
		Title:       cputil.I18n(ctx, "radar-total-quality-score"),
		Tip:         genTip(ctx),
	})

	return nil
}

// score = RATE(passed) * RATE(executed) * 100
// value range: 0-100
func (q *Q) calcMtPlanScore(ctx context.Context, h *gshelper.GSHelper) decimal.Decimal {
	mtPlans := h.GetGlobalManualTestPlanList()

	var numCasePassed, numCaseExecuted, numCaseTotal uint64
	for _, plan := range mtPlans {
		numCasePassed += plan.RelsCount.Succ
		numCaseExecuted += plan.RelsCount.Succ + plan.RelsCount.Block + plan.RelsCount.Fail
		numCaseTotal += plan.RelsCount.Total
	}

	if numCaseTotal == 0 {
		return decimal.NewFromInt(0)
	}

	numCasePassedDecimal := decimal.NewFromInt(int64(numCasePassed))
	numCaseExecutedDecimal := decimal.NewFromInt(int64(numCaseExecuted))
	numCaseTotalDecimal := decimal.NewFromInt(int64(numCaseTotal))

	ratePassed := numCasePassedDecimal.Div(numCaseTotalDecimal)
	rateExecuted := numCaseExecutedDecimal.Div(numCaseTotalDecimal)

	score := ratePassed.Mul(rateExecuted).Mul(decimal.NewFromInt(100))
	return score
}

// score = (SUM(api_passed)/SUM(api_total)) * (SUM(api_executed)/SUM(api_total)) * 100
// value range: 0-100
func (q *Q) calcAtPlanScore(ctx context.Context, h *gshelper.GSHelper) decimal.Decimal {
	atPlans := h.GetGlobalAutoTestPlanList()

	var numAPIPassed, numAPIExecuted, numAPITotal uint64
	for _, plan := range atPlans {
		numAPIPassed += uint64(plan.SuccessApiNum)
		numAPIExecuted += uint64(plan.ExecuteApiNum)
		numAPITotal += uint64(plan.TotalApiNum)
	}

	if numAPITotal == 0 {
		return decimal.NewFromInt(0)
	}

	numAPIPassedDecimal := decimal.NewFromInt(int64(numAPIPassed))
	numAPIExecutedDecimal := decimal.NewFromInt(int64(numAPIExecuted))
	numAPITotalDecimal := decimal.NewFromInt(int64(numAPITotal))
	apiPassedRate := numAPIPassedDecimal.Div(numAPITotalDecimal)
	apiExecutedRate := numAPIExecutedDecimal.Div(numAPITotalDecimal)
	score := apiPassedRate.Mul(apiExecutedRate).Mul(decimal.NewFromInt(100))
	return score
}

// unclosed bug score: score = 100 - DI (result must >= 0)
// DI = NUM(FATAL)*10 + NUM(SERIOUS)*3 + NUM(NORMAL)*1 + NUM(SLIGHT)*0.1
// value range: 0-100
func (q *Q) calcUnclosedBugScore(ctx context.Context, h *gshelper.GSHelper) decimal.Decimal {
	m, err := q.dbClient.CountBugBySeverity(q.projectID, h.GetGlobalSelectedIterationIDs(), true)
	if err != nil {
		panic(err)
	}

	var numFatal, numSerious, numNormal, numSlight uint64
	for severity, count := range m {
		switch severity {
		case apistructs.IssueSeverityFatal:
			numFatal += count
		case apistructs.IssueSeveritySerious:
			numSerious += count
		case apistructs.IssueSeverityNormal:
			numNormal += count
		case apistructs.IssueSeveritySlight:
			numSlight += count
		}
	}

	numFatalDecimal := decimal.NewFromInt(int64(numFatal) * 10)
	numSeriousDecimal := decimal.NewFromInt(int64(numSerious) * 3)
	numNormalDecimal := decimal.NewFromInt(int64(numNormal) * 1)
	numSlightDecimal := decimal.NewFromFloat(float64(numSlight) * 0.1)

	DI := numFatalDecimal.Add(numSeriousDecimal).Add(numNormalDecimal).Add(numSlightDecimal)

	score := decimal.NewFromInt(100).Sub(DI)
	return score
}

// score = code_coverage_rate * 100
// value range: 0-100
func (q *Q) calcCodeCoverage(ctx context.Context, h *gshelper.GSHelper) decimal.Decimal {
	// get the latest success record
	data, err := q.coco.ListCodeCoverageRecord(apistructs.CodeCoverageListRequest{
		ProjectID:      q.projectID,
		PageNo:         1,
		PageSize:       1,
		Asc:            false,
		ReportStatuses: []apistructs.CodeCoverageExecStatus{apistructs.SuccessStatus},
	})
	if err != nil {
		panic(err)
	}
	if len(data.List) == 0 {
		return decimal.NewFromInt(0)
	}

	score := decimal.NewFromFloat(data.List[0].Coverage)
	return score
}

// score = 100 - reopen_rate*100
// reopen_rate = reopen_count / total_count
// value range: 0-100
func (q *Q) calcBugReopenRate(ctx context.Context, h *gshelper.GSHelper) decimal.Decimal {
	reopenCount, totalCount, err := q.dbClient.BugReopenCount(q.projectID, h.GetGlobalSelectedIterationIDs())
	if err != nil {
		panic(err)
	}

	// reopen_rate
	var reopenRate decimal.Decimal
	if totalCount == 0 {
		reopenRate = decimal.NewFromInt(0)
	} else {
		reopenRate = decimal.NewFromInt(int64(reopenCount)).Div(decimal.NewFromInt(int64(totalCount)))
	}

	// score = 100 - reopen_rate*100
	score := decimal.NewFromInt(100).Sub(reopenRate.Mul(decimal.NewFromInt(100)))
	return score
}

// polishToFloat64Score set precision to 2, range from 0-100
func polishToFloat64Score(scoreDecimal decimal.Decimal) float64 {
	scoreDecimal = polishScore(scoreDecimal)
	score, _ := scoreDecimal.Float64()
	return numeral.Round(score, 2)
}

// calcGlobalQualityScore calc global average score according to
func (q *Q) calcGlobalQualityScore(ctx context.Context, scores ...decimal.Decimal) decimal.Decimal {
	if len(scores) == 0 {
		return decimal.NewFromInt(0)
	}
	total := decimal.NewFromInt(0)
	for _, score := range scores {
		total = total.Add(polishScore(score))
	}
	var avg decimal.Decimal
	avg = total.Div(decimal.NewFromInt(int64(len(scores))))
	return avg
}

// polishScore range from 0-100
func polishScore(scoreDecimal decimal.Decimal) decimal.Decimal {
	if scoreDecimal.LessThan(decimal.NewFromInt(0)) {
		scoreDecimal = decimal.NewFromInt(0)
	}
	if scoreDecimal.GreaterThan(decimal.NewFromInt(100)) {
		scoreDecimal = decimal.NewFromInt(100)
	}
	return scoreDecimal
}
