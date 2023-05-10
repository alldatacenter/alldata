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
	"fmt"
	"testing"

	"bou.ke/monkey"
	"github.com/golang/mock/gomock"
	"github.com/jinzhu/gorm"
	"google.golang.org/protobuf/types/known/structpb"

	monitor "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/alert/pb"
	tenant "github.com/erda-project/erda-proto-go/msp/tenant/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
)

////go:generate mockgen -destination=./alert_register_test.go -package alert github.com/erda-project/erda-infra/pkg/transport Register
////go:generate mockgen -destination=./alert_monitor_test.go -package alert github.com/erda-project/erda-proto-go/core/monitor/alert/pb AlertServiceServer
////go:generate mockgen -destination=./tenant_test.go -package alert github.com/erda-project/erda-proto-go/msp/tenant/pb TenantServiceServer
////go:generate mockgen -destination=./credential_context_test.go -package alert github.com/erda-project/erda-infra/base/servicehub Context

func Test_alertService_CreateCustomizeAlert(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	tenantService := NewMockTenantServiceServer(ctrl)
	monitorService := NewMockAlertServiceServer(ctrl)
	defer monkey.UnpatchAll()
	monkey.Patch(utils.NewContextWithHeader, func(ctx context.Context) context.Context {
		return context.Background()
	})
	monkey.Patch(checkCustomMetric, func(customMetrics *monitor.CustomizeMetrics, alert *monitor.CustomizeAlertDetail) error {
		return nil
	})
	monitorService.EXPECT().QueryCustomizeMetric(gomock.Any(), gomock.Any()).AnyTimes().Return(&monitor.QueryCustomizeMetricResponse{
		Data: &monitor.CustomizeMetrics{
			Metrics: []*monitor.MetricMeta{
				{
					Name: &monitor.DisplayKey{
						Key:     "erda",
						Display: "erda",
					},
					Fields: []*monitor.FieldMeta{
						{
							Field: &monitor.DisplayKey{
								Key:     "you key",
								Display: "erda",
							},
							DataType: "data",
						},
					},
					Tags: nil,
				},
			},
			FunctionOperators: []*monitor.Operator{
				{
					Key:     "erda",
					Display: "erda",
					Type:    "test",
				},
			},
			FilterOperators: []*monitor.Operator{
				{
					Key:     "erda",
					Display: "erda",
					Type:    "test",
				},
			},
			Aggregator: []*monitor.DisplayKey{
				{
					Key:     "erda",
					Display: "display",
				},
			},
			NotifySample: "",
		},
	}, nil)
	tenantService.EXPECT().GetTenantProject(gomock.Any(), gomock.Any()).AnyTimes().Return(&tenant.GetTenantProjectResponse{
		Data: &tenant.TenantProjectData{
			Workspace: "PROD",
			ProjectId: "98",
		},
	}, nil)
	monitorService.EXPECT().CreateCustomizeAlert(gomock.Any(), gomock.Any()).AnyTimes().Return(&monitor.CreateCustomizeAlertResponse{
		Data: 18,
	}, nil)
	pro := &provider{
		C:                      &config{},
		DB:                     &gorm.DB{},
		Register:               nil,
		Perm:                   nil,
		MPerm:                  nil,
		alertService:           &alertService{},
		Monitor:                monitorService,
		authDb:                 nil,
		mspDb:                  nil,
		bdl:                    &bundle.Bundle{},
		Tenant:                 tenantService,
		microServiceFilterTags: nil,
	}
	pro.alertService.p = pro
	monkey.Patch((*alertService).getTKByTenant, func(_ *alertService, tenantGroup string) (string, error) {
		return "e49b5fe96c144069c24f029f2df6559f", nil
	})
	_, err := pro.alertService.CreateCustomizeAlert(context.Background(), &pb.CreateCustomizeAlertRequest{
		Name: "erda_RRdddeeeeesss",
		Rules: []*monitor.CustomizeAlertRule{
			{
				Metric: "status_page",
				Window: 1,
				Functions: []*monitor.CustomizeAlertRuleFunction{
					{
						Field:      "code",
						Alias:      "dds",
						Aggregator: "max",
						Operator:   "eq",
						Value:      nil,
						DataType:   "",
						Unit:       "",
					},
				},
				Filters:             nil,
				Group:               nil,
				Outputs:             nil,
				Select:              nil,
				Attributes:          nil,
				ActivedMetricGroups: nil,
				CreateTime:          0,
				UpdateTime:          0,
			},
		},
		Notifies: nil,
	})
	if err != nil {
		fmt.Println("should not err")
	}
}

