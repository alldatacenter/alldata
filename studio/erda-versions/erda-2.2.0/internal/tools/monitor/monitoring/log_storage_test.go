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

// import (
// 	"reflect"
// 	"testing"

// 	"github.com/erda-project/erda/internal/tools/monitor/core/log/schema"
// 	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/metricq"
// )

// func Test_cassandraStorageLog_calculateUsage(t *testing.T) {
// 	type fields struct {
// 		metricQ metricq.Queryer
// 	}
// 	type args struct {
// 		orgMap map[string]string
// 		data   []*keyspaceUsage
// 	}
// 	tests := []struct {
// 		name   string
// 		fields fields
// 		args   args
// 		want   map[string]uint64
// 	}{
// 		{
// 			name: "normal",
// 			args: args{
// 				orgMap: map[string]string{
// 					schema.KeyspaceWithOrgName("org-1"): "org-1",
// 					"xxx":                               "xxx",
// 				},
// 				data: []*keyspaceUsage{
// 					{
// 						keyspace:   "spot_org_1",
// 						address:    "",
// 						usageBytes: 1024,
// 					},
// 				},
// 			},
// 			want: map[string]uint64{
// 				"org-1": 1024,
// 			},
// 		},
// 	}
// 	for _, tt := range tests {
// 		t.Run(tt.name, func(t *testing.T) {
// 			c := &cassandraStorageLog{
// 				metricQ: tt.fields.metricQ,
// 			}
// 			if got := c.calculateUsage(tt.args.orgMap, tt.args.data); !reflect.DeepEqual(got, tt.want) {
// 				t.Errorf("calculateUsage() = %v, want %v", got, tt.want)
// 			}
// 		})
// 	}
// }
