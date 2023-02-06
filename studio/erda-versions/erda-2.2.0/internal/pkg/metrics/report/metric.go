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

package report

import "time"

type Metric struct {
	Name      string                 `json:"name"`
	Timestamp int64                  `json:"timestamp"`
	Tags      map[string]string      `json:"tags"`
	Fields    map[string]interface{} `json:"fields"`
}

type BulkMetricRequest []*Metric

func CreateBulkMetricRequest() *BulkMetricRequest {
	metricRequest := make(BulkMetricRequest, 0)
	return &metricRequest
}

func (b *BulkMetricRequest) Add(name string, tags map[string]string, fields map[string]interface{}) *BulkMetricRequest {
	*b = append(*b, &Metric{
		Name:      name,
		Timestamp: time.Now().UnixNano(),
		Tags:      tags,
		Fields:    fields,
	})
	return b
}

func (b *BulkMetricRequest) AddWithTime(name string, tags map[string]string, fields map[string]interface{}, timestamp time.Time) *BulkMetricRequest {
	*b = append(*b, &Metric{
		Name:      name,
		Timestamp: timestamp.UnixNano(),
		Tags:      tags,
		Fields:    fields,
	})
	return b
}
