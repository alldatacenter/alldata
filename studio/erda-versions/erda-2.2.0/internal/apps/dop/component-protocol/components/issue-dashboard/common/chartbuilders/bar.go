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

package chartbuilders

import (
	"context"
	"sort"

	"github.com/go-echarts/go-echarts/v2/charts"
	"github.com/go-echarts/go-echarts/v2/opts"

	"github.com/erda-project/erda-infra/pkg/strutil"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/stackhandlers"
)

type BarBuilder struct {
	Items        []interface{}
	StackHandler stackhandlers.StackHandler
	FixedXAxisOrTop
	YAxisOpt
	StackOpt
	DataHandleOpt
	Result
}

type FixedXAxisOrTop struct {
	XAxis    []string
	Top      int
	XIndexer func(interface{}) string

	MaxValue          int
	XDisplayConverter func(opt *FixedXAxisOrTop) opts.XAxis
}

type YAxisOpt struct {
	YAxis             []string
	YDisplayConverter func(opt *YAxisOpt) opts.YAxis
}

type StackOpt struct {
	EnableSum bool
	SkipEmpty bool
}

type DataHandleOpt struct {
	SeriesConverter func(name string, data []*int) charts.SingleSeries
	DataWhiteList   []string
	FillZero        bool
	StatsProperty   func(interface{}) int
}

type Result struct {
	Bar           *charts.Bar
	Bb            interface{}
	Size          int
	PostProcessor func(*Result) error
}

func (f *BarBuilder) Generate(ctx context.Context) error {
	series, colors, realY, sum := f.groupToBarData(ctx)

	bar := charts.NewBar()
	bar.Colors = colors
	bar.MultiSeries = series

	if f.XDisplayConverter != nil {
		maxValue := 0
		for _, t := range sum {
			if t > maxValue {
				maxValue = t
			}
		}
		//maxValue = int(float32(maxValue) * 1.2)
		f.FixedXAxisOrTop.MaxValue = maxValue
		bar.XAxisList[0] = f.XDisplayConverter(&f.FixedXAxisOrTop)
	}

	if f.YDisplayConverter != nil {
		f.YAxisOpt.YAxis = realY
		bar.YAxisList[0] = f.YDisplayConverter(&f.YAxisOpt)
	}

	f.Result.Bar = bar
	f.Result.Size = len(sum)
	return f.PostProcessor(&f.Result)
}

func (f *BarBuilder) groupToBarData(ctx context.Context) (charts.MultiSeries, []string, []string, []int) {
	counter := make(map[string]map[string]int)
	counterSingle := make(map[string]int)

	stackIndexer := f.StackHandler.GetIndexer()

	for _, i := range f.Items {
		y := FixEmptyWord(stackIndexer(i))
		if !showStack(f.DataWhiteList, y) {
			continue
		}
		x := FixEmptyWord(f.XIndexer(i))
		if _, ok := counter[y]; !ok {
			counter[y] = make(map[string]int)
		}
		v := 1
		if f.DataHandleOpt.StatsProperty != nil {
			v = f.DataHandleOpt.StatsProperty(i)
		}
		counter[y][x] += v
		counterSingle[x] += v
	}

	var ms charts.MultiSeries
	xl := len(f.XAxis)
	if xl == 0 {
		// use top instead
		var sl counterList
		for k, v := range counterSingle {
			if v == 0 {
				continue
			}
			sl = append(sl, counterItem{Name: k, Value: v})
		}
		sort.Sort(sl)
		last := f.Top
		xAxis := make([]string, 0)
		xl = 0 // reset
		for _, item := range sl {
			if last <= 0 || item.Value <= 0 {
				break
			}
			xAxis = append(xAxis, item.Name)
			xl++
			last--
		}
		// reverse for top
		for i, j := 0, len(xAxis)-1; i < j; i, j = i+1, j-1 {
			xAxis[i], xAxis[j] = xAxis[j], xAxis[i]
		}
		f.XAxis = xAxis
	}
	var colors []string
	total := make([]int, len(f.XAxis))

	for _, stack := range f.StackHandler.GetStacks(ctx) {
		if !showStack(f.DataWhiteList, stack.Value) {
			continue
		}
		rowData := make([]*int, xl)
		for i, x := range f.XAxis {
			v := counter[FixEmptyWord(stack.Value)][x]
			if v > 0 || !f.SkipEmpty {
				rowData[i] = &v
			}
			total[i] += v
		}
		ms = append(ms, f.SeriesConverter(stack.Name, rowData))
		colors = append(colors, stack.Color)
	}

	if f.EnableSum && len(colors) > 1 {
		totalRes := make([]*int, len(total))
		for i := range total {
			if !f.SkipEmpty {
				totalRes[i] = &total[i]
			}
		}

		msNew := make(charts.MultiSeries, len(ms)+1)
		colorsNew := make([]string, len(colors)+1)

		msNew[0] = f.SeriesConverter(cputil.I18n(ctx, "all"), totalRes)
		colorsNew[0] = "gray"
		for i := range ms {
			msNew[i+1] = ms[i]
		}
		ms = msNew
		for i := range colors {
			colorsNew[i+1] = colors[i]
		}
		colors = colorsNew
	}

	return ms, colors, f.XAxis, total
}

