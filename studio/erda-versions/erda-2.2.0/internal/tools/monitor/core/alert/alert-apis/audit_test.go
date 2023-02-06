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
	"testing"

	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

func TestAuditOperateOrgAlert(t *testing.T) {
	tests := []struct {
		name        string
		req         interface{}
		res         interface{}
		wantEntity  map[string]interface{}
		wantScopeId string
		wantErr     bool
	}{
		{
			name:        "action enabled",
			req:         &pb.UpdateOrgAlertEnableRequest{Enable: true, Id: 111},
			res:         nil,
			wantEntity:  map[string]interface{}{"alertID": int64(111), "alertName": "test-alert-name", "orgName": "test-org", "action": "enabled"},
			wantScopeId: "123",
			wantErr:     false,
		},
		{
			name:        "action disabled",
			req:         &pb.UpdateOrgAlertEnableRequest{Enable: false, Id: 111},
			res:         nil,
			wantEntity:  map[string]interface{}{"alertID": int64(111), "alertName": "test-alert-name", "orgName": "test-org", "action": "disabled"},
			wantScopeId: "123",
			wantErr:     false,
		},
	}
	service := &alertService{
		p: &provider{
			bdl: bundle.New(bundle.WithScheduler(), bundle.WithCoreServices()),
		},
	}

	getOrg = func(m *alertService, idOrName interface{}) (*apistructs.OrgDTO, error) {
		return &apistructs.OrgDTO{
			ID:   123,
			Name: "test-org",
		}, nil
	}

	getAlert = func(m *alertService, ctx context.Context, req *pb.GetAlertRequest) (*pb.GetAlertResponse, error) {
		return &pb.GetAlertResponse{
			Data: &pb.Alert{
				Name: "test-alert-name",
			},
		}, nil
	}

	for _, i := range tests {
		t.Run(i.name, func(t *testing.T) {
			fn := service.auditOperateOrgAlert("")
			scopeID, entity, err := fn(transport.WithHeader(context.Background(), metadata.New(map[string]string{"org-id": i.wantScopeId})), i.req, i.res, nil)
			if err != nil {
				if !i.wantErr {
					t.Errorf("test on error: %v", err)
				}
			}
			require.Equal(t, i.wantScopeId, scopeID)
			for wantK, wantV := range i.wantEntity {
				if v, ok := entity[wantK]; ok {
					require.Equal(t, wantV, v)
				} else {
					t.Errorf("result entity should be value")
				}
			}
		})
	}
}
func TestAuditOperateOrgCustomAlert(t *testing.T) {
	tests := []struct {
		name        string
		req         interface{}
		res         interface{}
		wantEntity  map[string]interface{}
		wantScopeId string
		wantErr     bool
	}{
		{
			name:        "action enabled",
			req:         &pb.UpdateOrgCustomizeAlertEnableRequest{Enable: true, Id: 111},
			res:         nil,
			wantEntity:  map[string]interface{}{"alertID": uint64(111), "alertName": "test-customer-alert", "orgName": "test-org", "action": "enabled"},
			wantScopeId: "123",
			wantErr:     false,
		},
		{
			name:        "action disabled",
			req:         &pb.UpdateOrgCustomizeAlertEnableRequest{Enable: false, Id: 111},
			res:         nil,
			wantEntity:  map[string]interface{}{"alertID": uint64(111), "alertName": "test-customer-alert", "orgName": "test-org", "action": "disabled"},
			wantScopeId: "123",
			wantErr:     false,
		},
	}
	service := &alertService{
		p: &provider{
			bdl: bundle.New(bundle.WithScheduler(), bundle.WithCoreServices()),
		},
	}

	getOrg = func(m *alertService, idOrName interface{}) (*apistructs.OrgDTO, error) {
		return &apistructs.OrgDTO{
			ID:   123,
			Name: "test-org",
		}, nil
	}

	getGetCustomizeAlert = func(m *alertService, ctx context.Context, request *pb.GetCustomizeAlertRequest) (*pb.GetCustomizeAlertResponse, error) {
		return &pb.GetCustomizeAlertResponse{
			Data: &pb.CustomizeAlertDetail{
				Name: "test-customer-alert",
			},
		}, nil
	}

	for _, i := range tests {
		t.Run(i.name, func(t *testing.T) {
			fn := service.auditOperateOrgCustomAlert(apistructs.SwitchOrgCustomAlert, "")
			scopeID, entity, err := fn(transport.WithHeader(context.Background(), metadata.New(map[string]string{"org-id": i.wantScopeId})), i.req, i.res, nil)
			if err != nil {
				if !i.wantErr {
					t.Fatalf("test on error: %v", err)
				}
			}
			require.Equal(t, i.wantScopeId, scopeID)
			for wantK, wantV := range i.wantEntity {
				if v, ok := entity[wantK]; ok {
					require.Equal(t, wantV, v)
				} else {
					t.Fatalf("result entity should be value")
				}
			}
		})
	}

}
