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

package endpoints

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cap"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cluster"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/clusterinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/instanceinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/job"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/labelmanager"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/resourceinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/servicegroup"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/volume"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func TestHTTPEndpoints_ClusterHook(t *testing.T) {
	type fields struct {
		volumeImpl        volume.Volume
		serviceGroupImpl  servicegroup.ServiceGroup
		clusterImpl       cluster.Cluster
		job               job.Job
		labelManager      labelmanager.LabelManager
		instanceinfoImpl  instanceinfo.InstanceInfo
		clusterinfoImpl   clusterinfo.ClusterInfo
		componentinfoImpl instanceinfo.ComponentInfo
		resourceinfoImpl  resourceinfo.ResourceInfo
		Cap               cap.Cap
	}
	type args struct {
		ctx  context.Context
		r    *http.Request
		vars map[string]string
	}

	clusterImpl := &cluster.ClusterImpl{}

	req1 := apistructs.ClusterEvent{}
	reqStr1, _ := json.Marshal(req1)
	httpReq1, _ := http.NewRequest(http.MethodPost, "/clusterhook", bytes.NewBuffer(reqStr1))
	httpReq2, _ := http.NewRequest(http.MethodPost, "/clusterhook", bytes.NewBuffer(reqStr1))

	tests := []struct {
		name    string
		fields  fields
		args    args
		want    httpserver.Responser
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name: "Test_01",
			fields: fields{
				clusterImpl: clusterImpl,
			},
			args: args{
				r: httpReq1,
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			fields: fields{
				clusterImpl: clusterImpl,
			},
			args: args{
				r: httpReq2,
			},
			want: httpserver.HTTPResponse{
				Status:  http.StatusInternalServerError,
				Content: "failed to handle cluster event: failed",
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			h := &HTTPEndpoints{
				volumeImpl:        tt.fields.volumeImpl,
				ServiceGroupImpl:  tt.fields.serviceGroupImpl,
				clusterImpl:       tt.fields.clusterImpl,
				Job:               tt.fields.job,
				labelManager:      tt.fields.labelManager,
				instanceinfoImpl:  tt.fields.instanceinfoImpl,
				ClusterinfoImpl:   tt.fields.clusterinfoImpl,
				componentinfoImpl: tt.fields.componentinfoImpl,
				resourceinfoImpl:  tt.fields.resourceinfoImpl,
				Cap:               tt.fields.Cap,
			}

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(tt.fields.clusterImpl), "Hook", func(_ *cluster.ClusterImpl, event *apistructs.ClusterEvent) error {
				if tt.name == "Test_01" {
					return nil
				}

				return errors.Errorf("failed")
			})
			defer patch1.Unpatch()

			got, err := h.ClusterHook(tt.args.ctx, tt.args.r, tt.args.vars)
			if (err != nil) != tt.wantErr {
				t.Errorf("ClusterHook() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ClusterHook() got = %v, want %v", got, tt.want)
			}
		})
	}
}