func Test_alertService_UpdateCustomizeAlert(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	tenantService := NewMockTenantServiceServer(ctrl)
	monitorService := NewMockAlertServiceServer(ctrl)
	defer monkey.UnpatchAll()
	monkey.Patch(utils.NewContextWithHeader, func(ctx context.Context) context.Context {
		return context.Background()
	})
	monkey.Patch(checkCustomMetric, func(customMetrics *monitor.CustomizeMetrics, alert *monitor.CustomizeAlertDetail) error {
		return nil
	})
	monkey.Patch((*provider).checkCustomizeAlert, func(_ *provider, _ *monitor.CustomizeAlertDetail) error {
		return nil
	})
	monkey.Patch((*alertService).getTKByTenant, func(_ *alertService, tenantGroup string) (string, error) {
		return "e49b5fe96c144069c24f029f2df6559f", nil
	})
	monitorService.EXPECT().GetCustomizeAlert(gomock.Any(), gomock.Any()).AnyTimes().Return(&monitor.GetCustomizeAlertResponse{
		Data: &monitor.CustomizeAlertDetail{
			AlertType:    "alert",
			AlertScope:   "micro_service",
			AlertScopeId: "e49b5fe96c144069c24f029f2df6559f",
			Enable:       true,
		},
	}, nil)
	monitorService.EXPECT().QueryCustomizeMetric(gomock.Any(), gomock.Any()).AnyTimes().Return(&monitor.QueryCustomizeMetricResponse{
		Data: &monitor.CustomizeMetrics{
			Metrics: []*monitor.MetricMeta{
				{
					Name: &monitor.DisplayKey{
						Key:     "erda",
						Display: "erda",
					},
					Fields: []*monitor.FieldMeta{
						{
							Field: &monitor.DisplayKey{
								Key:     "you key",
								Display: "erda",
							},
							DataType: "data",
						},
					},
					Tags: nil,
				},
			},
			FunctionOperators: []*monitor.Operator{
				{
					Key:     "erda",
					Display: "erda",
					Type:    "test",
				},
			},
			FilterOperators: []*monitor.Operator{
				{
					Key:     "erda",
					Display: "erda",
					Type:    "test",
				},
			},
			Aggregator: []*monitor.DisplayKey{
				{
					Key:     "erda",
					Display: "display",
				},
			},
			NotifySample: "",
		},
	}, nil)
	monitorService.EXPECT().UpdateCustomizeAlert(gomock.Any(), gomock.Any()).AnyTimes().Return(&monitor.UpdateCustomizeAlertResponse{}, nil)
	tenantService.EXPECT().GetTenantProject(gomock.Any(), gomock.Any()).AnyTimes().Return(&tenant.GetTenantProjectResponse{
		Data: &tenant.TenantProjectData{
			Workspace: "PROD",
			ProjectId: "98",
		},
	}, nil)
	pro := &provider{
		C:                      &config{},
		DB:                     &gorm.DB{},
		Register:               nil,
		Perm:                   nil,
		MPerm:                  nil,
		alertService:           &alertService{},
		Monitor:                monitorService,
		authDb:                 nil,
		mspDb:                  nil,
		bdl:                    &bundle.Bundle{},
		audit:                  nil,
		Tenant:                 tenantService,
		microServiceFilterTags: nil,
	}
	pro.alertService.p = pro
	_, err := pro.alertService.UpdateCustomizeAlert(context.Background(), &pb.UpdateCustomizeAlertRequest{
		TenantGroup: "e49b5fe96c144069c24f029f2df6559f",
		Name:        "erda_test",
		Rules: []*monitor.CustomizeAlertRule{
			{
				Id:     0,
				Name:   "",
				Metric: "status_page",
				Window: 0,
				Functions: []*monitor.CustomizeAlertRuleFunction{
					{
						Field:      "retry",
						Alias:      "retry",
						Aggregator: "value",
						Operator:   "gte",
						Value:      structpb.NewNumberValue(0),
						DataType:   "",
						Unit:       "",
					},
				},
				Filters: []*monitor.CustomizeAlertRuleFilter{
					{
						Tag:      "url",
						Operator: "eq",
						Value:    structpb.NewStringValue("www.baidu.com"),
						DataType: "string",
					},
				},
				Group:               []string{"metric"},
				ActivedMetricGroups: []string{"application_status"},
			},
		},
		Notifies: []*monitor.CustomizeAlertNotifyTemplates{
			{
				Title:   "【xxx异常】",
				Content: "sss",
			},
		},
	})
	if err != nil {
		fmt.Println("should not err")
	}
}

