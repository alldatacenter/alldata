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

package monitoring

import (
	"net/url"
	"reflect"
	"testing"
	"time"

	"github.com/olivere/elastic"
)

func Test_getMetricName(t *testing.T) {
	type args struct {
		index string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "normal",
			args: args{index: "spot-application_http-full_cluster-r-000001"},
			want: "application_http",
		},
		{
			name: "non metric",
			args: args{index: "spot-empty"},
			want: "",
		},
		{
			name: "non metric",
			args: args{index: "xxxx"},
			want: "",
		},
		{
			name: "non metric",
			args: args{index: "spot-ta_event-full_cluster-r-000001"},
			want: "ta_event",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getMetricName(tt.args.index); got != tt.want {
				t.Errorf("getMetricName() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_timeRange(t *testing.T) {
	type args struct {
		start time.Time
		end   time.Time
	}
	tests := []struct {
		name string
		args args
		want url.Values
	}{
		{
			name: "",
			args: args{
				start: time.Date(2021, 8, 30, 0, 0, 0, 0, time.UTC),
				end:   time.Date(2021, 9, 30, 0, 0, 0, 0, time.UTC),
			},
			want: url.Values{
				"start": []string{"1630281600000"},
				"end":   []string{"1632960000000"},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := timeRange(tt.args.start, tt.args.end); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("timeRange() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_getMetricIdxMap(t *testing.T) {
	type args struct {
		resp elastic.CatIndicesResponse
	}
	tests := []struct {
		name string
		args args
		want map[string]*metricIndex
	}{
		{
			name: "",
			args: args{
				resp: elastic.CatIndicesResponse([]elastic.CatIndicesResponseRow{
					elastic.CatIndicesResponseRow{
						StoreSize: "2048",
						DocsCount: 1024,
						Index:     "spot-hello_world-full_cluster-r000001",
					},
				}),
			},
			want: map[string]*metricIndex{
				"hello_world": &metricIndex{
					sizeBytes: 2048,
					docCount:  1024,
					indices:   []string{"spot-hello_world-full_cluster-r000001"},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getMetricIdxMap(tt.args.resp); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getMetricIdxMap() = %v, want %v", got, tt.want)
			}
		})
	}
}
