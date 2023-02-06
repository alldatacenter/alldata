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

package taskerror

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestPipelineTaskErrCtx_CalculateFrequencyPerHour(t *testing.T) {
	type fields struct {
		StartTime time.Time
		EndTime   time.Time
		Count     uint64
	}
	now := time.Now().Round(0)
	tests := []struct {
		name   string
		fields fields
		want   uint64
	}{
		{
			name: "no start time",
			fields: fields{
				StartTime: time.Time{},
				EndTime:   time.Time{},
				Count:     1,
			},
			want: 1,
		},
		{
			name: "no end time",
			fields: fields{
				StartTime: now,
				EndTime:   time.Time{},
				Count:     2,
			},
			want: 2,
		},
		{
			name: "less than one hour",
			fields: fields{
				StartTime: now,
				EndTime:   now.Add(time.Minute),
				Count:     3,
			},
			want: 3,
		},
		{
			name: "normal",
			fields: fields{
				StartTime: now,
				EndTime:   now.Add(2 * time.Hour),
				Count:     100,
			},
			want: uint64(float64(100 / 2)),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := &ErrorContext{
				StartTime: tt.fields.StartTime,
				EndTime:   tt.fields.EndTime,
				Count:     tt.fields.Count,
			}
			assert.Equalf(t, tt.want, c.CalculateFrequencyPerHour(), "CalculateFrequencyPerHour()")
		})
	}
}
