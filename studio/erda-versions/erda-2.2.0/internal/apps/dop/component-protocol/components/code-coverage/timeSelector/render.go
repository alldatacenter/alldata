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

package timeSelector

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/code-coverage/common"
)

type ComponentAction struct {
	Type       string                 `json:"type,omitempty"`
	Props      map[string]interface{} `json:"props,omitempty"`
	State      State                  `json:"state,omitempty"`
	Operations map[string]interface{} `json:"operations,omitempty"`
	Data       map[string]interface{}
}

type State struct {
	Value []int64 `json:"value"`
}

func (i *ComponentAction) GenComponentState(c *cptype.Component) error {
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
	fmt.Println(state)
	i.State = state
	return nil
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	ca.Type = "DatePicker"
	now := time.Now()
	now = time.Date(now.Year(), now.Month(), now.Day(), 23, 59, 59, 0, time.Local)
	oneWeekAgo := now.AddDate(0, 0, -7)
	oneMonthAgo := now.AddDate(0, 0, -30)
	oneWeekRange := []int64{oneWeekAgo.Unix() * 1000, now.Unix() * 1000}
	oneMonthRange := []int64{oneMonthAgo.Unix() * 1000, now.Unix() * 1000}
	ca.Props = map[string]interface{}{
		"allowClear": false,
		"type":       "dateRange",
		"size":       "small",
		"borderTime": true,
		"ranges": map[string]interface{}{
			cputil.I18n(ctx, "within-a-week"):  oneWeekRange,
			cputil.I18n(ctx, "within-a-month"): oneMonthRange,
		},
	}
	ca.Operations = map[string]interface{}{
		"onChange": map[string]interface{}{
			"key":    "changeTime",
			"reload": true,
		},
	}
	switch event.Operation {
	case cptype.DefaultRenderingKey, cptype.InitializeOperation:
		ca.State.Value = oneMonthRange
	case common.DatePickerChangeTimeOperationKey:
		if err := ca.GenComponentState(c); err != nil {
			return err
		}
		if len(ca.State.Value) != 2 {
			return fmt.Errorf("invalid time range value: %v", ca.State.Value)
		}
	}
	return nil
}

func init() {
	base.InitProviderWithCreator("code-coverage", "timeSelector", func() servicehub.Provider {
		return &ComponentAction{}
	})
}
