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

package scene_and_api_filter

import (
	"context"
	"encoding/json"
	"time"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common/gshelper"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

func init() {
	base.InitProviderWithCreator(common.ScenarioKeyTestDashboard, "scene_and_api_filter", func() servicehub.Provider {
		return &Filter{}
	})
}

func (f *Filter) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	h := gshelper.NewGSHelper(gs)

	if err := f.initFromProtocol(ctx, c); err != nil {
		return err
	}
	times := f.State.Values.Time
	if len(times) != 2 {
		times = []int64{time.Now().AddDate(0, 0, -7).Unix() * 1000, time.Now().Unix() * 1000}
		f.State.Values.Time = times
	}
	timeStart := time.Unix(times[0]/1000, 0).Format("2006-01-02 15:04:05")
	timeEnd := time.Unix(times[1]/1000, 0).Format("2006-01-02 15:04:05")

	h.SetAtSceneAndApiTimeFilter(gshelper.AtSceneAndApiTimeFilter{
		TimeStart: timeStart, TimeEnd: timeEnd,
	})

	if err := f.setState(ctx); err != nil {
		return err
	}

	return f.setToComponent(c)
}

func (f *Filter) setState(ctx context.Context) error {
	f.State.Conditions = []filter.PropCondition{
		{
			CustomProps: func() map[string]interface{} {
				now := time.Now()
				weekAgo := now.AddDate(0, 0, -7)
				monthAgo := now.AddDate(0, -1, 0)

				customProps := CustomProps{
					AllowClear: false,
					Ranges: common.Ranges{
						Week: common.RangeItem{
							Label: cputil.I18n(ctx, "week"),
							Range: []int64{weekAgo.Unix() * 1000, now.Unix() * 1000},
						},
						Month: common.RangeItem{
							Label: cputil.I18n(ctx, "month"),
							Range: []int64{monthAgo.Unix() * 1000, now.Unix() * 1000},
						},
					},
					SelectableTime: []int64{
						0, now.Unix() * 1000,
					},
					BorderTime: true,
				}

				b, _ := json.Marshal(&customProps)
				customPropsMap := make(map[string]interface{}, 0)
				_ = json.Unmarshal(b, &customPropsMap)
				return customPropsMap
			}(),
			Label:     cputil.I18n(ctx, "time"),
			Type:      filter.PropConditionTypeRangePicker,
			Fixed:     true,
			ShowIndex: 2,
			Key:       "time",
		},
	}
	return nil
}
