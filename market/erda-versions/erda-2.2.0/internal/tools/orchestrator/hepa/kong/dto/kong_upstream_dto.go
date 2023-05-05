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

package dto

type HealthyDto struct {
	// 默认是0 active下关闭探测
	Interval int `json:"interval,omitempty"`
	// active默认是[200, 302] passive默认是2xx,3xx
	HttpStatuses []int `json:"http_statuses,omitempty"`
	// 默认是0
	Successes int `json:"successes,omitempty"`
}

type UnhealthyDto struct {
	// 默认是0 active下关闭探测
	Interval int `json:"interval,omitempty"`
	// active默认是[429, 404, 500, 501, 502, 503, 504, 505] passive默认是[429, 500, 503]
	HttpStatuses []int `json:"http_statuses,omitempty"`
	// 默认是0
	HttpFailures int `json:"http_failures,omitempty"`
	// 默认是0
	TcpFailures int `json:"tcp_failures,omitempty"`
	// 默认是0
	Timeouts int `json:"timeouts,omitempty"`
}

type ActiveHealthcheckDto struct {
	// 默认是1
	Timeout int `json:"timeout,omitempty"`
	// 默认是10
	Concurrency int          `json:"concurrency,omitempty"`
	HttpPath    string       `json:"http_path,omitempty"`
	Healthy     HealthyDto   `json:"healthy,omitempty"`
	Unhealthy   UnhealthyDto `json:"unhealthy,omitempty"`
}

type PassiveHealthcheckDto struct {
	Healthy   HealthyDto   `json:"healthy,omitempty"`
	Unhealthy UnhealthyDto `json:"unhealthy,omitempty"`
}

type HealthchecksDto struct {
	Active  ActiveHealthcheckDto  `json:"active,omitempty"`
	Passive PassiveHealthcheckDto `json:"passive,omitempty"`
}

type KongUpstreamDto struct {
	Id           string          `json:"id,omitempty"`
	Name         string          `json:"name"`
	Healthchecks HealthchecksDto `json:"healthchecks"`
}

func NewHealthchecks(checkPath string) HealthchecksDto {
	return HealthchecksDto{
		Active: ActiveHealthcheckDto{
			Timeout:     1,
			Concurrency: 3,
			HttpPath:    checkPath,
			Healthy: HealthyDto{
				Interval:  3,
				Successes: 1,
			},
			Unhealthy: UnhealthyDto{
				Interval:     3,
				HttpFailures: 3,
				TcpFailures:  3,
				Timeouts:     3,
			},
		},
		Passive: PassiveHealthcheckDto{
			Unhealthy: UnhealthyDto{
				HttpFailures: 0,
				TcpFailures:  0,
				Timeouts:     0,
			},
			Healthy: HealthyDto{
				Successes: 1,
			},
		},
	}
}