func Test_alertService_GetAlertConditions(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	monitorService := NewMockAlertServiceServer(ctrl)
	monitorService.EXPECT().GetAlertConditions(gomock.Any(), gomock.Any()).AnyTimes().Return(&monitor.GetAlertConditionsResponse{
		Data: []*monitor.Conditions{
			{
				Key:         "application_name",
				DisplayName: "应用",
			},
			{
				Key:         "service_name",
				DisplayName: "服务",
			},
		},
	}, nil)
	pro := &provider{
		C:                      &config{},
		DB:                     &gorm.DB{},
		Register:               nil,
		Perm:                   nil,
		MPerm:                  nil,
		alertService:           &alertService{},
		Monitor:                monitorService,
		authDb:                 nil,
		mspDb:                  nil,
		bdl:                    &bundle.Bundle{},
		microServiceFilterTags: nil,
	}
	pro.alertService.p = pro
	_, err := pro.alertService.GetAlertConditions(context.Background(), &pb.GetAlertConditionsRequest{
		ScopeType: "msp",
	})
	if err != nil {
		fmt.Println("should not err,err is ", err)
	}
}

func Test_alertService_GetAlertConditionsValue(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	monitorService := NewMockAlertServiceServer(ctrl)
	monitorService.EXPECT().GetAlertConditionsValue(gomock.Any(), gomock.Any()).AnyTimes().Return(&monitor.GetAlertConditionsValueResponse{
		Data: []*monitor.AlertConditionsValue{
			{
				Key: "application-name",
				Options: []*structpb.Value{
					structpb.NewStringValue("erda"),
				},
			},
		},
	}, nil)
	pro := &provider{
		C:                      &config{},
		DB:                     &gorm.DB{},
		Register:               nil,
		Perm:                   nil,
		MPerm:                  nil,
		alertService:           &alertService{},
		Monitor:                monitorService,
		authDb:                 nil,
		mspDb:                  nil,
		bdl:                    &bundle.Bundle{},
		microServiceFilterTags: nil,
	}
	pro.alertService.p = pro
	_, err := pro.alertService.GetAlertConditionsValue(context.Background(), &pb.GetAlertConditionsValueRequest{
		Conditions: []*monitor.ConditionsValueRequest{
			{
				Condition: "application-name",
				Filters: map[string]string{
					"org_name":     "erda",
					"project_id":   "12",
					"terminus_key": "c393550824b3d50aa758fee4593d6e31",
				},
				Index: "application_service_node",
			},
		},
	})
	if err != nil {
		fmt.Println("should not err,err is ", err)
	}
}
