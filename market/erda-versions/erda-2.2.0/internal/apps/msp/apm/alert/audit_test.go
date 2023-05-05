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

package alert

import (
	"context"
	"strconv"
	"testing"

	"github.com/stretchr/testify/require"

	monitor "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/alert/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

const (
	mockProjectId   = uint64(111)
	mockProjectName = "project-name"
)

func mockProject() {
	getTenantGroupDetails = func(a *alertService, tenantGroup string) (*apistructs.TenantGroupDetails, error) {
		return &apistructs.TenantGroupDetails{
			ProjectID: strconv.FormatUint(mockProjectId, 10),
		}, nil
	}
	getProject = func(a *alertService, id uint64) (*apistructs.ProjectDTO, error) {
		return &apistructs.ProjectDTO{
			Name: mockProjectName,
		}, nil
	}
}
func TestAuditOperateMicroserviceAlert(t *testing.T) {
	tests := []struct {
		name         string
		req          interface{}
		res          interface{}
		templateName apistructs.TemplateName
		action       string
		wantEntity   map[string]interface{}
		wantScopeId  interface{}
		wantErr      bool
	}{
		{
			name:         "UpdateMicroserviceAlert-update",
			req:          &pb.UpdateAlertRequest{Enable: true, Id: 111},
			templateName: apistructs.UpdateMicroserviceAlert,
			action:       "update",
			wantEntity:   map[string]interface{}{"projectId": mockProjectId, "projectName": mockProjectName, "alertName": "test-alert", "action": "update"},
			wantScopeId:  mockProjectId,
			wantErr:      false,
		},
		{
			name:         "DeleteMicroserviceAlert-delete",
			templateName: apistructs.DeleteMicroserviceAlert,
			action:       "delete",
			req:          &pb.DeleteAlertRequest{Id: 111},
			res: &pb.DeleteAlertResponse{Data: &pb.DeleteAlertData{
				Name: "delete-name",
			}},
			wantEntity:  map[string]interface{}{"projectId": mockProjectId, "projectName": mockProjectName, "alertName": "delete-name", "action": "delete"},
			wantScopeId: mockProjectId,
			wantErr:     false,
		}, {
			name:         "SwitchMicroserviceAlert",
			templateName: apistructs.SwitchMicroserviceAlert,
			action:       "",
			req:          &pb.UpdateAlertEnableRequest{Enable: false, Id: 111},
			wantEntity:   map[string]interface{}{"projectId": mockProjectId, "projectName": mockProjectName, "alertName": "test-alert", "action": "disabled"},
			wantScopeId:  mockProjectId,
			wantErr:      false,
		},
	}
	service := &alertService{
		p: &provider{
			bdl: bundle.New(bundle.WithScheduler(), bundle.WithCoreServices()),
		},
	}
	mockProject()
	queryAlert = func(a *alertService, ctx context.Context, request *monitor.QueryAlertRequest) (*monitor.QueryAlertsResponse, error) {
		return &monitor.QueryAlertsResponse{
			Data: &monitor.QueryAlertsData{
				List: []*monitor.Alert{
					&monitor.Alert{
						Name: "test-alert",
					},
				},
			},
		}, nil
	}
	for _, i := range tests {
		t.Run(i.name, func(t *testing.T) {
			fn := service.auditOperateMicroserviceAlert(i.templateName, i.action)
			scopeID, entity, err := fn(context.Background(), i.req, i.res, nil)
			if err != nil {
				if !i.wantErr {
					t.Fatalf("test on error: %v", err)
				}
			}
			require.Equal(t, i.wantScopeId, scopeID, "scope id check error")
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

func TestAuditOperateMicroserviceCustomAlert(t *testing.T) {
	tests := []struct {
		name         string
		req          interface{}
		res          interface{}
		templateName apistructs.TemplateName
		action       string
		wantEntity   map[string]interface{}
		wantScopeId  interface{}
		wantErr      bool
	}{
		{
			name:         "UpdateMicroserviceAlert-update",
			req:          &pb.UpdateCustomizeAlertRequest{Enable: true, Id: 111},
			templateName: apistructs.UpdateMicroserviceCustomAlert,
			action:       "update",
			wantEntity:   map[string]interface{}{"projectId": mockProjectId, "projectName": mockProjectName, "alertName": "customize-alert", "action": "update"},
			wantScopeId:  mockProjectId,
			wantErr:      false,
		},
		{
			name:         "DeleteMicroserviceAlert-delete",
			templateName: apistructs.DeleteMicroserviceCustomAlert,
			action:       "delete",
			req:          &pb.DeleteCustomizeAlertRequest{Id: 111},
			res: &pb.DeleteCustomizeAlertResponse{Data: &pb.DeleteCustomizeAlertData{
				Name: "delete-name",
			}},
			wantEntity:  map[string]interface{}{"projectId": mockProjectId, "projectName": mockProjectName, "alertName": "delete-name", "action": "delete"},
			wantScopeId: mockProjectId,
			wantErr:     false,
		}, {
			name:         "SwitchMicroserviceAlert",
			templateName: apistructs.SwitchMicroserviceCustomAlert,
			action:       "",
			req:          &pb.UpdateCustomizeAlertEnableRequest{Enable: false, Id: 111},
			wantEntity:   map[string]interface{}{"projectId": mockProjectId, "projectName": mockProjectName, "alertName": "customize-alert", "action": "disabled"},
			wantScopeId:  mockProjectId,
			wantErr:      false,
		},
	}
	service := &alertService{
		p: &provider{
			bdl: bundle.New(bundle.WithScheduler(), bundle.WithCoreServices()),
		},
	}
	mockProject()
	queryCustomizeAlert = func(a *alertService, ctx context.Context, request *monitor.QueryCustomizeAlertRequest) (*monitor.QueryCustomizeAlertResponse, error) {
		return &monitor.QueryCustomizeAlertResponse{
			Data: &monitor.QueryCustomizeAlertData{
				List: []*monitor.CustomizeAlertOverview{
					&monitor.CustomizeAlertOverview{
						Name: "customize-alert",
					},
				},
			},
		}, nil
	}
	for _, i := range tests {
		t.Run(i.name, func(t *testing.T) {
			fn := service.auditOperateMicroserviceCustomAlert(i.templateName, i.action)
			scopeID, entity, err := fn(context.Background(), i.req, i.res, nil)
			if err != nil {
				if !i.wantErr {
					t.Fatalf("test on error: %v", err)
				}
			}
			require.Equal(t, i.wantScopeId, scopeID, "scope id check error")
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
