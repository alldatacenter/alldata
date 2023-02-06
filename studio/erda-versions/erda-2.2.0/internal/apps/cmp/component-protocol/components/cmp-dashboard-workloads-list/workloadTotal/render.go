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

package workloadTotal

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
)

func init() {
	base.InitProviderWithCreator("cmp-dashboard-workloads-list", "workloadTotal", func() servicehub.Provider {
		return &ComponentWorkloadTotal{}
	})
}

func (w *ComponentWorkloadTotal) Render(ctx context.Context, component *cptype.Component, _ cptype.Scenario,
	_ cptype.ComponentEvent, _ *cptype.GlobalStateData) error {
	if err := w.GenComponentState(component); err != nil {
		return fmt.Errorf("failed to gen workloadTotal component state, %v", err)
	}

	count := addCount(w.State.Values.DeploymentsCount) + addCount(w.State.Values.DaemonSetCount) +
		addCount(w.State.Values.StatefulSetCount) + addCount(w.State.Values.JobCount) + addCount(w.State.Values.CronJobCount)

	w.Data.Data.Desc = cputil.I18n(ctx, "totalWorkload")
	w.Data.Data.Main = fmt.Sprintf("%d", count)
	w.Transfer(component)
	return nil
}

func (w *ComponentWorkloadTotal) GenComponentState(component *cptype.Component) error {
	if component == nil || component.State == nil {
		return nil
	}
	var state State
	jsonData, err := json.Marshal(component.State)
	if err != nil {
		return err
	}
	if err = json.Unmarshal(jsonData, &state); err != nil {
		return err
	}
	w.State = state
	return nil
}

func (w *ComponentWorkloadTotal) Transfer(c *cptype.Component) {
	c.Data = map[string]interface{}{
		"data": w.Data.Data,
	}
	c.State = map[string]interface{}{
		"values": w.State.Values,
	}
}

func addCount(count Count) int {
	return count.Active + count.Abnormal + count.Succeeded + count.Failed + count.Updating + count.Stopped
}
