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

package query

// 单点数据，对应API: {{scope}}?...
type Point struct {
	// Title string
	Name string
	Data []*PointData
}

type PointData struct {
	Name      string      `mapstructure:"name"`
	AggMethod string      `mapstructure:"agg"`
	Data      interface{} `mapstructure:"data"`
}

// 时序数据, 对应API：{{scope}}/histogram?...
type Series struct {
	Name       string
	Data       []*SeriesData
	TimeSeries []int // 毫秒
}

type SeriesData struct {
	Name      string    `mapstructure:"name"`
	AggMethod string    `mapstructure:"agg"`
	Data      []float64 `mapstructure:"data"`
	Tag       string    `mapstructure:"tag"`
}
