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

package common

import (
	"context"
	"encoding/json"

	"github.com/go-echarts/go-echarts/v2/charts"
	"github.com/go-echarts/go-echarts/v2/opts"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/stackhandlers"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

func FixEmptyWord(em string) string {
	if em == "" {
		return "无"
	}
	return em
}

func DeepCopy(src, dst interface{}) error {
	buf, err := json.Marshal(src)
	if err != nil {
		return err
	}
	return json.Unmarshal(buf, dst)
}

func GetPieSeriesOpt() func(*charts.SingleSeries) {
	return func(s *charts.SingleSeries) {
		s.Animation = true
		s.Top = "12"
		s.Radius = [2]string{"0", "60%"}
	}
}

func GroupToPieData(ctx context.Context, issueList []dao.IssueItem, stackHandler stackhandlers.StackHandler) ([]opts.PieData, []string) {
	counter := make(map[string]int)
	indexer := stackHandler.GetIndexer()

	for _, i := range issueList {
		if i.Type != pb.IssueTypeEnum_BUG.String() {
			continue
		}
		counter[FixEmptyWord(indexer(&i))]++
	}

	var data []opts.PieData
	var colors []string
	for _, stack := range stackHandler.GetStacks(ctx) {
		cnt := counter[stack.Value]
		if cnt <= 0 {
			continue
		}
		data = append(data, opts.PieData{
			Name:  stack.Name,
			Value: cnt,
			Label: &opts.Label{
				Formatter: PieChartFormat,
				Show:      true,
			},
		})
		colors = append(colors, stack.Color)
	}
	return data, colors
}

func IssueListRetriever(issues []dao.IssueItem, match func(i int) bool) []dao.IssueItem {
	res := make([]dao.IssueItem, 0)
	for i, issue := range issues {
		if match(i) {
			res = append(res, issue)
		}
	}
	return res
}

type counterItem struct {
	Name  string
	Value int
}

type counterList []counterItem

func (l counterList) Less(i, j int) bool { return l[i].Value > l[j].Value }
func (l counterList) Swap(i, j int)      { l[i], l[j] = l[j], l[i] }
func (l counterList) Len() int           { return len(l) }
