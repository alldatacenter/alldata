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

type ComponentWorkloadChart struct {
	Type  string `json:"type,omitempty"`
	State State  `json:"state"`
	Data  Data   `json:"data"`
}

type State struct {
	Values Values `json:"values,omitempty"`
}

type Values struct {
	DeploymentsCount Count `json:"deploymentsCount,omitempty"`
	DaemonSetCount   Count `json:"daemonSetCount,omitempty"`
	StatefulSetCount Count `json:"statefulSetCount,omitempty"`
	JobCount         Count `json:"jobCount,omitempty"`
	CronJobCount     Count `json:"cronJobCount,omitempty"`
}

type Count struct {
	Active    int `json:"active"`
	Abnormal  int `json:"abnormal"`
	Succeeded int `json:"succeeded"`
	Failed    int `json:"failed"`
	Updating  int `json:"updating"`
	Stopped   int `json:"stopped"`
}

type Data struct {
	Option Option `json:"option"`
}

type Option struct {
	Tooltip Tooltip  `json:"tooltip,omitempty"`
	Color   []string `json:"color,omitempty"`
	Legend  Legend   `json:"legend,omitempty"`
	XAxis   Axis     `json:"xAxis,omitempty"`
	YAxis   Axis     `json:"yAxis,omitempty"`
	Series  []Series `json:"series,omitempty"`
}

type Tooltip struct {
	Show bool `json:"show"`
}

type AxisPointer struct {
	Type string `json:"type,omitempty"`
}

type Legend struct {
	Data []string `json:"data,omitempty"`
}

type Axis struct {
	Type string   `json:"type,omitempty"`
	Data []string `json:"data,omitempty"`
}

type Series struct {
	Name     string `json:"name,omitempty"`
	Type     string `json:"type,omitempty"`
	BarGap   string `json:"barGap,omitempty"`
	BarWidth int    `json:"barWidth,omitempty"`
	Data     []*int `json:"data,omitempty"`
}
