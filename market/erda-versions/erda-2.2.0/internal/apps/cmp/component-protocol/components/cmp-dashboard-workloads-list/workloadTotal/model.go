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

type ComponentWorkloadTotal struct {
	Type  string `json:"type,omitempty"`
	Data  Data   `json:"data"`
	State State  `json:"state,omitempty"`
}

type Data struct {
	Data DataInData `json:"data"`
}

type DataInData struct {
	Main string `json:"main,omitempty"`
	Desc string `json:"desc,omitempty"`
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
