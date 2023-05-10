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

package orgapis

import (
	"testing"
)

func Test_calculateStatus(t *testing.T) {
	type args struct {
		raws []rawStatus
		name string
	}
	tests := []struct {
		name string
		args args
		want uint8
	}{
		{"status-0", args{
			raws: []rawStatus{
				{
					HealthStatus: 0,
					Weight:       0,
				},
				{
					HealthStatus: 0,
					Weight:       1,
				},
			},
			name: "kubernetes",
		}, 0},
		{"status-1", args{
			raws: []rawStatus{
				{
					HealthStatus: 1,
					Weight:       0,
				},
				{
					HealthStatus: 1,
					Weight:       1,
				},
			},
			name: "kubernetes",
		}, 1},
		{"status-2", args{
			raws: []rawStatus{
				{
					HealthStatus: 0,
					Weight:       0,
				},
				{
					HealthStatus: 1,
					Weight:       0,
				},
				{
					HealthStatus: 2,
					Weight:       0,
				},
			},
			name: "kubernetes",
		}, 1},
		{"status-2", args{
			raws: []rawStatus{
				{
					HealthStatus: 0,
					Weight:       0,
				},
				{
					HealthStatus: 1,
					Weight:       1,
				},
				{
					HealthStatus: 2,
					Weight:       1,
				},
			},
			name: "kubernetes",
		}, 2},
		{"status-3", args{
			raws: []rawStatus{
				{
					HealthStatus: 0,
					Weight:       0,
				},
				{
					HealthStatus: 1,
					Weight:       0,
				},
				{
					HealthStatus: 2,
					Weight:       1,
				},
			},
			name: "machine",
		}, 3},
		{"status-3", args{
			raws: []rawStatus{
				{
					HealthStatus: 2,
					Weight:       0,
				},
				{
					HealthStatus: 1,
					Weight:       0,
				},
				{
					HealthStatus: 2,
					Weight:       0,
				},
			},
			name: "machine",
		}, 2},
		{"status-3", args{
			raws: []rawStatus{
				{
					HealthStatus: 2,
					Weight:       1,
				},
				{
					HealthStatus: 2,
					Weight:       1,
				},
				{
					HealthStatus: 2,
					Weight:       1,
				},
			},
			name: "kubernetes",
		}, 3},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := calculateStatus(tt.args.raws, tt.args.name); got != tt.want {
				t.Errorf("calculateStatus() = %v, want %v", got, tt.want)
			}
		})
	}
}
