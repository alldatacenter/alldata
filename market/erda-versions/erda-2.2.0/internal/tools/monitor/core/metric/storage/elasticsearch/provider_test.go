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

package elasticsearch

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
)

func Test_processInvalidFields(t *testing.T) {
	type args struct {
		m *metric.Metric
	}
	tests := []struct {
		name     string
		args     args
		want     *metric.Metric
		wantJSON []byte
	}{
		{
			name: "invalid",
			args: args{m: &metric.Metric{
				Name:      "elasticsearch_indices",
				Timestamp: 1635228030000000000,
				Tags:      map[string]string{},
				Fields: map[string]interface{}{
					"segments_max_unsafe_auto_id_timestamp": float64(-9223372036854776000),
				},
			}},
			want: &metric.Metric{
				Name:      "elasticsearch_indices",
				Timestamp: 1635228030000000000,
				Tags:      map[string]string{},
				Fields: map[string]interface{}{
					"segments_max_unsafe_auto_id_timestamp": toNumber(-9223372036854776000),
				},
			},
			wantJSON: []byte("{\"name\":\"elasticsearch_indices\",\"timestamp\":1635228030000000000,\"tags\":{},\"fields\":{\"segments_max_unsafe_auto_id_timestamp\":-9223372036854775808.0}}"),
		},
		{
			name: "valid",
			args: args{m: &metric.Metric{
				Name:      "elasticsearch_indices",
				Timestamp: 1635228030000000000,
				Tags:      map[string]string{},
				Fields: map[string]interface{}{
					"segments_max_unsafe_auto_id_timestamp": float64(-1),
				},
			}},
			want: &metric.Metric{
				Name:      "elasticsearch_indices",
				Timestamp: 1635228030000000000,
				Tags:      map[string]string{},
				Fields: map[string]interface{}{
					"segments_max_unsafe_auto_id_timestamp": float64(-1),
				},
			},
			wantJSON: []byte("{\"name\":\"elasticsearch_indices\",\"timestamp\":1635228030000000000,\"tags\":{},\"fields\":{\"segments_max_unsafe_auto_id_timestamp\":-1}}"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			processInvalidFields(tt.args.m)
			assert.Equal(t, tt.want, tt.args.m)
			buf, err := json.Marshal(tt.args.m)
			assert.Nil(t, err)
			assert.Equal(t, tt.wantJSON, buf)
		})
	}
}