func showStack(wl []string, item string) bool {
	if len(wl) == 0 { // empty means not filter
		return true
	}
	return strutil.Exist(wl, item)
}

func GetStackBarSingleSeriesConverter() func(name string, data []*int) charts.SingleSeries {
	return func(name string, data []*int) charts.SingleSeries {
		return charts.SingleSeries{
			Name:  name,
			Stack: "total",
			Data:  data,
			Label: &opts.Label{
				//Formatter: "{a}:{c}",
				Show: true,
			},
			BarGap: "30%",
		}
	}
}

func GetVerticalBarPostProcessor() func(result *Result) error {
	return func(result *Result) error {
		bar := result.Bar

		bar.Legend.Show = true
		bar.Legend.SelectedMode = "false" // string "false" cannot work
		bar.Legend.TextStyle = &opts.TextStyle{FontSize: 10}
		bar.Legend.ItemHeight = 10
		bar.Legend.ItemWidth = 10
		bar.Tooltip.Show = true

		bb := bar.JSON()

		nl := make(map[string]interface{})
		if err := common.DeepCopy(bb["legend"], &nl); err != nil {
			return err
		}
		nl["selectedMode"] = false
		bb["legend"] = nl

		n := make([]map[string]interface{}, 0)
		if err := common.DeepCopy(bb["series"], &n); err != nil {
			return err
		}
		for i := range n {
			n[i]["barWidth"] = 10
		}
		bb["series"] = n
		result.Bb = bb
		return nil
	}
}

func GetHorizontalPostProcessor() func(result *Result) error {
	return func(result *Result) error {
		bar := result.Bar
		bar.Legend.Show = true
		bar.Legend.SelectedMode = "false" // string "false" cannot work
		bar.Legend.TextStyle = &opts.TextStyle{FontSize: 10}
		bar.Legend.ItemHeight = 10
		bar.Legend.ItemWidth = 10
		bar.Tooltip.Show = true
		bar.Tooltip.Trigger = "axis"

		bb := bar.JSON()

		nl := make(map[string]interface{})
		if err := common.DeepCopy(bb["legend"], &nl); err != nil {
			return err
		}
		nl["selectedMode"] = false
		bb["legend"] = nl

		bb["animation"] = false

		// TODO: check if top > 0 (means top chart)
		pageSize := 16

		if result.Size < 16 {
			// start padding
			for i := range bar.MultiSeries {
				s := &bar.MultiSeries[i]
				data := s.Data.([]*int)
				nData := make([]*int, 16-result.Size)
				for _, d := range data {
					nData = append(nData, d)
				}
				s.Data = nData
			}
			bb["series"] = bar.MultiSeries

			data := bar.YAxisList[0].Data.([]string)
			nData := make([]string, 16-result.Size)
			for _, d := range data {
				nData = append(nData, d)
			}
			bar.YAxisList[0].Data = nData
		}

		if result.Size > pageSize {
			endIdx := result.Size - 1
			startIdx := endIdx - 16
			bb["grid"] = map[string]interface{}{"right": 30}
			bb["dataZoom"] = []map[string]interface{}{
				{
					"type":       "inside",
					"orient":     "vertical",
					"zoomLock":   true,
					"startValue": startIdx,
					"endValue":   endIdx,
					"throttle":   0,
				},
				{
					"type":           "slider",
					"orient":         "vertical",
					"handleSize":     15,
					"width":          15,
					"zoomLock":       true,
					"startValue":     startIdx,
					"endValue":       endIdx,
					"throttle":       0,
					"showDataShadow": false,
					"showDetail":     false,
				},
			}
		}

		n := make([]map[string]interface{}, 0)
		if err := common.DeepCopy(bb["series"], &n); err != nil {
			return err
		}
		for i := range n {
			n[i]["barWidth"] = 8
		}
		bb["series"] = n

		result.Bb = bb
		return nil
	}
}
