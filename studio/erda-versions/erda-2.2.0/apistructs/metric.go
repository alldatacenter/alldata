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

package apistructs

// HostMetricResponse 主机监控资源响应
type HostMetricResponse struct {
	Header
	Data HostMetricResponseData `json:"data"`
}

// HostMetricResponseData 主机监控资源响应数据结构
/*
"data": {
	"results": [{
		"data": [{
			"avg.load5": {
				"agg": "avg",
				"axisIndex": 0,
				"chartType": "",
				"data": 0.45545454545454545,
				"name": "load5",
				"tag": "10.168.0.101",
				"unit": "",
				"unitType": ""
			}
         }],
		"name": "system"
	}]
}
*/
type HostMetricResponseData struct {
	Results []HostMetricResult `json:"results"`
}

// HostMetricResult result结构
type HostMetricResult struct {
	Name string                  `json:"name"`
	Data []map[string]MetricData `json:"data"`
}

// MetricData metric结构
type MetricData struct {
	Tag  string  `json:"tag"` // host ip
	Name string  `json:"name"`
	Data float64 `json:"data"`
	Agg  string  `json:"agg"`
}

// HostMetric host metric bundle数据结构
type HostMetric struct {
	CPU    float64 // 百分比值， eg: 19%, 则cpu为19
	Memory float64 // 百分比
	Disk   float64 // 百分比
	Load   float64
}
