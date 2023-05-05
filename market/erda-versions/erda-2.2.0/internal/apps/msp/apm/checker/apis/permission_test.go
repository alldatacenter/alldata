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

package apis

import (
	"context"
	"errors"
	"fmt"
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	projectpb "github.com/erda-project/erda-proto-go/msp/tenant/project/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/storage/db"
)

func Test_checkerV1Service_GetProjectFromMetricID(t *testing.T) {
	type fields struct {
		p             *provider
		projectDB     *db.ProjectDB
		metricDB      *db.MetricDB
		metricq       pb.MetricServiceServer
		projectServer projectpb.ProjectServiceServer
	}
	tests := []struct {
		name   string
		fields fields
		want   func(ctx context.Context, req interface{}) (string, error)
	}{
		{"case1", fields{p: nil, projectDB: nil, metricDB: nil, metricq: nil, projectServer: nil}, nil},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &checkerV1Service{
				p:             tt.fields.p,
				projectDB:     tt.fields.projectDB,
				metricDB:      tt.fields.metricDB,
				metricq:       tt.fields.metricq,
				projectServer: tt.fields.projectServer,
			}
			monkey.UnpatchAll()
			monkey.Patch(GetProjectFromMetricID, func(metricDB *db.MetricDB, projectDB *db.ProjectDB, projectService projectpb.ProjectServiceServer) func(ctx context.Context, req interface{}) (string, error) {
				return nil
			})
			if got := s.GetProjectFromMetricID(); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetProjectFromMetricID()")
			}
		})
	}
}

func TestGetMetric(t *testing.T) {
	type args struct {
		mid string
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{mid: "1"}, true},
		{"case2", args{mid: "-1"}, true},
		{"case3", args{mid: "xxx"}, true},
		{"case4", args{mid: "2"}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			monkey.UnpatchAll()
			var mdb *db.MetricDB
			monkey.PatchInstanceMethod(reflect.TypeOf(mdb), "GetByID",
				func(mdb *db.MetricDB, id int64) (*db.Metric, error) {
					if id == -1 {
						return nil, errors.New("error")
					}
					if id == 2 {
						return &db.Metric{ID: id, ProjectID: id, Extra: "xxx"}, nil
					}
					if id == 3 {
						return &db.Metric{ID: id, ProjectID: id, Extra: fmt.Sprintf("%v", id)}, nil
					}

					return nil, nil
				},
			)
			_, err := GetMetric(tt.args.mid, nil)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetMetric() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}
