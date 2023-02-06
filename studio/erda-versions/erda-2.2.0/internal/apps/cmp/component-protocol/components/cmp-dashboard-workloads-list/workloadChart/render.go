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

package workloadChart

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
)

func init() {
	base.InitProviderWithCreator("cmp-dashboard-workloads-list", "workloadChart", func() servicehub.Provider {
		return &ComponentWorkloadChart{}
	})
}

func (w *ComponentWorkloadChart) Render(ctx context.Context, component *cptype.Component, _ cptype.Scenario,
	_ cptype.ComponentEvent, _ *cptype.GlobalStateData) error {
	if err := w.GenComponentState(component); err != nil {
		return fmt.Errorf("failed to gen workloadChart component state, %v", err)
	}
	if err := w.SetComponentValue(ctx); err != nil {
		return fmt.Errorf("faield to set workloadChart component value, %v", err)
	}
	w.Transfer(component)
	return nil
}

func (w *ComponentWorkloadChart) GenComponentState(c *cptype.Component) error {
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
	w.State = state
	return nil
}

func (w *ComponentWorkloadChart) SetComponentValue(ctx context.Context) error {
	w.Data.Option.Tooltip.Show = false
	w.Data.Option.Color = []string{
		"primary7", "warning7", "primary6", "primary5", "primary4", "warning6",
	}
	w.Data.Option.Legend.Data = []string{
		cputil.I18n(ctx, "Active"), cputil.I18n(ctx, "Abnormal"),
		cputil.I18n(ctx, "Updating"), cputil.I18n(ctx, "Stopped"),
		cputil.I18n(ctx, "Succeeded"), cputil.I18n(ctx, "Failed"),
	}
	w.Data.Option.YAxis.Type = "value"
	w.Data.Option.XAxis.Type = "category"
	w.Data.Option.XAxis.Data = []string{
		"Deployments", "StatefulSets", "DaemonSets", "Jobs", "CronJobs",
	}

	// deployment
	activeDeploy := w.State.Values.DeploymentsCount.Active
	abnormalDeploy := w.State.Values.DeploymentsCount.Abnormal
	updatingDeploy := w.State.Values.DeploymentsCount.Updating
	stoppedDeploy := w.State.Values.DeploymentsCount.Stopped

	// daemonSet
	activeDs := w.State.Values.DaemonSetCount.Active
	abnormalDs := w.State.Values.DaemonSetCount.Abnormal
	updatingDs := w.State.Values.DaemonSetCount.Updating
	stoppedDs := w.State.Values.DaemonSetCount.Stopped

	// statefulSet
	activeSs := w.State.Values.StatefulSetCount.Active
	abnormalSs := w.State.Values.StatefulSetCount.Abnormal
	updatingSs := w.State.Values.StatefulSetCount.Updating
	stoppedSs := w.State.Values.StatefulSetCount.Stopped

	// job
	activeJob := w.State.Values.JobCount.Active
	succeededJob := w.State.Values.JobCount.Succeeded
	failedJob := w.State.Values.JobCount.Failed

	// cronjob
	activeCronJob := w.State.Values.CronJobCount.Active

	activeSeries := Series{
		Name:     cputil.I18n(ctx, "Active"),
		Type:     "bar",
		BarWidth: 10,
		BarGap:   "40%",
		Data: []*int{
			&activeDeploy, &activeSs, &activeDs, &activeJob, &activeCronJob,
		},
	}

	abnormalSeries := Series{
		Name:     cputil.I18n(ctx, "Abnormal"),
		Type:     "bar",
		BarWidth: 10,
		BarGap:   "40%",
		Data: []*int{
			&abnormalDeploy, &abnormalSs, &abnormalDs, nil, nil,
		},
	}

	succeededSeries := Series{
		Name:     cputil.I18n(ctx, "Succeeded"),
		Type:     "bar",
		BarWidth: 10,
		BarGap:   "40%",
		Data: []*int{
			nil, nil, nil, &succeededJob, nil,
		},
	}

	failedSeries := Series{
		Name:     cputil.I18n(ctx, "Failed"),
		Type:     "bar",
		BarWidth: 10,
		BarGap:   "40%",
		Data: []*int{
			nil, nil, nil, &failedJob, nil,
		},
	}

	updatingSeries := Series{
		Name:     cputil.I18n(ctx, "Updating"),
		Type:     "bar",
		BarWidth: 10,
		BarGap:   "40%",
		Data: []*int{
			&updatingDeploy, &updatingSs, &updatingDs, nil, nil,
		},
	}

	stoppedSeries := Series{
		Name:     cputil.I18n(ctx, "Stopped"),
		Type:     "bar",
		BarWidth: 10,
		BarGap:   "40%",
		Data: []*int{
			&stoppedDeploy, &stoppedSs, &stoppedDs, nil, nil,
		},
	}
	w.Data.Option.Series = []Series{
		activeSeries, abnormalSeries, updatingSeries, stoppedSeries, succeededSeries, failedSeries,
	}
	return nil
}

func (w *ComponentWorkloadChart) Transfer(component *cptype.Component) {
	component.State = map[string]interface{}{
		"values": w.State.Values,
	}
	component.Data = map[string]interface{}{
		"option": w.Data.Option,
	}
}
