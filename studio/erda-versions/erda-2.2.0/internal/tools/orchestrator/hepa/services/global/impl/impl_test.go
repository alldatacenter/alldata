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

package impl

import (
	"context"
	"reflect"
	"testing"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/endpoint_api"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/global"
)

func Test_encodeTenantGroup(t *testing.T) {
	type args struct {
		projectId      string
		env            string
		clusterName    string
		tenantGroupKey string
	}
	tests := []struct {
		name string
		args args
	}{
		{"case1", args{projectId: "1", env: "DEV", clusterName: "dev", tenantGroupKey: "dev"}},
		{"case2", args{projectId: "2", env: "TEST", clusterName: "test", tenantGroupKey: "test"}},
		{"case3", args{projectId: "", env: "", clusterName: "", tenantGroupKey: ""}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := encodeTenantGroup(tt.args.projectId, tt.args.env, tt.args.clusterName, tt.args.tenantGroupKey)
			if got == "" {
				t.Errorf("encodeTenantGroup() = %v", got)
			}
		})
	}
}

func TestGatewayGlobalServiceImpl_Clone(t *testing.T) {
	type fields struct {
		azDb       service.GatewayAzInfoService
		kongDb     service.GatewayKongInfoService
		packageBiz *endpoint_api.GatewayOpenapiService
		reqCtx     context.Context
	}
	type args struct {
		ctx context.Context
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   global.GatewayGlobalService
	}{
		{"case1", fields{azDb: nil, kongDb: nil, packageBiz: nil, reqCtx: nil}, args{}, &GatewayGlobalServiceImpl{}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			impl := GatewayGlobalServiceImpl{
				azDb:       tt.fields.azDb,
				kongDb:     tt.fields.kongDb,
				packageBiz: tt.fields.packageBiz,
				reqCtx:     tt.fields.reqCtx,
			}
			if got := impl.Clone(tt.args.ctx); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Clone() = %v, want %v", got, tt.want)
			}
		})
	}
}
